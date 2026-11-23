# Changelog

All notable changes to `kdla-components` are documented here.

> 本仓（T）发布到 enncloud Nexus（`nexus.enncloud.cn`），13 模块全面版。
> 2026-11-16 完成 R→T backport：将参考仓 R（裁剪版 9 模块）的 104 项修复 + 64 测试类回合至本仓。
> ⚠️ 两仓共享 GAV `top.kdla.framework:*:1.0.1-SNAPSHOT`，同名配置键默认值语义相反（见 2026-11-16 块"装配契约"），宿主务必确认构件来源。

---

## [1.0.1-SNAPSHOT] — 2026-11-16（R→T backport 回合）

### 回合概览

- 14 张实现票（01-13、17）+ 3 张收口票（14 验证 / 15 文档 / 16 人工验证），16 个 commit（fe53887..票14 修复），全仓 `mvn clean test` **171 用例全绿**（64 测试类，JDK8 target）。
- 覆盖模块：model / common / exception / domain / dal / validator / log / supplement / supplement-cache / supplement-dingding / supplement-monitor（reactor、supplement-job 零改动）。
- 明细单一真源：`.scratch/kdla-backport/`（spec、17 张票、装配矩阵、评审登记 P1-P20）。

### BREAKING CHANGES（T 采纳，下游升级必读）

- `BaseAssert`：`isBlank`×3 重命名为 `notBlank`（语义反转修正）；`isNull(Object,String,String)` 删除（与 `notNull` 签名冲突且零调用）。无 @Deprecated 桥接，下游升级即 `NoSuchMethodError`。
- `EnumValid`：null 值校验结果由 FALSE 改为 TRUE（对齐 JSR-303）；`IEnumValid`：`checkForNull` 默认值改为 false。依赖旧默认"null 拒绝"的可选字段校验被放宽。
- `Sm4Util`：加密由 ECB 改 CBC + 随机 IV，输出 `v1:` 前缀（对齐 AESUtil 约定）；旧 ECB 密文走兼容分支可解，**新密文旧代码不可解**；密钥文本编码统一 Base64，**存量 Hex 编码密钥不可读**（byte[] 重载可绕过）。旧 `generateKey`（低熵）标 `@Deprecated`，新增 `generateSecureKey`。
- `Sm2Util`：新增 `generateSecureKeyPair` 真随机 API，旧 `generateKeyPair(keySrc)` 标 `@Deprecated`。
- `AESUtil`：加密输出 `v1:Base64(IV + ciphertext)`（随机 IV，替代零 IV）；新密文旧代码不可解，旧密文兼容可解。
- `VertxHttpConfigure`：SSL trust-all 由硬编码 true 改为 `kdla.http.ssl.trust-all`，**默认 false**（自签证书端点需显式开启，开启时 log.warn MITM 风险）；新增 `kdla.http.request-timeout` 默认 30s（原先无超时，>30s 慢调用需调大）。
- `RedissonConfigProperties`/`RedissonLockFactory`：地址解析 fail-fast——空/非法节点启动即抛 `IllegalArgumentException`（配置错误从静默零节点变为启动失败）。
- `DistributeLockedAspect`：旧实现 getLock 后不真正加锁（注解形同虚设恒放行），现 `tryLock(0,-1,MS)` 真加锁，锁失败抛 `LockFailException`；SpEL 上下文改 `SimpleEvaluationContext.forReadOnlyDataBinding()`（防注入），**锁 key 表达式中的方法调用/bean 引用（如 `#args[0].getKey()`）不再支持**，仅纯属性访问可用。
- `SequenceNoGenerator`/V2：溢出、长度段非数字改抛 `BizException`（原静默清零重复发号/吞异常）。
- `MachineIdHelp`：注册改 SETNX 原子操作（修多实例抢同一 ID 竞态）；destroy 由 SETNX 覆盖改 `del`（消除 1 秒窗口）；随机回退取号升 ERROR 告警。
- `SqlFilter`：排序列黑名单改白名单正则 `^[a-zA-Z0-9_]+$`（≤64 字符）。**表限定排序列（如 `t.create_time`）由可用变为抛 `BizException`**。
- `IPageQuery`：limit 封顶 500（静默钳制，大分页请求被截断）。
- `UnifiedExceptionControllerAdvice`：认证 handler 由误绑 `javax.security.sasl.AuthenticationException` 改为 spring-security 的 `AuthenticationException`（认证失败正确返回 401 而非"系统繁忙"）；`DuplicateKeyException` 正确绑定 Spring DAO 异常（原绑 snakeyaml 同名类永不命中）。
- `CatchLogAspect`：仅 Response 可赋值返回类型转失败响应；`Error` 一律 rethrow（修事务不回滚）；未知异常不再外传 `e.getMessage()`（防 SQL/内部路径泄漏）。
- `MultiResponse`：新增 `totalCount` 字段并在 `buildSuccess(items,totalCount)` 回填（原静默丢弃参数）；JSON 出参新增字段，反序列化向前兼容。
- `ObjectValidator`：删 `sun.misc.Unsafe` 依赖 + 深度 16/IdentityHashMap 防环。
- `CommonValidator`：删 `Locale.setDefault` 全局副作用。
- `ThreadPoolHelp`：默认池由共享无界队列改独立有界队列。
- `SpringRedisHelp`：23 处 `e.printStackTrace()` 改 `log.error`；`unLock` Lua 脚本 key/value 改经 RedisTemplate 序列化器（原裸 bytes 与加锁序列化不一致致解锁永远静默失败）。

