# kdla 回合调查报告（aiot-robot/kdla → 全面版 kdla）

- 日期：2026-11-14
- 参考仓 R：/Users/kangll13/aiot/java-code/aiot-robot/kdla（remote: git.midea.com/DEP-MCA/robozone/individual/kangll13/kdla.git，仅 main 分支，23 提交）
- 目标仓 T：/Users/kangll13/aiot/java-code/self/gggggggood/kdla（remote: github.com/violetsunny/kdla.git，分支 1.0.1-SNAPSHOT，HEAD 320529d，基线干净仅 .DS_Store/.agents 未跟踪）
- 调查方式：2 个 explore subagent（R 仓考古 + 跨仓 diff）；主 agent 抽查 4 处证据全部通过（T Sm4Util.java:36 仍 ECB、T UnifiedExceptionControllerAdvice.java:19 仍 snakeyaml 误 import、T Response.java:20 仍有 success 字段、T SpringRedisHelp.java 恰 23 处 printStackTrace）
- subagent 会话可恢复：考古=ses_e288c9126ffeqC4mgSJFn5qImY，diff=ses_e288c51d0ffelzXKa2CAtm1P43
- 下文路径约定：`R/`、`T/` 为上述两仓绝对路径前缀；commit 短码均指 R 仓 git 历史

## 1. 版本线结论

**同属 1.x 线，可直接回合，无跨版本线适配问题。**
两仓均为 1.0.1-SNAPSHOT / JDK 1.8 / Boot 2.7.18 / Cloud 2021.0.9（R/pom.xml:9,31,32,44,45；T/pom.xml:9,35,48,49）。刻意 pin 项一致：snakeyaml 1.33（R/pom.xml:51）、spring-kafka 2.9.12（R/pom.xml:85）。R 仓根 pom 自初始导入后零修改（git log -- pom.xml 仅 6d617f2）。

R 仓 git 基线：初始导入 = 7d211b6（Initial commit，仅 README）+ 6d617f2（INIT project kdla，204 文件 19502 行，9 模块全量）；其后 21 个提交为 R 仓修改。

## 2. 基线对齐与 6 个分叉文件

R 初始导入 6d617f2 的 182 个 Java 文件 vs T 当前 HEAD 逐一 diff（忽略行尾）：**165 个完全一致，17 个有差异**。11 个为纯 import 风格/格式/语义等价写法差异（MQTT netty 包 8 文件、ResponseHandler、MqttPingHandler、SqlFilter contains vs indexOf）；**6 个实质分叉**：

| 文件 | 分叉点 |
|---|---|
| CommonConstants | R `LOG_TRACE_ID="nonce"` vs T `"traceId"` |
| MdcHelp | T 自有 `TRACE_ID_KEY="traceId"` |
| MdcAspect | trace 取值逻辑不同 |
| StopWatchWrapperAspect | R 基线日志带 traceId，T 不带 |
| CatchLogAspect | doBefore 取 trace 逻辑不同 |
| KdlaBodyReaderRequestFilter | R `String exclude` vs T `List<String> excludes` |

结论：R 拷贝源是 T 的更早形态（约 2024-06 前），T 的 traceId 链路已独立演化。这 6 个文件的修复**不能直接套 patch，须按 T 现状重定位**。

模块裁剪三锚点：① T/pom.xml:18,20,21,23 定义 supplement-dingding/monitor/reactor/job；② R 初始导入 6d617f2 pom modules 仅 9 个从未含这 4 个；③ R/pom.xml:10-20 仅 9 模块、R/AGENTS.md:53 明文不纳入 Maven build。

## 3. 回合项清单（112 项）

分级：A=bug修复 / B=性能优化 / C=功能增强 / D=结构重构 / E=依赖配置 / F=应用侧特有（不回合）。
每项：[级] 描述 | 证据 | 回合适用性。

### 批次 1：早期装配与 K1-K4（78adf79 / 54caefa / b3e1abf，2026-06）

1. [A] StopWatch/CatchLog/Mdc/RequestLog 四个 AutoConfigure 补 @ConditionalOnClass + @ConditionalOnProperty 开关（默认关）| 78adf79、54caefa；T 对应类无任何条件注解 | 适用
2. [A] 配置键统一 kdla.common.stop-watch.enabled→kdla.config.stop-watch.enabled、kdla.log.catch-log.enabled→kdla.config.catch-log.enabled | 54caefa | 修正（2026-11-14 复核）：T 源码旧键零命中，T 从未有 enable 开关，无失效风险；键名直接采用 R 新键，默认值随 S1
3. [A] UnifiedExceptionControllerAdvice 加 @ConditionalOnWebApplication(SERVLET) + @ConditionalOnProperty(kdla.exception.handler.enabled, matchIfMissing=true) | 78adf79 | 适用
4. [A] K1：CatchLogAspect.handleException 增 BaseException 兜底分支（下游自定义异常不再落 UNKNOWN_ERROR/error 级）| b3e1abf；R/CatchLogAspect.java:150-156 | 需适配（分叉文件）
5. [A] K2：KdlaBizDisLockService.biz() 无限等待 tryLock() → 有界 tryLock(waitTimeMs, leaseTimeMs)，unlock 加 isHeldByCurrentThread 守卫，InterruptedException 恢复中断位，入参判空 | b3e1abf+bb8022f；R/KdlaBizDisLockService.java:78-118 | 适用（T 同文件同缺陷）
6. [A] RedissonRedDisLock.lock(lockKey) null TimeUnit 防御 + 中断恢复 | b3e1abf | 适用
7. [A] CatchLogAspect 删重复嵌套 log.isWarnEnabled() | b3e1abf | 适用（分叉文件上下文适配）
8. [A] supplement-cache attachment/README.md 配置键文档修正（supplement.sequence.enable→supplement.cache.sequence.enable）| b3e1abf | 适用

### 批次 2：fix-project-wide-bugs 18 项（d6efa60，2026-07-10，openspec 已归档）

9. [A] SelfSnowflakeGeneratorHelp.getAutoIncreaseNumber() check-then-act → AtomicInteger.updateAndGet（修 traceId 并发重复）| d6efa60 | 适用
10. [A] DistributeLockedAspect：删 blanket catch(Exception)（业务异常透传，throwEx=false 不再吞异常返回 null）；unlock 包 try/catch 防覆盖业务异常；SpEL StandardEvaluationContext→SimpleEvaluationContext.forReadOnlyDataBinding()（防注入）| d6efa60；spec: distributed-lock-safety | 适用（行为变更需知会下游）
11. [A] CatchLogAspect.doBefore() 无参方法 args.get(0) 越界 → args.isEmpty() 守卫直接生成 traceId | d6efa60 | 需适配（分叉文件）
12. [A] SequenceNoGenerator/V2：规则 split 长度校验抛 BizException；异常时 noCacheMap.clear()→remove(code)（防缓存雪崩）；new Long()→Long.valueOf；StringBuffer→StringBuilder；V2 非缓存模式补 updateById() DB 写回（修重复流水号）| d6efa60；spec: sequence-no-correctness | 适用（数据正确性关键）
13. [A] AsyncThreadHelp.initExecutorService() 重建前 shutdown 旧池（修线程泄漏）| d6efa60 | 适用
14. [A] RedissonLockFactory REPLICATE 模式 passwords/nodes 数量校验 fail-fast | d6efa60 | 适用
15. [A] SpringRedisHelp 23 处 e.printStackTrace()→log.error + @Slf4j | d6efa60；T 当前仍 23 处（已抽查证实）| 适用
16. [A] UnifiedExceptionControllerAdvice DuplicateKeyException 误 import（snakeyaml→org.springframework.dao），409 分支从未生效修复 | d6efa60；T UnifiedExceptionControllerAdvice.java:19 仍是 snakeyaml import（已抽查证实）| 适用

