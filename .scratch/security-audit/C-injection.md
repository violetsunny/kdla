# C — 注入与输入校验专项审计（OWASP A03 / A08 / 路径遍历）

- 仓库：`/Users/kangll13/aiot/java-code/self/gggggggood/kdla`，分支 `2.0.0-SNAPSHOT`，JDK 21，Spring Boot 3.4.0，MyBatis-Plus 3.5.12（spring-boot3-starter），EasyExcel 3.3.2。
- 范围：`kdla-component-dal`、`kdla-component-validator`、`kdla-component-supplement`（excel/fieldvaluefinder/CsvUtil）、`kdla-component-common`（JacksonUtil/ObjectUtil 及相关）。
- 方法：只读。逐文件读实际源码；MyBatis-Plus 分页语义用本机 `~/.m2` 中 `mybatis-plus-jsqlparser-3.5.12-sources.jar` 源码核实；JDK 临时文件语义用 `jdk-21.jdk/lib/src.zip` 中 `java.io.File` 源码核实。未改任何源码/pom，未跑 mvn，未做 git 操作。
- 分类口径：(a) 框架真实漏洞；(b) 下游预期覆盖的不安全默认；(c) 下游责任。破坏性 = 改校验/查询 API 行为影响下游；非破坏性 = 安全内部修复。

## 结论概览

| 严重度 | 计数 | 编号 |
|---|---|---|
| Critical | 0 | — |
| High | 1 | C-01 |
| Medium | 3 | C-02、C-03、C-04 |
| Low | 7 | C-05 ~ C-11 |

前序评审两个关键结论核实结果：**SqlFilter 白名单确实无绕过**（证据见"已核实安全项"）；**N-4 的 limit=0 不构成 DoS/全表扫描**（MP 3.5.12 中 size=0 生成 `LIMIT 0` 返回空页，size<0 才跳过分页，而负数已被框架代码拦截）。真正的分页 DoS 面在 **PlusPageQuery.pageSize 无上限**（C-02）。

---

## High

### C-01 ObjectUtil.byteToObject 无过滤 Java 原生反序列化（CWE-502，A08）

- 分类：(a) 框架真实漏洞（对外发布的不安全公共 API）。
- 位置：`kdla-component-common/src/main/java/top/kdla/framework/common/utils/ObjectUtil.java:30-45`

```java
public static Object byteToObject(byte[] bytes) {
    Object obj = null;
    try {
        // bytearray to object
        ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
        ObjectInputStream oi = new ObjectInputStream(bi);

        obj = oi.readObject();

        bi.close();
        oi.close();
    } catch (Exception e) {
        log.error("ObjectUtil byteToObject Exception", e);
    }
    return obj;
}
```

- 攻击场景：下游把攻击者可控字节（MQ 消息体、Redis 缓存值、HTTP 报文、文件内容）传入 `byteToObject`。`ObjectInputStream` 未设任何 `ObjectInputFilter`（JEP 290），无类白名单。classpath 存在可用 gadget 链时（c3p0、commons-configuration、hibernate、groovy 等下游常见库），构造恶意序列化流即可 RCE。
- exploitability：框架本身不接收外部输入，触发前提是下游误用 + gadget 库。但这是框架主动提供并鼓励使用的反序列化入口（配套 `ObjectToByte`），属于典型危险 sink。JDK 21 强封装使 `TemplatesImpl` 类经典链失效、根 pom 管理的 commons-collections4 4.4 与 commons-beanutils 1.11.0 也不构成经典链，但下游 classpath 不受框架控制，不能据此判安全。异常被吞掉返回 null 还使攻击探测静默化。
- 严重度：**High**。
- 修复方案：
  1. 新增带 `ObjectInputFilter` 白名单参数的重载（或按包名前缀的默认拒绝策略），原方法标 `@Deprecated` 并在 javadoc 指明仅限可信字节；
  2. 文档明确"禁止用于不可信输入"，推荐下游改用 JSON（JacksonUtil 已核实无多态类型风险）。
