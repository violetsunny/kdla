# 切片 B：基础设施 只读评审报告

范围：`supplement` / `supplement-cache` / `supplement-monitor` / `supplement-dingding` / `reactor` 五模块。
取证：BASE=`136bf03`，HEAD=`0fa5ab7`；1.x 基线 `1.0.1-SNAPSHOT`。静态审查，未跑 mvn（主代理已全量复验 318 绿）。
约束遵守：未改任何源码/pom/注册文件，未做 git 写操作，未连外部服务。

## 分级结论速览

| 分级 | 条数 |
|---|---|
| Critical | **0** |
| 必改 | **2**（均在 `KRedissonLockFactory`，甲类漏修） |
| Nit | 3 |
| Optional | 2 |
| FYI | 12 |

**结论：GO（0 Critical）。** 但 B-01（密码明文日志，安全）与 B-02（fail-fast 加固缺失）**强烈建议发布前修复**——二者是同一根因：本轮把 1.x 修复打到了**非活路径的平行副本** `KRedissonConfigProperties`，而**真正的活路径** `KRedissonLockFactory` 本轮未被触碰（不在 BASE→HEAD diff 中），修复遗漏。

---

## 必改

### B-01【安全｜甲类漏修】密码明文进 INFO 日志（活路径）
- 路径：`kdla-component-supplement-cache/.../lock/KRedissonLockFactory.java:99`
- 问题：`configSingleNode()` 仍 `log.info("inti the redisson client with config: {}", config.toYAML())`。`Config.toYAML()` 序列化含 `setPassword(password)` 的明文密码。此类是 `distributeLockFactory` Bean 的实际构造路径（`KRedissonAutoConfigure.java:87 new KRedissonLockFactory(...)` → `KRedissonRedDisLock` → 切面/biz），SINGLE/REPLICATE 真实启动即触发。
- 1.x 基线已修：`RedissonLockFactory.java:107-110` 注释明言「toYAML输出含password明文，改为仅记录非敏感项」，改记 `address/codec/threads`。2.x 丢失该修复。
- 加重情节：本轮已对 `KRedissonConfigProperties.configSingleNode()` 去密码，但 `properties.redissons()` **无任何生产调用方**（仅测试引用，见 F-01），活路径是 factory 自己的 `redissons()`/`configSingleNode()`。修复打错了副本。
- 修复建议：按 1.x 恢复——删除 `toYAML()` 日志，改 `log.info("init the redisson client, address:{}, codec:{}, threads:{}", address, redissonConfig.getCodec(), redissonConfig.getThread())`。
- 状态：**未修（本轮遗漏）**。安全敏感，发布前处理。

### B-02【正确性｜甲类漏修】`redissons()` 缺失 1.x 三处 fail-fast 加固
- 路径：`kdla-component-supplement-cache/.../lock/KRedissonLockFactory.java:49-66`
- 问题（三个分支，均 1.x 已有、2.x 丢失）：
  - **(a) SINGLE :49-51** — 无 `splitNodes` 空白 fail-fast、无 `address.trim()`，空串/带空格地址直接传 Redisson 产生晦涩报错。1.x `RedissonLockFactory.java:49-51` 有 `splitNodes(...)` + `.trim()`。
  - **(b) REPLICATE :52-64** — 裸 `split(",")`，无 trim/空串过滤；密码数组与节点数不校验，密码少于节点时 `passwords[index]` 抛 `ArrayIndexOutOfBoundsException`。1.x `:53-60` 有 `splitNodes` + `passwords.length != nodes.size()` 抛 `IllegalArgumentException`。
  - **(c) 不支持类型 :49-66** — 仅处理 SINGLE/REPLICATE，`MASTER_SLAVE/SENTINEL/CLUSTER`（`RedisClusterType` 均有定义）**静默返回空 client 列表** → `getLock` 构造**零节点 `RedissonRedLock`**，锁语义失效。1.x `:71-75` 对不支持类型抛 `IllegalArgumentException` fail-fast。
- 修复建议：按 1.x `RedissonLockFactory.redissons()` 恢复三处守卫（`splitNodes` + trim + 密码数校验 + else 抛 `IllegalArgumentException`）。
- 状态：**未修（本轮遗漏）**。零节点 RedLock 的精确运行时行为 **not measured**，但缺失 fail-fast 守卫本身确定。

---

## Nit