### 批次 3：16 项缺陷（bb8022f，2026-08-28）

17. [A] MqttPingHandler 心跳超时判断反转修复 | bb8022f | 适用（该文件仅 import 位置差异）
18. [A] NettyHttpServer 启动即关闭问题修复 | bb8022f | 适用
19. [A] AESUtil 零 IV → SecureRandom 随机 IV，解密向后兼容旧密文 | bb8022f；T AESUtil.java:38 仍 zero IV | 适用（安全关键）
20. [A] FileChunkTransmitHelp/SslContextFactory try-with-resources；AsyncThreadHelp awaitTermination 后恢复中断位；KdlaBodyReaderRequestFilter finally 清 MDC | bb8022f | 适用（Filter 分叉需适配）
21. [A] MdcAspect/CatchLogAspect/SequenceNoGenerator/MachineIdHelp 空值防护；FieldValueFindHelp break 误用（应为 continue 语义）修正；RedissonConfigProperties REPLICATE 密码数量校验 | bb8022f | 适用
22. [A] RequestLogAspect 可靠性重构（165 行，null 安全）| bb8022f | 适用

### 批次 4：审查回归修复 8 项（9cdbe79，2026-09-04）

23. [A] SequenceNoConfigure 3 个配置键 @ConditionalOnProperty 语义回归（要求全 true）→ @ConditionalOnExpression OR 逻辑 | 9cdbe79；R/SequenceNoConfigure.java:27 | 适用
24. [A] AESUtil 加密输出加 v1: 前缀（v1:Base64(IV+密文)），消除新旧密文 1/256 概率静默乱码歧义 | 9cdbe79；R/AESUtil.java:21 | 适用（新密文旧代码不可解，[BREAKING]）
25. [A] SpringRedisHelp.getExpire 恢复（d6efa60 误删）并标 @Deprecated | 9cdbe79；R/SpringRedisHelp.java:55-58 | 适用（T 从未删过，仅需加 @Deprecated 或跳过）
26. [A] NettyHttpServer bind 异常关闭 EventLoopGroup 防泄漏 | 9cdbe79 | 适用
27. [A] DBLengthValid/EnumValid JUL→SLF4J | 9cdbe79 | 适用

### 批次 5：Critical 14 项（d7d7bfe，2026-10-29，25 回归测试）

28. [A] BigDecimalHelp 过滤方法 toString(20) 20 进制笔误 → 十进制 | d7d7bfe | 适用
29. [A] KdlaStringHelp 驼峰转换 contains 方向反了 | d7d7bfe | 适用
30. [A] BytesUtil.floatToLe 字节序错误（intToBe→intToLe）| d7d7bfe | 适用
31. [A] ThreadPoolHelp 默认池共享 static 无界队列 → 每池独立有界队列 | d7d7bfe | 适用
32. [A] Sm4Util 密钥编码 Hex/Base64 不一致统一为 Base64 | d7d7bfe | 适用（存量密文兼容归入 D2）
33. [A] CatchLogAspect：doBefore JSON 强解析加降级保护；@Around 仅返回类型可赋值 Response 才转失败响应、Error 一律 rethrow（修事务不回滚）| d7d7bfe | 需适配（分叉文件）
34. [A] KdlaHttpServletRequestWrapper body 空兜底 + UTF-8 显式编码 | d7d7bfe | 适用
35. [A] SqlFilter 黑名单子串 → 白名单正则 ^[a-zA-Z0-9_]+$ + 长度上限（修 update_time 误杀与 union 漏防）| d7d7bfe；R/SqlFilter.java:19,37 | 适用（SQL 注入防线关键）
36. [A] SequenceNoGenerator/V2 双锁单解死锁删除 + isHeldByCurrentThread 守卫；溢出改抛 BizException 禁止清零重复发号 | d7d7bfe | 适用（数据正确性关键，[BREAKING] 归入 D2-⑩）
37. [A] MachineIdHelp Redis SETNX 原子注册（修雪花 machineId 并发竞态致 ID 重复）| d7d7bfe；R/MachineIdHelp.java:94 | 适用
38. [A] MqttChannelHandler/MqttClientImpl QoS2 ByteBuf retain/release 配对（修 use-after-free 与双重释放）| d7d7bfe | 适用

### 批次 6：common/model/exception 必改 23 项（7d86361，39 测试）

39. [A] MdcAspect/MdcHelp 保存-恢复旧值替代 MDC.clear()（修嵌套丢外层 traceId）+ SpEL 解析降级 | 7d86361；R/MdcAspect.java:46,56 | 需适配（分叉文件；T 的 MDC key 已是 "traceId"，R 的「nonce 命名误导」Nit 在 T 不存在）
40. [A] StopWatchWrapperAspect 改 signature.getMethod()+AnnotationUtils 判空 | 7d86361 | 需适配（分叉文件）
41. [A] FileChunkTransmitHelp append→覆盖写、IOException 上抛、流级联关闭；ObjectUtil flush 防截断；JacksonUtil 异常带 cause；PinYinHelp 空串防护；BigDecimalHelp.compareTo 判零 | 7d86361 | 适用
42. [A] ThreadPoolHelp 单线程池改 LinkedBlockingQueue + Builder 全参默认值防拆箱 NPE；AsyncThreadHelp volatile+synchronized 防并发重建 + prettyPrint 判空 | 7d86361 | 适用
43. [A] LocalDateTimeHelp YearMonth 跨年误判；DateHelp 按 pattern 分派解析 | 7d86361 | 适用
44. [A] AESUtil 异常消息删 password 明文回显；Sm2Util UTF-8 显式编码；ResponseHandler 补 setMessage | 7d86361 | 适用
45. [A] UnifiedExceptionControllerAdvice：删 NIO AccessDeniedException 误 import、拆 @ConditionalOnClass 守护的 Security 内部 advice（403）、编码 null 回退 UTF-8、Throwable 兜底不回显 getMessage() | 7d86361；R/UnifiedExceptionControllerAdvice.java:59-78 | 适用

### 批次 7：log/dal/domain/validator 必改 18 项（9e4d29c，64 测试）

