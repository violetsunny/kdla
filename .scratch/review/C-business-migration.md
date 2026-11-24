# 切片 C 评审报告：业务与迁移（domain / model / exception / dal / validator / job / job-application / trd）

- BASE=`136bf03`，HEAD=`0fa5ab7`，分支 `2.0.0-SNAPSHOT`（JDK21 / Boot 3.4.0）。
- 取证：`git diff 136bf03 HEAD -- <8模块src>`、`git show 1.0.1-SNAPSHOT:<path>` 逐文件基线比对、read/grep 静态审查。未运行 mvn，未连外部服务，未改任何源码。
- 结论先行：**NO-GO，Critical = 1**（validator 自动配置未注册，甲类 API CommonValidator 恢复对下游失效）。其余恢复质量高：8 个模块 javax→jakarta 零残留、charset 全部显式 UTF-8、JwtUtil fail-closed 实现正确、exception/dal/domain/model 与 1.x 基线逐行一致。

---

## Critical

### C-1 ValidatorAutoConfigure 未注册，CommonValidator 甲类恢复对下游失效
- 分级：**Critical**（乙类迁移未完成，本轮新引入的恢复缺口）
- 路径：`kdla-component-validator/src/main/java/top/kdla/framework/validator/config/ValidatorAutoConfigure.java:20`；缺失文件 `kdla-component-validator/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 问题：validator 模块在 HEAD 与磁盘上**均无任何 resources 目录**（`git ls-tree -r HEAD -- kdla-component-validator/src/main/resources` 为空，本轮 16 commit 也未触碰该路径）。1.x 经 `spring.factories` 注册 ValidatorAutoConfigure；2.x 恢复了类体（javax→jakarta 正确）却未建 `.imports`。Boot 3 忽略 spring.factories 的 EnableAutoConfiguration 注册（项目约定），且 CommonValidator 已移除 `@Component`、validator 模块内无任何其他 `@Component/@Service`、全仓库无任何 `.imports` 引用 ValidatorAutoConfigure（grep 证实）。结果：下游 `@Autowired CommonValidator` 启动即 NoSuchBeanDefinitionException，`kdla.config.validator.enabled` 开关形同虚设。审计 raw-code-audit H24 明确要求"恢复类体并补 AutoConfiguration.imports"，只完成了一半；`.scratch/kdla-backport/issues/08-validator.md` 勾选的"[x] ValidatorAutoConfigure 注册且默认开"与实际树不符。318 测试全绿不能暴露此问题（CommonValidatorTest 直接 new，不走装配）。
- 修复建议：新建 `kdla-component-validator/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，内容一行 `top.kdla.framework.validator.config.ValidatorAutoConfigure`；补一个 ApplicationContextRunner 装配回归测试（断言默认注册 + enabled=false 不注册）。
- 状态：待修。

---

## 必改

无。

---

## Nit

### N-1 UnifiedExceptionControllerAdvice 残留未用 WebRequest import
- 分级：Nit（本轮新引入：bcf5d7a 为修编译加入，af9bb6c 恢复 1.x 版 handleThrowable(HttpServletRequest) 后成为孤儿）
- 路径：`kdla-component-exception/src/main/java/top/kdla/framework/exception/UnifiedExceptionControllerAdvice.java:23`
- 问题：`import org.springframework.web.context.request.WebRequest;` 全文件无使用点。
- 修复建议：删除该 import。
- 状态：待修（只列不删）。

### N-2 JwtUtil.extractToken 不校验 Bearer 前缀
- 分级：Nit（本轮新代码，无安全影响）
- 路径：`kdla-component-supplement-trdplatformcloud/src/main/java/top/kdla/framework/supplement/trdcloud/utils/JwtUtil.java:73-77`
- 问题：`auth.split(" ")` 后只查 parts.length>=2 且 parts[1] 非空，`Foo <合法token>` 也接受。token 本身仍强制验签，不构成绕过，仅与 javadoc "格式 Bearer <token>" 不符。
- 修复建议：如需严格可加 `!"Bearer".equalsIgnoreCase(parts[0])` 拒绝；或保持宽松并改 javadoc。
- 状态：待修（低优先）。