### 否决项（T 与 R 行为差异）

- **B1 否决**：`Response` **保留 `success` 字段**（R 已删除、`isSuccess()` 由 code 派生）。T 序列化输出含 `success`，与 R 构件不一致；两仓共享 GAV，下游切换构件来源时注意反序列化差异。

### 装配契约（T 与 R 偏离，矩阵详见 .scratch/kdla-backport/investigation.md 第 11 节）

R 侧 15 个配置类全部默认关；**T 侧 9 类默认开**（T 现状已开启，默认关=下游静默失效）+ 3 类独立无副作用默认开，6 类有运行期副作用默认关：

| 默认 | 配置类（开关键） |
|---|---|
| **开** | MdcAutoConfigure（`kdla.config.mdc.enabled`）、StopWatchAutoConfigure（`kdla.config.stop-watch.enabled`）、CatchLogAutoConfigure（`kdla.config.catch-log.enabled`）、UnifiedExceptionControllerAdvice（`kdla.exception.handler.enabled`）、ValidatorAutoConfigure（`kdla.config.validator.enabled`）、DomainAutoConfigure（无开关）、RedissonAutoConfigure（`kdla.cache.redisson.enabled`）、VertxConfigure（`kdla.config.vertx.enabled`）、VertxHttpConfigure（`kdla.config.vertx-http.enabled`）、FieldValueFindConfigure（`kdla.config.field-value-find.enabled`）、DingAlertConfigure（无开关，OnClass(Vertx)）、PrometheusAutoConfigure（无开关，OnClass(prometheus)） |
| **关** | RequestLogAutoConfigure（`kdla.config.request-log.enabled`）、LogInterceptorAutoConfigure（`kdla.config.log-interceptor.enabled`）、WebFilterAutoConfigure（`kdla.config.web-filter.enabled`）、CacheAutoConfigure（`kdla.cache.enabled`）、RestHttpConfigure（`kdla.config.rest-http.enabled`）、VertxMqttConfigure（`kdla.config.mqtt.enabled`）、CustomCorsConfig（`kdla.config.cors.enabled`）、SequenceNoConfigure（三键 OR，见下） |

