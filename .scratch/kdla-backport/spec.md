# Spec: kdla 回合（aiot-robot/kdla → 全面版 kdla）

Status: ready-for-agent
日期：2026-11-14
单一真源：`.scratch/kdla-backport/investigation.md`（112 项清单 + 25 项裁定结果 + 装配矩阵）
Nit 登记：`.scratch/kdla-backport/nit-inventory.md`（53 项 + V1-V8）

## Problem Statement

目标仓 T（全面版 kdla，13 模块，发布 nexus.enncloud.cn）与参考仓 R（裁剪版，9 模块）同源分叉。R 仓在 2026-06 至 2026-11 间完成了 9 个批次约 104 项 bug 修复、性能优化、安全加固、装配契约重构与 64 个测试类的建设；T 仓这些缺陷全部仍在（已抽查证实：Sm4 ECB、snakeyaml 误 import、23 处 printStackTrace、雪花 machineId 竞态、分布式锁死锁、流水号重复发号、traceId 并发重复等）。T 的下游应用持续暴露于这些已知缺陷，且 T 全仓无测试、无行为锁定。

## Solution

把 R 仓 A/B/C/D/E 级 104 项修改按 T 现状回合到 T：约 90% 项两仓代码基线一致可直接套用；6 个分叉文件（CommonConstants/MdcHelp/MdcAspect/StopWatchWrapperAspect/CatchLogAspect/KdlaBodyReaderRequestFilter）按 T 的 "traceId"/List<String> excludes 现状重定位；装配契约按用户裁定原则落地（独立无依赖→默认开，有依赖→联动注入+真实限制条件，有副作用且 T 现状关→默认关）；64 测试类全回合作为行为锁定；文档改写为 13 模块/enncloud 语境。B1 否决：Response 保留 success 字段。

## User Stories

