# 票 09 审计报告：supplement / supplement-cache 聚焦审计（P4 差额补齐）

- 日期：2026-11-14
- 审计性质：只读，未修改任何 Java/pom/配置文件
- 目标仓 T：/Users/kangll13/aiot/java-code/self/gggggggood/kdla（分支 1.0.1-SNAPSHOT）
- 参考仓 R：/Users/kangll13/aiot/java-code/aiot-robot/kdla（main）

## ① 审计范围与方法

**范围**：T 的 kdla-component-supplement 与 kdla-component-supplement-cache 两模块全部 src/main/java（supplement 23 个差异文件 + R-only vertx 包；supplement-cache 14 个差异文件 + T-only RedissonClientConfig）。

**方法**：
1. 读 R/openspec/changes/2026-10-29-assembly-contract-and-bug-fix/ 三文档，确认「33 项必改」处置原则：tasks.md:52-53（5.1 按 9 留存主题重定位；5.2 重读 73 文件补全，与主题不符标「无法复现」登记不修）、design.md:67（同原则）、proposal.md:21（行号截断丢失）。
2. `git show 32ba8d2 --stat` + 逐文件核对：13 项实修（9 主题重定位 + 4 附加），18 文件（13 主代码 + 5 测试... 实为 13 主代码文件中的 9 个 + 测试）。
3. `diff -rq --strip-trailing-cr` R/T 两模块 src/main/java，对每个 R≠T 文件逐 diff 判读；对 R 侧改动用 `git log --follow` 定位归属 commit。
4. **关键发现**：R 工作区存在 5 个文件的未提交修复（`git status`：HttpContextHelp、MqttChannelHandler、MqttClientImpl、NettyHttpClient、NettyHttpServer，共 +70/-11 行），investigation.md 完全未登记。
5. 对照票 10/11/12 正文与「依据」行引用的 investigation 条目编号（10←18/21/26/58/59/60/61/83/88；11←5/6/10/14/15/57/65/67/82/90/92；12←8/12/23/36/37/62/63/66/68/69/94/95），判定覆盖性。

**判读基准**：investigation.md 第 2 节已证 165/182 文件基线一致，故当前 R≠T 差异原则上 = R 侧修复；例外为已知的 MQTT netty 包 import 风格分叉（见 ③ 末尾「非缺陷差异」）。

## ② 已被票 10/11/12 覆盖项清单（简表）

