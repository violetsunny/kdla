# Nit 级登记清单（53 项）+ 人工验证清单 V1-V8

来源：R/docs/kdla-optimization.md:288-384（2026-10-29 全模块审计，185 类）。R 仓明文「只登记不修，待后续任务」。
标注说明：[已修-切片N]=R 仓审计切片 1-4（0209b43/89fc933/b9695b2/e3a4acd）已修复但原表未更新；[部分缓解]=有改动但 Nit 本体未消除；无标注=未修。
本清单供 P2 裁定勾选：默认全部不修仅继承登记；用户可按「模块-行号」勾选个别项纳入本次回合。

## common 模块（27 项）

| # | 位置 | 问题 | 状态 |
|---|---|---|---|
| C1 | MdcAspect / StopWatchWrapperAspect | 切面无 @Order，与宿主切面顺序不定 | |
| C2 | StopWatchAutoConfigure vs StopWatchWrapperAspect | 配置键风格不一致（kdla.config.stop-watch.enabled vs kdla.stop.watch.error.timeout）| |
| C3 | CommonConstants:13 | traceId 的 MDC key 叫 "nonce" 命名误导 | T 不适用（T 已是 "traceId"）|
| C4 | MdcHelp.resetTraceId | traceId null 时 MDC.put(key,null) 行为依赖 slf4j 实现；内部仍用 MDC.clear() | |
| C5 | DateHelp:69-75 | 每次调用新建 ThreadLocal 再 remove 反模式；catch 后返回 null | |
| C6 | KdlaStringHelp:117-119 | isEmpty(String) 把空格串视为空，与父类 StringUtils.isEmpty 语义冲突（重载非覆写）| |
| C7 | KdlaStringHelp:349-351 | 末位字符沿用上一轮 nexteCharIsUpperCase 陈旧值 | |
| C8 | RegexHelp:19-115 | Pattern 全部 public static 非 final 可被改写；NUMBER="[0-9]*" 匹配空串 | |
| C9 | QrCodeHelp:60,69,71-73,129 | png 输出配 image/jpeg 前缀；catch 返回 null；wordHeight 拆箱 NPE；fillRect 固定 1000 | |
| C10 | ClassUtil:25-31,64 | for 循环覆盖 isJar 只有最后 URL 生效；catch(Throwable ignore) 空块 | |
| C11 | GzipUtil:32,69 | getBytes()/toString() 平台默认字符集 | |
| C12 | Md5Util:28,36 | 异常日志打印 input 明文；默认字符集跨平台哈希不一致 | |
| C13 | SHAUtil:95 | RuntimeException 不带 cause | |
| C14 | Sm3Util:87 | Arrays.equals 非常量时间比较（HMAC 时序侧信道，低风险）| |
| C15 | Sm4Util:36 | ECB 模式密码学弱点 | [已修-切片3] b9695b2 CBC+v1: |
| C16 | Sm2Util:69,95-105 | publicKey 非 BCECPublicKey 时 NPE；keySrc 种子熵依赖调用方 | [部分缓解] 切片3 增 generateSecureKeyPair，NPE 未修 |
| C17 | BloomFilterHelp:27,38,49 | defaultCharset 跨机器不一致；insertions<<1 大值溢出 | |
| C18 | FileChunkTransmitHelp:95,249 | beginPos int 乘法 >2GB 溢出；deleteOnExit 与方法名语义不符 | |
| C19 | FileChunkTransmitHelp.splitDetail2(~157) | IOException 仅 log 不抛（与已修三处同类）| |
| C20 | LocalIpHelp:24-27,156 | static 缓存无同步（良性竞态）；只认 eth/ens 网卡名 | [部分缓解] 切片3 加 volatile 缓存，网卡名限制未修 |
| C21 | MultiThreadInvokeHelp:105,133 | 默认 ForkJoinPool.commonPool，MDC/TTL 不传递 | |
| C22 | ObjectUtil:13,32 | ObjectToByte 命名违规；byteToObject 未 try-with-resources、反序列化不可信字节 RCE 风险（调用方约束）、catch 返回 null | |
| C23 | ThreadPoolHelp:275-277 | 注释"cpu的20倍"实际 5 倍 | |
| C24 | AsyncThreadHelp:63-64,126,152 | setKeepAliveTime 重复调用；list 非线程安全；supplier.toString() 作 cost key 同名覆盖 | |
| C25 | AopTargetHelp:19,42 | IllegalAccessError 应为 UnsupportedOperationException；反射 CGLIB$CALLBACK_0 依赖 Spring 内部实现 | |
| C26 | StopWatchWrapperAspect | @Value 字段直接 new 时 null 拆箱 NPE；:68-75 error/warn 两档都打 warn 级别 | |
| C27 | JacksonUtil:48-91 | 一半方法吞异常返回 ""/null、一半抛 RuntimeException 契约不一致；LocalIpHelp:111 异常返回伪 IP "x.0.0.1"；DateTimeFormat:69-76 注释 yyyyMMdd 实际 yyMMdd；common banner.txt 可能劫持宿主启动 banner | |