### N-3 文件末尾缺换行（已知候选扩展）
- 分级：Nit（已登记候选"部分 .imports/JwtUtil/AopTargetHelp 文件末尾缺换行"的确认与扩展）
- 路径：`kdla-component-validator/.../CommonValidator.java:49`、`kdla-component-supplement-trdplatformcloud/.../JwtUtil.java:80`、exception/domain/trd/job 的 `.imports` 末行
- 问题：diff 显示 `\ No newline at end of file`；job `.imports` 本轮新增 XxlJobConfig 行后仍无尾换行。
- 修复建议：统一补尾换行。
- 状态：已知候选，只列不删。

### N-4 IPageQuery 分页 0 值边界与 PlusPageQuery 不一致
- 分级：Nit（1.x 既有，两文件与 1.x 基线逐行一致，非本轮回归）
- 路径：`kdla-component-dal/src/main/java/top/kdla/framework/infra/dal/mybatis/util/IPageQuery.java:85` vs `PlusPageQuery.java:69-74`
- 问题：parsePagingValue 仅 `parsed < 0` 回退默认，`limit=0`/`page=0` 透传（limit=0 预期产生空结果页）；PlusPageQuery 用 `> 0` 判断，0 回退默认。同库两个分页工具边界语义不一致。IPageQueryTest 只测了负数未测 0。
- 修复建议：统一为 `parsed <= 0 ? defaultValue : parsed` 并补 0 值用例；因属 1.x 既有行为，改动需评估下游是否依赖 0 值语义。
- 状态：待修（低优先）。

### N-5 EnumValid.findCheckMethod 重载选择不确定
- 分级：Nit（1.x 既有，findCheckMethod 与基线一致）
- 路径：`kdla-component-validator/src/main/java/top/kdla/framework/validator/annotation/EnumValid.java:107-121`
- 问题：枚举校验方法存在重载（如 `check(Object)` 与 `check(String)`）时，按 `getMethods()` 返回顺序取第一个参数可赋值的方法，JVM 不保证顺序，可能命中较宽泛的重载。
- 修复建议：候选中优先选参数类型最具体（isAssignableFrom 链最短）者。
- 状态：待修（低优先）。

---

## Optional

### O-1 CommonValidator 实例初始化块急建 ValidatorFactory：无实现即炸 + 未 close
- 分级：Optional（1.x 既有设计；C-1 修复后暴露面扩大，建议一并处理）
- 路径：`kdla-component-validator/src/main/java/top/kdla/framework/validator/CommonValidator.java:29-32`、`config/ValidatorAutoConfigure.java:17`
- 问题：`@ConditionalOnClass(Validator.class)` 守的是 jakarta.validation **API**；实例初始化块 `Validation.buildDefaultValidatorFactory()` 需要 **实现**（hibernate-validator）。下游 classpath 只有 API 无实现时，C-1 修复后默认装配会在启动期抛 NoProviderFoundException。另 factory 未 close（已登记 nit-inventory L8）。
- 修复建议：守卫改实现类（如 `@ConditionalOnClass(org.hibernate.validator.HibernateValidator.class)`）或 factory 懒加载；单例 bean 场景 close 问题影响小，可随懒加载一并处理。
- 状态：建议项。

---

## FYI（1.x 既有 / 刻意决策 / 已知候选确认，不作为本轮缺陷）

### F-1 JWT fail-closed 的覆盖边界：不带 blade-auth 头仍匿名放行
- 路径：`kdla-component-supplement-trdplatformcloud/.../controller/TrdPlatformAPIController.java:71-75`（及其余 4 个 controller/4 个 VO 同模式）
- 说明：所有调用点均为 `@RequestHeader(value="blade-auth", required=false)` + `if (StringUtils.isNotEmpty(bladeAuth))` 才调 JwtUtil。完全省略该头 → 不验签、createUser/updateUser 为 null、业务照常执行。此为 1.x/BASE 既有设计（controller 本轮仅 import 替换），**非本轮引入的绕过**。JwtUtil 自身无绕过：强制 hs256 signer 下 alg=none/RS256 伪造均验签失败，未配 key/容器未注入/格式非法/过期全抛 BizException，7 个回归用例真实断言。是否要求 trd 管理接口强制携带 token 属业务裁定，超出本轮范围。

### F-2 无 exp 声明的已签名 token 通过 validate(0)
- 路径：`JwtUtil.java:46`
- 说明：hutool 仅校验存在的日期声明，无 exp 的合法签名 token 永不过期。签发方选择，非攻击者绕过（无密钥无法伪造）。密钥编码假设 not measured 已按裁定文档化，不重复标记。

