# 切片 F：测试质量横切评审（Prove-It）

仓库根：`/Users/kangll13/aiot/java-code/self/gggggggood/kdla`（下文路径均相对此根）。
评审范围：BASE=`136bf03` → HEAD=`0fa5ab7` 新增/移植的全部 111 个测试文件（8204 行、318 用例，主代理已全量 `mvn clean test` 复验全绿）。
评审方式：静态只读审查（read/grep/git diff/git show 1.x 基线），未运行任何 mvn，未连接任何外部服务。需运行时定论处标 not measured。

## 结论

**NO-GO。Critical = 1（F-01），必改 = 1（F-02）。**

核心问题：cache 分布式锁的"丢失修复恢复"把 1.x 修复恢复进了**死代码类** `KRedissonConfigProperties`，而真实生产路径 `KRedissonLockFactory` 仍是未修复的 1.0.0 形态；9+5 个回归用例验证的全是死路径，318 绿给出假信心。其余约 95 个测试文件质量高：真 Prove-It、行为断言、隔离良好、Netty 坑处理正确、无真实外部服务、移植保真。

---

## 发现清单

### F-01 ｜ Critical ｜ cache 锁回归测试验证死代码路径，生产工厂丢失全部 1.x 修复（假绿）

**路径:行号**
- 生产接线：`kdla-component-supplement-cache/src/main/java/top/kdla/framework/supplement/cache/lock/config/KRedissonAutoConfigure.java:87`（`new KRedissonLockFactory(this.config, prefix)`）
- 生产路径（未修复）：`kdla-component-supplement-cache/src/main/java/top/kdla/framework/supplement/cache/lock/KRedissonLockFactory.java:46-67`（私有 `redissons()`）、`:136`（零节点 `new RedissonRedLock(...)`）
- 死代码（修复被错误恢复到这里）：`kdla-component-supplement-cache/src/main/java/top/kdla/framework/supplement/cache/lock/properties/KRedissonConfigProperties.java:84-99`（`splitNodes`）、`:101-139`（public `redissons()`）
- 假绿测试：`.../lock/properties/KRedissonConfigPropertiesRedissonsTest.java`（6 用例全部 `p::redissons`）、`.../lock/properties/KRedissonConfigPropertiesPasswordTest.java`（3 用例反射调 `configCluster`/`configMasterSlave`/`configSingleNode`，均属死类）、`.../lock/properties/KRedissonConfigPropertiesSplitNodesTest.java`（5 用例，`splitNodes` 仅被死链接线）
- 利用 bug 的测试：`.../lock/config/KRedissonAutoConfigureLockPrefixTest.java:20-21`（注释明言利用 CLUSTER 静默空列表避免触网）

**问题**
1. 全仓 grep 确认 `KRedissonConfigProperties.redissons()` 与 `splitNodes()` **零生产调用方**（`splitNodes` 仅被同文件死链 :106/:110/:115/:209 调用；1.x 中该类同样未接线）。
2. 1.x 基线（`git show 1.0.1-SNAPSHOT:.../lock/RedissonLockFactory.java`）的修复在**工厂**里：SINGLE/REPLICATE 接 `splitNodes` fail-fast + trim、REPLICATE 密码数量校验、else 分支抛 `IllegalArgumentException("RedissonLockFactory 不支持集群类型…仅支持SINGLE/REPLICATE")`。
3. 2.x 工厂 `KRedissonLockFactory.redissons()` 丢失全部上述修复：SINGLE 裸地址不 trim 不校验（:50）；REPLICATE 裸 `split(",")`（:52）、密码数量不匹配时 `passwords[index]` 抛 ArrayIndexOutOfBoundsException 而非清晰报错（:57）；**MASTER_SLAVE/CLUSTER/SENTINEL 无匹配分支，静默返回空 client 列表**（:66）→ `getLock` 构造零节点 RedLock（:136），下游锁静默不可用。
4. 测试 javadoc 声称"无修复则失败"，但对照的"旧实现"是死类而非生产工厂——对生产路径而言这些用例**恒绿**，属 Prove-It 反模式（验证了不存在的接线）。`KRedissonAutoConfigureLockPrefixTest` 更把静默空列表 bug 当测试便利，工厂修复后该测试反而要改。