### N-01【正确性·边缘】SpEL 解析后未复查 blank key
- 路径：`.../lock/annotation/KDistributeLockedAspect.java:70-77`
- 问题：blank 检查在 SpEL 解析**前**（:64）；解析后 `lockKey = eval==null?"":valueOf(eval)` 未再判空。非空 SpEL 表达式解析为空串时会以 `""` 上锁。1.x `DistributeLockedAspect.java:83` 在解析**后** `StringUtils.isBlank(key)` 放行。BASE 既有。
- 建议：解析后补一次 blank 放行。低危边缘。

### N-02【可读性】冗余 throws 声明
- 路径：`.../lock/annotation/KDistributeLockedAspect.java:49`
- 问题：`throws Exception, Throwable` 冗余（`Throwable` 已含 `Exception`）。
- 建议：`throws Throwable`。

### N-03【弃用 API】`Class.newInstance()` + properties 类死注解
- 路径：`.../lock/properties/KRedissonConfigProperties.java`（`getCodecInstance` 用 `newInstance()`）；类上 `@ConditionalOnMissingBean`
- 问题：`Class.newInstance()` 自 JDK9 弃用（建议 `getDeclaredConstructor().newInstance()`）；`@ConfigurationProperties` 类上的 `@ConditionalOnMissingBean` 不生效（死注解）。均 1.x 既有。
- 建议：顺手替换；死注解只列不删。

---

## Optional

### O-01【安全·纵深】SpEL 上下文可收紧
- 路径：`.../lock/annotation/KDistributeLockedAspect.java:73`
- 问题：2.x 用 `new StandardEvaluationContext(jp.getArgs())`（全能力 SpEL）；1.x 用 `SimpleEvaluationContext.forReadOnlyDataBinding()`（只读数据绑定）。key 来自注解字面量（开发者控制，非用户输入），实际不可注入，故非必改。BASE 既有。
- 建议：可恢复 1.x 只读上下文作纵深防御。

### O-02【结构】双 `redissons()` 重复实现分叉（B-01/B-02 根因）
- 路径：`KRedissonLockFactory.redissons()`（private，活）vs `KRedissonConfigProperties.redissons()`（public，无生产调用方）
- 问题：两份平行实现，本轮只修了非活的一份，导致活路径漏修。
- 建议：让 factory 委托 properties（或删其一），消除分叉，避免未来再次漏修。属重构，非本轮必须。

---

## FYI（1.x 既有 / 刻意保留 / 已知候选 / 死代码，只列不删）

- **F-01** `KRedissonConfigProperties.redissons()` 无生产调用方（仅测试）；活路径是 `KRedissonLockFactory.redissons()`。双副本分叉。
- **F-02** `sequence/config/RedissonClientConfig.java` 整文件被注释（死代码）。
- **F-03** `KDistributeLockedAspect.LOCK_KEY_PREFIX="KUnblockDistributeLock:"` 定义但 :82 `//key = LOCK_KEY_PREFIX + key;` 注释未用（死常量）；锁 key 无命名空间前缀，与 1.x `"UnblockDistributeLock:"` 分叉。BASE 既有。
- **F-04** 锁语义 vs 1.x：1.x `tryLock(0,-1,MS)` 零等待+看门狗；2.x 注解新增 `timeout()=5000`/`leaseTime()=30000`，走 `redDisLock.lock(...)`。BASE 既有的 2.x 设计，行为变更需下游知晓。
- **F-05** `RedissonRedLock` 刻意保留（任何 3.x 未移除，仅 deprecated；RedLock=多数派 quorum vs MultiLock=全锁）；`KRedissonRedDisLock.SHADOW_KEY` 未用私有常量（1.x 亦然）。已知决策。
- **F-06** `KRedissonConfigProperties` `failedSlaveCheckInterval` 被 redisson 3.52.0 弃用（建议 `failedSlaveNodeDetector`）；`configMasterSlave` 用原始逗号串 `setMasterAddress/addSlaveAddress` + `setFailedSlaveCheckInterval` 双调用覆盖。1.x 既有。已知候选。
- **F-07** `MachineIdHelp` static volatile 字段跨实例共享、注册 1h TTL 无续期、`del` 无 owner 校验。1.x 既有。（本轮 SETNX 原子注册 + `del` 清理 + 随机回退告警均正确；TTL 单位已核实：`lock()` 用 MILLISECONDS，`REGISTER_EXPIRE_MILLIS=3600*1000` 正确。）
- **F-08** monitor `PrometheusAutoConfigure` 未用 import `EnableAspectJAutoProxy`/`StopWatchWrapperAspect`；`PrometheusPointAspect` 注释反射死代码 + `Method`/`MethodSignature` 死 import。已知候选。
- **F-09** dingding `DingDingHttpUtil` 整文件注释（死代码）；`commons-httpclient` 依赖已随之正确移除。已知。
- **F-10** `VertxHttpConfigure` 跨 4 模块 .imports 重复注册（含 dingding，Boot 去重无害）；部分 .imports/pom 缺尾换行。已知。
- **F-11** supplement 侧 1.x 既有项：`NettyHttpClient` SSL 回退无 hostname 校验/SNI、一次性 workerGroup、POST headers null NPE、`sendRequestAsync` callback null NPE；`MqttClientImpl` disconnect→reconnect 不可用、publish 失败路径 retained payload 泄漏、`selfCreatedEventLoop` 非 volatile；`HttpContextHelp` JVM 级 trust-all（已 `@Deprecated`+文档化）；`BaseHttpClient.post` raw headerMap 大小写敏感 get；`ExcelReadListener` `deleteOnExit` 长跑累积；`KdlaExcelWriteHelp` 全 sheet 名 "sheet1"。
- **F-12** `SequenceNoGeneratorV2` `ruleTypeEnum==null` 守卫：审计称自 1.x 即死代码（`getByCode` 或恒非 null），本轮新增为防御性，无害。