### F-3 LoginService token 为 hex(用户JSON) 无签名
- 路径：`kdla-component-supplement-job-application/src/main/java/com/xxl/job/admin/service/LoginService.java:30-40`
- 说明：makeToken/parseToken 是上游 xxl-job 原样设计（token 含 password md5，可离线构造），本轮仅补 UTF-8 charset，未改语义。既有上游问题，记录备查。

### F-4 ApplicationContextHelp 静态持有无 volatile / 无关闭清理 / 宽泛 catch
- 路径：`kdla-component-domain/src/main/java/top/kdla/framework/domain/ApplicationContextHelp.java:19,40-44`
- 说明：与 1.x 逐行一致。static context 非 volatile（Spring 初始化发布后实际安全，严格 JMM 无保证）；无 @PreDestroy 置空（容器关闭后静态引用残留）；getBean(Class) catch Exception 会把 BeanCreationException 降级为"name 也找不到"的 RuntimeException，创建失败根因仅留在 warn 日志。均 1.x 既有。

### F-5 exception advice 兜底与消息外泄边界
- 路径：`UnifiedExceptionControllerAdvice.java:236,242-255`
- 说明：handleThrowable 签名 Throwable 但 Spring MVC 只把 Exception 路由到 @ExceptionHandler，Error 不会进入此方法（无吞 Error 问题，与 1.x 一致）；兜底已改固定文案"系统繁忙"（本轮安全改进，有测试）。handleHttpMessageNotReadableException 仍返回 `ex.getMessage()`（Jackson 解析错误可能含内部类名/路径），1.x 既有，未随兜底一起收敛。403 响应体用 ErrorCode.UNAUTHORIZED("3002","无权限") 与 1.x 一致，语义可接受。

### F-6 SqlFilter.toLowerCase() 未指定 Locale.ROOT
- 路径：`kdla-component-dal/src/main/java/top/kdla/framework/infra/dal/mybatis/util/SqlFilter.java:42`
- 说明：土耳其语 Locale 下 `I→ı` 会改写列名。白名单 `^[a-zA-Z0-9_]+$`+64 长度上限已从根本上拦截注入（union/注释/引号/分号全拒），且修复了黑名单误杀 update_time/is_deleted 的问题，方向正确；Locale 问题 1.x 既有。

### F-7 DBLengthValid BigDecimal 分支不校验 scale、负数放行
- 路径：`kdla-component-validator/src/main/java/top/kdla/framework/validator/annotation/DBLengthValid.java:69-74`
- 说明：`value <= 10^length-1 + 0.99...` 只限上界，decimal=2 时 1.234 仍通过；负数恒通过。1.x 既有（本轮只修 Integer/Long 的 pow 溢出，BigDecimal 全程比较修复正确）。

### F-8 ResponseHandler 用已弃用 Class.newInstance + 静默回退
- 路径：`kdla-component-exception/src/main/java/top/kdla/framework/exception/ResponseHandler.java:27,34-36`
- 说明：JDK21 仍可用（deprecated 未删除）；实例化失败静默回退 Response.buildFailure 无日志。1.x 既有。本轮新增 setMessage 双字段回填与 Response.buildFailure 对齐，正确（Response 确有 msg/message 双字段）。

### F-9 ObjectValidator 深度 16 静默停遍历
- 路径：`kdla-component-validator/src/main/java/top/kdla/framework/validator/ObjectValidator.java:75`
- 说明：超深对象图第 17 层起字段不再校验（静默跳过）。刻意防护（防 StackOverflowError），javadoc 已载明；IdentityHashMap 防环、父类字段收集、静态字段跳过、isAssignableFrom 排除、删 sun.misc.Unsafe 均正确且有测试。

### F-10 XxlJobConfig 属性键怪异 + 本轮新注册
- 路径：`kdla-component-supplement-job/src/main/java/top/kdla/framework/supplement/timer/config/XxlJobConfig.java:49`、job `.imports:2`
- 说明：`@ConditionalOnProperty(prefix="xxl.enabled", name="xxl")` 实际键为 `xxl.enabled.xxl`，下游设 `xxl.enabled=true` 不会启用 executor。类体本轮未改（1.0.1 无此文件，属 2.x 既有），本轮仅新增 .imports 注册；@Value 全有默认值、executor bean 默认关，注册本身无害。

