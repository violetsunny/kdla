# docs/architecture.md 核查报告

- 核查对象：`/Users/kangll13/aiot/java-code/self/gggggggood/kdla/docs/architecture.md`（296 行，Generated 2026-11-17）
- 核查时间：2026-11-17
- 核查方式：只读源码/pom/git log/测试日志比对，未重跑 mvn
- 结论：**FAIL 4 / WARN 8**。装配矩阵与配置键速查表逐键核对全部正确，模块依赖、测试计数、FAQ 技术声明基本干净；错误集中在装配计数句（line 109）、Nexus server id（line 296）、K2/K4 脚注日期（line 258）。

---

## FAIL（事实错误）

### FAIL-1 line 296：Nexus server ID `kdla-nexus` 与 pom 不符
> 原行：`需公司内网/VPN，并在 $HOME/.m2/settings.xml 中配置 server ID kdla-nexus 的凭据，指向 nexus.enncloud.cn。`

- 问题：根 pom `distributionManagement` 的 id 是 `maven-releases`（pom.xml:96）与 `maven-snapshots`（pom.xml:102），全仓 grep `kdla-nexus` 只命中文档自身；照文档配置凭据 `mvn deploy` 会因 id 不匹配鉴权失败。
- 修法：server ID 改为 `maven-releases` / `maven-snapshots`（或改 pom 统一为 kdla-nexus，二选一，文档跟随真源）。
- 验证：`sed -n '94,106p' /Users/kangll13/aiot/java-code/self/gggggggood/kdla/pom.xml`；`grep -rn 'kdla-nexus' --include='*.xml'` 零命中（仅 md 文档）。
- 备注：`docs/project-map.md:15` 同错（不在本次核查范围，需连带修）。

### FAIL-2 line 109："另有 3 类无开关靠 @ConditionalOnClass 守护" — DomainAutoConfigure 无任何 OnClass
> 原行：`T 存量下游已依赖默认开启行为，故 9 类有运行期行为的配置类 matchIfMissing=true 默认开，另有 3 类无开关靠 @ConditionalOnClass 守护；6 类有侵入性副作用的默认关`

- 问题：3 类无开关配置类中，`DingAlertConfigure` 有类级 `@ConditionalOnClass(Vertx.class)`（DingAlertConfigure.java:24）、`PrometheusAutoConfigure` 有 bean 方法级 `@ConditionalOnClass({CollectorRegistry, PushGateway})`（PrometheusAutoConfigure.java:23），但 `DomainAutoConfigure` 只有 `@Configuration` + bean 级 `@ConditionalOnMissingBean`，无任何 OnClass 守护（DomainAutoConfigure.java:11-18 全文核实）。
- 修法：改为"另有 3 类无开关：DingAlert / Prometheus 靠 @ConditionalOnClass 守护，Domain 无条件装配（仅 COBM 保护）"。
- 验证：read 全文 `kdla-component-domain/src/main/java/top/kdla/framework/domain/DomainAutoConfigure.java`（19 行）；grep 全仓 `@ConditionalOnClass` 清单中无 DomainAutoConfigure。

### FAIL-3 line 109："6 类有侵入性副作用的默认关" 与本文件装配矩阵（8 类）自相矛盾
> 原行：同 FAIL-2（line 109 后半句）

- 问题：本文件 line 119 矩阵"关"行列了 8 个配置类（RequestLog / LogInterceptor / WebFilter / Cache / RestHttp / VertxMqtt / CustomCors / SequenceNo），源码核实 8 者默认全关（7 个 `matchIfMissing=false` + SequenceNo 三键表达式默认 `false`）。"6 类"少计 2 类，且 9+3+6=18 ≠ 实际 20 个配置类。错误源头是 CHANGELOG.md 装配契约块同句（"6 类有运行期副作用默认关"），两处与其各自矩阵均不一致。
- 修法：改为"8 类有侵入性副作用的默认关"，并同步修 CHANGELOG.md 同句。
- 验证：grep 全仓 `@ConditionalOnProperty` 逐条清点（16 个属性开关：9 true / 7 false）+ SequenceNoConfigure.java:27-31 表达式三键均 `:false`；spring.factories 9 文件清点共 20 个唯一配置类。