- 破坏性：直接给原方法加默认过滤 = **破坏性需裁定**（下游反序列化任意类的既有行为被改变）；新增过滤重载 + 废弃原方法 = **非破坏性**。

---

## Medium

### C-02 PlusPageQuery.pageSize 无上限，超大 LIMIT 致全表扫描 DoS（A03 相关资源耗尽）

- 分类：(b) 不安全默认（同库 IPageQuery 已有上限，PlusPageQuery 遗漏）。
- 位置：`kdla-component-dal/src/main/java/top/kdla/framework/infra/dal/mybatis/util/PlusPageQuery.java:69-77`（113-126 的同名重载同样问题）

```java
if (this.getPageNum() > 0) {
    curPage = this.getPageNum();
}
if (this.getPageSize() > 0) {
    limit = this.getPageSize();
}

//分页对象
Page<T> page = new Page<>(curPage, limit);
```

对照 `IPageQuery.java:22,31-34`：

```java
private static final long MAX_LIMIT = 500;
...
long limit = parsePagingValue(params.get(DoConstants.LIMIT), 10);
if (limit > MAX_LIMIT) {
    limit = MAX_LIMIT;
}
```

- 攻击场景：`PlusPageQuery` 经 `PageQuery` DTO 绑定 HTTP 查询参数（`PlusPageQuery(PageQuery)` 构造器，`kdla-component-model/.../dto/PageQuery.java` 的 orderBy/pageSize 无上限校验）。攻击者传 `pageSize=9223372036854775807` → `new Page<>(1, Long.MAX_VALUE)` → MP 生成 `LIMIT 9223372036854775807`，数据库全表扫描 + 驱动尝试拉取全量结果 → DB 负载打满 / 应用 OOM。MP 拦截器自身的 `maxLimit` 默认未配置（`PaginationInnerInterceptor.handlerLimit` 仅在配置了 maxLimit 时收敛），不能兜底。
- exploitability：下游把分页参数暴露给前端是这套 API 的预期用法，单个未认证/低权限请求即可触发，无需注入。
- 严重度：**Medium**（DoS，非数据泄露）。
- 修复方案：与 IPageQuery 对齐，给 PlusPageQuery 两个 `getPage` 重载加 `MAX_LIMIT` 收敛；上限值建议做成可配置（根 pom 约定版本集中，配置项可走 `kdla.config.*`），避免一刀切 500 伤害合法大批量导出场景。
- 破坏性：**破坏性需裁定**（改变查询 API 行为：原本合法的超大 pageSize 请求被收敛）。

### C-03 KdlaExcelReadListener 无行数上限，全量累积致 OOM DoS

- 分类：(b) 不安全默认（同包 ExcelReadListener 有 maxRow=10000，此监听器无任何限制 API）。
- 位置：`kdla-component-supplement/src/main/java/top/kdla/framework/supplement/excel/imp/KdlaExcelReadListener.java:34-38,60-68`

```java
private final List<T> dataList = new ArrayList<>();
...
private final List<E> resList = new ArrayList<>();
...
@Override
public void invoke(T t, AnalysisContext analysisContext) {
    if (t != null) {
        dataList.add(t);
        E e = checkData(t);
        if (e != null) {
            resList.add(e);
        }
    }
}
```

对照 `ExcelReadListener.java:32,164-166,301-305` 有 `DEFAULT_MAX_ROW = 1000_0` 与 `verifyMaxRow` 超限抛异常。

- 攻击场景：下游用 `KdlaExcelReadListener` 处理用户上传的 xlsx（该抽象类的预期用途）。攻击者构造百万行文件，`dataList`/`resList` 无界增长直至 OOM，拖垮整个宿主应用（不止当前请求）。
- exploitability：需要下游选用此监听器做面向用户的导入；无行数/文件大小任何防线。
- 严重度：**Medium**。
- 修复方案：加默认行数上限（对齐 ExcelReadListener 的 10000）并提供 `importRowLimit` 式覆盖入口；超限显式抛异常（失败可见）。
- 破坏性：**破坏性需裁定**（超大导入的既有行为被截断）。

