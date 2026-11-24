# verify-project-map.md — docs/project-map.md 核查报告

核查时间：2026-11-17 | 核查对象：`/Users/kangll13/aiot/java-code/self/gggggggood/kdla/docs/project-map.md`（194 行）
角色：冷读开发者 + 开仓事实核查员 | 约束：未重跑 mvn，测试数据引自 `/tmp/kdla-full-test.log`

结论：**2 FAIL / 4 WARN / 1 无法验证**。文档主体事实密度高且绝大部分准确（54 个子包路径、79 个类名逐一核实仅 1 处错），错误集中在 `KdlaHttp` 类名与 `kdla-nexus` server id 两处硬事实。

---

## FAIL（事实错误）

### F1. L93 `KdlaHttp` 类不存在，且 http/ 包内无 Netty 类

> 原行：`│       ├── http/                       HTTP 客户端（Vertx, Netty, KdlaHttp）`

问题：仓库中无 `KdlaHttp.java`，实际类名为 `KdlaHttpClient`；http/ 包实际内容为 BaseHttpClient、HttpContextHelp、KdlaHttpClient、RestHttpConfigure、VertxHttpClient、VertxHttpConfigure、ssl/，不含任何 Netty 类（Netty HTTP 在文档 L95 已单列的 netty/ 包）。
修法：改为 `HTTP 客户端（VertxHttpClient, KdlaHttpClient, RestHttpConfigure）`，删去 Netty。
验证：`ls kdla-component-supplement/src/main/java/top/kdla/framework/supplement/http/`；`find .../kdla-component-supplement -iname '*kdlahttp*'` 仅命中 KdlaHttpClient.java；全局 find `KdlaHttp.java` 结果 0。

### F2. L15 settings server id `kdla-nexus` 与根 pom 矛盾，无任何佐证

> 原行：`| 发布仓库 | 公司内部 Nexus（`nexus.enncloud.cn`，settings server id `kdla-nexus`） |`

问题：根 pom `<distributionManagement>` 的 repository id 是 `maven-releases`（pom.xml:96）与 `maven-snapshots`（pom.xml:102），不是 `kdla-nexus`；本机 `~/.m2/settings.xml` 不存在，`kdla-nexus` 无处可证。deploy 鉴权按 pom id 匹配 settings server，写错 id 会误导下游配置。
修法：改为 `settings server id 需匹配 pom 的 maven-releases / maven-snapshots`，或核实公司 settings 模板后写真实 id。
验证：`sed -n '90,120p' pom.xml`（id 为 maven-releases/maven-snapshots，url http://nexus.enncloud.cn/repository/{releases,snapshots}/）；`ls ~/.m2/settings.xml` → 不存在；`grep kdla-nexus ~/.m2/settings.xml` → 无。

---

## WARN（结构 / 表达 / 不完整）

### W1. L70、L84、L118 三个模块的依赖标注漏掉直接依赖 model

> 原行：`├── kdla-component-log/                 日志切面（依赖 common/exception）`
> 原行：`├── kdla-component-dal/                 数据访问层（依赖 domain/common/exception）`
> 原行：`├── kdla-component-supplement-dingding/ 钉钉告警（依赖 supplement/common/exception）`

问题：三个模块 pom 均直接声明 `top.kdla.framework:kdla-component-model`，文档标注不完整（同文件 L55 exception 条目就写全了 model/common，口径不一致）。
修法：分别补为 `common/model/exception`、`domain/common/model/exception`、`supplement/common/model/exception`。
验证：grep 各模块 pom.xml 的 top.kdla.framework 内部依赖 —— log: common+model+exception；dal: exception+model+common+domain；dingding: common+exception+model+supplement。

### W2. L144-158 依赖关系图把 log/validator/supplement-cache/supplement-monitor 画在 domain 之上层

> 原行：`domain（依赖 exception 及以下）\n  ↑\n dal / log / validator / supplement / supplement-cache / supplement-monitor（横切与支撑层）`

问题：层间 `↑` 暗示该层全部依赖 domain，实际 log、validator、supplement-cache、supplement-monitor 四个模块 pom 均不依赖 domain（仅 dal 与 supplement 依赖）；architecture.md「模块依赖流向」第 5 条的表述（"dal（依赖 domain）、…依赖 exception 及以下"）比本图准确，两文档同仓不一致。
修法：图中把 dal/supplement 与 log/validator/cache/monitor 拆开标注，或直接链接 architecture.md 的流向节。
验证：同 W1 的 pom 依赖提取（log/validator/cache/monitor 依赖列表中无 kdla-component-domain）；对照 docs/architecture.md:73-85。

### W3. L20-140 目录树封闭列举但漏项：根 `AGENTS.md`、`docs/agents/`

> 原行：`├── docs/                               技术说明文档\n│   ├── project-map.md …│   └── nit-inventory.md`

问题：docs/ 实际还有 `agents/` 子目录，根目录实际还有 `AGENTS.md`，树用 `└──` 封闭列举却缺项，冷读开发者会以为 docs/ 只有 3 个文件（test-run/ 为临时验证目录，可不列）。
修法：补 `│   ├── agents/` 与 `├── AGENTS.md` 两行，或在树顶注明"仅列主要文件"。
验证：`ls /Users/kangll13/aiot/java-code/self/gggggggood/kdla`（含 AGENTS.md）；`ls .../docs/`（agents、architecture.md、nit-inventory.md、project-map.md）。

### W4. L160-164 「依赖原则」4 条与 architecture.md canonical 内容近逐字重复