1. 作为下游应用开发者，我想要 traceId 生成使用原子自增计数器，以便并发请求不再产生重复 traceId。
2. 作为下游应用开发者，我想要分布式锁 biz() 使用有界 tryLock(waitTime, leaseTime)，以便锁不可达时快速失败而不是无限挂起。
3. 作为下游应用开发者，我想要 unlock 前有 isHeldByCurrentThread 守卫，以便不会误释放他人持有的锁或抛 IllegalMonitorStateException。
4. 作为下游应用开发者，我想要 @DistributeLocked 切面透传业务异常（不再 blanket catch 吞异常返回 null），以便 throwEx=false 时能拿到真实的失败语义且事务正常回滚。
5. 作为下游应用开发者，我想要 DistributeLockedAspect 的 SpEL 改用 SimpleEvaluationContext 只读绑定，以便锁 key 表达式不可注入任意方法调用。
6. 作为下游应用开发者，我想要流水号生成器规则 split 长度前置校验并在异常时只 remove 失败 code 的缓存，以便不再出现重复流水号或缓存雪崩。
7. 作为下游应用开发者，我想要 SequenceNoGeneratorV2 非缓存模式补 DB 写回，以便重启后流水号不从旧值重复发放。
8. 作为下游应用开发者，我想要 Sequence 双锁单解死锁删除、溢出改抛 BizException，以便高并发下不卡死且溢出不再静默清零重号。
9. 作为下游应用开发者，我想要雪花 machineId 用 Redis SETNX 原子注册、destroy 用 del 立即释放，以便多实例并发启动不再撞号产生重复 ID。
10. 作为下游应用开发者，我想要 machineId 随机回退取号记 ERROR 级告警且 localIp 字段正确赋值，以便撞号风险可见可查。
11. 作为下游应用开发者，我想要 AsyncThreadHelp 重建线程池前 shutdown 旧池、volatile+synchronized 防并发重建，以便不再泄漏线程。
12. 作为下游应用开发者，我想要 ThreadPoolHelp 每池独立有界队列、Builder 全参默认值防拆箱 NPE，以便默认池不再共享无界队列导致 OOM。
13. 作为下游应用开发者，我想要 SpringRedisHelp 23 处 printStackTrace 改 log.error，以便异常进入统一日志管道。
14. 作为下游应用开发者，我想要 SpringRedisHelp.unLock 的 Lua 脚本 key/value 经 RedisTemplate 序列化器，以便解锁不再因序列化不一致而永远静默失败。
15. 作为下游应用开发者，我想要 lock() 的 setIfAbsent 返回值用 Boolean.TRUE.equals 判断，以便不再拆箱 NPE。
16. 作为下游应用开发者，我想要 KdlaCafCache 用 expireAfter(Expiry) 实现 per-key TTL，以便 put(key,value,duration) 的过期时间真实生效。
17. 作为下游应用开发者，我想要 UnifiedExceptionControllerAdvice 修复 DuplicateKeyException 误 import（snakeyaml→spring dao），以便 409 分支真实生效。
18. 作为下游应用开发者，我想要异常 advice 恢复 spring.factories 注册且默认开、@Order 最低优先级、Security/SASL 异常拆 @ConditionalOnClass 守护内层 Advice，以便下游不加 @Import 也有统一异常响应且认证失败返回 401 而非「系统繁忙」。
19. 作为下游应用开发者，我想要异常响应不回显未知异常的 getMessage()（SQL/内部路径），以便不泄漏内部信息。
20. 作为下游应用开发者，我想要 Sm4 从 ECB 改 CBC+随机 IV+v1: 前缀且旧密文走兼容分支，以便密文不再可被模式分析且存量数据可解。
21. 作为下游应用开发者，我想要 Sm4 密钥编码统一 Base64、新增 generateSecureKey 真随机 API 且旧确定性 API 标 @Deprecated，以便密钥来源一致且可生成不可预测密钥。
22. 作为下游应用开发者，我想要 AES 零 IV 改 SecureRandom 随机 IV+v1: 前缀（旧密文兼容解密）、异常消息不回显 password，以便加密语义安全且日志不泄密。
23. 作为下游应用开发者，我想要 Sm2 新增 generateSecureKeyPair 真随机 API、UTF-8 显式编码，以便密钥对不可预测且跨平台一致。
24. 作为下游应用开发者，我想要 SqlFilter 黑名单子串改白名单正则 ^[a-zA-Z0-9_]+$+长度上限，以便 update_time 不再误杀且 union 注入真实被拦。
25. 作为下游应用开发者，我想要 ObjectValidator 删 sun.misc.Unsafe、深度 16+IdentityHashMap 防环、遍历父类字段，以便校验器在 JDK 升级后仍可用且不死循环。
26. 作为下游应用开发者，我想要 EnumValid null 校验对齐 JSR-303（null→通过）、IEnumValid.checkForNull 默认 false+Objects.equals 防 NPE，以便枚举校验语义与标准一致。
27. 作为下游应用开发者，我想要 BaseAssert.isBlank 更名 notBlank（语义反转修正）、删除零调用的 isNull，以便断言语义与方法名一致。
28. 作为下游应用开发者，我想要 DBLengthValid 全程 BigDecimal 比较修 pow 溢出、JUL→SLF4J，以便大长度校验不溢出且日志统一。
29. 作为下游应用开发者，我想要 CommonValidator 删 Locale.setDefault 全局副作用，以便框架不篡改宿主 JVM 的默认 Locale。
30. 作为下游应用开发者，我想要 MdcAspect/MdcHelp 保存-恢复旧值替代 MDC.clear()、SpEL 解析降级，以便嵌套切面不丢外层 traceId 且解析失败不炸业务。
31. 作为下游应用开发者，我想要 CatchLogAspect 的 doBefore 对无参方法加 args.isEmpty() 守卫、JSON 强解析加降级、@Around 仅返回类型可赋值 Response 才转失败响应、Error 一律 rethrow，以便切面不再越界/吞 Error/破坏事务回滚。
32. 作为下游应用开发者，我想要 CatchLogAspect 嵌套时保存-恢复外层 traceId、未知异常不外传 e.getMessage()，以便日志链路正确且不泄漏内部信息。
33. 作为下游应用开发者，我想要 LogInterceptor 往 MDC 放裸 traceId 而非整个 JSON 串、打印 header 不再 new 包装器消费原始输入流、afterCompletion 时间戳判空，以便 %X{traceId} 输出干净且 controller 能读到 body。
34. 作为下游应用开发者，我想要 RequestLogAspect 可靠性重构（null 安全）并接入 LogSanitizeUtil 脱敏（authorization/cookie/password/token/mobile+2KB 截断），以便请求日志不泄漏凭证。
35. 作为下游应用开发者，我想要 KdlaHttpServletRequestWrapper body 空兜底+UTF-8 显式编码、KdlaBodyReaderRequestFilter finally 清 MDC，以便包装器不再 NPE 且线程复用不串 traceId。
36. 作为下游应用开发者，我想要 MqttPingHandler 心跳超时判断反转修复、QoS2 ByteBuf retain/release 配对、MqttClientImpl 自建 eventLoop disconnect 时关闭，以便 MQTT 连接不再误判超时/内存泄漏/线程泄漏。
37. 作为下游应用开发者，我想要 NettyHttpServer 启动即关闭问题修复、bind 失败 shutdownGracefully+close 显式释放、异常响应固定文案，以便内嵌服务器可重复启动且不泄漏堆栈。
38. 作为下游应用开发者，我想要 Vertx trust-all 硬编码改 kdla.http.ssl.trust-all 默认 false（开启时 log.warn MITM）、新增 kdla.http.request-timeout 默认 30s、content-type 小写匹配、回调异常走 future，以便 HTTPS 默认安全且远端挂起不再永久阻塞（钉钉告警四处 futureRes.get() 受益）。
39. 作为下游应用开发者，我想要 NettyHttpClient 端口优先 URL 显式值、缺省按协议 80/443、connectTimeout 默认 3000，以便连接行为可预期。
40. 作为下游应用开发者，我想要 Redisson 地址解析 fail-fast（空/非法节点启动即抛 IllegalArgumentException）、REPLICATE 模式 passwords/nodes 数量校验，以便配置错误在启动期暴露而不是运行期锁不可用。
41. 作为下游应用开发者，我想要 Redisson 装配去除 config.toYAML() 密码明文 INFO 日志，以便日志不泄漏 Redis 密码。
42. 作为下游应用开发者，我想要 RedissonAutoConfigure 锁前缀 appId 缺失时回落固定值 "kdla"+log.warn（替代每实例雪花 ID），以便多实例分布式锁互斥真实生效。
43. 作为下游应用开发者，我想要 BigDecimalHelp 20 进制笔误修十进制、compareTo 判零，KdlaStringHelp 驼峰 contains 方向修正，BytesUtil.floatToLe 字节序修正、reverse(data,offset,length) 交换索引错位修正+越界显式抛异常，LocalDateTimeHelp YearMonth 跨年误判修正，DateHelp 按 pattern 分派解析，PinYinHelp 空串防护，JacksonUtil 异常带 cause，ObjectUtil flush 防截断，FileChunkTransmitHelp 覆盖写+IOException 上抛+流级联关闭+try-with-resources，KdlaExcelWrite try/finally finish()，以便工具类基础语义全部正确。
44. 作为下游应用开发者，我想要 IPageQuery try-parse+limit 封顶 500、PlusPageQuery 基本类型判 >0+params 判空、Page(domain) sameValueAs 按字段比较+pageSize<=0 防溢出、ApplicationContextHelp 空 catch 记日志+requireContext 明确异常、FieldValueFindHelp break 误用修正、EsSourceData/EsBaseData @SuperBuilder 修父字段丢失、ResponseHandler 补 setMessage、MultiResponse.buildSuccess 回填 totalCount（新增字段，JSON 向前兼容），以便分页/上下文/ES/响应模型行为正确。
45. 作为下游应用开发者，我想要 LocalIpHelp.getLocalIp4Address 加 static volatile 缓存，以便 traceId 热路径不再每请求枚举网卡。
46. 作为下游应用开发者，我想要四个切面 AutoConfigure（Mdc/StopWatch/CatchLog/RequestLog）的恒真 @ConditionalOnClass 改指 org.aspectj.lang.annotation.Aspect，以便宿主缺 aspectj 时不再误装配。
47. 作为框架维护者，我想要 15 个未注册配置类按装配矩阵补进 spring.factories（默认开/关按第 11 节矩阵），@Bean 一律 COBM，FieldValueFindHelp/CommonValidator/DistributeLockedAspect 移除 @Component 立体注解，删除整文件注释残留 RedissonClientConfig.java，以便装配契约统一且无双定义。
48. 作为框架维护者，我想要 SequenceNoConfigure 注册闭环（真实 OnClass(RedissonClient)+AutoConfigureAfter(RedissonAutoConfigure)+OnBean(RedissonRedDisLock)）且 3 个配置键改 @ConditionalOnExpression OR 语义，以便 K4 死类问题闭环且任一开启方式都生效。
49. 作为框架维护者，我想要 CacheAutoConfigure 的 @EnableCaching 拆隔离内部静态类+kdla.cache.enabled 默认关+OnBean(RedisTemplate)，以便注册不再全局改变宿主缓存行为。
50. 作为框架维护者，我想要新增 VertxConfigure 统一提供 Vertx Bean（OnClass(io.vertx.core.Vertx)+COBM）、VertxHttp/Mqtt 复用，并核对与 T dingding 跨模块注册 VertxHttpConfigure 的去重，以便 Vertx 实例不重复创建且钉钉告警链路不断。
51. 作为框架维护者，我想要新增 MpBaseDo（@TableLogic Integer deleted）与 KdlaLockProperties（kdla.lock.waitTimeMs 默认 3000/leaseTimeMs 默认 30000）、ValidatorAutoConfigure 注册 CommonValidator，以便 MP 逻辑删除/锁超时/校验器有标准支撑。
52. 作为框架维护者，我想要 64 测试类/162 用例全回合+9 模块 pom 补 spring-boot-starter-test(provided)+mockito-junit-jupiter（exception 另补 spring-security-core provided,optional），以便 T 从零测试变为行为锁定。
53. 作为框架维护者，我想要 exception pom 补 spring-jdbc(provided)、validator pom 补 slf4j-api(provided)，以便编译期依赖解析正确且不向下游传递强依赖。
54. 作为框架维护者，我想要 CHANGELOG.md 回合+补 2026-11-13 切片 1-4 版本块+记录本次装配偏离与 B1 否决，docs/architecture.md 与 project-map.md 改写为 13 模块/enncloud 语境且以 git 为准，nit-inventory 随文档带入，以便文档与代码一致。
55. 作为运维/值班，我想要配置错误 fail-fast、随机回退取号 ERROR 告警、密码不落日志、脱敏工具接入三处日志切面，以便故障可见且不泄密。
56. 作为运维/值班，我想要 V1-V8+新增项人工验证清单在实现完成后交付执行，以便真实 Redis/端口/MQTT/宿主环境的行为经人工确认后才宣布完成。