46. [A] LogInterceptor MDC 放裸 traceId 替代 JSON 串（T 侧同 bug 已坐实：T 的 CatchLogAspect/LogInterceptor 把 traceIdInfo.toJSONString() 整个 JSON 串写入 MDC 值）| 9e4d29c | 适用
47. [A] CatchLogAspect 嵌套切面保存-恢复外层 traceId | 9e4d29c | 需适配（分叉文件）
48. [A] KdlaBodyReaderRequestFilter 补 GET 排除对齐注释 | 9e4d29c；R/KdlaBodyReaderRequestFilter.java:87 | 需适配（T 已重构为 List<String> excludes，GET 排除逻辑可能已不同）
49. [A] IPageQuery try-parse + limit 封顶 500；PlusPageQuery 基本类型判 >0 替代恒真 nonNull + params 判空 | 9e4d29c；R/IPageQuery.java:22,31-33 | 适用
50. [A] ApplicationContextHelp 空 catch 记日志 + requireContext 明确异常；Page(domain) sameValueAs 按字段比较 + pageSize<=0 防溢出 | 9e4d29c | 适用
51. [A] ObjectValidator：删 sun.misc.Unsafe、排除列表 isAssignableFrom、深度 16 + IdentityHashMap 防环、遍历父类字段 | 9e4d29c；R/ObjectValidator.java:51,165 | 适用
52. [A] DBLengthValid BigDecimal 全程比较修 pow 溢出 | 9e4d29c | 适用（[BREAKING] 语义修正随批）
53. [A] CommonValidator 删 Locale.setDefault 全局副作用 | 9e4d29c | 适用
54. [A] EsSourceData/EsBaseData @SuperBuilder 修父字段丢失（伴生 @NoArgsConstructor）| 9e4d29c | 适用
55. [A] [BREAKING] EnumValid null 值校验 FALSE→TRUE（对齐 JSR-303）+ 方法按可赋值性匹配；IEnumValid.checkForNull 默认 false + Objects.equals 防 NPE | 9e4d29c；R/EnumValid.java:61-63 | 适用（归入 D2-③④）
56. [A] [BREAKING] BaseAssert isBlank 重命名 notBlank（语义反转修正）、isNull 删除（与 notNull 签名冲突且零调用）| 9e4d29c；R/BaseAssert.java:155-170 | 需裁定（D2-②）

### 批次 8：supplement/supplement-cache 必改 13 项（32ba8d2，13 测试）

57. [A] RedissonConfigProperties/RedissonLockFactory 地址解析 fail-fast（空/非法节点启动即抛 IllegalArgumentException，不再静默零节点 client）| 32ba8d2 | 适用（[BREAKING] 归入 D2）
58. [A] NettyHttpServer bind 失败 shutdownGracefully+close() 显式释放、异常响应固定文案不泄漏堆栈 | 32ba8d2 | 适用
59. [A] MqttClientImpl 自建 eventLoop disconnect 时关闭防线程泄漏；BaseHttpClient.post headerMap null 安全 | 32ba8d2 | 适用
60. [A] [BREAKING] VertxHttpConfigure trust-all 硬编码改 kdla.http.ssl.trust-all 默认 false（开启时 log.warn MITM）；VertxHttpClient content-type 统一小写匹配+回调异常改走 future+新增 kdla.http.request-timeout 默认 30s；NettyHttpClient 端口优先 URL 显式值缺省按协议 80/443+connectTimeout 默认 3000 | 32ba8d2；R/VertxHttpConfigure.java:47-48,80 | 适用（归入 D2-⑨）
61. [A] KdlaExcelWrite try/finally finish()（修文件残缺与句柄泄漏）| 32ba8d2 | 适用
62. [A] SequenceNoGenerator 日期翻越重置先于溢出检查 + 长度段非数字显式抛 BizException + 异常根因透传 | 32ba8d2 | 适用
63. [A] MachineIdHelp.destroy SETNX 覆盖改 del（消除 1 秒窗口阻塞其他实例注册）| 32ba8d2；R/MachineIdHelp.java:119-120 | 适用（[BREAKING] 归入 D2）
64. [A] 4 处恒真 @ConditionalOnClass（Mdc/StopWatch/CatchLog/RequestLog 指向同 jar 内部类）改指 org.aspectj.lang.annotation.Aspect（宿主缺 aspectj 不再误装配）| 691478b | 适用

### 批次 9：审计切片 1-4（0209b43 / 89fc933 / b9695b2 / e3a4acd，2026-11-13）

65. [A] RedissonLockFactory/RedissonConfigProperties 去除 config.toYAML() 密码明文 INFO 日志，改记 address+codec+threads 非敏感项 | 0209b43 | 适用（安全）
66. [A] MachineIdHelp 随机回退取号 INFO→ERROR 显式告警撞号风险 + localIp 字段赋值修复（日志恒 null）| 0209b43；R/MachineIdHelp.java:141 | 适用
67. [A] SpringRedisHelp.unLock Lua 脚本 key/value 改经 RedisTemplate 序列化器（原裸 getBytes 与加锁序列化不一致致解锁永远静默失败）+ lock() 的 setIfAbsent 返回值 Boolean.TRUE.equals 防拆箱 NPE | 0209b43；R/SpringRedisHelp.java:206-209 | 适用（高价值隐蔽 bug）
68. [A] KdlaCafCache expireAfterWrite→expireAfter(Expiry)（使 put(key,value,duration) 的 per-key TTL 真实生效，原恒空 Optional 静默忽略）| 0209b43；R/KdlaCafCache.java:83-92 | 适用
69. [A] SequenceNoGeneratorV2.generateSequenceNo 补 cfg 判空抛 BizException | 0209b43 | 适用
70. [A] UnifiedExceptionControllerAdvice 认证 handler 误绑 javax.security.sasl.AuthenticationException → spring-security 的 AuthenticationException 并移入 @ConditionalOnClass 隔离内层 Advice（原认证失败落兜底返回「系统繁忙」而非 401）| 89fc933 | 适用
71. [A] LogInterceptor.printHttpRequestHeaderInfo 不再 new KdlaHttpServletRequestWrapper 消费原始输入流（原致 controller 读空 body），仅当已被 KdlaBodyReaderRequestFilter 缓存时打印；afterCompletion BEGIN_TIMESTAMP 判空防拆箱 NPE（-1=未计时）| 89fc933 | 适用
72. [A] CatchLogAspect 未知异常不再外传 e.getMessage()（SQL/内部路径泄漏）改通用文案对齐 handleThrowable 既有约定 | 89fc933 | 需适配（分叉文件）
73. [A] BytesUtil.reverse(data,offset,length) 交换索引错位修复（rev=len-i 未加 offset，offset>0 必错且可能越界）→ 标准区间反转+越界显式抛 IndexOutOfBoundsException，无参重载行为不变 | b9695b2 | 适用
74. [A] [BREAKING-兼容分支] Sm4Util 加密 ECB→CBC+随机 IV 输出 v1: 前缀（对齐 AESUtil 既有约定），decrypt 无前缀旧 ECB 密文走兼容分支；新增 generateSecureKey 真随机 API，旧 generateKey 标 @Deprecated | b9695b2；R/Sm4Util.java:37,50,125；T Sm4Util.java:36 仍 ECB（已抽查证实）| 适用（归入 D2-⑦）
75. [A] Sm2Util 新增 generateSecureKeyPair 真随机 API，旧 generateKeyPair(keySrc) 标 @Deprecated（实测当前 BC/JDK8 下同 keySrc 两次生成私钥并不相同，文档宣称的确定性实际不成立）| b9695b2 | 适用
76. [A] MultiResponse.buildSuccess(items,totalCount) 静默丢弃 totalCount 参数（类中无该字段）→ 新增 totalCount 字段+getter/setter 并在 buildSuccess 回填，单参重载默认 items.size() 行为不变，JSON 出参新增字段向前兼容 | e3a4acd；R/MultiResponse.java:25,52 | 适用

### B 级 — 性能优化

77. [B] LocalIpHelp.getLocalIp4Address 加 static volatile 缓存（traceId 生成每请求枚举网卡/建 DatagramSocket 热路径，解析一次复用）| b9695b2；R/LocalIpHelp.java:187-197 | 适用
78. [B] IPageQuery limit 封顶 500 防超大分页拖垮 DB | 9e4d29c | 适用（与 49 同源）
79. [B] ThreadPoolHelp 默认池独立有界队列防 OOM | d7d7bfe | 适用（与 31 同源）
80. [B] SequenceNoGenerator.fillLeft StringBuffer→StringBuilder | d6efa60 | 适用（微优化）