**修复建议**（属源码修复，超出本切片只读权限，列出供主代理处置）
- 按 1.x 基线恢复 `KRedissonLockFactory.redissons()`：接 `KRedissonConfigProperties.splitNodes`、REPLICATE 密码数量校验、不支持类型 fail-fast；`configSingleNode` 日志同步脱敏（见 F-02）。
- 回归测试改打生产路径：对 `KRedissonLockFactory` 构造器/`getLock` 断言 fail-fast（空白地址、密码数量不匹配、CLUSTER/SENTINEL/MASTER_SLAVE 抛异常），这些用例在 `Redisson.create` 之前抛出、不触网，可离线跑。
- `KRedissonAutoConfigureLockPrefixTest` 改用 SINGLE + 空白地址 fail-fast 之前断言前缀，或 mock 工厂构造，不再依赖静默空列表。
- 死代码 `KRedissonConfigProperties.redissons()`/`configMasterSlave()`/`configCluster()` 只列不删（先问再删）；若决定删除，连同其 14 个测试用例一并处置。

**状态**：open（本轮新引入的恢复错位；COMMON-CONTEXT 已登记 `KRedissonLockFactory` 为"未逐一 diff 分类项"，本条从测试质量角度确认为 Critical）。

---

### F-02 ｜ 必改（安全）｜ 生产工厂日志回归 toYAML 密码明文，1.x 已修复被丢失，无测试覆盖

**路径:行号**：`kdla-component-supplement-cache/src/main/java/top/kdla/framework/supplement/cache/lock/KRedissonLockFactory.java:98-100`（`log.info("inti the redisson client with config: {}", config.toYAML())`）

**问题**：`config.toYAML()` 输出含 password 明文。1.x 工厂已修复为仅记录非敏感项（1.x 基线 :107-109 注释"toYAML输出含password明文，改为仅记录非敏感项"）；2.x 把该修复恢复进了死类 `KRedissonConfigProperties.configSingleNode:169-172`，生产工厂仍是旧写法。与 F-01 同根因。现有测试（PasswordTest 只断言密码落在正确配置段）不覆盖日志脱敏。

**修复建议**：随 F-01 一并按 1.x 恢复工厂日志为 `address/codec/threads` 非敏感项；补一条 ListAppender 断言日志不含密码字符串的回归用例。

**状态**：open。

---

### F-03 ｜ Nit ｜ MachineIdHelp 两测试不清理静态 machineId/dataCenterId

**路径:行号**：`kdla-component-supplement-cache/src/test/java/top/kdla/framework/supplement/cache/cache/MachineIdHelpSetNxTest.java:48`、`MachineIdHelpDestroyTest.java:43-44`

**问题**：构造 `MachineIdHelp` 会写静态 machineId/dataCenterId，两用例无 @AfterEach 复位。同模块 `MachineIdHelpFailPathTest` 有 @BeforeEach/@AfterEach 清理，风格不一致。缓解因素：构造器无条件 init()，跨用例污染目前不致错。

**修复建议**：对齐 FailPathTest，@AfterEach 反射复位静态字段。

**状态**：open。

---

### F-04 ｜ Nit ｜ NettyHttpClientTimeoutAndSslTest 两用例未 close 客户端（线程泄漏）

**路径:行号**：`kdla-component-supplement/src/test/java/top/kdla/framework/supplement/netty/NettyHttpClientTimeoutAndSslTest.java:101-120`（sendRequest_peerSilent）、`:122-142`（sendRequest_normalResponse）

**问题**：前两个用例有 try/finally `client.close()`（:57/:74），后两个用例创建的 `NettyHttpClient` 未关闭，NioEventLoopGroup 线程残留。断言本身正确（超时抛 TimeoutException、响应体完整），仅卫生问题。

**修复建议**：两用例补 try/finally `client.close()`。

**状态**：open。

---

### F-05 ｜ Nit ｜ nullEncoding 用例只证"不抛 NPE"，未验 UTF-8 解码内容

**路径:行号**：`kdla-component-exception/src/test/java/top/kdla/framework/exception/UnifiedExceptionControllerAdviceTest.java:51-66`