## Implementation Decisions

来源：investigation.md 第 10 节（25 项裁定，全部已回填）与第 11 节（装配矩阵）。此处为决策摘要，证据与行号以 investigation.md 为准。

1. **范围（P1-P4）**：A/B/C/D/E 级 104 项全回合（扣除 B1 否决的 96 项）；F 级 8 项不回合。Nit 53 项只登记不修，nit-inventory.md 带入 T。K5 本次不做。supplement/supplement-cache 约 20 项行号丢失差额：实现期重跑一轮聚焦审计（基准=R openspec proposal），审计作独立票、产出补票。
2. **B1 否决**：Response 保留 success 字段，Response.java 不动；R 的 Response 相关测试按 T 现状适配（断言不得假设字段已删）。MultiResponse 新增 totalCount 字段照常回合（与 success 无关）。
3. **BREAKING 全接受（B2-B4/B6/B8-B13）**：notBlank 重命名、EnumValid/IEnumValid 语义对齐、Redisson fail-fast、Sm4 CBC+IV+v1:+Base64 密钥（无兼容分支，B9）、AES 随机 IV+v1:、Vertx trust-all 默认 false+timeout 30s、Sequence 溢出抛错、MachineIdHelp destroy 改 del。全部写入 CHANGELOG BREAKING 段。
4. **装配矩阵（S1/S2/B5/B7/B14，第 11 节为唯一真源）**：
   - 保持默认开（matchIfMissing=true）+补真实限制条件：Mdc/StopWatch/CatchLog/Redisson(OnClass 真实 RedissonClient)/Domain(不动)/DingAlert(补真实 OnClass)/VertxHttp(OnClass(io.vertx.core.Vertx)，dingding 联动关键点)/VertxConfigure(新增)/Prometheus(补真实 OnClass)。
   - 新注册默认开（独立无副作用）：exception advice(OnWebApplication(SERVLET)+@Order(LOWEST)+Security 内层拆分，B7)/ValidatorAutoConfigure/FieldValueFindConfigure/DistributeLockedAspect(OnClass(Aspect))/SequenceNoConfigure(联动 OnBean(RedissonRedDisLock)+AutoConfigureAfter)。
   - 新注册默认关（有运行期副作用）：RequestLog/Cache(@EnableCaching 隔离)/rest-http/VertxMqtt/Cors。
   - 配置键采用 R 新键 kdla.config.*（B14，T 旧键零命中无兼容负担）。
   - 偏离 R 契约处（9 类默认开 vs R 全默认关）写入 CHANGELOG。