- ⚠️ **同名键两仓语义相反**：`kdla.config.mdc.enabled`、`kdla.config.stop-watch.enabled`、`kdla.config.catch-log.enabled`、`kdla.cache.redisson.enabled`、`kdla.config.vertx-http.enabled` 等在 T `matchIfMissing=true`（默认开）、在 R `false`（默认关）。两仓共享 GAV，宿主 ~/.m2 可能解析到另一仓构件——**行为完全相反**，排障时先确认构件来源（`mvn dependency:tree` + 仓库地址）。
- SequenceNoConfigure：`@ConditionalOnExpression` 三键任一为 true 即启用（`kdla.sequence.enabled` / `top.kdla.framework.supplement.cache.sequence.enable` / `top.kdla.framework.supplement.sequence.enable`），且联动 `@ConditionalOnBean(RedissonRedDisLock)` + `@AutoConfigureAfter(RedissonAutoConfigure)`——Redisson 装配才装配。
- 恒真 `@ConditionalOnClass`（引用自身模块内部类）全部修正为真实外部类：Mdc/StopWatch/CatchLog/RequestLog→`Aspect`、Redisson→`RedissonClient`、DingAlert→`Vertx`、Prometheus→`CollectorRegistry`+`PushGateway`。

### 下游需知会事项

1. **异常响应格式自动生效**：exception advice 默认开（SERVLET 应用），未处理异常统一转 `Response` 兜底文案；不想要需显式 `kdla.exception.handler.enabled=false`。
2. **自签证书需显式 trust-all**：Vertx HTTPS 客户端默认校验证书链，内网自签端点需 `kdla.http.ssl.trust-all=true`（有 MITM 风险告警）。
3. **>30s 慢调用需调 timeout**：`kdla.http.request-timeout` 默认 30000ms，超时 future 完成为失败。
4. **配置错误 fail-fast**：Redisson 地址空/非法启动即失败，不再静默产出零节点 client。
5. **分布式锁真正生效**：`@DistributeLocked` 从恒放行变为真加锁，锁竞争失败抛 `LockFailException`；存量 SpEL 锁 key 含方法调用的必须改写为纯属性访问。

### 收口修复（票 14 评审发现，本次回合引入/放大）

- `CacheAutoConfigure`：补 `@ConditionalOnBean(RedisTemplate)` + `@AutoConfigureAfter(RedisAutoConfiguration)`（无 RedisTemplate 不再装配炸启动）；`caffeineCacheManager` 改 `ObjectProvider`（宿主无 KdlaCafCache bean 时不再 UnsatisfiedDependencyException）。
- `DistributeLockedAspect` @Bean 补 `@ConditionalOnClass(Aspect)` 守护。
- `UnifiedExceptionControllerAdvice`：`DuplicateKeyException` handler 移入 `@ConditionalOnClass` 守护的内层 advice（spring-jdbc provided 不传递，纯 web 宿主不再 NoClassDefFoundError 启动炸）。
- 历史 commit 误入的 20 个 IDE 噪音文件（.classpath/.project/.settings）已从索引移除，.gitignore 加固。
- R 上游同源遗留 20 项登记 `docs/nit-inventory.md` P1-P20（只登记不修，保两仓 diff 可核验）。
- **P1 已修（2026-11-16 用户裁定最小修复）**：`MachineIdHelp.init` 改为 SETNX 注册成功才发布静态 `machineId`/`dataCenterId`，失败路径（Redis 异常/1024 槽耗尽）字段保持 null → `getGenerator` 判空命中走随机回退+ERROR 告警，消除脏值静默撞号；槽耗尽补 ERROR 日志；+2 回归测试（`MachineIdHelpFailPathTest`）。R 侧未修，两仓该类出现 diff。

---

## [1.0.1-SNAPSHOT] — 2026-11-13（R 仓审计切片 1-4，补登）

> R 仓 CHANGELOG 缺失本块（文档滞后于 git），按 R git 提交记录补登。T 回合时四切片修复已全部包含。

### 切片 1 — supplement-cache 6 项（R commit: 0209b43）