## model / exception 模块（7 项）

| # | 位置 | 问题 | 状态 |
|---|---|---|---|
| M1 | BaseModel/MultiData/EnumResponse | Serializable 无 serialVersionUID；EnumResponse @Data 继承 callSuper=false | |
| M2 | MultiResponse:39-44,60 | buildSuccess totalCount 参数被忽略；of(null) NPE | [部分缓解] totalCount 已修-切片4 e3a4acd；of(null) NPE 未修 |
| M3 | PageResponse:75-86 | setTotalPages 死代码；getTotalPages 绕过 getter 防护 | |
| M4 | Response:30 | toString 用 message= 标签打印 msg 字段 | |
| M5 | BizException:24-26 | String.format 当 message 含意外 % 时异常构造中再抛异常 | |
| M6 | ResponseHandler:27,32-38 | newInstance() 已废弃；catch 吞异常静默降级；isKdlaResponse 孙类不命中 | |
| M7 | UnifiedExceptionControllerAdvice:95 | BizException 打全栈 warn 日志噪音；handleBiz/SysException 仍返回 getMessage() | |

## log / dal / domain / validator 模块（13 项）

| # | 位置 | 问题 | 状态 |
|---|---|---|---|
| L1 | LogTraceHolder:12 | public static 可变字段；get() 带 set 副作用；InheritableThreadLocal 线程池脏值（LogInterceptor:27 同）| |
| L2 | LogInterceptor:66 | afterCompletion 未经 preHandle 调用时拆箱 NPE | [已修-切片2] 89fc933 判空 |
| L3 | CatchLogAspect:52,140 | pointcut 体内打日志无意义；裸类型 Class | |
| L4 | RequestLogAspect:52,58,96,122,134,137 | 裸类型；var17/var24 反编译命名；List 含 null NPE；paraMap 无截断脱敏（批次4仅修三处调用点）| |
| L5 | KdlaHttpServletRequestWrapper:49-51 | isFinished 恒 false 违反 Servlet 3.1 语义 | |
| L6 | IPageQuery:39 / PlusPageQuery:81 | Page 对象覆盖 params 原始参数，map 复用类型突变 | |
| L7 | ValidatorUtils:37,54 | 错误消息拼 <br> 非 HTML 场景脏字符 | |
| L8 | CommonValidator:29 | ValidatorFactory 未 close | |
| L9 | ValidatorMsg:20,87 | Boolean 包装类型无必要拆箱 | |
| L10 | StateEnum:26-28 | 枚举字段未 final | |
| L11 | Page(domain) | setter 收 int、getter 返 long 类型不一致 | |
| L12 | PageConvertor:14 | 裸类型 IPage；无判空；工具类无私有构造器 | |
| L13 | EsSourceData:27 | 行尾双分号 | |

## supplement / supplement-cache 模块（6 项）