### C-04 EasyExcel 3.3.2 携带 POI 4.1.2，受 CVE-2025-31672 影响（crafted OOXML DoS）

- 分类：(a) 依赖层漏洞，落在 excel 导入攻击面上。
- 位置：根 `pom.xml:77` `<easyexcel.version>3.3.2</easyexcel.version>`；`~/.m2/repository/com/alibaba/easyexcel-core/3.3.2/easyexcel-core-3.3.2.pom` 声明 `org.apache.poi:poi / poi-ooxml / poi-ooxml-schemas` 均为 **4.1.2**（compile scope，传递给下游）。
- 依据：CVE-2025-31672 —— Apache POI `poi-ooxml` < 5.4.0 对构造的 OOXML 文件输入校验不足，可致资源耗尽 DoS，5.4.0 修复。`ExcelReadListener.doRead(MultipartFile, ...)`（`ExcelReadListener.java:151-160`）与 `KdlaExcelReadListener` 的解析路径直接消费攻击者上传文件。
- 攻击场景：上传畸形 xlsx（异常压缩结构/超大声明尺寸），POI 解析阶段内存/ CPU 耗尽。与 C-03 叠加放大。
- exploitability：导入接口暴露即可触发；仅 DoS，无 RCE 证据。
- 严重度：**Medium**。
- 修复方案：评估升级 EasyExcel 4.x（POI 5.x 线）或显式将 POI 提升至 ≥5.4.0 并做导入回归；升级前下游可用上传文件大小限制缓解。
- 破坏性：**破坏性需裁定**（依赖大版本升级可能改变解析行为，且 2.x 根 pom 版本集中管理，需统一升）。

---

## Low

### C-05 O-1 核实：CommonValidator 急建 ValidatorFactory，@ConditionalOnClass 守 API 不守实现 → 特定 classpath 启动失败

- 分类：可靠性/可用性问题，**不是攻击者可利用的安全漏洞**（触发条件是下游 classpath 配置，攻击者不可控）。
- 位置：
  - `kdla-component-validator/src/main/java/top/kdla/framework/validator/CommonValidator.java:30-33`

```java
{
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
}
```

  - `kdla-component-validator/src/main/java/top/kdla/framework/validator/config/ValidatorAutoConfigure.java:10,18`

```java
import jakarta.validation.Validator;
...
@ConditionalOnClass(Validator.class)
```

  - 同型问题：`ValidatorUtils.java:21-23` 静态块 `Validation.buildDefaultValidatorFactory()`，类加载即触发，失败形态为 `ExceptionInInitializerError`。
- 核实结论：`@ConditionalOnClass` 守的是 `jakarta.validation.Validator`（API）。下游只有 validation-api、无 hibernate-validator 实现时条件成立，`new CommonValidator()` 实例初始化块抛 `jakarta.validation.NoProviderFoundException`，Spring 上下文启动失败。validator 模块 pom 中 `spring-boot-starter-validation` 为 provided（`kdla-component-validator/pom.xml:46-60`），实现完全依赖下游提供，该场景真实存在。自动配置已经 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册（2.x 生效），默认开启（`matchIfMissing = true`），放大触发面。
- 附带问题：每次 `new CommonValidator()` 新建一个 `ValidatorFactory` 且从不 close（`ValidatorFactory` 是 `Closeable`），多实例时资源泄漏。
- 严重度：**Low**。
- 修复方案：`@ConditionalOnClass` 改守实现类名（如 `name = "org.hibernate.validator.HibernateValidator"`）或改懒加载（首次 `defaultValidate` 时建 factory 并缓存）；factory 复用单例并在 Bean 销毁时 close。
- 破坏性：**非破坏性**（只影响本就启动失败的错误配置下游，正常下游行为不变）。

### C-06 N-4 核实：IPageQuery limit=0 透传 → 空页，非 DoS；与 PlusPageQuery 回退不一致

- 分类：功能不一致，安全影响核实为无。
- 位置：`IPageQuery.java:79-89`