- `RedissonLockFactory`/`RedissonConfigProperties`：去除 `config.toYAML()` 密码明文 INFO 日志，改记 address+codec+threads 非敏感项。
- `MachineIdHelp`：随机回退取号 INFO 升 ERROR 显式告警撞号风险；localIp 字段赋值修复日志恒 null。
- `SpringRedisHelp`：`unLock` Lua 脚本 key/value 改经 RedisTemplate 序列化器（修解锁永远静默失败）；`lock()` 的 `setIfAbsent` 返回值 `Boolean.TRUE.equals` 防拆箱 NPE。
- `KdlaCafCache`：构建由 `expireAfterWrite` 改 `expireAfter(Expiry)`，`put(key,value,duration)` 的 per-key TTL 真实生效（原恒空 Optional 静默忽略）。
- `SequenceNoGeneratorV2.generateSequenceNo`：补 cfg 判空抛 `BizException`。

### 切片 2 — exception + log 4 项（R commit: 89fc933）

- `UnifiedExceptionControllerAdvice`：认证 handler 改绑 spring-security `AuthenticationException` 并移入 `@ConditionalOnClass` 隔离内层 Advice（原误绑 SASL 异常，认证失败落兜底）。
- `LogInterceptor.printHttpRequestHeaderInfo`：不再 new wrapper 消费原始输入流（修 controller 读空 body），仅当请求已被缓存时打印 body。
- `LogInterceptor.afterCompletion`：BEGIN_TIMESTAMP 判空防 preHandle 未执行路径拆箱 NPE。
- `CatchLogAspect`：未知异常不再外传 `e.getMessage()`（SQL/内部路径泄漏），改通用文案。

### 切片 3 — common 4 项密码学与正确性（R commit: b9695b2）

- **[BREAKING-兼容分支]** `Sm4Util`：ECB 改 CBC + 随机 IV，输出 `v1:` 前缀；旧 ECB 密文走兼容分支；新增 `generateSecureKey`，旧 `generateKey` 标 `@Deprecated`。
- `Sm2Util`：新增 `generateSecureKeyPair`，旧 `generateKeyPair(keySrc)` 标 `@Deprecated`（实测同 keySrc 两次生成私钥并不相同，文档宣称的确定性不成立）。
- `BytesUtil.reverse(data,offset,length)`：修复交换索引未加 offset 的错位（offset>0 必错且可能越界），越界显式抛 `IndexOutOfBoundsException`。
- `LocalIpHelp.getLocalIp4Address`：加 static volatile 缓存（traceId 生成热路径不再每请求枚举网卡）。

### 切片 4 — model 1 项（R commit: e3a4acd）

- `MultiResponse.buildSuccess(items,totalCount)`：原实现静默丢弃 totalCount 参数；新增 `totalCount` 字段并回填，单参重载默认 `items.size()` 行为不变。

---

## [1.0.1-SNAPSHOT] — 2026-10-29（R 仓回合）

> 以下按 R 仓原文收录；与 T 实际不符的条目已内联标注 **[T 适配]**。

### Assembly contract fix — R commit: 1fa3289

