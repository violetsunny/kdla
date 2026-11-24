# README.md fresh-context 事实核查报告（round 2）

- 核查对象：/Users/kangll13/aiot/java-code/self/gggggggood/kdla/README.md（98 行）
- 核查时间：2026-11-17
- 约束遵守：未重跑 mvn；测试证据引用 /tmp/kdla-full-test.log（BUILD SUCCESS ×1）
- 结论：**10 PASS / 0 FAIL / 1 WARN**

---

## 1. Maven 坐标示例 — PASS

README:24-28 示例 `top.kdla.framework : kdla-component-common : 1.0.1-SNAPSHOT`。

验证方式：读根 pom.xml 与 kdla-component-common/pom.xml。
- 根 pom.xml:7-9 → groupId `top.kdla.framework`、version `1.0.1-SNAPSHOT`
- kdla-component-common/pom.xml:12 → artifactId `kdla-component-common`；parent GAV（:6-8）与根 pom 一致

三项全部吻合。

## 2. server ID — PASS

README:34 声称 settings.xml server ID 为 `maven-releases` / `maven-snapshots`。

验证方式：读根 pom.xml distributionManagement（:94-105）。
- repository id = `maven-releases`（:96，url nexus.enncloud.cn/repository/releases/）
- snapshotRepository id = `maven-snapshots`（:101，url .../snapshots/）

## 3. yaml 示例键名与嵌套展开 — PASS

验证方式：grep 源码条件注解 + 逐字比对。

- README:41-46 yaml 展开为 `kdla.config.catch-log.enabled=false`。
  源码 CatchLogAutoConfigure.java:26-27（kdla-component-log）：
  `@ConditionalOnProperty(name = "kdla.config.catch-log.enabled", havingValue = "true", matchIfMissing = true)`。
  键名一致；默认开、显式 false 可关，与 README:39「关闭不想要的默认能力」语义一致。
- README:50-54 yaml 展开为 `kdla.sequence.enabled=true`。
  源码 SequenceNoConfigure.java:27-31（kdla-component-supplement-cache）：
  `@ConditionalOnExpression("${top.kdla.framework.supplement.cache.sequence.enable:false} or ${top.kdla.framework.supplement.sequence.enable:false} or ${kdla.sequence.enabled:false}")`。
  `kdla.sequence.enabled` 是三个 OR 键之一，设 true 即生效；默认 `:false` 与 README:48「开启默认关的能力」一致。
  附带核实 README:48 前置条件：`@ConditionalOnBean(RedissonRedDisLock.class)`（:34，对应「Redisson 已装配」）、`CodeGeneratorCfgMapper`（:18/:42，对应 `code_generator_cfg` 表）。

## 4. 模块概览表 — PASS

验证方式：`ls -d kdla-component-*/` 得 13 个真实目录，与 README:64-76 表 13 行逐名比对，全部一致、无缺漏无多余（common/dal/domain/exception/log/model/reactor/supplement/supplement-cache/supplement-dingding/supplement-job/supplement-monitor/validator）。

职责抽查 4 行（对照包结构与类名）：
- **kdla-component-common**（README:66）：utils/ 下 AESUtil、Sm2Util、Sm3Util、Sm4Util（加解密 AES/SM2/SM3/SM4 ✓）；help/ 下 ThreadPoolHelp*、SelfSnowflakeGeneratorHelp（线程池、雪花 ID ✓，测试类名佐证）；aspect/mdc + aspect/watch（MDC 与 StopWatch 切面 ✓）。
- **kdla-component-exception**（README:67）：exception/ 下 BizException、SysException、LockFailException ✓；UnifiedExceptionControllerAdvice（Controller 全局异常拦截 ✓）；ResponseHandler（响应包装 ✓）。
- **kdla-component-supplement-cache**（README:73）：包 cache/（Redis 工具）、lock/（含 RedissonAutoConfigure，分布式锁 ✓）、sequence/（SequenceNoGenerator*，序列号生成 ✓）。
- **kdla-component-dal**（README:71）：infra/dal/es（ES 基类 ✓）、infra/dal/mybatis/common+convertor+util（MyBatis-Plus 基类、分页转换、SQL 过滤 ✓，SqlFilterWhitelistTest/IPageQueryTest 佐证）。

## 5. 「65 测试类 / 173 用例」 — PASS

验证方式：
- `find */src/test/java -name '*Test.java' | wc -l` → **65**，与 README:85 一致。
- /tmp/kdla-full-test.log 中 9 个模块级汇总行 `Tests run:`（排除 per-class 行）：
  6 + 56 + 10 + 8 + 16 + 28 + 7 + 17 + 25 = **173**，与 README:85 一致。
- 日志含 `BUILD SUCCESS`（grep -c = 1）。
- 栈声称 JUnit 5 + Mockito：根 pom.xml:79 mockito-junit-jupiter 4.6.1 ✓。

## 6. 开发命令 — PASS

验证方式：
- `ls /Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home/bin/javac` → 存在，README:83 JAVA_HOME 路径有效。
- README:89 模块名 `kdla-component-supplement-cache` → 目录真实存在（见第 4 条 ls 输出）。
- README:81「系统默认 JDK 21 可直接编译，target 1.8」与根 pom java.version=1.8（:35）一致，且 AGENTS.md 记载已验证通过。

## 7. 装配默认值速查（12 开 / 8 关） — PASS（附 1 WARN）

验证方式：grep 全部 auto-config 类的 @ConditionalOnProperty/@ConditionalOnExpression，抽查远超 6 键（实际核对 16 个有键开关 + 4 个无键装配）：