| # | 位置 | 问题 | 状态 |
|---|---|---|---|
| S1 | KdlaExcelWrite.writeSheet | 所有 map 条目写入同一 "sheet1" 相互覆盖（仅存最后一个）| |
| S2 | SequenceNoGenerator.DEFAULT_RULE | "yyyyMMdd+8" 仅 2 段但 getSequenceNo 要求 3 段，默认规则必抛错 | |
| S3 | MachineIdHelp | 注册无 TTL 续期，3600s 过期后可能撞号；destroyMachineId 的 del 可能误删其他实例重注册 | [部分缓解] 切片1 ERROR 告警撞号风险；TTL 续期未做 |
| S4 | MqttChannelHandler QoS2 | channel 保持打开且 PUBREL 永不到达时 pending 条目滞留（需逐消息超时）| |
| S5 | RedissonConfigProperties:29 | @ConditionalOnMissingBean 挂在 @EnableConfigurationProperties 注册的属性类上，死注解 | |
| S6 | KdlaBodyReaderRequestFilter:40 | @Value 依赖 Spring 托管，@ServletComponentScan 方式注册时 exclude 恒 null 静默失效 | |

## T 特有模块 dingding / monitor / job（票 13 登记，只登记不动手）

| # | 位置 | 问题 | 状态 |
|---|---|---|---|
| T1 | monitor PrometheusAutoConfigure:9 | 死 import StopWatchWrapperAspect（票 13 点名，S2 裁定只登记）| 票13登记 |
| T2 | monitor PrometheusAutoConfigure:8 | 死 import EnableAspectJAutoProxy（票 13 执行期新发现，同类）| 票13登记 |
| T3 | job pom.xml | 声明 7 个内部模块依赖（common/exception/domain/supplement/supplement-cache/supplement-dingding/log）代码零使用（S2 裁定只登记）| 票13登记 |

## 票 14 质量校验层评审登记（2026-11-16，均为 R 上游同源或 BREAKING 记录项，T 忠实移植不单边修）

核验方法：逐项 diff R 工作区 vs T，SAME=R 上游；修复授权边界=票 14"仅限本次改动引入"（组合放大型已修，见 verify-14.md 第 5 节）。

**数据正确性（高亮，票 16 报告建议 R/T 同步修）**：

| # | 位置 | 问题 | 来源 |
|---|---|---|---|
| P1 | cache MachineIdHelp.init:88-106 | 注册失败（Redis 异常）或 1024 槽耗尽时静态 machineId/dataCenterId 残留脏值，getGenerator 判空不进随机回退 → 多实例 (0,0) 或他人已注册 ID 撞号（雪花 ID 重复→主键冲突）。现有测试只覆盖成功路径 | 已修复（2026-11-16 用户裁定最小修复：注册成功才发布静态字段，失败路径保持 null 走随机回退；+2 回归测试 MachineIdHelpFailPathTest。R 侧未修，两仓该类出现 diff）|

**Bug/泄漏类（R 上游）**：

| # | 位置 | 问题 |
|---|---|---|
| P2 | supplement MqttChannelHandler:226-227 | QoS2 重复 PUBLISH（同 packetId 合法重发）map.put 覆盖旧条目，旧 ByteBuf 永不 release → 堆外内存泄漏 |
| P3 | supplement MqttClientImpl:446 | disconnect 关闭 eventLoop 未置 null，同实例 reconnect 时跳过重建 → RejectedExecutionException |
| P4 | log CatchLogAspect.doBefore:72-83 | 首参为非 JSON String 时 parseObject 必抛，每次调用 WARN 全栈+JSON 往返成本；T 默认开（B14）放大为热路径噪音 |
| P5 | supplement AESUtil:70 / Sm4Util:141 | decrypt 对 null 及 v1: 密文短于 16 字节无防护 → NegativeArraySizeException |
| P6 | supplement NettyHttpServer:285 | RandomAccessFile try-finally 包裹，raf.length() 抛异常时句柄泄漏 |
| P7 | common LocalIpHelp:207 | 解析失败兜底值 DEFAULT_IP 被永久缓存，瞬时故障后 traceId 永远带伪 IP |
| P8 | common ThreadPoolHelp:113 | 单线程池换无界 LinkedBlockingQueue 后 AbortPolicy 永不触发，积压可 OOM |
| P9 | log KdlaBodyReaderRequestFilter:47 | 请求头 traceId 未过滤直接入 MDC，CRLF 可伪造日志行（可复用 LogSanitizeUtil） |
| P10 | common DateHelp:103 | containsTimeField 未排除 pattern 引号字面量（yyyy'Mm' 误判含时间） |

