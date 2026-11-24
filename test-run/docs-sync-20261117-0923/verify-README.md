# README.md 核查报告

- 核查对象：/Users/kangll13/aiot/java-code/self/gggggggood/kdla/README.md（102 行）
- 核查时间：2026-11-17
- 核查方式：只读检查（ls / grep / sed / test -f / awk），未重跑 mvn；测试证据引用 /tmp/kdla-full-test.log
- 结论：FAIL 1 条，WARN 7 条。事实层几乎全部准确（31 个源码链接、20 项装配默认值、版本号、模块数、spring.factories 代码块逐一核实无误），主要问题在名称拼写、占位小节和入口职责缺口。

---

## FAIL（事实错误）

### F1. 第 2 行：项目名展开拼写错误 + HTML 实体残留

> 原行（第 2 行）：`K-DDD-Layed-Application&amp;Architect`

- 问题：`Layed` 不是英文单词，canonical 名称在 docs/architecture.md:9 为 `K-DDD-Layered Application & Architect`；且原始字节含 `&amp;` HTML 实体（od -c 验证），纯文本阅读时是脏字符。
- 修法：改为 `K-DDD-Layered-Application&Architect`（用裸 `&`，Markdown 渲染无歧义）。
- 验证方式：`sed -n '2p' README.md | od -c` 确认字节为 `...Application&amp;Architect`；`grep -n "K-DDD" docs/architecture.md` 对照第 9 行。

---

## WARN（表达 / 结构 / 职责）

### W1. 第 27 行：链接标签与目标文件不符

> 原行（第 26-27 行）：
> `- [ErrorCodeI](./kdla-component-model/src/main/java/top/kdla/framework/dto/exception/ErrorCodeI.java)`
> `- [ErrorCodeI](./kdla-component-model/src/main/java/top/kdla/framework/dto/exception/ErrorCode.java)`

- 问题：第二条标签写成 `ErrorCodeI`，实际链接目标是 `ErrorCode.java`（文件存在，链接可点，但标签误导冷读者）。
- 修法：第 27 行标签改为 `[ErrorCode]`。
- 验证方式：`test -f` 两个路径均存在；标签与文件名肉眼比对。

### W2. 第 74-102 行：9 个模块小节是悬空冒号占位

> 原行示例（第 74-75 行）：`## kdla-component-domain` / `业务：`；同类还有第 80-81（日志：）、83-84（校验：）、86-87（响应式编程工具：）、89-90、92-93、95-96、98-99、101-102 行。

- 问题：domain / log / validator / reactor / supplement / supplement-cache / supplement-dingding / supplement-monitor / supplement-job 共 9 个小节只有以冒号结尾的一行描述、无任何内容，形似未完成草稿。
- 修法：每个小节补一句实际职责 + 指向 docs/project-map.md 对应条目，或删掉冒号改为完整陈述句。
- 验证方式：通读 README 第 74-102 行；对照 docs/project-map.md 中同模块已有实质描述。

### W3. 第 17-102 行：逐类链接清单与 docs/project-map.md 职责重叠

> 原行（第 20-56 行）：model / common 两模块共 31 条逐文件链接清单。

- 问题：模块导航与目录结构的 canonical owner 是 docs/project-map.md（README 第 10 行自己声明），README 保留 86 行逐类清单属于串位，且清单只覆盖 2/13 个模块，粒度不一致（common 列到每个 Util，其余模块一行带过）。
- 修法：README 每模块保留一句职责 + 链接到 project-map.md 对应小节，逐类清单移入或删除。
- 验证方式：`head -60 docs/project-map.md` 确认其已含全部 13 模块的目录树与类级说明。

### W4. 全文：入口文档缺"怎么最快开始"

- 问题：README 角色是入口（这是什么、适合谁、怎么最快开始），但全文没有 Maven 坐标（`top.kdla.framework:kdla-components:1.0.1-SNAPSHOT`）、没有依赖引入示例、没有 settings.xml server id 提示——这些散落在 docs/project-map.md:9-16。冷读者无法从 README 直接完成第一步接入。
- 修法：文档导航下方加 3-5 行"快速开始"：坐标 + `<dependency>` 片段 + 私服 server id（`kdla-nexus`），或至少一行链接直指 project-map.md 的项目信息表。
- 验证方式：`grep -n "dependency\|坐标\|快速开始" README.md` 无命中；坐标本身对照 pom.xml:7-9 核实为真。