「默认开」侧 matchIfMissing=true 核实 9 键：
| 键 | 源码位置 | matchIfMissing |
|---|---|---|
| kdla.config.mdc.enabled | MdcAutoConfigure.java:20-21 | true ✓ |
| kdla.config.stop-watch.enabled | StopWatchAutoConfigure.java:26-27 | true ✓ |
| kdla.config.catch-log.enabled | CatchLogAutoConfigure.java:26-27 | true ✓ |
| kdla.exception.handler.enabled | UnifiedExceptionControllerAdvice.java:57-58 | true ✓ |
| kdla.config.validator.enabled | ValidatorAutoConfigure.java:19 | true ✓ |
| kdla.cache.redisson.enabled | RedissonAutoConfigure.java:38 | true ✓ |
| kdla.config.vertx.enabled | VertxConfigure.java:22 | true ✓ |
| kdla.config.vertx-http.enabled | VertxHttpConfigure.java:26 | true ✓ |
| kdla.config.field-value-find.enabled | FieldValueFindConfigure.java:15 | true ✓ |

「默认关」侧 matchIfMissing=false / 表达式默认 false 核实 8 键：
| 键 | 源码位置 | 默认 |
|---|---|---|
| kdla.config.request-log.enabled | RequestLogAutoConfigure.java:20-21 | false ✓ |
| kdla.config.log-interceptor.enabled | LogInterceptorAutoConfigure.java:22-23 | false ✓ |
| kdla.config.web-filter.enabled | WebFilterAutoConfigure.java:23-24 | false ✓ |
| kdla.cache.enabled | CacheAutoConfigure.java:41 | false ✓ |
| kdla.config.rest-http.enabled | RestHttpConfigure.java:29 | false ✓ |
| kdla.config.mqtt.enabled | VertxMqttConfigure.java:36 | false ✓ |
| kdla.config.cors.enabled | CustomCorsConfig.java:18 | false ✓ |
| kdla.sequence.enabled | SequenceNoConfigure.java:30 | `:false` ✓ |

**WARN**：速查中「Domain / 钉钉告警 / Prometheus 默认开」三项无对应条件键——DomainAutoConfigure、DingAlertConfigure、PrometheusAutoConfigure 经各自 spring.factories 无条件注册（grep 三个模块 src/main/java 无任何 @ConditionalOnProperty/@ConditionalOnExpression）。「默认开」语义成立（引入即装配），但不存在 `enabled=false` 开关可关，速查表读者可能误以为可关。建议在 docs/architecture.md 配置键速查中注明「无开关」，README 本身不算事实错误。

## 8. 版本线表 — PASS

验证方式：
- `git branch --show-current` → `1.0.1-SNAPSHOT`，与 README:95「当前」一致。
- 根 pom.xml:35 java.version=`1.8`、:48 spring.boot.version=`2.7.18`、:49 spring.cloud.version=`2021.0.9`，与 README:6 技术基线一致。
- `git branch -a | grep 2.0.0` → 本地 `2.0.0-SNAPSHOT` + `remotes/origin/2.0.0-SNAPSHOT` 均存在。
- 附加核实（git show，未 checkout）：`git show 2.0.0-SNAPSHOT:pom.xml` → version `2.0.0-SNAPSHOT`、java.version `21`、spring.boot.version `3.4.0`，README:95 括号内容准确。
- README:96「版本号硬编码在根 pom 与 13 个模块 pom 的 parent」：模块数 13 已核实（第 4 条）；common pom parent 硬编码 version 已核实（第 1 条）。

## 9. 链接完整性 — PASS

验证方式：`ls` 逐一核实文档导航 4 个链接目标：
- ./CHANGELOG.md → 存在
- ./docs/architecture.md → 存在
- ./docs/project-map.md → 存在
- ./docs/nit-inventory.md → 存在

正文其余链接（README:17/48/56/60 指向 docs/architecture.md、docs/project-map.md）目标同上，均存在。

## 10. 表达检查 — PASS

验证方式：grep 禁用词清单（开创性/赋能/彰显/至关重要/蓬勃发展/令人叹为观止/值得注意的是/综上所述/随着…的发展/不是…而是/强大的/极致/一站式/保驾护航）→ 0 命中。

- 营销语：无。首段「统一的技术支撑能力」为功能描述，与 docs/architecture.md:8 canonical 表述同源。
- AI 腔：无三段式排比、无结尾金句、无模糊归因。
- 中英文空格：抽查全文（「13 模块 Maven 库」「Spring Boot 2.7.x / Java 8」「GAV `top.kdla.framework:*:1.0.1-SNAPSHOT`」等）间距一致规范。
- README:2 tagline `K-DDD-Layered Application & Architect` 与 docs/architecture.md:8 canonical 名称逐字一致（round 1 报告的 `Layed`/`&amp;` 问题已修复）。

---

## 汇总

| # | 条目 | 结果 |
|---|---|---|
| 1 | Maven 坐标 | PASS |
| 2 | server ID | PASS |
| 3 | yaml 键名与展开 | PASS |
| 4 | 模块表 13 行 + 职责抽查 4 行 | PASS |
| 5 | 65 测试类 / 173 用例 | PASS |
| 6 | 开发命令（JAVA_HOME / -pl 模块名） | PASS |
| 7 | 默认值速查 12 开 / 8 关 | PASS + WARN |
| 8 | 版本线表 | PASS |
| 9 | 链接完整性 | PASS |
| 10 | 表达 | PASS |

**10 PASS / 0 FAIL / 1 WARN**

WARN 明细：速查中 Domain / 钉钉告警 / Prometheus 三项为无条件装配（无 enabled 开关），「默认开」语义成立但不可通过配置关闭，建议在 docs/architecture.md 注明「无开关」。