### C 级 — 功能增强/新增

81. [C] 新增 MpBaseDo（@TableLogic Integer deleted，适配 MyBatis-Plus 逻辑删除；K3 方案 A）| b3e1abf；R/kdla-component-dal/.../common/MpBaseDo.java（T 该目录仅 BaseDo/DoConstants）| 适用（纯增量）
82. [C] 新增 KdlaLockProperties（kdla.lock.waitTimeMs 默认 3000 / kdla.lock.leaseTimeMs 默认 30000）| b3e1abf | 适用
83. [C] 新增 VertxConfigure 统一提供 Vertx Bean，删 VertxHttpConfigure/VertxMqttConfigure 内重复 vertx() | 6ff6775 | 适用（注意 T dingding 模块 spring.factories 跨模块引用 VertxHttpConfigure，回合后核对装配去重）
84. [C] 新增 LogSanitizeUtil（Header 黑名单脱敏 authorization/cookie/password/token/mobile + 2KB 截断）接入 LogInterceptor/CatchLogAspect/RequestLogAspect 三处 | 9e4d29c；R/kdla-component-log/.../sanitize/LogSanitizeUtil.java | 适用（安全增强）
85. [C] 新增 ValidatorAutoConfigure 注册 CommonValidator（开关 kdla.config.validator.enabled 默认关）| 1fa3289 | 适用
86. [C] Sm4Util.generateSecureKey() / Sm2Util.generateSecureKeyPair() 真随机密钥 API，旧确定性 API 标 @Deprecated | b9695b2 | 适用（与 74/75 同源）
87. [C] MultiResponse 新增 totalCount 字段（JSON 出参新增字段向前兼容）| e3a4acd | 适用（与 76 同源）
88. [C] 新配置项 kdla.http.ssl.trust-all（默认 false）、kdla.http.request-timeout（默认 30s，原先无超时）| 32ba8d2 | 适用（[BREAKING] 归入 D2-⑨）
89. [C] 测试设施从零到 64 测试类/162 用例（全模块 src/test/java），各模块 pom 补 spring-boot-starter-test(provided)+mockito-junit-jupiter，exception 补 spring-security-core(provided,optional) | d6efa60/d7d7bfe/7d86361/9e4d29c/32ba8d2/b9695b2/e3a4acd；find 计数=64 | 适用（T 全仓无测试，纯增量；归入 D4 裁定）
90. [C] SpringRedisHelp.getExpire 标 @Deprecated（引导 RedisTemplate 直调）| 9cdbe79 | 适用

### D 级 — 结构重构

91. [D] 装配契约统一（核心结构项）：15 个未注册配置类补进 spring.factories（log 3、supplement 6、supplement-cache 2、validator 1、exception 恢复 1 等）；统一模式 = 真实外部类 @ConditionalOnClass + @ConditionalOnProperty 默认关 + @Bean 一律 COBM；FieldValueFindHelp/CommonValidator/DistributeLockedAspect 移除 @Component 立体注解消除双定义；删除整文件注释残留 RedissonClientConfig.java | 1fa3289；R 7 个 spring.factories 已核验；T 现状：exception 注释、log 仅 1 类、cache 仅 1 类、supplement/validator 无 | 适用（T 存在同样死类/未注册问题；T 特有 dingding/monitor 模块需同规则审视，归入 D5 裁定）
92. [D] [BREAKING] RedissonAutoConfigure：加 kdla.cache.redisson.enabled 开关（默认开→默认关）+ OnClass 改真实 RedissonClient + 锁前缀 appId 缺失回落由「每实例雪花 ID」改固定值 "kdla"+log.warn（修多实例互斥失效）+ 注册 DistributeLockedAspect | 1fa3289；R/RedissonAutoConfigure.java:36-37,41,64-66,74-76；T RedissonAutoConfigure.java:29,51 仍内部类 OnClass+雪花回落 | 适用（默认关归入 D2-⑤）
93. [D] UnifiedExceptionControllerAdvice 恢复 spring.factories 注册（默认开 matchIfMissing=true，R 仓用户裁定）+ @Order(LOWEST_PRECEDENCE) + Security 异常拆 @ConditionalOnClass 守护内层静态 Advice | 1fa3289/7d86361/89fc933；T spring.factories 仍注释 | 适用（默认开归入 D2-⑥，T 下游从「需 @Import」变「自动生效」需知会）
94. [D] CacheAutoConfigure 的 @EnableCaching 拆隔离内部静态配置类 + kdla.cache.enabled 开关（避免注册即全局改变宿主缓存行为）+ @ConditionalOnBean(RedisTemplate) | 1fa3289 | 适用
95. [D] SequenceNoConfigure 注册闭环（K4）：真实 @ConditionalOnClass(RedissonClient) + @AutoConfigureAfter(RedissonAutoConfigure) + @ConditionalOnBean(RedissonRedDisLock) | 1fa3289/9cdbe79；R/SequenceNoConfigure.java:27-34 | 适用
96. [D] [BREAKING] Response 删 success 字段，isSuccess() 由 code 派生唯一真源（序列化输出不变，反序列化 success 成未知字段）| 7d86361；R/Response.java:28,49；T Response.java:20 仍有字段（已抽查证实）| ❌ 不回合（B1 否决：T 保留 success 字段，Response.java 不动；R 相关测试按 T 现状适配）
97. [D] RequestLogAspect 整体重构（可靠性+null 安全，165 行）| bb8022f | 适用（与 22 同源）
98. [D] 文档体系新建：docs/architecture.md（Mermaid 架构图/模块表/数据流/FAQ）、docs/project-map.md、CHANGELOG.md、AGENTS.md、README 全面重写 | 94bdcbf/6ff6775/c3fb79c/691478b | 需适配（含 R 特有信息：midea Nexus、9 模块、IntelliJ 4 模块说明；改写为 13 模块/enncloud 语境，归入 D6 裁定）

### E 级 — 依赖配置变更

99. [E] exception pom 补 spring-jdbc(provided)（DuplicateKeyException 解析）| d6efa60 | 适用
100. [E] validator pom 补 slf4j-api(provided) | 9cdbe79 | 适用
101. [E] exception pom 补 spring-security-core(provided, optional) | 7d86361 | 适用
102. [E] 9 个模块 pom 补 spring-boot-starter-test(provided) + mockito-junit-jupiter | d7d7bfe/7d86361/9e4d29c/32ba8d2 | 适用（配合 89，归入 D4）
103. [E] validator pom：fluent-validator-spring.version property 更名 fluent-validator.version、javax.annotation-api/validation-api 显式版本、删 logback-classic/core 两依赖 | 78adf79+54caefa 净效果 | 需适配（核对 T validator pom 现状后取舍）
104. [E] 78adf79 曾将 6 模块 pom 大量依赖 provided→compile，54caefa 全部改回 provided（净效果为零）| 78adf79/54caefa | 不适用（净变更已回滚；佐证 provided 策略是刻意维持）
105. [E] R 根 pom 零修改（第三方版本 properties 与 T 完全一致）| git log -- pom.xml 仅 6d617f2 | 无回合项

### F 级 — 应用侧特有（不回合）