**问题**：`handleHttpMessageNotReadable_nullEncoding_fallsBackToUtf8` 仅断言 `assertNotNull(response)` + BAD_REQUEST code。请求体"中文请求体"按 UTF-8 解码后只进 `log.error`（源码 `UnifiedExceptionControllerAdvice.java:224-237`），用例未验证解码结果正确——若回退成 ISO-8859-1 产生乱码，测试照样绿。

**修复建议**：挂 ListAppender 断言日志含"中文请求体"原文。

**状态**：open。

---

### F-06 ｜ Nit ｜ 线程池默认队列断言过弱（<=1024 应为 ==1024）

**路径:行号**：`kdla-component-common/src/test/java/top/kdla/framework/common/help/ThreadPoolHelpBuilderDefaultTest.java:30`、`ThreadPoolHelpDefaultQueueTest.java:30,32`

**问题**：`assertTrue(queue.remainingCapacity() <= 1024)` 对任何更小容量（如 16）也绿，无法锁定"默认 1024"这一修复值。

**修复建议**：改 `assertEquals(1024, ...)`。

**状态**：open。

---

### F-07 ｜ Nit ｜ V2 序列两测试向静态 ruleContainerCacheMap 留残留

**路径:行号**：`kdla-component-supplement-cache/src/test/java/top/kdla/framework/supplement/cache/sequence/SequenceNoGeneratorV2NonCacheTest.java:39`（TEST_SEQ）、`SequenceNoGeneratorV2LockReleaseTest.java:70-74`（LOCK_RELEASE_V2_CODE）

**问题**：setUp 向静态 `ruleContainerCacheMap` put 后无 @AfterEach remove。唯一 key 避免跨用例碰撞（V2RuleGuardTest 用 UUID 同理），目前不致错，属隔离卫生。对比：V1 的 InvalidLength/OverflowReject 测试有 `removeStaticCacheEntry` 清理，风格不一致。

**修复建议**：补 @AfterEach remove 对应 key。

**状态**：open。

---

### F-08 ｜ Nit ｜ ObjectValidatorTest 恒真用例（annotationClass_noUnsafeImport）

**路径:行号**：`kdla-component-validator/src/test/java/top/kdla/framework/validator/ObjectValidatorTest.java:131-138`

**问题**：遍历类注解调 `annotationType()` 包在 assertDoesNotThrow 里——import 是编译期问题（注释自己也承认"若仍import，JDK9+直接编译失败"），该运行时断言任何情况下都绿，无验证价值。真正的验证在同文件 `commonlyClassList_noUnsafeReference`（:118-128，有效）。

**修复建议**：删除该用例（测试死代码，先问再删）。

**状态**：open。

---

### F-09 ｜ Optional ｜ asLocalDate 正向用例仅 assertNotNull

**路径:行号**：`kdla-component-common/src/test/java/top/kdla/framework/common/help/LocalDateTimeHelpAsLocalDateTest.java:21-30`

**问题**：10/13 位时间戳正向用例只断言非 null，未断言解析出的日期值；核心回归（非法长度返回 null 而非抛异常，:15-18）断言充分。时区依赖是只断 notNull 的合理理由，但可用固定 ZoneId 钉死期望值。

**修复建议**：可选增强——固定时区后 assertEquals 具体 LocalDate。

**状态**：open。

---

### F-10 ｜ FYI ｜ 裸 assert 为 1.x 既有（非本轮新引入）

**路径:行号**：`kdla-component-common/src/test/java/top/kdla/framework/common/help/SelfSnowflakeGeneratorHelpTest.java:65`、`kdla-component-log/src/test/java/top/kdla/framework/log/catchlog/CatchLogAspectNoArgsTest.java:69`

**说明**：两处 `assert ... : msg` 均经 `git show 1.0.1-SNAPSHOT` 确认 1.x 原样存在（后者 1.x :67）。surefire 默认开 -ea 可用，IDE 无 -ea 时静默跳过。按"1.x 既有降级"规则记 FYI。建议顺手改 assertEquals/assertTrue，不阻断。

**状态**：1.x 既有。

---

### F-11 ｜ FYI ｜ 时间余量与概率性用例的 flake 风险