### FAIL-4 line 258：K2/K4 修复日期在本仓 git log 无对应提交
> 原行：`K2（KdlaBizDisLockService 死锁风险）已在 2026-08-28 修复；K4（SequenceNoGenerator 缺装配开关）已在 2026-09-04 修复；P1（MachineIdHelp 注册失败脏值撞号）已在 2026-11-16 修复…`

- 问题：本仓 `git log --all` 全部提交日期只有 2023-*、2024-*、2026-11-14、2026-11-16（另有 3 个 2026-09-24 LICENSE 提交），不存在 2026-08-28 / 2026-09-04 的提交。这两个日期是 CHANGELOG.md:171 / CHANGELOG.md:143 两个"**R 仓回合**"块的标题日期（R commit bb8022f / 9cdbe79）；K2/K4 修复实际落地本仓是 2026-11-16 的 d675e97（distribute lock safety）与 d1e2bd3（configure closure，CHANGELOG 明写"K4 闭环"）。脚注省略"R 仓"限定，开着本仓 git log 的读者查无此日期，且会误以为 T 构件 8/9 月已含修复。P1 的 2026-11-16 正确（commit 30490b8 machineid + nit-inventory.md:96 + CHANGELOG P1 块三方一致）。
- 修法：改为"K2 已在 R 仓 2026-08-28 修复、K4 已在 R 仓 2026-09-04 修复，均于 2026-11-16 backport 落地本仓"。
- 验证：`git log --date=short --pretty='%ad' | sort -u`；`git log -- .../KdlaBizDisLockService.java`（最近改动 2026-11-16 d675e97）；`sed -n '140,180p' CHANGELOG.md`。

---

## WARN（表达 / 结构 / 职责问题）

### WARN-1 line 286：DDL 路径尾斜杠误导（`sql` 是文件不是目录）
> 原行：`MySQL 表 code_generator_cfg（DDL 在 kdla-component-supplement-cache/src/main/attachment/sql/）`

- 问题：`attachment/sql` 是一个 2113 字节的**无扩展名文件**（`ls` 报 "Not a directory"），`code_generator_cfg` 建表 DDL 确在其中（head 核实），但尾斜杠让读者 `cd`/`ls` 落空。
- 修法：去掉尾斜杠，写 `…/src/main/attachment/sql`（文件）。
- 验证：`ls -la .../attachment/`；`head -20 .../attachment/sql` 见 `CREATE TABLE code_generator_cfg`。

### WARN-2 line 288：`kdla.sequence.lock.key.prefix` 写成前置条件，实际有默认值非必填
> 原行：`3. 三个配置键任一为 true（见配置键速查）+ kdla.sequence.lock.key.prefix`

- 问题：该键经 `@Value("${kdla.sequence.lock.key.prefix:${kdla.sequence.lock-key-prefix:top::sequenceNo::}}")`（SequenceNoConfigure.java:37）注入，缺省回落 `top::sequenceNo::`，不配置也能跑；列为前置条件会让下游以为必须配。
- 修法：改为"可选覆盖 `kdla.sequence.lock.key.prefix`（默认 `top::sequenceNo::`）"。
- 验证：grep SequenceNoConfigure.java:37。

### WARN-3 line 43-70 架构图缺 `Job --> Domain` 边，与 line 82 文字不一致
> 原行：架构图 `Job --> Supplement / Cache / Dingding / Log`（无 Domain）；line 82 文字 `supplement-job 聚合最多（supplement / supplement-cache / supplement-dingding / log / domain）`

- 问题：supplement-job/pom.xml 实际声明依赖 domain，文字对、图漏边，同一文件内图文不一致。
- 修法：图中补 `Job --> Domain`。
- 验证：grep supplement-job/pom.xml 内部依赖 = common/exception/domain/supplement/supplement-cache/supplement-dingding/log。

### WARN-4 结构：5 组事实与 project-map.md / README.md 双写（一个事实住两个文件）
- 模块依赖流向：architecture.md:73-84 ↔ project-map.md:142-164（两份文字表述，将来改 pom 要同步两处）
- 测试计数 65/173 与 `-am` 命令：architecture.md:239-248 ↔ project-map.md:175-185
- 版本线/分支约定：architecture.md:233-237 ↔ project-map.md:187-194
- GAV 共享警告：architecture.md:17 ↔ README.md:13
- 装配默认值：architecture.md 装配矩阵（真源）↔ README.md:15 又摘要复述一份开/关清单
- 修法：architecture.md 保留装配矩阵与依赖流向为唯一真源，project-map/README 对应段落改为一行链接引用；README:15 的清单删除只留链接。
- 验证：read 三份文件全文逐段比对。