| R≠T 文件 | R 侧修复来源 | 覆盖票 |
|---|---|---|
| supplement：CustomCorsConfig / RestHttpConfigure / VertxMqttConfigure / FieldValueFindConfigure | 1fa3289 装配条件 | 10（矩阵 11.3/11.2）|
| supplement：FieldValueFindHelp（@Component + break→continue）| bb8022f/1fa3289 | 10（项 21）|
| supplement：VertxHttpConfigure / VertxHttpClient / BaseHttpClient | 32ba8d2 | 10（项 59/60/88，B11）|
| supplement：NettyHttpClient（端口/connectTimeout 部分）| 32ba8d2 | 10（项 60）|
| supplement：NettyHttpServer（启动即关闭/bind 释放/固定文案）| bb8022f/9cdbe79/32ba8d2 | 10（项 18/26/58）|
| supplement：MqttPingHandler（超时判断反转）| bb8022f | 10（项 17）|
| supplement：MqttChannelHandler / MqttClientImpl（QoS2 ByteBuf、eventLoop 关闭）| d7d7bfe/32ba8d2 | 10（项 38/59）|
| supplement：KdlaExcelWrite（try/finally finish）| 32ba8d2 | 10（项 61）|
| supplement：R-only vertx/VertxConfigure.java | 6ff6775 | 10（项 83）|
| cache：SpringRedisHelp（printStackTrace×23/Lua 序列化/拆箱/getExpire）| d6efa60/9cdbe79/0209b43 | 11（项 15/25/67/90）|
| cache：RedissonLockFactory / RedissonConfigProperties（fail-fast/密码校验/toYAML 脱敏/cluster 逐节点）| d6efa60/bb8022f/32ba8d2/0209b43 | 11（项 14/21/57/65，B6）|
| cache：RedissonAutoConfigure（开关/OnClass/锁前缀/COBM/注册 Aspect）| 1fa3289 | 11（项 92，B5/S1）|
| cache：KdlaBizDisLockService（K2 有界 tryLock/守卫/判空）| b3e1abf/bb8022f | 11（项 5）|
| cache：RedissonRedDisLock（null TimeUnit/中断恢复）| b3e1abf/bb8022f | 11（项 6）|
| cache：DistributeLockedAspect（blanket catch/SpEL/@Component 移除）| d6efa60 | 11（项 10）※另一部分未覆盖，见 ③-8 |
| cache：R-only KdlaLockProperties.java | b3e1abf | 11（项 82）|
| cache：SequenceNoGenerator / V2（双锁单解/溢出/翻越/remove(code)/DB 写回/cfg 判空）| d6efa60/d7d7bfe/32ba8d2/0209b43 | 12（项 12/36/62/69，B12）|
| cache：MachineIdHelp（SETNX/destroy del/ERROR 告警/判空）| bb8022f/d7d7bfe/32ba8d2/0209b43 | 12（项 21/37/63/66，B13）|
| cache：KdlaCafCache（expireAfter(Expiry) 构建器部分）| 0209b43 | 12（项 68）※put 方法部分未覆盖，见 ③-5 |
| cache：CacheAutoConfigure（@EnableCaching 隔离/开关/COBM/OnBean）| 1fa3289 | 12（项 94，矩阵 11.3）※@PreDestroy 部分未覆盖，见 ③-6 |
| cache：SequenceNoConfigure（闭环 K4/OR 表达式）| 9cdbe79/1fa3289 | 12（项 23/95）|
| 两模块 spring.factories / resources | 1fa3289 | 10/11/12 装配部分 + 矩阵 11 节 |

## ③ 差额项清单（R 已修、T 仍有缺陷、未被 10/11/12 覆盖）

共 **15 项**（其中 8 与 2 为同文件同主题，执行可合并 → 实际 14 个独立修复点）。根因：investigation.md 把 bb8022f（批次 3，提交自述 16 项缺陷）仅登记为 6 个捆绑条目（项 17-22），漏登 9 处 supplement/cache 修复；另 R 工作区 5 文件未提交修复完全未登记。

### A 组：R 已提交修复（bb8022f，2026-08-28），investigation.md 漏登

**1.【高】CsvUtil.parseCsv 无限流必炸** → 并入票 10
- 缺陷：`Stream.iterate(0, i -> i + 1).map(record::get)` 无终止条件，越过 record.size() 后 `CSVRecord.get` 抛 IndexOutOfBoundsException——parseCsv 对任何非空输入必然失败，功能完全不可用。
- T 证据：T/kdla-component-supplement/src/main/java/top/kdla/framework/supplement/excel/utils/CsvUtil.java:39-41
- R 修复：bb8022f，R 同路径 :40-41 改 `IntStream.range(0, record.size()).mapToObj(record::get)`

**2.【中】HttpContextHelp 危险 API 无告警 + 过时协议** → 并入票 10（与第 14 项合并执行）
- 缺陷：trustAllHttpsCertificates() 静默信任全部证书且用已废弃协议名 "SSL"；buildCors() 反射任意 Origin + Allow-Credentials:true 无任何告警。
- T 证据：T/.../supplement/http/HttpContextHelp.java:39（`getInstance("SSL")`）、:95（任意 Origin 反射）
- R 修复：bb8022f，R 同路径 :35-37（log.warn）、:42（"TLS"）、:98-100（buildCors warn）