> 原行：`- 所有模块的外部依赖默认使用 `provided` scope`、`- 模块间依赖版本由根 pom `dependencyManagement` 统一管理（`${project.version}`），模块 pom 内不写版本号` 等

问题：assembly 矩阵/配置键已正确只留链接（L168、L173），但依赖原则 4 条与 architecture.md「依赖作用域策略」（L224-232）和「模块依赖流向」结尾警示（L85）重复，形成双真源，日后改动易失同步（W2 已是失同步实例）。
修法：保留流向图 + 一句"依赖原则与作用域策略见 docs/architecture.md"，删 4 条 bullet。
验证：`sed -n '73,85p;224,232p' docs/architecture.md` 逐条比对。

---

## 无法验证的声明

1. **L1 `Files scanned: 367`** — "scanned" 口径未定义。实测 `git ls-files | wc -l` = 378，`find . -name '*.java'`（排除 test-run）= 344，两种合理口径都不是 367。建议注明统计口径或删除该数字。

---

## 核实通过项（干净部分）

| 声明 | 结果 | 验证方式 |
|---|---|---|
| L16 模块数 13 | ✓ | ls 根目录，13 个 kdla-component-* |
| L12 版本 1.0.1-SNAPSHOT | ✓ | pom.xml:9 |
| L14 Java 编译目标 1.8 | ✓ | pom.xml:35 `<java.version>1.8</java.version>` |
| L191 版本线 1.0.1：JDK8 / Boot 2.7.18 / Cloud 2021.0.9 | ✓ | pom.xml:35,48,49 |
| L192 版本线 2.0.0：JDK21 / Boot 3.4.0 / Cloud 2024.0.1 | ✓ | `git show origin/2.0.0-SNAPSHOT:pom.xml` |
| L15 发布仓库 nexus.enncloud.cn | ✓ | pom.xml:98,103（server id 见 F2） |
| L172 当前 9 个模块有 spring.factories | ✓ | find src/main/resources -name spring.factories = 9（common/domain/exception/log/supplement/supplement-cache/supplement-dingding/supplement-monitor/validator） |
| L185 65 个测试类 | ✓ | find src/test/java -name '*.java' = 65，且全部 *Test*.java 命名 |
| L185 173 个用例 | ✓ | /tmp/kdla-full-test.log 模块级 Tests run 合计 6+56+10+8+16+28+7+17+25 = 173，BUILD SUCCESS |
| L185 JUnit 5 + Mockito 经 provided spring-boot-starter-test | ✓ | 与 AGENTS.md 测试节一致，根 pom 有 mockito-junit-jupiter 版本管理（pom.xml:238） |
| L20-140 目录树 54 个子包路径 | ✓ 全部存在 | 逐路径 `[ -d ]` 脚本检查，0 缺失 |
| 目录树 79 个关键类名 | ✓ 78/79 存在 | 逐类 `find -name '<类名>.java'`，唯一缺失 KdlaHttp（见 F1） |
| L30 model 零内部依赖、除 Lombok | ✓ | model pom 无 top.kdla.framework 内部依赖；pom.xml:27-28 有 lombok |
| L38 reactor 零内部依赖 | ✓ | reactor pom 无内部依赖 |
| L46/L55/L62/L91/L101 依赖标注 common→model、exception→model/common、validator→common/exception、supplement→domain/common/exception、cache→common/exception | ✓ | grep 各 pom 内部依赖 |
| L131/L157 job 聚合 supplement/cache/dingding/log/domain | ✓ | job pom：common+exception+domain+supplement+supplement-cache+supplement-dingding+log（common/exception 为下层，列举口径可接受） |
| L59 UnifiedExceptionControllerAdvice 经 spring.factories 自动注册 | ✓ | cat kdla-component-exception/.../spring.factories：EnableAutoConfiguration = ...UnifiedExceptionControllerAdvice |
| L120 DingAlertConfigure OnClass(Vertx)、无开关 | ✓ | DingAlertConfigure.java:24 `@ConditionalOnClass(Vertx.class)`；dingding 模块 grep ConditionalOnProperty = 0 命中 |
| L48 加解密 AES/SM2/SM3/SM4 | ✓ | common/utils/ 下 AESUtil、Sm2Util、Sm3Util、Sm4Util |
| L128-129 monitor 包结构 | ✓ | annotation/ 含 PrometheusCounter、PrometheusPointAspect、PrometheusAutoConfigure；PushGatewayManager 在 prometheus/ 根 |
| L133-137 job 包结构 | ✓ | handler/ 含 IJobHandler、annotation/XxlJob、impl/{Method,Script,Glue}JobHandler；executor/biz/thread/glue/server/context/log/util/enums 目录均在 |
| L28 nit-inventory.md P1-P20 / T1-T3 | ✓ | grep -oE 'P[0-9]+|T[0-9]+' 去重 = P1..P20 + T1..T3，无缺号 |
| L168/L173 装配矩阵、配置键只留链接 | ✓ | 链接指向 docs/architecture.md，其 :114「装配矩阵」、:121「配置键速查」章节存在 |

## 职责与表达检查

- **职责串位**：无。全文为结构导航，无设计解释段落；设计性内容（装配矩阵/配置键）均以链接指向 architecture.md。唯一越界是 W4 的依赖原则重复。
- **空泛营销语 / 无证据断言**：未发现。所有形容词性描述（"聚合最多""保持纯净"）均有 pom 证据支撑。
- **AI 腔**：未发现（无"值得注意的是""赋能"类措辞，无三段式排比）。
- **中英文空格**：抽查全文，中英文/数字混排处空格规范一致，无缺失。