### WARN-5 职责串位：explanation 文档混入 how-to
> 原行：line 244-248 `验证命令必须带 -am … mvn -pl kdla-component-common -am clean test`；line 290-296 `### 如何发布到内部 Nexus？ mvn deploy …`

- 问题：architecture.md 定位是设计与决策解释，"怎么跑测试""怎么发布"是操作指引，属 README / project-map（project-map §测试入口已承载同一命令）。
- 修法：architecture 只保留"为什么必须 -am"（GAV 污染因果，这是设计解释），命令本体收敛到 project-map；Nexus 发布 FAQ 移 README。
- 验证：read architecture.md §测试/§FAQ 与 project-map.md:175-185 对照。

### WARN-6 line 88/123/147："T 默认 / T 仓"术语先用后定义
> 原行：line 88 表头 `自动配置（T 默认）`；line 147 `上表默认值为 T 仓语义`

- 问题："T"直到 line 109 "T 与裁剪版参考仓不同" 才可推断指本仓，冷读者在 line 88 表头处无法理解"T 默认"是什么；全文无 "T = 本仓（T repo）" 的显式定义。
- 修法：line 17 警告块首次出现处加一句"本仓下称 T，裁剪版参考仓下称 R"。
- 验证：通读全文检索"T"首次出现位置（line 88）与可推断位置（line 109）。

### WARN-7 line 8：KDLA 全称与 README 拼写不一致
> 原行：`KDLA（K-DDD-Layered Application & Architect）`

- 问题：README.md:2 写 `K-DDD-Layed-Application&Architect`（Layed），本文件写 Layered，两文件同名异拼，读者无法判断哪个是正式全称。
- 修法：两文件统一（建议 Layered，README 同步改）。
- 验证：read README.md:2 对照。

### WARN-8 line 1：`Files scanned: 367` 无法复现
> 原行：`<!-- Generated: 2026-11-17 | Files scanned: 367 -->`

- 问题：排除 target/.git/test-run 后全仓文件 500 个，仅 java/xml/factories/md 为 419 个，367 与任何可复现口径都对不上，且未定义扫描范围。
- 修法：删掉计数或在注释里写明统计口径（如 `find -name '*.java' -path '*/src/*'`）。
- 验证：两次 `find … | wc -l`（500 / 419）。

---

## 无法验证的声明

| 行 | 声明 | 原因 |
|---|---|---|
| 265 | "（裁剪版参考仓需显式 `@Import`，与本仓不同。）" | R 仓不在本机，无法打开对照 |
| 109 | "T 存量下游已依赖默认开启行为" | 下游项目使用情况不在本仓；CHANGELOG"T 现状已开启（用户裁定）"仅间接佐证 |
| 256 | K5 "`ApplicationContextHelp` 静态方法不可 mock，强制集成测试" | 类存在已核实（domain 模块），但"不可 mock"取决于测试栈是否引 mockito-inline，未逐一验证依赖树 |
| 8 | KDLA 全称语义本身 | 仅 README 一处佐证且拼写冲突（见 WARN-7） |

---

## 核实为干净的项（逐条附验证方式）

**① 装配矩阵 + 配置键速查（line 114-147）— 全部 16 个属性键逐一对照源码，零错误：**