### W5. 第 30 行：中英文空格缺失 + 类名小写

> 原行（第 30 行）：`通用工具：工具类，help类，stopwatch，mdc`

- 问题：`help类` 中英文之间无空格；`stopwatch`/`mdc` 指代的是 `StopWatchWrapper`/`MdcDot`，小写易被误读为泛指概念而非本仓类。
- 修法：改为 `通用工具：工具类、help 类、StopWatch、MDC`。
- 验证方式：肉眼比对第 32-33 行链接目标类名。

### W6. 第 78 行：技术名大小写不规范

> 原行（第 78 行）：`持久化：ES Mybatis`

- 问题：官方写法是 MyBatis；`ES` 未展开，冷读者需自行猜测 Elasticsearch。
- 修法：改为 `持久化：Elasticsearch、MyBatis`。
- 验证方式：`ls kdla-component-dal/src/main/java/top/kdla/framework/infra/dal/` 确认 es、mybatis 两个子包存在（内容声明本身为真）。

### W7. 第 59 行：spring.factories 代码块无模块限定

> 原行（第 58-59 行）：`***` / `spring.factories `（行尾含空格）

- 问题：该代码块紧跟 common 模块的类清单之后、以 `***` 分隔，未写明这是 kdla-component-common 的 spring.factories；冷读者可能误以为是全仓汇总（实际全仓有 9 个模块各有一份 spring.factories）。
- 修法：改为 `kdla-component-common/src/main/resources/META-INF/spring.factories：` 并去掉行尾空格。
- 验证方式：`cat kdla-component-common/src/main/resources/META-INF/spring.factories` 确认代码块内容与 common 模块文件逐字节一致；`find ... -name spring.factories` 确认全仓共 9 份。

---

## 已核实为真的事实（全部通过）