106. [F] distributionManagement/repositories 指向美的 Nexus（mvn.midea.com，rdc-releases/rdc-snapshots）| R/pom.xml:90-114 | 不适用（T 保持 nexus.enncloud.cn/maven-releases）
107. [F] 模块裁剪（不含 reactor/dingding/job/monitor）| 三锚点见第 2 节 | 不适用（T 保留 13 模块）
108. [F] AI 工具目录 .agents/.claude/.qoder/.trae/.workspace/.aiassistant/.mybatis | 8d831aa、d6efa60 | 不适用
109. [F] openspec/ 规格工作流目录（config.yaml + 2 change）| 78adf79 起 | 不适用（spec 文档内容可作回合验收标准参考）
110. [F] docs/agents/（domain/issue-tracker/triage-labels，引用不存在的 .scratch/、CONTEXT.md）| c3fb79c | 不适用（T 已自建）
111. [F] AGENTS.md 中 JDK 路径、midea deploy 说明、IntelliJ-only 4 模块说明 | R/AGENTS.md:11,53,161 | 不适用
112. [F] .gitignore AI 目录增补反复调整；历史文档 KDLA-优化计划.md（916 行，已删）| 各提交 | 不适用

## 4. openspec / CHANGELOG 盘点（R 仓）

openspec/changes/2026-10-29-assembly-contract-and-bug-fix（活跃，tasks 6.5 归档/6.6 人工验证未勾）：proposal=装配契约破损+Critical 15 项+必改约 60 项，用户裁定「死类补注册不删除」，Non-goals=不做 SB3/JDK21 迁移、不修 Nit、不 push/deploy；design=统一装配模式+12 条关键决策；tasks=6 批次，1-5 与 6.1-6.4 已勾。
openspec/changes/archive/2026-07-10-fix-project-wide-bugs（已归档）：18 bug/4 模块/9 文件；specs 3 份——distributed-lock-safety（业务异常透传/SpEL 受限/unlock 守卫）、sequence-no-correctness（非缓存模式必须持久化/缓存失效限定失败 code/规则格式前置校验）、traceid-atomicity（自增计数器原子性/doBefore 无参不越界）。**这 3 份 spec 可直接作 T 仓回合的验收标准。**

CHANGELOG.md 4 个版本块（均 1.0.1-SNAPSHOT）：2026-10-29（装配契约+Critical 14+必改 54，141 回归测试，10 项 BREAKING 清单）、2026-09-04（批次 4）、2026-08-28（批次 3）、2026-07-10（批次 2）。**CHANGELOG 未覆盖 2026-11-13 审计切片 1-4（含 Sm4 CBC 迁移 BREAKING）——文档滞后于 git，以 git 为准。**

docs/kdla-optimization.md：K1-K5 优化建议（来源 inventory-middle 接入反馈）——K1 异常识别（已修 b3e1abf）、K2 锁死锁（已修 b3e1abf+bb8022f）、K3 BaseDo/MP 逻辑删除（部分，MpBaseDo）、K4 SequenceNo 注册（已闭环 9cdbe79+1fa3289）、**K5 ApplicationContextHelp 不可 mock（open 未实施）**；Nit 级登记 53 项「只登记不修」（common 27/model+exception 7/log+dal+domain+validator 13/supplement+cache 6，其中 Sm4 ECB、MultiResponse totalCount、LocalIpHelp 缓存、MachineIdHelp TTL 后被切片 1-4 修复但表格未更新）；人工验证清单 V1-V8（Redisson fail-fast/真实 Redis 锁互斥/sequence 并发/NettyHttpServer 端口/MQTT 线程泄漏/Vertx HTTPS 默认策略/VertxHttpClient 行为/宿主试点接入）——**均待用户回贴，无执行记录**。

## 5. R 仓文档与 git 矛盾（6 处，一律以 git/代码为准）

1. R/docs/architecture.md:79,87,98,201 称 Redisson 默认开、exception advice 需 @Import——与 1fa3289 后代码矛盾（RedissonAutoConfigure.java:37 matchIfMissing=false；exception spring.factories 已恢复）。architecture.md 写于 94bdcbf 未同步。
2. R/docs/project-map.md:153 称 4 个测试类——实际 64 类/162 用例。
3. R/docs/kdla-optimization.md:12-13 K1/K2 状态仍标「待处理」——与 R/AGENTS.md:171-172 及 git 矛盾（漏更）。
4. R/docs/kdla-optimization.md:270 引用 openspec/changes/fix-project-wide-bugs/——已改名 archive/2026-07-10-*，路径失效。
5. Critical 项数：CHANGELOG.md:21 与 d7d7bfe 称 14 项，openspec proposal.md:8/tasks.md:19 称 15 项——以 git 14 项为准（tasks 2.1-2.5 实际 14 落点）。
6. CHANGELOG 缺 2026-11-13 四切片版本块。

## 6. 无法判定项（6 项）

1. openspec tasks 5.2「无法复现」具体清单：33 项 supplement 必改行号在审计中截断丢失（proposal.md:21），仅 32ba8d2 的 13 项实修记录，差额约 20 项去向无法判定。
2. 人工验证清单 V1-V8 是否执行、结果如何：无记录，tasks 6.5/6.6 未勾。涉及真实 Redis/端口/宿主环境的修复实际效果未经人工确认。
3. R/AGENTS.md:53「4 模块在 IntelliJ config」：.idea 不入 git 无法核验；「不在 Maven build」已由 pom+目录双证据坐实。
4. R 基线的确切来源（T 的哪个历史提交或其他 fork）：6 文件分叉与 T 1.0.0-SNAPSHOT 分支抽查也不一致，无法定位。
5. K5（ApplicationContextHelp 不可 mock）是否实施：三处文档标 open，无提交无 change，属「明确未做」。
6. Nit 53 项中未被切片 1-4 覆盖项（MdcAspect 无 @Order、banner.txt 劫持宿主、KdlaExcelWrite 多 sheet 覆盖、MachineIdHelp 无 TTL 续期、LogTraceHolder InheritableThreadLocal 脏值等）处置计划：无后续任务，是否一并处理需用户裁定。

## 7. T 特有 4 模块编译影响 = 零（diff subagent 补充取证）

- reactor：全模块无任何跨模块引用（import+全限定名双 grep 0 命中），完全自包含。
- supplement-job：代码级零跨模块引用；T/kdla-component-supplement-job/pom.xml 声明依赖 7 个 kdla 模块但代码一个都没用（纯 pom 级声明）。附带发现：job pom 依赖 supplement-dingding（若反向移植需连 dingding 一起搬，本次 R→T 无此问题）。
- supplement-dingding：三个依赖点全安全——CharsetEnum/ContentTypeEnum/MsgTypeEnum（common）、BaseModel/IEnum（model）两仓逐字节相同；VertxHttpClient diff 76 行但三个 sendRequest 重载签名两仓一致、构造器未变，R 改动是内部修复+纯增量 setter setRequestTimeoutMs，dingding 调用零修改可编译。行为面反而改善：DingDingServiceImpl 四处 futureRes.get() 均无超时参数，T 版远端挂起永久阻塞（正是 R 修的 bug），R 版 30s 后 TimeoutException→catch→FAIL 码。
- supplement-monitor：唯一跨模块 import 是 T/kdla-component-supplement-monitor/.../PrometheusAutoConfigure.java:9 的 StopWatchWrapperAspect——死 import（25 行文件体只注册 PrometheusPointAspect 从未使用）。R 对该切面修改均为内部实现，公开 API 不变。
- Response.success 删除（BREAKING）影响面：4 个特有模块零引用 Response 系列；dingding 的 result.isSuccess() 是 DingDingAlertResult 自有方法（"0".equals(errcode)）与之无关。影响仅限下游应用。
- LOG_TRACE_ID（R="nonce" vs T="traceId"）：T 仓内消费方仅 log 模块 2 文件；T 特有 4 模块零引用；两仓均无 logback*.xml → MDC 键名实际影响面 = 下游应用 logback pattern 的 %X{traceId}/%X{nonce}。R 保留 nonce 非文档化决策（CHANGELOG 无取舍记载）。