| 键 | 文档默认 | 源码证据 |
|---|---|---|
| `kdla.config.mdc.enabled` | true | MdcAutoConfigure.java:20-21 `matchIfMissing=true` |
| `kdla.config.stop-watch.enabled` | true | StopWatchAutoConfigure.java:26-27 `true` |
| `kdla.config.catch-log.enabled` | true | CatchLogAutoConfigure.java:26-27 `true`（line 145 强调"不是 kdla.log.catch-log.enabled"正确） |
| `kdla.exception.handler.enabled` | true | UnifiedExceptionControllerAdvice.java:57-58 `true`；line 56 `@ConditionalOnWebApplication(SERVLET)` 佐证 line 93/110/265 "仅 SERVLET" |
| `kdla.config.validator.enabled` | true | ValidatorAutoConfigure.java:19 `true` |
| `kdla.cache.redisson.enabled` | true | RedissonAutoConfigure.java:38 `true` |
| `kdla.config.vertx.enabled` | true | VertxConfigure.java:22 `true` |
| `kdla.config.vertx-http.enabled` | true | VertxHttpConfigure.java:26 `true` |
| `kdla.config.field-value-find.enabled` | true | FieldValueFindConfigure.java:15 `true` |
| `kdla.cache.enabled` | false | CacheAutoConfigure.java:41 `false`；line 119 "另需容器存在 RedisTemplate" 对（:39 `@ConditionalOnBean(RedisTemplate.class)`） |
| `kdla.config.request-log.enabled` | false | RequestLogAutoConfigure.java:20-21 `false` |
| `kdla.config.log-interceptor.enabled` | false | LogInterceptorAutoConfigure.java:22-23 `false` |
| `kdla.config.web-filter.enabled` | false | WebFilterAutoConfigure.java:23-24 `false` |
| `kdla.config.rest-http.enabled` | false | RestHttpConfigure.java:29 `false` |
| `kdla.config.mqtt.enabled` | false | VertxMqttConfigure.java:36 `false` |
| `kdla.config.cors.enabled` | false | CustomCorsConfig.java:18 `false` |

- `kdla.http.ssl.trust-all` 默认 false：VertxHttpConfigure.java:48 `@Value("${kdla.http.ssl.trust-all:false}")`；MITM 告警：:80 `log.warn(…MITM…)` ✓
- `kdla.http.request-timeout` 默认 30000：VertxHttpConfigure.java:52 `@Value("${kdla.http.request-timeout:30000}")` ✓
- 序列号三键 OR：SequenceNoConfigure.java:27-31 `@ConditionalOnExpression` 三键与文档 line 143 逐字一致，均 `:false` 默认关 ✓；:33 `@AutoConfigureAfter(RedissonAutoConfigure.class)`、:34 `@ConditionalOnBean(RedissonRedDisLock.class)` 与 line 146 注释一致 ✓
- 无开关三类默认开：DomainAutoConfigure（spring.factories 注册，无条件）、DingAlertConfigure:24 OnClass(Vertx)、PrometheusAutoConfigure:23 bean 级 OnClass(CollectorRegistry,PushGateway) ✓（守护表述问题见 FAIL-2）
- line 111 "所有可替换 Bean 使用 @ConditionalOnMissingBean"：全仓 main 源码 `@Bean` 31 处 vs COBM 非 import 引用约 31 处，量级吻合，抽查 Domain/Redisson/Prometheus/SequenceNo 均带 COBM ✓
- line 112 "所有 @ConditionalOnClass 引用真实外部类"：grep 全仓 24 处 OnClass 逐条看，全部为 Aspect/Filter/AsyncHandlerInterceptor/RedisTemplate/RedissonClient/Vertx/WebClient/RestTemplate/CorsFilter/MqttClient/Validator/CollectorRegistry/PushGateway/AccessDeniedException/DuplicateKeyException 等外部类，无自身模块内部类 ✓

**② 模块依赖流向（line 73-84 + 架构图）：** grep 13 个模块 pom 的 `top.kdla.framework` 内部依赖，与文字逐条一致：model/reactor 零内部依赖（model pom 仅 lombok + provided starter-test，line 77 "除 Lombok" 正确）、common→model、exception→model+common、domain→common+exception(+model)、dal→domain 及以下、log/validator→exception 及以下、supplement→domain 及以下、cache→common+exception、dingding→supplement 等、monitor→common+exception、job→supplement/cache/dingding/log/domain/common/exception（line 82 "聚合最多"正确）。唯一问题是图漏 Job→Domain 边（WARN-3）。