- 15 个未注册配置类补 `spring.factories` 注册并统一条件装配（真实 `@ConditionalOnClass` + `@ConditionalOnProperty` 开关默认关 + `@ConditionalOnMissingBean`）。**[T 适配]** T 侧 9 类默认开（见 2026-11-16 块装配契约），非 R 的全默认关。
- `RedissonAutoConfigure`：新增 `kdla.cache.redisson.enabled` 开关（R 默认关；**[T 适配]** T 默认开）；`@ConditionalOnClass` 改真实 `RedissonClient`；`disLockService`/`distributeLockFactory` 补 COBM；锁前缀 appId 缺失回落固定值 `kdla`（修多实例互斥失效）；注册 `DistributeLockedAspect`。
- exception：恢复 `UnifiedExceptionControllerAdvice` 自动注册（默认开，用户裁定）+ `@Order(Ordered.LOWEST_PRECEDENCE)`。
- supplement-cache：补注册 `CacheAutoConfigure`（`@EnableCaching` 隔离内部类 + `kdla.cache.enabled` 开关）与 `SequenceNoConfigure`（真实 OnClass + `@AutoConfigureAfter` + `@ConditionalOnBean`，K4 闭环）。
- log：补注册 `LogInterceptorAutoConfigure`/`RequestLogAutoConfigure`/`WebFilterAutoConfigure` 并加 web 条件与开关；supplement：新建 spring.factories 注册 6 个 Configure；validator：新建 `ValidatorAutoConfigure`。
- `FieldValueFindHelp`/`CommonValidator`/`DistributeLockedAspect` 移除 `@Component` 立体注解，消除双定义；删除整文件注释残留 `RedissonClientConfig`。
- 恒真 `@ConditionalOnClass`（引用自身模块类，永远为真）修正 4 处：`MdcAutoConfigure`/`StopWatchAutoConfigure`/`CatchLogAutoConfigure`/`RequestLogAutoConfigure` 改引用 `org.aspectj.lang.annotation.Aspect`。

### Bug fixes — R commits: d7d7bfe, 7d86361, 9e4d29c, 32ba8d2

- **Critical 14 项**（25 个回归测试）：`BigDecimalHelp` 20 进制转十进制、`KdlaStringHelp` 驼峰 contains 方向、`BytesUtil` floatToLe 字节序、`ThreadPoolHelp` 默认池共享无界队列改独立有界队列、`Sm4Util` 密钥编码统一 Base64、`CatchLogAspect` JSON 解析降级 + 仅 Response 返回类型转失败响应且 Error 一律 rethrow（修事务不回滚）、`KdlaHttpServletRequestWrapper` body 空兜底 + UTF-8、`SqlFilter` 黑名单改白名单正则、`SequenceNoGenerator`/V2 删双锁单解死锁 + `isHeldByCurrentThread` 守卫、`MachineIdHelp` SETNX 原子注册修雪花 ID 重复竞态、`SequenceNoGenerator` 溢出改抛 `BizException`、MQTT QoS2 ByteBuf retain/release 配对修 use-after-free。
- **必改 54 项**（116 个回归测试）：common/model/exception 23 项（MDC 保存-恢复替代 clear、`AESUtil` 异常消息删 password 明文、`ThreadPoolHelp` 拆箱 NPE 等）+ log/dal/domain/validator 18 项（`LogSanitizeUtil` 敏感 Header 脱敏 + 2KB 截断、`ObjectValidator` 删 sun.misc.Unsafe + 防环、`IPageQuery` limit 封顶 500 等）+ supplement/supplement-cache 13 项（`NettyHttpServer` bind 失败资源释放、`KdlaExcelWrite` try/finally finish、`MachineIdHelp` destroy 改 del 等）。

### BREAKING CHANGES（R 侧清单）

- `Response`：删除 `success` 字段，`isSuccess()` 由 `code` 派生为唯一真源。**[T 适配：B1 否决，T 保留 `success` 字段，本条不适用于 T]**
- `BaseAssert`：`isBlank` 重命名为 `notBlank`（语义反转修正）；`isNull` 删除（与 `notNull` 签名冲突且零调用）。
- `EnumValid`：null 值校验结果由 FALSE 改为 TRUE（对齐 JSR-303 语义）。
- `IEnumValid`：`checkForNull` 默认值改为 false。
- `VertxHttpConfigure`：SSL trust-all 由硬编码 true 改为 `kdla.http.ssl.trust-all` 配置，默认 false（开启时 log.warn MITM 风险）。
- Vert.x HTTP 客户端：新增 `kdla.http.request-timeout`，默认 30s（原先无超时）。
- `RedissonConfigProperties`/`RedissonLockFactory`：地址解析 fail-fast——空/非法节点启动即抛 `IllegalArgumentException`，不再静默产出零节点 client。
- `RedissonAutoConfigure`：装配默认改为关闭，需显式 `kdla.cache.redisson.enabled=true` 启用。**[T 适配：T 默认开]**
- `SequenceNoGenerator`：溢出、长度段非数字改抛 `BizException`（原静默清零重复发号/吞异常）。
- `MachineIdHelp`：destroy 由 SETNX 覆盖改为 `del`，消除 1 秒 SETNX 窗口。