**BREAKING/行为变更（票 15 CHANGELOG 必记，与既有 Comments 记录交叉核对）**：

| # | 位置 | 变更 |
|---|---|---|
| P11 | validator BaseAssert | isBlank×3、isNull(Object,String,String) 直接删除无 @Deprecated 桥接，下游升级 NoSuchMethodError（R 上游 SAME）|
| P12 | dal SqlFilter:19 | 白名单 [a-zA-Z0-9_] 不含点号，表限定排序列（t.create_time）由可用变抛 BizException |
| P13 | cache DistributeLockedAspect:77 | SimpleEvaluationContext 只读绑定禁方法调用，存量 SpEL 锁 key（#args[0].getKey() 形式）抛 SpelEvaluationException |
| P14 | domain IPageQuery:22 | MAX_LIMIT=500 静默钳制无日志（失败不可见），下游大分页被截断难察觉 |

**结构/风格（judgement call，不动手）**：

| # | 位置 | 问题 |
|---|---|---|
| P15 | cache RedissonConfigProperties:79-230 | redissons()/configSingleNode/configCluster/configMasterSlave 本仓零调用方，与 RedissonLockFactory.redissons() 近似拷贝（~150 行）；已发布库 public API，删除前需下游确认 |
| P16 | cache KdlaCafCache:85-100,147-152 | duration=null 拆箱 NPE 推迟到首次 put；put(key,value,duration) javadoc 写"分"实际秒 |
| P17 | cache DistributeLockedAspect:62-63,92-93 + RedissonRedDisLock:47-49 | 每次加锁固定 3 条 INFO，高频路径日志放大 |
| P18 | common AESUtil:32-90 / Sm4Util:91-150 | v1:Base64(IV+密文) 格式与双分支解密逻辑同构重复，可提取共享 helper（R 上游结构）|
| P19 | domain PlusPageQuery:62-75,111-124 | 两个 getPage 重载逐字重复分页回退块，且与 IPageQuery.parsePagingValue 语义重叠 |
| P20 | exception SecurityAccessDeniedExceptionAdvice | HTTP 403 但响应体 ErrorCode.UNAUTHORIZED，语义不一致 |

## 附：R 仓人工验证清单 V1-V8（R/docs/kdla-optimization.md:371-384，均未执行无回贴记录）

| # | 验证项 | 步骤摘要 | 预期/判定标准 |
|---|---|---|---|
| V1 | Redisson 地址 fail-fast | 开启 kdla.cache.redisson.enabled=true，address 留空/非法后启动 | 启动即抛 IllegalArgumentException 指明 address 问题，不静默零节点 |
| V2 | 真实 Redis 锁互斥 | 两实例（app.id 一致）同时调 DistributeLockedAspect 保护的同一 lockKey | 同一时刻仅一实例进入；另一实例抛 LockFailException；锁 key 带 appId 前缀 |
| V3 | sequence 并发无重复 | 开启 kdla.cache.sequence.enabled=true，≥50 并发调 getSequenceNo | 无重复号；溢出/非法长度段抛 BizException 不清零重发 |
| V4 | NettyHttpServer 端口占用与异常响应 | ①先占端口再启动 ②请求会抛异常的 handler | ①log.error+shutdownGracefully 不僵死端口可复用 ②固定文案不含堆栈 |
| V5 | MQTT eventLoop 无线程泄漏 | MqttClientImpl connect→disconnect 循环 10 次观察线程数 | disconnect 后线程回落无累积 |
| V6 | Vertx HTTPS 默认策略 | ①默认请求自签证书端点 ②trust-all=true 再请求 | ①证书校验拒绝 ②成功且出现 MITM log.warn |
| V7 | VertxHttpClient 行为 | ①大写 Content-Type ②服务端 sleep 35s ③回调抛异常 | ①JSON 正常解析 ②30s 超时失败 ③异常经 future 传播 |
| V8 | 宿主试点接入 | 宿主引入 1.0.1-SNAPSHOT 不加 kdla.* 配置启动，再逐个开关打开 | 默认：Redisson/Cache/Sequence/supplement 各 Configure 不装配；exception advice 默认生效（⚠️ 预期值随 S1/B5/B7 裁定联动调整）|