## 8. 回合执行要点

1. 同版本线，约 90% A/B/C 级项在 T 有完全相同缺陷代码（165/182 文件基线一致），patch 可套用。
2. 6 个分叉文件必须人工适配（见第 2 节表），R 基于 "nonce"/String exclude 的修复需按 T 的 "traceId"/List<String> excludes 现状重定位。
3. ~~10+ 项 BREAKING 需用户逐项裁定~~ 已完成（2026-11-14，第 10 节 25 项全部回填）：仅 B1 否决（Response 保留 success 字段），其余 BREAKING 项全接受。
4. 装配契约回合需扩展到 T 特有 4 模块；dingding 跨模块注册 VertxHttpConfigure 与 supplement 自身注册（83 项）会重叠，需核对去重。
5. 建议连同 64 测试类一起回合作为行为锁定；T 需先补各模块 test 依赖（102 项）。
6. 文档回合需改写语境（13 模块/enncloud）；CHANGELOG 需补 2026-11-13 切片 1-4 版本块。
7. R 仓人工验证清单 V1-V8 未回贴——对应修复项在 T 回合后同样需要人工验证（真实 Redis/端口/MQTT/宿主试点）。

## 9. 现状核查补充（2026-11-14，主 agent 直接核验）

T 仓 spring.factories 注册现状（逐模块 cat 核验）：
- common：MdcAutoConfigure + StopWatchAutoConfigure 已注册，类上仅 @ConditionalOnMissingBean（@Bean 方法级），**无 @ConditionalOnProperty/@ConditionalOnClass → 现状无条件开启**
- log：仅 CatchLogAutoConfigure 已注册，**无开关 → 现状开启**
- supplement-cache：仅 RedissonAutoConfigure 已注册，@ConditionalOnClass(RedissonRedDisLock.class) 是同 jar 内部类恒真，**无开关 → 现状开启（classpath 有依赖即装配）**
- exception：UnifiedExceptionControllerAdvice 注册**整体被注释 → 现状关闭（需 @Import）**
- domain：DomainAutoConfigure 已注册（R 未改动，保持）
- model/dal/validator/supplement：无 spring.factories（与 R 报告一致）

**关键推论**：R 的「注册+默认关」契约直接套到 T，会使 Mdc/StopWatch/CatchLog/Redisson 四项从开→关，T 下游相关功能静默失效（违反失败可见原则）→ 单列 S1 裁定。

## 10. 完整裁定清单（25 项，裁定结果回填「结果」列，作为唯一真源）

Nit 53 项完整清单与 V1-V8 验证步骤已落盘 `.scratch/kdla-backport/nit-inventory.md`。

### P 组：范围与原则

| # | 决策点 | 现状+证据 | 建议 | 影响面 | 结果 |
|---|---|---|---|---|---|
| P1 | 回合总范围 | A/B/C/D/E 级 104 项适用或需适配（第 3 节），F 级 8 项不回合 | 全部回合，6 分叉文件按 T 现状重定位 | 9 共有模块 | ✅ 接受：全量回合（扣除 B1 否决的 96 项），6 分叉文件按 T 现状重定位 |
| P2 | Nit 53 项处置 | R 明文「只登记不修」；其中 4 项已被切片修复、4 项部分缓解（nit-inventory.md 已标注） | 不修，仅继承登记；如需个别修按 C/M/L/S 编号勾选 | 勾选则扩大范围 | ✅ 接受：不修，仅继承登记（nit-inventory.md 随文档回合带入 T） |
| P3 | K5（ApplicationContextHelp 静态不可 mock）是否本次实施 | R 三处文档标 open 无提交（R/docs/kdla-optimization.md:214）；属 API 设计变更 | 本次不做，登记后续独立任务 | API 设计/下游用法 | ✅ 接受：本次不做，登记后续独立任务 |
| P4 | supplement 必改 33 项中约 20 项行号丢失差额 | R openspec proposal.md:21 截断丢失，仅 32ba8d2 的 13 项有实修记录 | 回合时对 T supplement/supplement-cache 重跑一轮聚焦审计补齐（基准复用 R openspec proposal）；备选=只回合 13 项有记录项 | 审计成本一轮 subagent | ✅ 接受：实现期对 T supplement/supplement-cache 重跑聚焦审计（基准=R openspec proposal），审计作独立票、产出补票 |

### B 组：BREAKING/行为变化逐项（R CHANGELOG 10 项 + 切片新增）