**③ 测试计数（line 242）：** `/tmp/kdla-full-test.log`（2026-11-17 09:20 生成，BUILD SUCCESS）中 `Tests run … -- in` 行恰 65 条 = 65 测试类；9 个模块小结 6+56+10+8+16+28+7+17+25 = 173 用例 ✓；测试类包名分布核实覆盖 model(dto 2)/common 23/exception 3/domain 2/dal(infra) 4/validator 6/log 10/supplement 4/supplement-cache 11，与"分布于 model、common、exception、domain、dal、validator、supplement、supplement-cache、log"一致 ✓。（注：CHANGELOG 写 64 类/171 用例，是 11-16 旧口径，日志更新，文档从新，不算错。）

**④ spring.factories（line 106）：** `find … -name spring.factories -path '*/src/main/*'` 恰 9 个模块（common/domain/exception/log/supplement/supplement-cache/supplement-dingding/supplement-monitor/validator）✓；9 文件内容逐一 cat，注册的 20 个唯一配置类与装配矩阵名单完全对应 ✓。

**⑤ FAQ 技术声明：**
- AESUtil/Sm4Util `v1:Base64(IV+密文)`、解密按前缀分支、无前缀回退旧格式：AESUtil.java:21,48,70-71,85 与 Sm4Util.java:50,134,138-139,149 核实 ✓；Sm4Util 密钥文本 Base64（:155 `Base64.getDecoder().decode(strKey)`）✓
- `@DistributeLocked` SpEL 用 `SimpleEvaluationContext.forReadOnlyDataBinding()`：DistributeLockedAspect.java:77 ✓；锁失败抛 LockFailException：:106 ✓
- SqlFilter 白名单：SqlFilter.java:19 `^[a-zA-Z0-9_]+$` + :24 长度 64 上限 ✓
- `@CatchAndLog`：切点 `@within(…CatchAndLog) && execution(public * *(..))`（CatchLogAspect.java:46）✓ 类内 public 方法全拦截；Error 无条件 rethrow（:118-120）✓；仅 BizException/SysException/LockFailException/BaseException 转 Response，未知异常落 UNKNOWN_ERROR 通用文案（:160-184）✓——同时佐证 K1 描述
- BaseDo/MpBaseDo：MpBaseDo.java:22-23 `@TableLogic Integer deleted`、BaseDo.java:22 `Boolean isDeleted` ✓
- 分布式锁时序图：`biz(lockKey, dbPredicate, bizSupplier, errCode)` 签名（KdlaBizDisLockService.java:38-65）、LockFailException（:104-106）、unlock 由 `isHeldByCurrentThread` 守卫（:110）✓
- 序列号流程图：`cfg.getIsCache()==1` 分支（SequenceNoGenerator.java:252）、加锁取号（:109）、DB 写回（:190 updateByIdAndRemark）✓
- 请求链路图：KdlaBodyReaderRequestFilter 存在（webfilter/KdlaBodyReaderRequestFilter.java:27 implements Filter），MDC traceId 设置/透传（:46-52）✓

**⑥ 版本与分支约定（line 233-237）：** 根 pom `java.version=1.8`（:35）、`spring.boot.version=2.7.18`（:48）、`spring.cloud.version=2021.0.9`（:49）、`kdla.framework.version=1.0.1-SNAPSHOT`（:36）、根 `<version>1.0.1-SNAPSHOT`（:9）✓；`grep -l '<version>1.0.1-SNAPSHOT</version>' kdla-component-*/pom.xml | wc -l` = 13，`<module>` 数 = 13 ✓；`git branch -a` 见 2.0.0-SNAPSHOT，`git show 2.0.0-SNAPSHOT:pom.xml` 核得 java 21 / boot 3.4.0 / cloud 2024.0.1 ✓；snakeyaml 1.33（pom:55 带 bug 注释）、spring-kafka 2.9.12（pom:89 带 bug 注释）✓；提交信息风格 `kdla <version> and upgrade <topic>` 与 git log 一致 ✓。

**⑦ 已知限制其余项：** line 259 "其余上游同源遗留 19 项（P2-P20）登记于 docs/nit-inventory.md" — nit-inventory grep 得 P1-P20 恰 20 项，P1 已修，剩 19 项 ✓；"只登记不修"与 nit-inventory 标题口径一致 ✓。

**⑧ 表达层：** 全文无"开创性/赋能/彰显"类营销语，无三段式排比与结尾金句；代码段外中英文空格抽查（perl 正则扫描）无缺失；line 17 双仓 GAV 警告与 CHANGELOG 头部声明一致。