```java
private static long parsePagingValue(Object value, long defaultValue) {
    if (value == null) {
        return defaultValue;
    }
    try {
        long parsed = Long.parseLong(String.valueOf(value).trim());
        return parsed < 0 ? defaultValue : parsed;
    } catch (NumberFormatException e) {
        return defaultValue;
    }
}
```

- 核实证据（MP 3.5.12 源码，`mybatis-plus-jsqlparser-3.5.12-sources.jar` 内 `PaginationInnerInterceptor`）：
  - `beforeQuery`：`if (page.getSize() < 0 && null == _limit) { ... return; }` —— **仅 size<0 且未配 maxLimit 才跳过分页（全表扫描）**；
  - `handlerLimit`：`if (limit != null && limit > 0 && (size > limit || size < 0)) page.setSize(limit);` —— size=0 不被改写；
  - size=0 → `dialect.buildPaginationSql(..., 0)` → `LIMIT 0` → 空结果。
  - 负数到不了 Page：IPageQuery `parsed < 0` 回退 10，PlusPageQuery `> 0` 判断回退 10。
- 结论：limit=0 返回空页，**不构成 DoS/全表扫描**；与 PlusPageQuery（0 回退 10）行为不一致，下游同一参数两条路径结果不同，属正确性/一致性问题。
- 严重度：**Low**。
- 修复方案：统一语义（建议 0 一律回退默认 10，与 PlusPageQuery 对齐），在 javadoc 写明。
- 破坏性：**破坏性需裁定**（改变查询 API 行为：依赖 limit=0 返回空页的下游会拿到数据）。

### C-07 ObjectValidator 递归深度 >16 静默跳过校验，递归 DTO 可被构造深层嵌套绕过

- 分类：(a) 校验逻辑缺口（窄场景）。
- 位置：`kdla-component-validator/src/main/java/top/kdla/framework/validator/ObjectValidator.java:75-77`

```java
if (t == null || depth > MAX_RECURSION_DEPTH) {
    return;
}
```

- 攻击场景：DTO 含递归结构（如树形 `children`）且深层字段挂校验注解时，攻击者提交 17 层以上嵌套，第 17 层起所有注解校验被静默跳过（fail-open）。深度上限本意是防 StackOverflow/性能问题，但"超限即放行"把 DoS 防护做成了校验绕过。
- exploitability：需要下游 DTO 恰好是递归类型且注解挂在深层字段，场景窄；普通扁平 DTO 深度由类结构固定，攻击者无法控制。
- 严重度：**Low**。
- 修复方案：超限改 fail-closed（抛 BizException"对象层级过深"）而非静默 return；或超限记入 msg 使校验失败。
- 破坏性：**破坏性需裁定**（改变校验 API 行为：原本放行的超深对象改为拒绝）。

### C-08 ExcelImpValid 纯空白单元格绕过全部三个校验注解

- 分类：(a) 输入校验缺口。
- 位置：`kdla-component-supplement/src/main/java/top/kdla/framework/supplement/excel/imp/ExcelImpValid.java:37,46,56`

```java
if (isExcelValid && Objects.isNull(fieldValue)) {          // 只判 null
...
if (StringUtils.isNotBlank(cellStr) && cellStr.length() > length) {   // blank 跳过长度
...
if (StringUtils.isNotBlank(cellStr) && !cellStr.matches(regexp)) {    // blank 跳过正则
```

- 攻击场景：单元格填 `"   "`（纯空白）：`@ExcelNotNullValid` 只判 null 放行；长度与正则校验因 `isNotBlank=false` 整体跳过。下游若用 `@ExcelPatternValid` 强制手机号/编号格式，空白值长驱直入入库。
- exploitability：导入接口暴露即可；危害取决于下游是否把这三个注解当唯一防线。语义上 NotNull 只判 null 与 JSR-303 一致，但"blank 跳过 pattern"使格式约束对空白输入失效，是实际缺口。
- 严重度：**Low**。
- 修复方案：`@ExcelNotNullValid` 增加对 String 的 blank 判定（或新增 `allowBlank` 属性默认 false）；pattern/length 校验对 blank 不再无条件跳过，交由注解语义决定。
- 破坏性：**破坏性需裁定**（改变校验语义：原本放行的空白输入改为拒绝）。