**3.【中】SslContextFactory 流泄漏 + 抛 Error** → 并入票 10
- 缺陷：4 处 keystore/truststore InputStream 未关闭（泄漏 fd）；初始化失败 `throw new Error` 绕过宿主一切 catch(Exception) 异常处理，直接杀线程/进程。
- T 证据：T/.../supplement/http/ssl/SslContextFactory.java:35,42,75,82（裸 load）、:50,:89（throw new Error）
- R 修复：bb8022f，R 同路径 :35-37,41-43,72-74,78-80（try-with-resources）、:49,:86（IllegalStateException）
- 备注：investigation 项 20 有登记（"FileChunkTransmitHelp/SslContextFactory try-with-resources"）但项 20 归属票 06（log 模块），supplement 侧无人认领——归属缝隙。

**4.【低】KdlaGcache.put(key,duration) assert 失效** → 并入票 12
- 缺陷：`assert value != null` 在 JVM 默认（无 -ea）下不执行，loaderFunction 返回 null 时静默入缓存，后续读取 NPE 且根因不可溯。
- T 证据：T/kdla-component-supplement-cache/.../cache/cache/KdlaGcache.java:135
- R 修复：bb8022f，R 同路径 :136 改 `Objects.requireNonNull(value, "loaderFunction returned null for key: " + key)`

**5.【中】KdlaCafCache.put 两处：assert 失效 + per-key TTL 顺序错误** → 并入票 12
- 缺陷：① put(key,duration) 同样 `assert value != null` 失效；② 两个 put 重载均先 `setExpiresAfter` 后 `loadingCache.put`——对不存在的 key 调 setExpiresAfter 是 no-op，随后 put 按构建器默认 TTL 覆盖，per-key duration 永远不生效。票 12 已有的项 68（expireAfter(Expiry)）只改构建器，不改此顺序则 per-key TTL 依然失效，两项必须同批落地。
- T 证据：T/.../cache/cache/KdlaCafCache.java:121（assert）、:123→125 与 :137→139（先设过期后 put）
- R 修复：bb8022f，R 同路径 :119-124,:132-137（requireNonNull + put 先于 setExpiresAfter）

**6.【高】CacheAutoConfigure @PreDestroy 带参方法** → 并入票 12（与票 12 的注册动作强耦合）
- 缺陷：`@PreDestroy public void destroy(MachineIdHelp machineIdHelp)` 违反 JSR-250 无参约束，Spring 构建 LifecycleMetadata 时抛 IllegalStateException("Lifecycle method annotation requires a no-arg method")。T 现状该类未注册（死类）故潜伏；票 12 按矩阵 11.3 将其注册进 spring.factories 后，kdla.cache.enabled=true 即 bean 创建失败——必须与注册同批修复。
- T 证据：T/.../cache/cache/config/CacheAutoConfigure.java:71-73
- R 修复：bb8022f，R 同路径 :40,:48-49,:60-61（字段持有）+ :74-77（无参 destroy + 判空）

**7.【中】SequenceNoGeneratorV2 随机字符池无防护** → 并入票 12
- 缺陷：`poolList.addAll(randomTypeRule.getCustomizePool())` 无判空（customizePool 为 null 直接 NPE）；池为空时后续 `random.nextInt(0)` 抛 IllegalArgumentException，根因均不可溯。
- T 证据：T/.../cache/sequence/SequenceNoGeneratorV2.java:294
- R 修复：bb8022f，R 同路径 :296-301（null 守卫 + 空池抛 BizException("随机字符池不能为空")）