---

## [1.0.1-SNAPSHOT] — 2026-09-04（R 仓回合）

### Bug Fixes — R commit: 9cdbe79

#### Behavior changes (may affect downstream)

**`SequenceNoConfigure` (K4 resolved)**
- 修复配置回归：原 `@ConditionalOnProperty` 配置 3 个 name 时，Spring Boot 语义要求**全部**为 true 才装配。现改用 `@ConditionalOnExpression` OR 逻辑，任一配置为 true 即启用序列号模块。

**`AESUtil`**
- 加密输出新增 `v1:` 版本前缀，格式为 `v1:Base64(IV + ciphertext)`。解密时按前缀区分新旧格式，彻底消除旧密文误判风险（原方案约 1/256 概率静默返回乱码）。
- **注意**：新加密的密文无法被旧版本 `decrypt` 方法解密。

**`SpringRedisHelp`**
- 恢复被删除的 `getExpire(String key)` 方法并标记 `@Deprecated`，建议改用 `RedisTemplate#getExpire(Object, TimeUnit)` 直接调用。

#### Non-breaking fixes

| Component | Fix |
|-----------|-----|
| `NettyHttpServer` | bind 异常时关闭 EventLoopGroup 防资源泄漏 |
| `DBLengthValid` / `EnumValid` | 日志从 JUL 改用 SLF4J，validator 模块新增 `slf4j-api` provided 依赖 |
| `DistributeLockedAspect` | 补充行为变更 Javadoc（旧代码从未真正加锁，新代码使用 `tryLock(0, -1, MS)`） |
| `AsyncThreadHelp` | 补充 `initExecutorService` 线程池重建语义 Javadoc |
| `SequenceNoGeneratorV2` | 补充非缓存模式 DB 写入成本 Javadoc |

---

## [1.0.1-SNAPSHOT] — 2026-08-28（R 仓回合）

### Bug Fixes — R commit: bb8022f

#### Behavior changes (may affect downstream)

**`KdlaBizDisLockService` (K2 resolved)**
- Removed `synchronized` from `biz()` method. Lock is now acquired via `redDisLock.lock(lockKey, TimeUnit.MILLISECONDS, waitTimeMs, leaseTimeMs)` with configurable timeout from `KdlaLockProperties`, replacing the infinite-wait `tryLock()` that risked deadlocks.
- `unlock()` in `finally` is now guarded by `locked && lock.isHeldByCurrentThread()`, eliminating `IllegalMonitorStateException` when lock acquisition failed.

**`AESUtil`**
- Encryption now uses a random IV (`SecureRandom`) prepended to the ciphertext, output as `Base64(IV + ciphertext)`. Previously used a zero IV, which was insecure for CBC mode.
- Decryption is backward-compatible: tries random-IV format first, then falls back to zero-IV for legacy ciphertext.

**`DistributeLockedAspect`**
- Now uses `tryLock(0, -1, TimeUnit.MILLISECONDS)` (non-blocking, auto-lease) instead of `tryLock()` (infinite wait).
- `unlock()` guarded by `locked && lock.isHeldByCurrentThread()`.

**`MqttPingHandler`**
- Fixed heartbeat timeout judgment inversion — was triggering on valid heartbeats and ignoring actual timeouts.

**`NettyHttpServer`**
- Fixed startup-then-immediately-close issue — server no longer shuts down right after binding.

#### Non-breaking fixes