### C-09 ExcelImpValid 长度违规取错注解 message，单挂 @ExcelStrLengthValid 时 NPE

- 分类：可靠性缺陷（fail-closed，不构成绕过）。
- 位置：`ExcelImpValid.java:43-48`

```java
boolean isExcelStrValid = field.isAnnotationPresent(ExcelStrLengthValid.class);
if (isExcelStrValid && fieldValue != null) {
    String cellStr = fieldValue.toString();
    int length = field.getAnnotation(ExcelStrLengthValid.class).length();
    if (StringUtils.isNotBlank(cellStr) && cellStr.length() > length) {
        errorMessages.add(field.getAnnotation(ExcelNotNullValid.class).message());
    }
}
```

- 现象：字段只挂 `@ExcelStrLengthValid` 不挂 `@ExcelNotNullValid` 时，`getAnnotation(ExcelNotNullValid.class)` 返回 null → NPE。异常经 EasyExcel `onException` 记为行错误，该行仍被拒绝（fail-closed），但真实错误信息丢失、导入报告失真。
- 严重度：**Low**。
- 修复方案：改为 `field.getAnnotation(ExcelStrLengthValid.class).message()`。
- 破坏性：**非破坏性**（一行内部修复）。

### C-10 IPageQuery.mapPage 对 sidx/order 硬转 String、params 无 null 防护

- 分类：输入健壮性（畸形输入 → 500 而非 400）。
- 位置：`kdla-component-dal/src/main/java/top/kdla/framework/infra/dal/mybatis/util/IPageQuery.java:40,44-45`

```java
params.put(DoConstants.PAGE, page);
...
String orderField = SqlFilter.sqlInject((String)params.get(DoConstants.ORDER_FIELD));
String order = (String)params.get(DoConstants.ORDER);
```

- 攻击场景：`sidx`/`order` 传非字符串（JSON 对象/数组 → Map/List）→ `ClassCastException` 500；`params=null` → NPE。page/limit 已做容错解析（`parsePagingValue`），sidx/order 未对齐。批量畸形请求可制造错误日志噪声/轻微资源消耗，无注入（转型失败先于 SQL 拼接）。
- 严重度：**Low**。
- 修复方案：`instanceof String` 判断或 `String.valueOf` 容错（非字符串按非法参数处理）；params null 时与 PlusPageQuery 一致跳过 put。
- 破坏性：**非破坏性**（原本抛 500 的畸形输入改为显式参数错误，合法输入行为不变）。

### C-11 FieldValueFindHelp 失败日志整体序列化 sourceObject，敏感字段入日志（CWE-532）

- 分类：(a) 信息泄露面（日志层）。
- 位置：`kdla-component-supplement/src/main/java/top/kdla/framework/supplement/fieldvaluefinder/FieldValueFindHelp.java:114,117,176,179`

```java
log.error("findAndInjectValue failed,dto is:{},exception is:", JacksonUtil.toJson(dto), e);
```

- 场景：`ProcessFieldDto.sourceObject` 是业务对象整体，`JacksonUtil.toJson` 无脱敏，字段值（可能含手机号/证件号等）随异常日志落盘。日志读取权限通常宽于 DB。
- 严重度：**Low**。
- 修复方案：日志只输出类名 + 字段名 + 注解目标（queryClass#queryMethod），不序列化 sourceObject 值。
- 破坏性：**非破坏性**（仅日志内容变化）。

---

## 已核实安全项（前序结论复核 + 扩展排查）

1. **SqlFilter 白名单无绕过**（`SqlFilter.java:19,37-42`）：