**路径:行号**：`kdla-component-common/src/test/java/top/kdla/framework/common/help/MultiThreadInvokeHelpTimeoutTest.java:38`（`elapsed < 1500`，预算 1s）；`kdla-component-supplement/src/test/java/top/kdla/framework/supplement/mqtt/netty/MqttClientImplConcurrencyAndCleanupTest.java`（concurrentSubscribe_noLostUpdate 竞态复现）

**说明**：慢 CI 上 1500ms 余量可能误红；竞态用例无修复时概率性红而非确定红（Prove-It 强度打折，但方向正确）。本机 318 绿，not measured 于慢环境。

**状态**：观察项。

---

### F-12 ｜ FYI ｜ StopWatchWrapperAspectTest 为适配 2.x pointcut 的合法改写

**路径:行号**：`kdla-component-common/src/test/java/top/kdla/framework/common/aspect/watch/StopWatchWrapperAspectTest.java`

**说明**：2.x pointcut 绑定注解实例，1.x 的 null-annotation 守卫路径不再可直接构造；移植改为测拦截行为并新增 warn/error 分级用例，断言强度不降。合法适配，非保真缺陷。

**状态**：确认无问题。

---

### F-13 ｜ FYI ｜ RedLock quorum 无真对象测试（刻意保留决策，not measured）

**路径:行号**：`kdla-component-supplement-cache/src/test/java/top/kdla/framework/supplement/cache/lock/KRedissonRedDisLockTest.java`

**说明**：全部 mock RedissonRedLock，未验证多数派 quorum 语义。保留 RedLock（而非迁 MultiLock）是刻意决策（COMMON-CONTEXT 明示），commit 称手工验证。离线无法连真实 Redis 集群，标 not measured。deprecated 仅 FYI。

**状态**：刻意保留，not measured。

---

### F-14 ｜ FYI ｜ RestHttpConfigureTtlTest 反射 httpclient5 私有字段，库升级脆弱

**路径:行号**：`kdla-component-supplement/src/test/java/top/kdla/framework/supplement/http/RestHttpConfigureTtlTest.java:53-67`

**说明**：反射 `connManager`/`connectionConfigResolver` 私有字段取 TTL，httpclient5 内部结构变更即碎。但这是离线验证 TTL 的唯一手段（不触网），断言值 60s 明确，可接受。

**状态**：接受现状。

---

### F-15 ｜ FYI ｜ 38 个测试文件末尾缺换行（已登记候选类别的扩展确认）

**路径:行号**：common 6（StopWatchWrapperAspectTest/AopTargetHelpTest/LocalDateTimeHelpAsLocalDateTest/MultiThreadInvokeHelpTimeoutTest/GzipUtilTest/Md5UtilTest）、log 5、cache 7、monitor 2、trd 2、supplement 16（完整清单可用 `for f in $(git diff 136bf03 HEAD --name-only -- '*/src/test/*'); do [ -n "$(tail -c 1 "$f")" ] && echo "$f"; done` 复现）。

**说明**：COMMON-CONTEXT 已登记".imports/JwtUtil/AopTargetHelp 文件末尾缺换行"候选项，本条确认该类别扩展至 38 个测试文件。纯风格，不阻断。

**状态**：已登记类别确认。

---

## 覆盖缺口清单（任务核对项逐条）

| 核对项 | 状态 | 证据 |
|---|---|---|
| LVT 参数名回退 | ✓ 覆盖 | MdcAspectTest（#name SpEL 真解析）、RequestLogAspectDegradeTest（日志含 userName） |
| JWT fail-closed | ✓ 覆盖 | JwtUtilTest 7 用例（未配 sign-key/验签失败/过期全路径），tearDown 清 ApplicationContextHelp 静态 |
| AES 旧格式兼容 | ✓ 覆盖 | AESUtilTest.decrypt_legacyZeroIvFormat_stillWorks（独立 Cipher 构造全零 IV 存量密文） |
| SM4 旧 ECB 迁移 | ✓ 覆盖 | Sm4UtilCbcMigrationTest |
| sequence key 别名 | ✓ 覆盖 | SequenceNoConfigureLockKeyAliasTest（反射读 @Value 表达式 + MockEnvironment 解析） |
| Counter 重复 register | ✓ 覆盖 | PrometheusPointAspectTest 真 CollectorRegistry + AspectJProxyFactory，两次调用后 counter==2.0 |
| charset UTF-8 | ✓ 基本覆盖 | Md5UtilTest 已知向量、GzipUtilTest、Sm2UtilTest 闭环；exception nullEncoding 仅证不抛 NPE（F-05） |
| 密码内联 per-mode / splitNodes fail-fast / 不支持类型 fail-fast | ✗ **仅死路径覆盖** | F-01：生产工厂无对应用例，14 个用例打在死类上 |
| 工厂日志脱敏 | ✗ 无覆盖 | F-02 |
| RedLock quorum 真对象 | ✗ not measured | F-13（刻意保留，全 mock） |