### F-11 已知候选确认（只列不删）
- `StringUtil.transform2`（trd utils/StringUtil.java:57-61）：value 为 null 时 `Collectors.toMap` NPE，确认存在；文件本轮零改动，1.x 既有。
- `StringUtil.replaceUrl`（同文件:39-41）：空 catch 确认；`appendReplacement` 未转义 `$`/`\` 的实际风险低——UTF-8 恒被支持，encode 成功后产物不含 `$`/`\`，空 catch 分支近乎不可达。1.x 既有。
- `transform/transform2` 重复 key 分支不可达（H43 半误报）：遵守裁定，不报。
- trd/job `.imports` 跨模块重复注册 VertxHttpConfigure/StopWatch/exception advice/CatchLog：与已登记候选一致（Boot 去重无害冗余）；trd 的 4 条注册与 1.x spring.factories 内容一一对应，迁移忠实。

---

## 核对通过项（证据摘要）

1. **javax→jakarta 完整性**：8 模块 src 全量 grep，活代码 javax 残留 = 0。仅存合法命中：`javax.sql.DataSource`（XxlJobAdminConfig.java:13，JDK 包保留）、`javax.net.ssl.*`（XxlJobRemotingUtil.java:7，JDK 包保留）、application.properties 的 SSLSocketFactory（JDK）、注释行与 javadoc 提及。job-application jakarta import 恰 33 处，servlet/annotation/mail 全改（EmailJobAlarm 用 jakarta.mail，pom 有 spring-boot-starter-mail）。
2. **甲类 API 恢复**：BaseAssert.notBlank（3 重载，trim 判空白，与 1.x 逐行一致，isNull 语义反转方法按 1.x 移除）；MultiResponse.totalCount（int 字段 + buildSuccess(items,totalCount) 回填，与 1.x 一致，有测试）；CommonValidator 类体恢复正确（jakarta 直校、删 Locale.setDefault 全局副作用有 javadoc+测试）——**唯一缺口是 C-1 注册**。
3. **charset**：GlueFactory code-source md5（M1）、LoginService token/password、UserController 3 处 password md5 全部显式 StandardCharsets.UTF_8；job/trd/dal/validator/domain/model/exception 主代码再无裸 `getBytes()`/`new String(bytes)`（exception advice 的 new String 用请求编码+UTF-8 回退，正确）。
4. **JwtUtil fail-closed**：强制 hs256 signer（无 alg 混淆绕过）、verify+validate(0) 失败即抛、未配 key/空白 key/容器未注入全抛、去签名明文日志（warn 不带 token 内容）；JwtUtilTest 7 用例覆盖合法/篡改/错钥/过期/缺钥/空容器/非法头。强制验签破坏性变更遵守用户裁定，不当 bug。
5. **getByPCodes**：toMap merge `(a,b)->a` keep-first，与 getByPCode getOne(wrapper,false) 语义一致，有离线 MP 元数据回归测试。
6. **exception advice**：与 1.x 基线逐行一致（仅 javax→jakarta）；DuplicateKeyException 从 snakeyaml 误绑改为 spring-jdbc 正确类型并移入 @ConditionalOnClass 内层；Security 内层 @Import+@ConditionalOnClass 模式正确，FilteredClassLoader 双向测试实证；spring-jdbc/security-core provided（security 另 optional）；.imports 已注册；兜底固定文案防外泄有测试。
7. **dal**：IPageQuery/SqlFilter/PlusPageQuery 与 1.x 逐行一致；SqlFilter 白名单防注入正确；EsBaseData/EsSourceData @SuperBuilder 继承链正确（EsSourceData extends EsBaseData），有测试。
8. **domain/model**：ApplicationContextHelp/DomainAutoConfigure（COBM 注册+ .imports 在册）、Page（pageSize<=0 防除零、sameValueAs 值比较）、Response 双字段一致；MpBaseDo 删除遵守已登记裁定不报。
9. **测试质量**：抽查 IPageQueryTest/SqlFilterWhitelistTest/JwtUtilTest/TrdPlatformInfoServiceImplTest/UnifiedExceptionControllerAdviceTest/SecurityAccessDeniedAdviceConditionTest 均断言真实行为（含修复前失败场景复现），非空转测试。

---

## 结论

**NO-GO** — Critical 计数：**1**（C-1：validator 自动配置注册缺失，CommonValidator 甲类恢复对下游失效）。
修复 C-1（补一个 .imports 文件 + 装配回归测试）后，本切片无其他阻断项；Nit/Optional 均为低优先或 1.x 既有，可后续批次处理。