```java
private static final Pattern ORDER_COLUMN_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
...
if (str.length() > MAX_COLUMN_LENGTH || !ORDER_COLUMN_PATTERN.matcher(str).matches()) {
    throw ExceptionFactory.buildBizException("包含非法字符");
}
return str.toLowerCase();
```

   逐项核实：大小写——白名单含大小写字母但统一 toLowerCase，不产生新字符；注释/分号/引号/空格/括号——`matches()` 全串匹配，`-`、`/`、`*`、`'`、`;`、空白一律拒绝；URL 编码——`%` 不在白名单，解码前后的载荷都进不来；Unicode/全角——ASCII 字符集拒绝；长度——>64 拒绝。排序方向（`PlusPageQuery.java:90-95`、`IPageQuery.java:48-53`）只做 `equalsIgnoreCase("asc")` 二值选择，其余一律 desc，方向不可注入。MP 3.5.12 `concatOrderBy` 经 JSqlParser 重建 ORDER BY，但白名单已是有效前置防线。**结论：前序"防注入正确"成立。** 残留面：白名单允许任意合法列名排序，敏感列的排序授权属下游责任。
2. **JacksonUtil 无多态反序列化 RCE 面**（`JacksonUtil.java:29-40`）：两个 ObjectMapper 仅配置 `FAIL_ON_UNKNOWN_PROPERTIES=false` 与 SnakeCase 命名策略，全文件无 `activateDefaultTyping`/`enableDefaultTyping`/`@JsonTypeInfo`，全仓 grep 亦无 `PolymorphicTypeValidator`。gadget 反序列化前提（多态类型绑定）不存在。附带注意：`toBean(String, Class)` 解析失败静默返回 null（`JacksonUtil.java:68-79`），下游若把 null 当"无数据"处理可能产生逻辑缺口，属下游使用责任。
3. **MdcAspect SpEL 表达式来源可信**（`MdcAspect.java:79-85,95-108`）：表达式仅取自 `@MdcDot.bizCode()` 注解元数据（编译期常量），攻击者输入只作为 `context.setVariable` 的变量值绑定，值本身不会被二次解析为 SpEL。`StandardEvaluationContext` 虽是全能上下文（T()、BeanResolver），但表达式不可被攻击者注入。**无 SpEL 注入。** 信任边界提示：下游若把用户输入拼进 bizCode 注解值（不可能动态改注解）或在别处复用此模式则另当别论。
4. **FieldValueFindHelp 反射目标全部注解驱动**（`FieldValueFindHelp.java:89-109,124-127`）：`queryClass`/`queryMethod`/`fromField` 来自 `@FieldValueFind` 注解（`annotation/FieldValueFind.java:26-37`），攻击者只能控制 fromField 的**值**（作为参数传给开发者指定的 Spring Bean 方法），不能选择反射目标。`setAccessible(true)` 作用于开发者自己的 DTO 字段。**无反射滥用。**
5. **Excel 临时文件路径不可被攻击者控制、无遍历**（`ExcelReadListener.java:139-143,239-248`）：prefix/suffix 由开发者经 `errorReportFileName` 设置，默认 `excel-import-report`/`.xlsx`，非请求输入。JDK 21 `File.createTempFile` 源码核实（src.zip `java.base/java/io/File.java` TempDirectory.generateFile）：`prefix = (new File(prefix)).getName()` 剥掉前缀中的目录成分，随机数插入文件名，即使开发者误传含 `../` 的前缀也无法逃出 tmpdir。`deleteOnExit()` 已注册，`ExcelReadListenerTempFileCleanupTest` 用子进程探针覆盖清理行为。suffix 理论上可含分隔符（JDK 不 sanitize suffix），但 suffix 是开发者常量，非攻击面。
6. **CsvUtil 无 CSV 公式注入 sink**（`CsvUtil.java:31-44`）：仅解析（`CSVFormat.parse`），全仓无 `CSVPrinter`/CSV 写出代码。公式注入需要导出 sink；解析出的值若经 `KdlaExcelWriteHelp` 导出为 xlsx，POI 写的是字符串单元格（STRING type），Excel 不将其求值为公式。`CsvUtilParseTest` 覆盖无限流修复。**无 CSV 注入面。**
7. **writeWeb 响应头无 CRLF 注入**（`KdlaExcelWriteHelp.java:46-47`）：fileName 先 `URLEncoder.encode`（CR/LF 变 %0D%0A）再拼 `Content-disposition`。
8. **范围内无 MyBatis `${}` 拼接**：dal/validator/supplement/common 四模块 main 下无 XML mapper，grep `\${` 零命中；排序拼接全部经 SqlFilter + OrderItem。
9. **范围内无命令注入/XPath/LDAP sink**：grep `Runtime.exec`/`ProcessBuilder`/`XPath`/`InitialDirContext` 在四模块 main 代码零命中（ThreadPoolHelp 的 `availableProcessors()` 除外，非执行）。