## 正面核实（抽样证据，非发现）

1. **断言真实性**：111 文件全部含行为断言（grep 计数核实）；无 `assertTrue(true)` 恒真式（F-08 是唯一恒真用例）；测试目录无独立桩类，内部类均为合法 fixture（如 ObjectValidatorTest 的 Parent/Child/Node、FieldValueFindHelpBatchTest 的记录型 fake）。
2. **Netty 坑处理正确**：MqttPingHandlerTimeoutTest 3 处 `advanceTimeBy` 均紧接 `runScheduledPendingTasks()`；MqttChannelHandlerQos2RefCntTest / MqttClientImplConcurrencyAndCleanupTest 在 refCnt 归零前先取出 payload ByteBuf，后者用 deadline 轮询等引用计数落定。
3. **无真实外部服务**：仅 loopback ServerSocket/JDK HttpServer/EmbeddedChannel/mock RestTemplate；127.0.0.1 只出现在不触网的 Config 构造与本地回环服务器。
4. **trd 不越界**：TrdPlatformInfoServiceImplTest 仅测 getByPCodes keep-first（离线 TableInfoHelper 初始化），未测 Validator。
5. **移植保真**：1.x 65 个测试的删改经 diff 核实均为 javax→jakarta/API 重命名/注释压缩，断言保留；DistributeLockedAspectBizExceptionTest(104行)→KDistributeLockedAspectBizExceptionTest(87行) 两用例齐全，且把 1.x 裸 assert 改进为 assertNull。
6. **隔离总体良好**：MDC remove（CatchLog 系列）、Locale.getDefault finally 恢复（CommonValidatorTest）、ApplicationContextHelp 静态 save/restore（ApplicationContextHelpTest）、V1 序列测试 removeStaticCacheEntry、JwtUtilTest tearDown 清静态。
7. **高质量 Prove-It 样例**：SqlFilterWhitelistTest（白名单安全回归，union/注释/引号/超长全拒绝）、IPageQueryTest（5 用例含边界）、DBLengthValidTest（10^10/10^19 溢出边界）、PageTest（Infinity 溢出）、SequenceNoGeneratorOverflowRejectTest（never() 断言不持久化 + unlock 恰好一次）、KdlaBizDisLockServiceLockPropertiesTest（四参 tryLock verify）。

## 未重复标记的已知误报/刻意决策

AES SHA1PRNG、H13 Content-Type、H8 handleConack default、ruleTypeEnum==null 死代码、trd StringUtil 半误报、SENTINEL 1.x 语义、RedLock 保留（仅 F-13 FYI）、JwtUtil 强制验签（用户裁定）、xxl-job 包名、MpBaseDo、jsqlparser、未加 -parameters（LVT 回退已覆盖）、failedSlaveCheckInterval 弃用（已登记）、LSP 误报全部忽略。

## 汇总

| 分级 | 条数 | ID |
|---|---|---|
| Critical | 1 | F-01 |
| 必改 | 1 | F-02 |
| Nit | 6 | F-03 ~ F-08 |
| Optional | 1 | F-09 |
| FYI | 6 | F-10 ~ F-15 |

**GO/NO-GO：NO-GO**（Critical=1）。F-01/F-02 同根因（1.x 修复恢复进死类而非生产工厂），建议合并为一个修复任务：按 1.x 基线恢复 `KRedissonLockFactory`，回归测试改打生产路径，随后本切片可转 GO。其余发现均不阻断。
