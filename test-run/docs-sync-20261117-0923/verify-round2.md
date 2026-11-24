# verify-round2 — scoped 事实核查（仅核查指定改动行）

日期：2026-11-17 ｜ 范围：6 条改动行声明 ｜ 方法：源码/文件/git log 直接比对，未重跑 mvn

## 结论：6 PASS / 0 FAIL

---

## 1. architecture.md「自动配置机制」节 9+3+8 分类 — PASS

验证方式：枚举 9 个模块 `src/main/resources/META-INF/spring.factories` 全部配置类（去重后 20 个，`VertxHttpConfigure` 在 supplement 与 supplement-dingding 两处重复登记同一 FQCN），逐一读取类源码条件注解。

**9 类带开关默认开（`@ConditionalOnProperty ... matchIfMissing = true`）**：
| 类 | 配置键 | 证据位置 |
|---|---|---|
| MdcAutoConfigure | kdla.config.mdc.enabled | common/.../mdc/MdcAutoConfigure.java:20-21 |
| StopWatchAutoConfigure | kdla.config.stop-watch.enabled | common/.../watch/StopWatchAutoConfigure.java:26-27 |
| CatchLogAutoConfigure | kdla.config.catch-log.enabled | log/.../catchlog/CatchLogAutoConfigure.java:26-27 |
| UnifiedExceptionControllerAdvice | kdla.exception.handler.enabled | exception/.../UnifiedExceptionControllerAdvice.java:57-58 |
| ValidatorAutoConfigure | kdla.config.validator.enabled | validator/.../ValidatorAutoConfigure.java:19 |
| RedissonAutoConfigure | kdla.cache.redisson.enabled | supplement-cache/.../RedissonAutoConfigure.java:38 |
| VertxConfigure | kdla.config.vertx.enabled | supplement/.../VertxConfigure.java:22 |
| VertxHttpConfigure | kdla.config.vertx-http.enabled | supplement/.../VertxHttpConfigure.java:26 |
| FieldValueFindConfigure | kdla.config.field-value-find.enabled | supplement/.../FieldValueFindConfigure.java:15 |

**3 类无开关默认开**：
- DomainAutoConfigure：仅 `@Configuration`，无任何条件注解（domain/.../DomainAutoConfigure.java:11-12）→ "无条件注册" 属实。
- DingAlertConfigure：`@ConditionalOnClass(Vertx.class)`，无 property 开关（supplement-dingding/.../DingAlertConfigure.java:23-24）→ 属实。
- PrometheusAutoConfigure：类级仅 `@Configuration`；唯一 Bean 由 `@ConditionalOnClass({CollectorRegistry.class, PushGateway.class})` 守护（supplement-monitor/.../PrometheusAutoConfigure.java:18-23）→ "靠 @ConditionalOnClass 守护" 属实（守护在 Bean 级，与文档表述一致）。

**8 类默认关**：
| 类 | 证据 |
|---|---|
| LogInterceptorAutoConfigure | matchIfMissing = false（LogInterceptorAutoConfigure.java:22-23） |
| RequestLogAutoConfigure | matchIfMissing = false（RequestLogAutoConfigure.java:20-21） |
| WebFilterAutoConfigure | matchIfMissing = false（WebFilterAutoConfigure.java:23-24） |
| CacheAutoConfigure | matchIfMissing = false（CacheAutoConfigure.java:41） |
| SequenceNoConfigure | `@ConditionalOnExpression` 三键均 `:false` 默认（SequenceNoConfigure.java:27-31） |
| CustomCorsConfig | matchIfMissing = false（CustomCorsConfig.java:18） |
| RestHttpConfigure | matchIfMissing = false（RestHttpConfigure.java:29） |
| VertxMqttConfigure | matchIfMissing = false（VertxMqttConfigure.java:36） |

9 + 3 + 8 = 20，与 spring.factories 去重总数吻合，无遗漏无多算。

## 2. architecture.md 已知限制脚注 K2/K4/P1 时间线 — PASS

验证方式：`git log`/`git show` 比对 commit 30490b8 + CHANGELOG.md 相应块 + architecture.md:258 脚注原文。