| # | README 声明 | 行号 | 验证方式 | 结果 |
|---|---|---|---|---|
| 1 | 13 模块 Maven 框架库 | 4 | `grep -n "<module>" pom.xml`（13 条，pom.xml:11-23）+ 根目录 ls 13 个模块目录 | ✅ |
| 2 | 发布到 nexus.enncloud.cn | 4 | `grep -n "enncloud" pom.xml`（pom.xml:98,103,111,116 distributionManagement） | ✅ |
| 3 | Java 8 编译目标 | 4 | pom.xml:35 `<java.version>1.8</java.version>` | ✅ |
| 4 | Spring Boot 2.7.18 | 4 | pom.xml:48 `<spring.boot.version>2.7.18</spring.boot.version>` | ✅ |
| 5 | Spring Cloud 2021.0.9 | 4 | pom.xml:49 `<spring.cloud.version>2021.0.9</spring.cloud.version>` | ✅ |
| 6 | 4 个文档链接（CHANGELOG.md、docs/architecture.md、docs/project-map.md、docs/nit-inventory.md） | 8-11 | `ls` 逐一确认存在 | ✅ |
| 7 | CHANGELOG 含 2026-11-16 R→T backport 块 + BREAKING 清单 | 8 | `grep -nE "2026-11-16\|backport\|BREAKING" CHANGELOG.md`（CHANGELOG.md:6,11,19） | ✅ |
| 8 | 另一裁剪版仓库 9 模块、共享 GAV `top.kdla.framework:*:1.0.1-SNAPSHOT` | 13 | CHANGELOG.md:6-7 同声明；GAV 对照 pom.xml:7-9（groupId=top.kdla.framework, artifactId=kdla-components, version=1.0.1-SNAPSHOT） | ✅（R 仓侧见"无法验证"） |
| 9 | "本仓多数默认开" | 13 | 装配矩阵清点：12 开 / 8 关 | ✅ |
| 10 | 速查"开"12 项：MDC / StopWatch / CatchLog / 异常拦截 / Validator / Domain / Redisson / Vertx / VertxHttp / FieldValueFind / 钉钉告警 / Prometheus | 15 | 逐一对照源码 `@ConditionalOnProperty(matchIfMissing=true)`：MdcAutoConfigure.java:20-21、StopWatchAutoConfigure.java:26-27、CatchLogAutoConfigure.java:26-27、UnifiedExceptionControllerAdvice.java:57-58、ValidatorAutoConfigure.java:19、RedissonAutoConfigure.java:38、VertxConfigure.java:22、VertxHttpConfigure.java:26、FieldValueFindConfigure.java:15；Domain/Prometheus 无属性开关无条件注册（DomainAutoConfigure.java、PrometheusAutoConfigure.java 通读）；钉钉 DingAlertConfigure 仅 @ConditionalOnClass(Vertx)，DingAlertProperties.java:28 `enable = true` | ✅ 12/12 |
| 11 | 速查"关"8 项：RequestLog / LogInterceptor / WebFilter / Cache / RestHttp / MQTT / CORS / 序列号 | 15 | matchIfMissing=false：RequestLogAutoConfigure.java:20-21、LogInterceptorAutoConfigure.java:22-23、WebFilterAutoConfigure.java:23-24、CacheAutoConfigure.java:41、RestHttpConfigure.java:29、VertxMqttConfigure.java:36、CustomCorsConfig.java:18；序列号 SequenceNoConfigure.java `@ConditionalOnExpression` 三键均 `:false` 默认 | ✅ 8/8 |
| 12 | 速查与 canonical 装配矩阵一致 | 15 | docs/architecture.md:114-121 装配矩阵、123-141 配置键速查逐项比对，无冲突；README 保持"速查+链接"未复制矩阵全表 | ✅ |
| 13 | 31 个源码相对链接（model 8 条 + common 23 条） | 20-56 | bash `test -f` 循环逐一检查，0 个 MISSING | ✅ 31/31 |
| 14 | common spring.factories 代码块（MdcAutoConfigure + StopWatchAutoConfigure） | 60-64 | `cat kdla-component-common/src/main/resources/META-INF/spring.factories` 逐字节一致 | ✅ |
| 15 | exception 模块 5 个类：BizException / LockFailException / SysException / UnifiedExceptionControllerAdvice / ResponseHandler | 68-72 | `ls kdla-component-exception/src/main/java/top/kdla/framework/exception/` 全部存在 | ✅ |
| 16 | model 模块"实体类，枚举类，异常类" | 18 | `ls .../dto/`：BaseModel（实体基类）、IEnum/EnumResponse（枚举）、exception/ 子包 | ✅ |
| 17 | dal 模块"持久化：ES Mybatis" | 78 | `ls .../infra/dal/`：es、mybatis 两个子包 | ✅（写法问题见 W6） |
| 18 | 13 个模块小节标题与 pom modules 一一对应 | 17-102 | 小节名与 pom.xml:11-23 的 13 个 `<module>` 逐一比对 | ✅ |
| 19 | 仓库健康佐证（README 未声明，核查过程引用） | — | /tmp/kdla-full-test.log：模块汇总行 awk 求和 = 173 用例，0 Failures 0 Errors，BUILD SUCCESS（第 2372 行） | ✅ |

---

## 无法验证的声明

| 声明 | 行号 | 原因 |
|---|---|---|
| "发布到公司内部 Nexus 供下游应用依赖"中的"供下游应用依赖"（实际有下游在用） | 4 | 下游使用情况超出本仓范围，仓内只能证实 distributionManagement 配置存在（pom.xml:98-116），无法证实实际发布与被依赖。 |
| "同名配置键默认值语义相反"（R 仓侧默认关） | 13 | 裁剪版参考仓 R 的源码不在本机此仓内，无法独立核对；本仓侧声明（多数默认开）已验证为真，R 仓侧仅有 CHANGELOG.md:7 与 docs/architecture.md:147 的同源转述。 |

---

## 核查覆盖说明

- 事实：README 全部 102 行逐行过；31 个相对链接、4 个文档链接、20 项装配默认值、5 个版本号/坐标类声明、3 段模块内容描述全部对照源码或 pom 核实。
- 结构：装配默认值仅保留速查 + 链接（第 15 行），未复制 architecture.md 矩阵全表，canonical 归属正确；扣分点在模块清单与 project-map.md 重叠（W3）。
- 职责：入口三问中"这是什么"完整，"适合谁"隐含（下游应用开发者），"怎么最快开始"缺失（W4）。
- 表达：无营销语、无 AI 腔套话、无无证据断言；细节问题见 W5/W6/W7。