| # | 决策点 | 现状+证据 | 建议 | 影响面 | 结果 |
|---|---|---|---|---|---|
| B1 | Response 删 success 字段，isSuccess() 由 code 派生 | T Response.java:20 仍有字段；T 特有 4 模块零引用（已核验）| 接受（序列化输出不变，反序列化 success 成未知字段）| 下游编译/反序列化 | ❌ 否决（用户裁定「Response 不删 success」）：T 保留 success 字段，96 项不回合，Response.java 不动；R 的 Response 相关测试回合时按 T 现状适配 |
| B2 | BaseAssert.isBlank→notBlank 重命名、isNull 删除 | R/BaseAssert.java:155-170；语义反转修正，isNull 零调用 | 接受 | 下游编译 | ✅ 接受 |
| B3 | EnumValid null 值校验 FALSE→TRUE（对齐 JSR-303）| R/EnumValid.java:61-63 | 接受 | 下游校验行为 | ✅ 接受 |
| B4 | IEnumValid.checkForNull 默认 false + Objects.equals 防 NPE | 9e4d29c | 接受 | 下游校验行为 | ✅ 接受 |
| B5 | Redisson 加 kdla.cache.redisson.enabled 开关且默认关 | T 现状无条件开启（第 9 节）；R/RedissonAutoConfigure.java:36-37 | 与 S1 联动裁定；单独看建议接受默认关 | 下游 Redisson 功能 | ✅ 随 S1：保持默认开（matchIfMissing=true）+ OnClass 改真实 RedissonClient + 锁前缀 appId 回落固定值 "kdla"+warn + COBM |
| B6 | Redisson 地址解析 fail-fast（空/非法节点启动即抛）| 32ba8d2；现状静默产出零节点 client | 接受（配置错误从「运行但锁不可用」变「启动失败」，属失败可见改进）| 配置错误的下游启动失败 | ✅ 接受（CHANGELOG 标注：配置错误场景从静默变启动失败）|
| B7 | exception advice 恢复注册且默认开（matchIfMissing=true）| T spring.factories 现状注释=关闭；R 仓用户曾裁定默认开 | 接受需知会：T 下游全局异常响应格式将自动改变；备选=注册但默认关 | 下游异常响应格式 | ✅ 接受：恢复注册+默认开，CHANGELOG 显式标注下游异常响应格式变化 |
| B8 | Sm4 ECB→CBC+随机 IV+v1: 前缀，旧密文兼容分支 | T Sm4Util.java:36 仍 ECB（已抽查证实）；b9695b2 | 接受（密码学安全必修；旧密文可解，新密文旧代码不可解）| 混部新旧版本互解 | ✅ 接受（含旧密文兼容分支；CHANGELOG 标注新密文旧代码不可解）|
| B9 | Sm4 密钥编码 Hex→Base64 统一 | d7d7bfe；⚠️ 下游若有存量 Hex 编码密钥将无法读取，R 无密钥兼容分支 | 需用户确认下游是否有存量 Sm4 密钥；有→加密钥双格式兼容分支；无→直接统一 | 存量密钥/密文 | ✅ 接受：直接统一 Base64，不加密钥兼容分支（用户裁定「可以改」）|
| B10 | AES 随机 IV + v1: 前缀（旧密文兼容解密）| T AESUtil.java:38 仍 zero IV；bb8022f+9cdbe79 | 接受（同 B8 模式）| 混部新旧版本互解 | ✅ 接受（同 B8 模式：随机 IV+v1: 前缀+旧密文兼容分支）|
| B11 | Vertx trust-all 硬编码→kdla.http.ssl.trust-all 默认 false + request-timeout 默认 30s | 32ba8d2；R/VertxHttpConfigure.java:47-48,80；T 现状 trust-all 硬编码 true、无超时 | 接受（安全默认；依赖自签证书的下游需显式开 trust-all；>30s 慢调用需调 timeout）| 自签证书/慢调用下游 | ✅ 接受（CHANGELOG 标注：自签证书下游需显式开 trust-all，>30s 慢调用需调 timeout）|
| B12 | Sequence 溢出改抛 BizException 禁止清零重复发号 | d7d7bfe | 接受（数据正确性）| 溢出场景从静默重号变抛错 | ✅ 接受 |
| B13 | MachineIdHelp.destroy SETNX 覆盖→del | 32ba8d2；R/MachineIdHelp.java:119-120；伴生 Nit S3「del 可能误删其他实例重注册」 | 接受 + Nit S3 继承登记 | 实例下线窗口行为 | ✅ 接受（用户明示「MachineIdHelp 修」）+ Nit S3 继承登记 |
| B14 | 配置键更名 kdla.common.stop-watch.enabled→kdla.config.stop-watch.enabled、kdla.log.catch-log.enabled→kdla.config.catch-log.enabled | **修正（2026-11-14 主 agent 复核）**：T 源码 grep 旧键零命中——T 从未有 enable 开关，「旧键失效」风险不存在，原 subagent 结论有误 | 键名直接采用 R 新键（kdla.config.*），无需双键兼容；开关默认值归 S1 | 无独立分歧 | ✅ 键名采用 R 新键；默认值随第 11 节矩阵（mdc/stop-watch/catch-log 默认开，request-log 默认关）|

### S 组：装配与结构

| # | 决策点 | 现状+证据 | 建议 | 影响面 | 结果 |
|---|---|---|---|---|---|
| S1 | T 已开启的 4 个 AutoConfigure（Mdc/StopWatch/CatchLog/Redisson）开关默认值 | T 现状无条件开启（第 9 节）；R 契约默认关 | 方案 a=R 契约默认关（下游静默失效，需改配置）；方案 b=这 4 项 matchIfMissing=true 保持开启、其余新注册项默认关（对 T 下游全平滑，推荐）；方案 c=默认关+启动 WARN 提示配置迁移 | T 全部下游 | ✅ 已裁定（用户原则）：独立无依赖→直接注入默认开；有依赖→联动注入+真实限制条件（OnClass 真实外部类/OnBean 前置 bean/AutoConfigureAfter 顺序），依赖满足才装配；有运行期副作用且 T 现状为关→开关默认关。落地矩阵见第 11 节 |
| S2 | 装配契约扩展 T 特有 4 模块（dingding/monitor/reactor/job）| 均未经审视；monitor PrometheusAutoConfigure.java:9 死 import；job pom 声明 7 模块依赖代码零使用 | 按同契约补注册+条件化（死类补注册不删除，沿用 R 用户裁定）；job/monitor 冗余只登记不动手 | 4 特有模块 | ✅ 已裁定：按 S1 同原则执行。实测 T dingding spring.factories 已跨模块注册 VertxHttpConfigure（DingDingServiceImpl 依赖 VertxHttpClient）→ 联动注入保持默认开+真实 OnClass(Vertx)；monitor PrometheusAutoConfigure 已注册保持默认开+补真实 OnClass；reactor/job 无 spring.factories；job pom 冗余/monitor 死 import 只登记不动手 |

### A 组：配套事项

| # | 决策点 | 现状+证据 | 建议 | 影响面 | 结果 |
|---|---|---|---|---|---|
| A1 | 测试回合 | R 64 测试类/162 用例 JDK8 全绿；T 全仓无测试 | 全部回合 + 9 模块 pom 补 spring-boot-starter-test(provided)+mockito | src/test + 模块 pom | ✅ 接受：64 测试类全回合+9 模块 pom 补 test 依赖(provided)；⚠️ Response 相关测试按 B1 否决适配（T 保留 success 字段，断言不得假设字段已删）|
| A2 | MDC 键名 | T="traceId"（已独立演化），R="nonce"；两仓无 logback*.xml，消费在下游 pattern | T 保持 "traceId"，R 修复重定位 | log/common，下游零改动 | ✅ 接受：T 保持 "traceId"，R 修复按 T 键名重定位 |
| A3 | 文档回合 | R 文档 6 处滞后 git（第 5 节）；语境 midea/9 模块 | CHANGELOG 必回合+补 2026-11-13 切片版本块；architecture/project-map 改写 13 模块/enncloud 语境且以 git 为准；kdla-optimization.md 不回合（由本报告+nit-inventory 替代）；README 按需修订 | docs/、README、CHANGELOG | ✅ 接受：按建议执行；CHANGELOG 另需记录 B1 否决（success 字段保留）与第 11 节装配偏离 |
| A4 | 版本号 | 两仓均 1.0.1-SNAPSHOT；T 分支即版本线；14 pom 硬编码 | 保持 1.0.1-SNAPSHOT（非发布动作）| 无 | ✅ 接受：保持 1.0.1-SNAPSHOT，14 个 pom 版本号不动 |
| A5 | 人工验证清单 | R V1-V8 未执行无回贴（nit-inventory.md 附表）| 继承 V1-V8（预期值按 S1/B5/B7 裁定联动调整）+ 本次新增项，实现完成后列清单交用户人工执行回贴 | 用户人工验证 | ✅ 接受：V1-V8（预期值按第 11 节矩阵调整）+新增项（Sm4/AES v1: 混部互解、Vertx timeout、fail-fast 启动失败、B1 保留字段序列化），实现完成后列清单交用户，回贴前不宣布完成 |

### 无需裁定、仅知会（处理方案已定）

1. R 基线确切来源无法定位 → 不影响回合：6 分叉文件一律按 T 现状重定位（A2 已定键名方向）。
2. R/AGENTS.md:53 IntelliJ 配置说明 → R 内部文档，F 级不回合。
3. R 文档 vs git 6 处矛盾 → 一律以 git/代码为准，无裁定空间。
4. VertxConfigure 统一 Vertx Bean 与 dingding 跨模块引用重叠 → 实现期核对装配去重（83 项注意点），非歧义。
5. SpringRedisHelp.getExpire → T 从未删除，仅补 @Deprecated（25 项），无歧义。
6. validator pom property 更名/删 logback 依赖（103 项）→ 实现期按「保持 T 依赖策略、只补缺失（slf4j-api provided）」原则处理，无需裁定。
7. dingding/monitor 的 LSP 报错（log/getter undefined）→ Lombok 注解处理器误报，以 mvn clean compile 为准，不处理。