5. **6 分叉文件重定位（A2）**：MDC 键保持 T 的 "traceId"；R 基于 "nonce"/String exclude 的修复按 T 现状重定位；KdlaBodyReaderRequestFilter 适配 T 的 List<String> excludes。
6. **依赖策略（E 级）**：新增第三方依赖一律评估 provided（仓库既有约定）；根 pom 第三方版本 properties 不动；snakeyaml 1.33/spring-kafka 2.9.12 刻意 pin 不随 BOM 升级；validator pom 按「保持 T 依赖策略、只补缺失」处理。
7. **版本与提交（A4）**：保持 1.0.1-SNAPSHOT，14 个 pom 版本号不动；每票独立 commit，提交信息风格 `kdla <version> and upgrade <topic>`；只 stage 本任务文件，禁 git add -A；**禁止 git push**（用户人工执行）。
8. **JDK8 约束**：当前分支 target 1.8，禁 var/record/text blocks/JDK9+ API；编译验证 `mvn clean compile`（系统 JDK21 编 target 1.8 已验证可行），测试 `mvn test`。
9. **T 特有 4 模块**：编译影响为零（已核验）；dingding/monitor 按 S2 裁定补真实 OnClass 保持默认开；reactor/job 无 spring.factories 不动；job pom 冗余依赖与 monitor 死 import 只登记不动手。
10. **文档（A3）**：CHANGELOG 必回合+补切片版本块；architecture/project-map 改写 13 模块/enncloud 语境、以 git 为准（R 文档 6 处滞后不继承）；kdla-optimization.md 不回合。
11. **LSP 误报**：dingding/monitor 的 log/getter undefined 为 Lombok 注解处理器误报，不处理，以 mvn 为准。