## 下游责任 sink 清单（(c)，框架层不构成漏洞，建议文档标注信任边界）

| sink | 位置 | 说明 |
|---|---|---|
| 任意路径写文件 | `KdlaExcelWriteHelp.java:74-83`（`writeDataFile(list, fileUrl)`）、`:105-133`（`multiGetExcelFile` 用 map key 当路径） | `new File(fileUrl)` 无沙箱/校验；下游若把请求参数当 fileUrl 传入 → 任意文件覆盖。API 契约即"文件全路径"，信任边界在调用方 |
| 文件分块/合并路径 | `kdla-component-common/.../help/FileChunkTransmitHelp.java:34-50,171-213` | srcDir/destDir/srcPaths/destPath 全为调用方参数，同上 |
| 类发现与实例化 | `kdla-component-common/.../utils/ClassUtil.java:35-73` | location 模式与 loadFunction 调用方提供，SPI 用途；下游若传用户输入 → 类路径内任意类实例化 |

## 范围外线索（建议后续专项跟进，本次未展开）

- `kdla-component-reactor/.../utils/SerializeUtil.java:60,412`：自定义序列化含 `input.readObject()`，需核输入来源。
- `kdla-component-supplement-job/.../com/xxl/job/core/util/JdkSerializeTool.java:58-59`：xxl-job 内嵌 `ObjectInputStream.readObject()`，job 参数反序列化面。
- `kdla-component-supplement-job/.../com/xxl/job/core/util/ScriptUtil.java:76`：`Runtime.getRuntime().exec` 执行 GLUE 脚本，xxl-job 设计使然，需核脚本来源鉴权。
- `kdla-component-supplement-cache/.../KDistributeLockedAspect.java:71-76`：SpEL，已快速核实表达式来自注解 `lockAction.key()`（可信），与 MdcAspect 同模式。

---

## 分组汇总

### 非破坏性可直接修（安全内部修复，不改下游可见行为）

| 编号 | 修复 |
|---|---|
| C-05 | ValidatorAutoConfigure 条件改守实现类 / CommonValidator 懒加载 + factory 复用关闭；ValidatorUtils 静态块同步处理 |
| C-09 | ExcelImpValid.java:47 改取 `ExcelStrLengthValid.class` 的 message |
| C-10 | IPageQuery sidx/order 容错转型 + params null 防护 |
| C-11 | FieldValueFindHelp 错误日志去掉 sourceObject 值序列化 |
| C-01（部分） | 新增带 ObjectInputFilter 的重载 + 原方法 @Deprecated（不动原方法行为） |

### 破坏性需裁定（改校验/查询 API 行为或依赖版本，影响下游）

| 编号 | 修复 | 影响 |
|---|---|---|
| C-01（彻底） | 原 byteToObject 直接加默认过滤 | 下游反序列化任意类的行为被改变 |
| C-02 | PlusPageQuery pageSize 加上限（建议可配置） | 超大 pageSize 请求被收敛 |
| C-03 | KdlaExcelReadListener 加默认行数上限 | 超大导入被截断 |
| C-04 | EasyExcel/POI 升级至 POI ≥5.4.0 线 | 依赖大版本升级，解析行为可能变化 |
| C-06 | limit=0 语义与 PlusPageQuery 对齐（回退 10） | 依赖空页行为的下游拿到数据 |
| C-07 | ObjectValidator 超深改 fail-closed | 超深对象从放行变拒绝 |
| C-08 | ExcelNotNullValid 拒空白 / blank 不再跳过 pattern | 空白输入从放行变拒绝 |