## 11. 装配矩阵（S1/S2 裁定落地，2026-11-14）

用户裁定原则：**独立无依赖 → 直接注入（默认开）；有依赖 → 联动注入 + 真实限制条件（OnClass 真实外部类 / OnBean 前置 bean / AutoConfigureAfter 顺序），依赖满足才装配**。补充推导规则（维持 T 下游零意外）：有运行期副作用且 T 现状未开启 → 注册 + 开关默认关。

### 11.1 保持默认开（T 现状已开启，matchIfMissing=true，本次补真实限制条件）

| 配置类 | 模块 | 补充的限制条件 | 备注 |
|---|---|---|---|
| MdcAutoConfigure | common | OnClass(org.aspectj.lang.annotation.Aspect) 替换恒真内部类 + COBM | 修恒真 OnClass（64 项）；开关 kdla.config.mdc.enabled |
| StopWatchAutoConfigure | common | 同上 | 开关 kdla.config.stop-watch.enabled（B14 新键）|
| CatchLogAutoConfigure | log | 同上 | 开关 kdla.config.catch-log.enabled（B14 新键）|
| RedissonAutoConfigure | supplement-cache | OnClass 改真实 RedissonClient + COBM | B5；锁前缀 appId 缺失回落固定值 "kdla"+log.warn（92 项，修多实例互斥失效）|
| DomainAutoConfigure | domain | 不动 | R 未改动 |
| DingAlertConfigure | dingding | 补真实 OnClass（钉钉 SDK 类）| T 现状已注册 |
| VertxHttpConfigure | supplement（被 dingding 跨模块注册）| OnClass(io.vertx.core.Vertx) + COBM | **联动关键点**：dingding DingDingServiceImpl 依赖 VertxHttpClient，必须默认开，否则钉钉告警断；偏离 R 默认关 |
| VertxConfigure（新增，83 项）| supplement | OnClass(io.vertx.core.Vertx) + COBM | 联动：统一提供 Vertx bean，VertxHttp/Mqtt 复用；核对与 dingding 注册去重（Spring 同类自动去重）|
| PrometheusAutoConfigure | monitor | 补真实 OnClass（prometheus simpleclient）| T 现状已注册；死 import 只登记不动 |

### 11.2 新注册 · 独立无副作用 → 直接注入默认开

| 配置类 | 模块 | 限制条件 | 备注 |
|---|---|---|---|
| UnifiedExceptionControllerAdvice | exception | OnWebApplication(SERVLET)；Security/SASL 异常拆 @ConditionalOnClass 守护的内层 Advice | B7 已裁定默认开（matchIfMissing=true）+ @Order(LOWEST_PRECEDENCE) |
| ValidatorAutoConfigure | validator | COBM | CommonValidator 纯 bean 无副作用；开关 kdla.config.validator.enabled 默认开（偏离 R 默认关）|
| FieldValueFindConfigure | supplement | COBM；移除 @Component 立体注解 | 工具 bean |
| DistributeLockedAspect | supplement-cache | OnClass(Aspect) + COBM；移除 @Component | 仅作用 @DistributeLocked 注解方法，无注解=零影响 |
| SequenceNoConfigure | supplement-cache | **联动**：OnBean(RedissonRedDisLock) + AutoConfigureAfter(RedissonAutoConfigure) | Redisson 装配才装配（K4 闭环，95 项）；保留 R 的 OR 表达式修复（23 项）语义 |

### 11.3 新注册 · 有运行期副作用 → 开关默认关（T 现状=关，保持零意外，下游显式开启）

| 配置类 | 模块 | 副作用 | 开关 |
|---|---|---|---|
| RequestLogAutoConfigure | log | 全量请求日志（日志量/隐私）| kdla.config.request-log.enabled 默认关 |
| CacheAutoConfigure | supplement-cache | @EnableCaching 全局改变宿主缓存行为 | kdla.cache.enabled 默认关 + OnBean(RedisTemplate) + @EnableCaching 隔离内部静态类（94 项）|
| rest-http（NettyHttpServer）| supplement | 启动绑定端口 | 默认关 + 端口配置缺失不启动 |
| VertxMqttConfigure | supplement | 连接 MQTT broker | 默认关 |
| CorsConfigure | supplement | 改变全局跨域策略 | 默认关 |

### 与 R 契约的偏离记录（回合时写入 CHANGELOG）

1. R 全部 15 类默认关；本矩阵 9 类默认开（T 现状已开 7 类 + B7 裁定 exception advice + 独立无副作用 bean 3 类），5 类默认关同 R。
2. 偏离理由：T 与 R 起点不同——R 这些类原本未注册（默认关零影响），T 已开启（默认关=下游静默失效）。
3. V8 人工验证预期值联动调整：默认启动时 Mdc/StopWatch/CatchLog/Redisson(有 redisson 库时)/VertxHttp(有 vertx 库时)/exception advice 装配；RequestLog/Cache/rest-http/mqtt/cors 不装配。

## 12. 票 09 聚焦审计结果（2026-11-14，P4 裁定落地）

报告全文：`.scratch/kdla-backport/audit-supplement.md`（subagent ses_e2821e42dffeQid6uDlbR74qzI）。

结论：32ba8d2 的 13 项实修已被票 10/11/12 全覆盖，批次 5 无 T 侧遗漏；「约 20 项差额」实为口径差（33 项行号级计数 vs 9 主题根因级归并）+更早提交（9cdbe79/bb8022f）已修+装配缺口被 1fa3289 闭环核销+「无法复现」项 R 侧即不可追溯。真正遗漏为 15 项差额，全部并入现有票（10 票+8、11 票+2、12 票+4，明细见各票 Comments）：

- A 组（R 已提交 bb8022f 但本报告第 3 节欠登记，9 项）：CsvUtil.parseCsv 全废、HttpContextHelp trust-all/SSL/Cors 无告警、SslContextFactory 流泄漏+throw Error、KdlaGcache.put assert 失效、KdlaCafCache.put assert+TTL 顺序、CacheAutoConfigure @PreDestroy 带参（注册即启动炸）、SequenceNoGeneratorV2 customizePool NPE、DistributeLockedAspect getLock 不加锁（注解形同虚设）、NettyHttpClient 无 close+双 shutdown 竞态。
- B 组（R 工作区未提交修复，5 项，回合时逐项复核）：MqttChannelHandler CONACK 悬挂+PUBREC/PUBCOMP NPE、MqttClientImpl 集合并发读写、NettyHttpClient setSsl 无效+get 无超时+https NPE、NettyHttpServer CONTENT_LENGTH 恒 0+FileRegion fd 泄漏、HttpContextHelp @Deprecated 迁移告警。
- C 组（结构缺口，1 项）：RedissonClientConfig.java 整文件注释残留删除（项 91 有载、此前无票归属）。

高优先 5 项：CsvUtil、CacheAutoConfigure @PreDestroy、DistributeLockedAspect 不加锁、MqttChannelHandler 悬挂、NettyHttpClient setSsl 无效。DistributeLockedAspect 修复属行为变更（恒放行→锁失败抛 LockFailException），票 15 CHANGELOG 必须记录。