- architecture.md:258 声明："K2…2026-08-28、K4…2026-09-04 于参考仓修复，本仓经 2026-11-16 backport 回合；P1…2026-11-16 直接在本仓修复"。
- P1：commit `30490b8`，作者日期 **2026-11-16 11:18:57 +0800**，message "machineid init publish id only after setnx success with regression tests"，改动含 `MachineIdHelp.java` + `MachineIdHelpFailPathTest.java` → 与"2026-11-16 直接在本仓修复（注册成功才发布静态字段，失败路径随机回退+ERROR 告警）"一致。
- CHANGELOG.md:73 "**P1 已修（2026-11-16 用户裁定最小修复）**…R 侧未修" → 一致。
- K2：CHANGELOG.md:171 块 "[1.0.1-SNAPSHOT] — 2026-08-28（R 仓回合）"，:177 "`KdlaBizDisLockService` (K2 resolved)" → 与脚注 2026-08-28 参考仓修复一致。
- K4：CHANGELOG.md:143 块 "2026-09-04（R 仓回合）"，:149 "`SequenceNoConfigure` (K4 resolved)" → 与脚注 2026-09-04 一致。
- backport 回合日期：CHANGELOG.md:11 "[1.0.1-SNAPSHOT] — 2026-11-16（R→T backport 回合）" → 一致。

## 3. server ID `maven-releases` / `maven-snapshots` — PASS

验证方式：读根 pom.xml `<distributionManagement>`（pom.xml:94-105）与两处文档原文比对。

- pom.xml:96 `<id>maven-releases</id>`（releases，url nexus.enncloud.cn/repository/releases/）；pom.xml:101 `<id>maven-snapshots</id>`（snapshots，url .../repository/snapshots/）。
- docs/architecture.md:296（FAQ 发布节）："配置 server ID `maven-releases` / `maven-snapshots` 的凭据（与根 pom `distributionManagement` 的 id 一致）" → 逐字吻合。
- docs/project-map.md:15（项目信息表）："settings server id `maven-releases` / `maven-snapshots`" → 逐字吻合。

## 4. project-map.md 目录树 supplement/http 行 `KdlaHttpClient` — PASS

验证方式：`ls kdla-component-supplement/src/main/java/top/kdla/framework/supplement/http/`。

- 实际文件：BaseHttpClient.java、HttpContextHelp.java、**KdlaHttpClient.java**、RestHttpConfigure.java、VertxHttpClient.java、VertxHttpConfigure.java、ssl/。
- docs/project-map.md:93 "http/ HTTP 客户端（Vertx, Netty, KdlaHttpClient）" 中的 `KdlaHttpClient` 与真实文件名逐字一致。

## 5. CHANGELOG 回合概览 "14 张实现票（01-13、17）+ 3 张收口票（14/15/16）" — PASS

验证方式：`ls .scratch/kdla-backport/issues/`，共 17 个票文件，编号 01-17 连续无缺。

- 实现票 14 张 = 01-model-multiresponse、02-common-helpers、03-common-mdc-stopwatch、04-exception-advice、05-log-catchlog、06-log-requestlog-sanitize、07-domain-dal、08-validator、09-supplement-audit、10-supplement-http-mqtt-vertx、11-cache-redis-lock、12-cache-sequence-machineid、13-t-specific-modules（01-13 共 13 张）+ 17-common-crypto（1 张）= 14 ✓。
- 收口票 3 张 = 14-full-build-verify（验证）、15-docs-changelog（文档）、16-manual-verification（人工验证），文件名语义与 CHANGELOG.md:15 括注一一对应 ✓。
- 总数 14+3=17，与目录文件数及 CHANGELOG.md:17 "17 张票" 一致 ✓。

## 6. README.md 第 2 行与 architecture.md 首节拼写一致性 — PASS

验证方式：直接比对两文件原文。

- README.md:2 `K-DDD-Layered Application & Architect`。
- docs/architecture.md 首节「系统概览」第 8 行：`KDLA（K-DDD-Layered Application & Architect）`。
- 展开名逐字符一致（含 `&` 与大小写、连字符）✓。