---

## 已验证正确的本轮修复（甲/乙/丙类，抽样列举）

- **cache 分布式锁**：`KDistributeLockedAspect` held-guard（`locked && lock!=null` + `isHeldByCurrentThread` + catch warn）、业务 `proceed` 不被 catch 吞、`LockFailException` 替代裸 `Exception`；`KdlaBizDisLockService` blank/null 守卫 + 持锁才 unlock；`KRedissonAutoConfigure` 固定前缀 "kdla" 回退+warn（替代随机雪花前缀）、第三方类 `@ConditionalOnClass(RedissonClient)`。`KRedissonRedDisLock.lock()` 契约确认：要么返回非 null RLock 要么抛 `LockFailException`，绝不返回 null → sequence 的 `isLocked` 逻辑安全。
- **cache sequence**：`SequenceNoGenerator`/`V2` 溢出拒绝（throw 替代 break 清零防重号）、日期翻越先于溢出判断、规则段数/长度合法性守卫、`noCacheMap.remove(code)` 替代 `clear()`、V2 `isCache=0` 持久化 `updateById` 防跨重启重号、`customizePool` 判空+空池抛错、`cfg==null` 抛错。`SequenceNoConfigure` key 三别名 `@ConditionalOnExpression` + `@AutoConfigureAfter` + `@ConditionalOnBean` + lock-key 嵌套默认。
- **cache config**：`CacheAutoConfigure` `ObjectProvider` 替代 `@Autowired List`（防空集合注入炸）、`@PreDestroy` 改无参+字段持有（修 Spring 无参约束）、`@ConditionalOnBean(RedisTemplate)`+`@AutoConfigureAfter(RedisAutoConfiguration)`+`kdla.cache.enabled` 默认 false。.imports 正确注册 3 类，删 spring.factories（乙类）。
- **monitor**：`PushGatewayManager` 去裸 `@Component` 改由 auto-config 按第三方类条件注册；`PrometheusAutoConfigure` 条件钉 `{CollectorRegistry,PushGateway}`（替代恒真的自有类）；`PrometheusPointAspect` `ConcurrentHashMap.computeIfAbsent` 防同名 Counter 二次 register 抛 `IllegalArgumentException`（丙类真 bug）。.imports 注册（乙类）。
- **dingding**：`DingAlertConfigure` 加 `@ConditionalOnClass(Vertx.class)`；.imports 已承接注册，删 spring.factories 无损失（乙类）。
- **reactor**：本轮零改动，审计零问题确认。
- **pom**：cache 排除 `redisson-spring-data-35` 改引 `-34`（provided，对齐 Spring Data Redis 3.4）；cache/monitor 增 `spring-boot-starter-test`（provided）；dingding 移除死代码依赖 `commons-httpclient`。均正确。

## 验证局限
- 静态审查，未运行 mvn/测试（约束）；以主代理 318 全绿为准。
- 零节点 `RedissonRedLock.tryLock` 精确运行时行为、`Config.toYAML()` 在 3.52.0 是否掩码密码：**not measured**（但 1.x 注释实证 toYAML 含明文，B-01 成立）。