**8.【高】DistributeLockedAspect 从未真正加锁（注解形同虚设）** → 并入票 11
- 缺陷：`redissonLockFactory.getLock(key)` 仅构造 RedissonRedLock 对象、不获取锁，`lock != null` 恒真 → 业务永远放行，@UnblockDistributeLocked 并发互斥完全失效；finally 中 unlock 无 isHeldByCurrentThread 守卫，对未持有锁 unlock 抛 IllegalMonitorStateException 覆盖业务异常。票 11 已含该切面的 blanket-catch 删除/SpEL 受限/移除 @Component（d6efa60 部分），但 bb8022f 的真实加锁语义未列入。
- T 证据：T/.../cache/lock/annotation/DistributeLockedAspect.java:83-84（getLock 后直接 proceed）、:90（throw new Exception 非 LockFailException）、:101-102（unlock 无守卫）
- R 修复：bb8022f，R 同路径 :89-110（`tryLock(0, -1, MILLISECONDS)` + locked 标志 + isHeldByCurrentThread 守卫 + LockFailException 类型化）+ :24-34 行为变更 javadoc（R 明文「旧版本注解形同虚设，业务永远放行……下游需知晓」）
- 备注：这是行为变更（原本不加锁的并发路径升级后会真抢锁），回合时 CHANGELOG 需标注。

**9.【低】NettyHttpClient 无 close() + AtomicBoolean 双 shutdown 竞态** → 并入票 10
- 缺陷：3 个 send 方法各自维护 `AtomicBoolean close` 做 check-then-act，与 channel 关闭回调竞态（shutdownGracefully 幂等故实害有限，属清理性修复）；实例无 close()，长持有时 workerGroup 无法显式释放。
- T 证据：T/.../supplement/netty/NettyHttpClient.java:43,86,123,167
- R 修复：bb8022f，R 同路径 :76-78（新增 close()）、:88-115 等（删 AtomicBoolean，仅 catch 分支 shutdownGracefully）

### B 组：R 工作区未提交修复（git status 5 文件，+70/-11 行，无 commit 证据）

> 性质判定：R 侧修复内容具体、注释完整（「旧实现……」风格与已提交修复一致），疑为 R 仓某次审计会话中断遗留。回合时建议逐项复核后采纳，不可盲信为已验证修复。

**10.【高】MqttChannelHandler CONACK 未知码 future 永久悬挂 + PUBREC/PUBCOMP 判空缺失** → 并入票 10
- 缺陷：① handleConack 的 switch 无 default 分支，broker 返回未知 returnCode 时 connectFuture 既不 success 也不 failure，连接方无限等待；② handlePubrec/handlePubcomp 对 pendingPublishes.get() 结果不判空，重复/错序 PUBREC/PUBCOMP 时 NPE 杀死 netty event loop（handlePuback 已有判空，此两处漏）。
- T 证据：T/.../supplement/mqtt/netty/MqttChannelHandler.java:124（switch 无 default）、:232-234（handlePubrec 裸调）、:256-259（handlePubcomp 裸调）
- R 修复：工作区未提交，R 同路径 :172-178（default setFailure + close）、:258-262,:292-296（判空 return）

**11.【中】MqttClientImpl 非线程安全集合并发读写** → 并入票 10
- 缺陷：serverSubscriptions/pendingSubscribeTopics（HashSet）、subscriptions/handlerToSubscription（HashMultimap）被调用线程（on/once/subscribe）与 netty event loop（handleConack/handlePublish）并发读写——丢更新、ConcurrentModificationException、HashMap 扩容死循环风险。
- T 证据：T/.../supplement/mqtt/netty/MqttClientImpl.java:47,51,53,54
- R 修复：工作区未提交，R 同路径 :70-77（ConcurrentHashMap.newKeySet() + Multimaps.synchronizedSetMultimap）

**12.【高】NettyHttpClient setSsl 事后无效 + 无超时永久挂起 + https 默认路径 NPE** → 并入票 10
- 缺陷：① 构造器已按当时参数建好 HttpClientInitializer，之后调 setSsl 对已建 handler 无效（SSL 配置静默丢失）；② `respPromise.get()` / `future.get()` 无超时，对端不回包且不断连时调用线程永久挂起（与票 10 已覆盖的 Vertx 30s 超时同型缺陷，Netty 侧漏）；③ https URL 未提供自定义 keystore 时 `SslContextFactory.getClientContext(null,...)` 必 NPE，无法走 JDK 默认信任链。
- T 证据：T/.../supplement/netty/NettyHttpClient.java:77-85（setSsl 不重建 handler）、:119,:156（无超时 get）、:219（getClientContext 无判空）
- R 修复：工作区未提交，R 同路径 :86-95（setSsl 重建 handler + requestTimeout 字段/setter）、:125-133,:162-170（get(30s) + TimeoutException 时 shutdownGracefully）、:225-234（keystore 判空回退 SSLContext.getDefault()）