## Testing Decisions

- **好测试的标准**：只测外部行为（公共 API 输入输出/异常/状态），不测实现细节；R 的 162 用例已按此标准编写并全绿，直接作为 T 的行为锁定。
- **接缝**：①单元接缝=各 helper/util/aspect/config 类的公共 API（R 64 测试类原样回合）；②构建接缝=`mvn clean compile` + `mvn test` 全绿；③运行接缝=V1-V8+新增人工验证清单（真实 Redis/端口/MQTT/HTTPS/宿主试点，交用户执行回贴，回贴前不宣布完成）。
- **适配项**：Response 相关测试按 B1 否决适配（success 字段保留）；MDC 相关测试按 A2 适配（键名 "traceId"）；分叉文件（CatchLogAspect 等）的测试按 T 重定位后的行为断言。
- **测试依赖**：9 模块 pom 补 spring-boot-starter-test(provided)+mockito-junit-jupiter；exception 模块另补 spring-security-core(provided,optional)（A1/102 项）。
- **新增人工验证项**（A5）：Sm4/AES v1: 前缀混部新旧互解、Vertx trust-all 默认 false 下自签证书行为、request-timeout 30s 生效、Redisson fail-fast 启动失败、Response 保留 success 字段的序列化输出不变、钉钉告警链路（VertxHttp 联动注入后仍可用）。

## Out of Scope

- F 级 8 项：midea Nexus 配置、模块裁剪、AI 工具目录、openspec 目录、R 的 AGENTS.md 特有内容、.gitignore 反复调整、历史文档 KDLA-优化计划.md。
- B1：Response 删 success 字段（用户否决）。
- K5：ApplicationContextHelp 静态不可 mock（登记后续独立任务）。
- Nit 53 项修复（只登记；含 S3 del 竞态、MdcAspect 无 @Order、banner.txt 劫持、KdlaExcelWrite 多 sheet 覆盖、MachineIdHelp 无 TTL 续期、LogTraceHolder InheritableThreadLocal 脏值等）。
- job pom 冗余依赖清理、monitor 死 import 删除（只登记不动手）。
- Spring Boot 3/JDK 21 迁移、版本号变更、git push、mvn deploy、CI/CD 改动。
- R 文档 6 处滞后内容的继承（一律以 git/代码为准）。

## Further Notes

- 证据与行号：investigation.md（第 3 节 112 项、第 9 节 T spring.factories 现状、第 11 节装配矩阵）；nit-inventory.md（Nit+V1-V8）。
- R 仓验收标准可直接引用：openspec archive 的 3 份 spec（distributed-lock-safety/sequence-no-correctness/traceid-atomicity）。
- subagent 会话可恢复：考古=ses_e288c9126ffeqC4mgSJFn5qImY，diff=ses_e288c51d0ffelzXKa2CAtm1P43。
- 实现顺序建议（to-tickets 细化）：先依赖/pom（E 级）→ 底层模块（model/common）→ exception/log/domain/dal/validator → supplement/supplement-cache（含 P4 审计票）→ T 特有 4 模块装配核对 → 测试回合 → 文档/CHANGELOG → 人工验证清单交付。
- 每票完成即 `mvn -pl <module> -am clean compile` 验证，全量收口 `mvn clean compile` + `mvn test`。