| Component | Fix |
|-----------|-----|
| `MdcAspect` | Null safety for MDC context values |
| `CatchLogAspect` | Null safety improvements |
| `KdlaBodyReaderRequestFilter` | MDC cleaned in `finally` block to prevent thread-local leakage |
| `FileChunkTransmitHelp` | Switched to try-with-resources for stream management |
| `SslContextFactory` | Switched to try-with-resources for resource cleanup |
| `AsyncThreadHelp` | Restores thread interrupt status after `awaitTermination` |
| `SequenceNoGenerator` | Null safety improvements |
| `MachineIdHelp` | Null safety improvements |
| `FieldValueFindHelp` | Fixed `break` misused as `continue` in field value iteration |
| `RedissonConfigProperties` | Added password count validation for REPLICATE mode |
| `NettyHttpClient` | Resource cleanup improvements |
| `RequestLogAspect` | Refactored for reliability and null safety |
| `KdlaCafCache` / `KdlaGcache` | Null safety improvements |
| `EnumValid` / `DBLengthValid` | Null safety improvements |
| `CsvUtil` | Null safety improvements |
| `HttpContextHelp` | Null safety improvements |

---

## [1.0.1-SNAPSHOT] — 2026-07-10（R 仓回合）

### Bug Fixes — change: fix-project-wide-bugs

#### Behavior changes (may affect downstream)

**`DistributeLockedAspect` (`@DistributeLocked` annotation)**
- Business exceptions thrown by the annotated method now propagate correctly regardless of `throwEx` value. Previously, a blanket `catch(Exception)` swallowed all business exceptions when `throwEx=false`. If downstream code relied on exceptions being silently swallowed, it will now see them.
- SpEL evaluation context changed from `StandardEvaluationContext` to `SimpleEvaluationContext.forReadOnlyDataBinding()`. This prevents SpEL injection but also means method calls and bean references in `lockKey` expressions will no longer work. Pure property access (`#param.fieldName`) continues to work.

**`SequenceNoGeneratorV2` (non-cache mode)**
- When `isCache=0`, the generator now immediately calls `codeGeneratorCfgMapper.updateById()` after computing the next sequence number. Previously, the DB counter was not updated in non-cache mode, causing repeated sequence numbers across restarts. This adds a DB write per call in non-cache mode.

**`UnifiedExceptionControllerAdvice` (`DuplicateKeyException` handling)**
- Now correctly catches `org.springframework.dao.DuplicateKeyException` (Spring's DAO exception hierarchy). Previously it caught `org.yaml.snakeyaml.constructor.DuplicateKeyException`, which never matched any DB duplicate key errors — those fell through to the generic handler. The `409 Conflict` response branch now activates correctly on DB unique constraint violations.

#### Non-breaking fixes

| Component | Fix |
|-----------|-----|
| `SelfSnowflakeGeneratorHelp` | `getAutoIncreaseNumber()` is now fully atomic (no more check-then-act race) |
| `CatchLogAspect.doBefore()` | No longer throws `IndexOutOfBoundsException` for no-argument methods; MDC traceId is still set |
| `SequenceNoGenerator` | `noCacheMap.remove(code)` instead of `clear()` on error — only the failing code's cache is evicted |
| `SequenceNoGeneratorV2` | Same `remove` vs `clear` fix |
| `SequenceNoGenerator.getHeadStr/getSequenceNo` | Malformed format strings now throw `BizException` instead of `ArrayIndexOutOfBoundsException` |
| `AsyncThreadHelp.initExecutorService()` | Old thread pool is shut down before replacing it — no more thread leak on re-initialization |
| `RedissonLockFactory` REPLICATE mode | Mismatched `passwords`/`nodes` array lengths now fail fast with `IllegalArgumentException` |
| `SpringRedisHelp` | All 23 `e.printStackTrace()` calls replaced with `log.error()` — Redis errors are now captured in the application log |
| `SequenceNoGenerator.generate()` | `new Long()` (deprecated constructor) replaced with `Long.valueOf()` |
| `SequenceNoGenerator.fillLeft()` | `StringBuffer` replaced with `StringBuilder` (non-synchronized, faster) |