**13.【中】NettyHttpServer.writeHttpResponseHtml CONTENT_LENGTH 恒 0 + FileRegion fd 泄漏** → 并入票 10
- 缺陷：① CONTENT_LENGTH 取空 content 的 writerIndex() 恒 0，客户端按 0 长度截断，keep-alive 下后续响应协议流错位；② 三参 DefaultFileRegion 传输完成不关 FileChannel，raf/fd 泄漏。
- T 证据：T/.../supplement/netty/NettyHttpServer.java:269（setInt writerIndex）、:271（三参 DefaultFileRegion）
- R 修复：工作区未提交，R 同路径 :285-292（setLong raf.length() + 四参 closeOnComplete=true）
- 备注：同文件 :250（writeHttpResponseImage）的同名写法不是缺陷——该路径 content 已 writeBytes 填充，writerIndex 正确；R 也未改。

**14.【低】HttpContextHelp ignoreSSL/buildCors 未标 @Deprecated + 无迁移告警** → 并入票 10（与第 2 项合并执行）
- 缺陷：ignoreSSL() 修改 JVM 级默认 SSLSocketFactory/HostnameVerifier（一次调用使宿主全进程 HTTPS 可被 MITM）、buildCors() 任意 Origin 反射，均为危险 API 但无 @Deprecated 标记与迁移指引（应指向 kdla.http.ssl.trust-all / kdla.config.cors.enabled）。
- T 证据：T/.../supplement/http/HttpContextHelp.java:71-88（ignoreSSL 无标记）、:90-99（buildCors 无标记）
- R 修复：工作区未提交，R 同路径 :74-79,:89,:97-102,:110（@Deprecated + javadoc + log.warn 迁移提示）

### C 组：结构缺口

**15.【低】RedissonClientConfig.java 整文件注释残留删除无票归属** → 并入票 11
- 缺陷：T-only 文件，整文件被注释的死代码残留（无可注册内容），R 用户裁定「例外删除」。investigation 项 91 与 spec.md:64 均有记载，但票 10/11/12/13 正文与依据行均未引用项 91，无票落地此删除。
- T 证据：T/kdla-component-supplement-cache/src/main/java/top/kdla/framework/supplement/cache/sequence/config/RedissonClientConfig.java（全文件注释）
- R 修复：1fa3289 已删除（R 侧该文件不存在）
- 备注：文件位于 sequence/config 目录，若按目录归属可并入票 12；按主题（Redisson 装配）建议票 11，二选一即可，勿两票重复。

### 非缺陷差异（不列差额，仅备案）

- MQTT netty 6 文件（MqttClientConfig/MqttPendingPublish/MqttPendingSubscription/MqttPendingUnsubscription/MqttSubscription/RetransmissionHandler）：仅 import 排序差异，investigation 第 2 节已定性为基线风格分叉，T 侧自有演化，无需动作。
- T 的 VertxHttpConfigure/VertxMqttConfigure 内联 vertx() Bean：基线形态，票 10 项 83（VertxConfigure 统一 + 去重）已覆盖。
- SequenceNoGenerator.fillLeft 的 R 侧等价重构（bb8022f）：语义等价，非缺陷。
- SequenceNoConfigure T 侧多 @Slf4j：风格差异，非缺陷。

## ④ R 也未修登记区

无新增。R docs/kdla-optimization.md「supplement / supplement-cache 模块」Nit 6 项已被 T nit-inventory.md S1-S6 完整继承（P2 裁定：只登记不修）：

| # | 位置 | 现状核验（本次审计确认 R 仍未修）|
|---|---|---|
| S1 | KdlaExcelWrite.writeSheet 多 class 写同一 "sheet1" 相互覆盖 | R 32ba8d2 仅修 finish()，sheet 覆盖仍在（R:64-68 同 T）|
| S2 | SequenceNoGenerator.DEFAULT_RULE "yyyyMMdd+8" 仅 2 段，与 3 段校验冲突，默认规则必抛错 | R:55 仍 "yyyyMMdd+8"；32ba8d2 加 <3 段抛 BizException 后由静默错变显式错，默认值本身未修 |
| S3 | MachineIdHelp 注册无 TTL 续期（3600s 后撞号）；destroy 的 del 可能误删他实例重注册 | R 未修（B13 裁定接受 + S3 登记）|
| S4 | MqttChannelHandler QoS2 PUBREL 永不到达时 pending 条目滞留（需逐消息超时）| R 未修；注意与本报告第 10 项（判空防 NPE）相关但不同 |
| S5 | RedissonConfigProperties:28 @ConditionalOnMissingBean 挂属性类上死注解 | R:28 仍在 |
| S6 | KdlaBodyReaderRequestFilter:40 @Value 依赖 Spring 托管 | log 模块，超出本审计范围，仅确认归属票 06 |

## ⑤ 结论：33 项 vs 13 项差额去向判定

**批次 5（32ba8d2）本身无 T 侧遗漏**：13 项实修（9 主题重定位 + 4 附加）对应 investigation 项 57-63，已全部被票 10/11/12 覆盖（含 B6/B11/B12/B13 裁定）。

**「约 20 项丢失差额」的去向**（按 R tasks.md 5.1/5.2 与 design.md:67 原则 + 本次代码核验推定）：
1. **同根因合并**：33 项是「行号级」计数，9 主题是「根因级」归并——如 VertxHttpClient 的 content-type 大小写与 future 无超时两项并入一个修复、NettyHttpClient 的 @Value 与端口两项并入一个修复、RedissonConfigProperties 与 RedissonLockFactory 的地址解析多项并入 splitNodes 一处。行号数 > 实修项数是必然结果。
2. **装配缺口主题核销**：tasks 5.1 明文「装配缺口（批次 1 已闭环则核销）」——该主题对应项由 1fa3289 在批次 1 修复，批次 5 不再计数。
3. **更早批次已修**：部分主题（EventLoopGroup 泄漏 = 9cdbe79、SSL 相关前置 = bb8022f）在批次 5 之前已修，重定位时发现即核销。
4. **「无法复现」登记不修**：tasks 5.2 规定与主题描述不符的标「无法复现」；但全 R 仓 grep「无法复现」仅命中 tasks/design 的原则条款，**无逐项显式清单**——这部分项的个体去向在 R 侧即不可追溯（R 自身的过程记录缺陷，非 T 回合风险）。
5. **Nit 级分流**：重读 73 文件发现的非必改项进入 docs/kdla-optimization.md（S1-S6），已由 T nit-inventory 继承。

**本次审计的真正遗漏不在批次 5，而在三处登记缝隙**：
- investigation.md 对 bb8022f（批次 3）欠登记：该提交自述 16 项缺陷仅被登记为 6 个捆绑条目，supplement/cache 侧 9 处实修（本报告 A 组）无票归属；
- R 工作区 5 文件未提交修复（本报告 B 组）从未进入任何登记；
- RedissonClientConfig 删除（项 91/spec 47）无票引用（本报告 C 组）。

**处置**：15 项差额（合并后 14 个修复点）全部可并入现有票，无需新票——票 10 +8 项（1/2/3/9/10/11/12/13，另 14 并入 2）、票 11 +2 项（8/15）、票 12 +4 项（4/5/6/7）。三票正文已有「合并 09 审计归属本票的补项」钩子，建议将本报告 ③ 对应条目摘录回填各票正文及 investigation.md 新增小节。高优先级 5 项（1/6/8/10/12）建议实现时先落。
