# kdla 2.0.0-SNAPSHOT JDK 21 迁移审计报告

日期：2026-11-23。分支：2.0.0-SNAPSHOT（JDK 21 / Boot 3.4.0 / Cloud 2024.0.1），对照基线 1.0.1-SNAPSHOT（JDK 8 / Boot 2.7.18）。
方法：3 个并行只读 explore 审计（自动配置/jakarta、pom 依赖、JDK21 代码级）+ 主 agent mvn 编译验证 + 8 处证据抽查（路径:行号 grep/read 复核）。
原始代码审计全文：`.scratch/jdk21-audit/raw-code-audit.md`（来源标记 C=代码审计，A=自动配置审计，B=依赖审计）。
路径勘误：C 报告中 dal 模块路径缺 `infra` 段，实际为 `top/kdla/framework/infra/dal/...`，本报告已修正。

## 0. 根因诊断

git 考证：merge-base(HEAD, 1.0.1-SNAPSHOT) = 320529d；1.0.1-SNAPSHOT 领先 HEAD **19 个提交**（`git rev-list --count HEAD..1.0.1-SNAPSHOT` = 19），提交信息明确是 bugfix（sqlfilter、cache onbean guard、advice duplicatekey、machineid setnx 等，带回归测试）。2.x 迁移后合并的是 **1.0.0-SNAPSHOT**（cbf097f），不是 1.0.1。

结论：多数"迁移 bug"实为 **1.x 既有修复丢失**（2.x 业务代码比 1.0.1 旧），而非 JDK21 新语义踩坑。问题分三类：
- 甲类：丢失修复（~40 项，可从 1.0.1 按文件恢复 + 重做 javax→jakarta）
- 乙类：迁移未完成（javax 残留、自动配置注册丢失/注释、pom 版本错配）
- 丙类：真 JDK21 语义问题（强封装 AopTargetHelp/ObjectValidator、SecureRandom 确定性失效、JEP 400 charset）

## 1. 编译验证（实测，非推断）

| 轮次 | 结果 |
|---|---|
| 初始 `mvn clean compile` | FATAL：trdplatformcloud / job-application parent 钉 1.0.0-SNAPSHOT 不可解析，reactor 整体失败 |
| 修 2 行 parent 后 | exception 模块 `UnifiedExceptionControllerAdvice.java:198` cannot find symbol: WebRequest（缺 import，三个静态审计均漏报，编译实锤） |
| 补 import 后 `-fae` | 13/16 通过；supplement-job 失败：IotXxlJobManager.java:16 / LocalJobManager.java:26 `package javax.annotation does not exist`（证实 A/H2、B/C1 推断） |
| 修 2 行 javax→jakarta 后 | **15/16 SUCCESS**；仅 job-application FAILURE：EmailJobAlarm.java:15 `package javax.mail.internet does not exist` |

job-application 说明：其 pom 以 compile 引入 javax.servlet-api/javax.annotation-api，故 javax.servlet 代码暂时编译可过；javac 早期阶段错误（package 不存在）会掩盖后续 @Override 签名错误（PermissionInterceptor.java:26-27 用 javax 参数覆写 Spring 6 jakarta 接口，修 mail 后必暴露）。**该模块整体 jakarta 迁移不可省**：即使编译过，Spring 6 不处理 javax @Resource（注入 null）、MVC 不解析 javax.servlet 参数。
LSP 大量报错（log cannot be resolved、getter undefined、跨模块 import）为 Lombok/类路径误报，以 mvn 为准，不处理。

### 本轮已改文件（5 行，均为唯一改法的构建阻断，未提交）
1. kdla-component-supplement-trdplatformcloud/pom.xml:9 → 2.0.0-SNAPSHOT
2. kdla-component-supplement-job-application/pom.xml:9 → 2.0.0-SNAPSHOT
3. kdla-component-exception/.../UnifiedExceptionControllerAdvice.java → 补 `import org.springframework.web.context.request.WebRequest;`
4. kdla-component-supplement-job/.../IotXxlJobManager.java:16 → jakarta.annotation.Resource
5. kdla-component-supplement-job/.../LocalJobManager.java:26 → jakarta.annotation.Resource

## 2. Critical（构建阻断 / 启动必炸 / 静默数据丢失）

| ID | 位置 | 问题 | 修复 | 状态 |
|---|---|---|---|---|
| BUILD-1 (A/C1,B/C3,C/C1) | trdplatformcloud/pom.xml:9、job-application/pom.xml:9 | parent 钉 1.0.0-SNAPSHOT，reactor 解析失败 | 改 2.0.0-SNAPSHOT | 已修 |
| BUILD-2 (编译实测) | exception/UnifiedExceptionControllerAdvice.java:198 | WebRequest 缺 import | 补 import | 已修 |
| BUILD-3 (A/H2,B/C1,C/H40) | supplement-job IotXxlJobManager.java:16、LocalJobManager.java:26 | javax.annotation 无依赖来源，编译失败 | jakarta 替换 | 已修 |
| BUILD-4 (A/C5,B/C1,C/C11) | job-application 全模块 ~24 文件 | javax.servlet×12 文件、javax.annotation.Resource×12 文件、javax.mail×1；pom:94-105 compile 引入 javax 三件套传递下游 | 全量 javax→jakarta（servlet/annotation/mail），删 pom javax 三件套；或升级内嵌 xxl-job-admin 3.x | 待裁定#3 |
| REG-1 (A/C2) | common/domain/exception/log/cache/monitor 六模块 .imports | 注册行全部被 `#` 注释（提交 6dc7220），Boot 3 自动配置静默失效。实锤消费链：DomainAutoConfigure 不生效 → ApplicationContextHelp 静态 context=null → DomainFactory.java:12、FieldValueFindHelp.java:108,127、TrdPlatformCloudServerImpl.java:531,533 NPE | 取消注释；若为规避启动冲突应改用条件注解而非注释注册 | 待裁定#4 |
| REG-2 (A/C3,C/C3) | supplement/src/main/resources 整目录不存在 | 1.x 注册 6 类，f718134 整删 spring.factories 未补 .imports：CustomCorsConfig/RestHttpConfigure/VertxMqttConfigure/FieldValueFindConfigure 彻底丢失；VertxHttpConfigure 仅靠 dingding/job/trd 跨模块补偿；VertxConfigure 类已删职责并入他类（合理） | 新建 .imports 写 5 类；VertxMqttConfigure 须先加条件开关（见 CODE-C4） | 待裁定#4 |
| REG-3 (A/C2) | log、cache .imports | LogInterceptorAutoConfigure/RequestLogAutoConfigure/WebFilterAutoConfigure、CacheAutoConfigure/SequenceNoConfigure 五类在任何注册文件中连注释行都没有（1.x 曾注册） | .imports 补全：log 4 行、cache 3 行 | 待裁定#4 |
| REG-4 (A/C4) | supplement-job .imports 仅 VertxHttpConfigure | 2.x 新增 XxlJobConfig 未注册；IotXxlJobManager.java:27-28 @Autowired 强依赖 → xxl.enabled.xxl=true 时 NoSuchBeanDefinitionException | .imports 追加 XxlJobConfig；两个 manager 改 @Bean 或一并注册 | 待裁定#4 |
| CODE-C2 (C/C2) | supplement/excel/exp/KdlaExcelWriteHelp.java:61-67 | writeSheet 不 finish/close ExcelWriter → xlsx 中央目录未写、文件损坏 + 句柄泄漏（1.x 有 finally finish） | try-with-resources 或 finally finish | 待修 |
| CODE-C4 (C/C4) | supplement/mqtt/vertx/VertxMqttConfigure.java:51 | `@Value("${kdla.mqtt.topics}")` 无默认值，@ConditionalOnClass/@ConditionalOnProperty 被删 → 注册恢复后未配 mqtt 的下游启动炸 | 默认值 `:` + 恢复 kdla.config.mqtt.enabled 条件 | 待修（与 REG-2 联动） |
| CODE-C5 (C/C5) | supplement/netty/NettyHttpServer.java start() | bind 成功后 finally 立即 close channel + shutdownGracefully（自带 TODO 注释）→ 服务端口立刻不可用 | 删 finally，关闭只留 stop()/close() | 待修 |
| CODE-C6 (C/C6) | supplement/netty/NettyHttpClient.java:114-121,156-163,201-208 | 三发送方法 finally 立即关连接 + respPromise.get() 无超时（1.x 有 30s）→ 调用线程永久挂起 | 连接关闭移到响应读完；get 加超时 | 待修 |
| CODE-C7 (C/C7) | supplement/excel/utils/CsvUtil.java:40 | Stream.iterate 无限流无 limit（1.x 为 IntStream.range(0,record.size())）→ 解析任意非空 CSV 越界 | IntStream.range 限定 | 待修 |
| CODE-C8 (C/C8) | infra/dal/mybatis/util/PlusPageQuery.java:70,73,117,120 | pageNum/pageSize 是基本类型 long（:28,:32），Objects.nonNull 装箱恒真 → 默认 1/10 永不生效，new Page<>(0,0) → MP size<=0 关闭分页 → 全表查询 | 改 `>0` 判断（1.x 原样） | 待修 |
| CODE-C9 (C/C9) | log/LogInterceptor.java:31 | 直接 new KdlaHttpServletRequestWrapper 消费原始输入流未替换下游 request → @RequestBody 空 body；wrapper 删 IOException 兜底 → NPE | 恢复 instanceof 取缓存 body 模式 | 待修 |
| CODE-C10 (C/C10) | log/catchlog/CatchLogAspect.java:77-78 | catch Throwable 无条件转 Response：吞 Error（OOM 被转"失败 Response"）+ 非 Response 返回类型强转 CCE（1.x 两道守卫被删） | 恢复 instanceof Error 重抛 + 返回类型守卫 | 待修 |
| DEP-C2 (B/C2) | 根 pom:82 redisson 3.20.1；cache/pom.xml:151-193 | starter 传递 redisson-spring-data-30（钉 SD Redis 3.0.4，m2 缓存 pom 实证），cache 只 exclude 了 spring-data-redis 未换 -3x → 下游 Boot 3.4 启用 RedissonAutoConfiguration 时 AbstractMethodError/NoSuchMethodError | 升 redisson（含 -34 的版本，最低版本需中央仓确认）+ exclude -30 引入 -34；连带 RedissonRedLock 弃用 API 改造（KRedissonLockFactory.java:136、KRedissonRedDisLock.java:51,72,82） | 待裁定#7 |

## 3. High（特定路径运行时炸 / 数据错误 / 安全）

### 3.1 真 JDK21 语义（丙类）
| ID | 位置 | 问题 | 修复 |
|---|---|---|---|
| C/H1 | common/help/AopTargetHelp.java:55-56 | 反射 java.lang.reflect.Proxy.h + setAccessible → JDK17+ InaccessibleObjectException（两分支都有，升级直接触发） | 改 `((Advised) proxy).getTargetSource().getTarget()` |
| C/H2 | validator/ObjectValidator.java:66-69,107,137-143 | 递归防护全丢（无深度上限/visited/static 跳过）→ 枚举 $VALUES 自引用 StackOverflowError；精确 equals 取代 isAssignableFrom → java.time 字段 setAccessible 炸；不遍历父类字段漏校验 | 恢复 1.x 防护全套 |
| C/H36 | common/utils/Sm2Util.generateKeyPair(String)、Sm4Util.generateKey(String) | SHA1PRNG setSeed 确定性派生在 JDK21 失效（jshell 实测 deterministic=false, algo=NativePRNG）→ 存量 keySrc 派生密文永久不可解 | 改确定性 KDF（SHA-256 digest 截取）；存量数据需迁移方案，见裁定#6 |

### 3.2 并发（common）
| ID | 位置 | 问题 | 修复 |
|---|---|---|---|
| C/H3 | ThreadPoolHelp.java:131,74,163,200-207 | 单任务池 SynchronousQueue+AbortPolicy 第二任务即拒；静态共享队列多池串扰；getExecutorService 去 null 回退拆箱 NPE | 独立队列/LinkedBlockingQueue(1)/恢复默认值回退 |
| C/H4 | MultiThreadInvokeHelp.java:74-83,108-117 | timeout 重载内部 allOf().join() 无超时，参数从未参与等待 → hang 永久阻塞（ExcelReadListener:190 在用） | allOf().get(timeout) + 超时 cancel |
| C/H5 | SelfSnowflakeGeneratorHelp.java:72-79 | updateAndGet 退回 check-then-act → 并发重复 ID；getAndIncrement 可返回 10000 破坏 5 位格式 | 恢复单原子 updateAndGet((p+1)%10000) |
| C/H6 | AsyncThreadHelp.java:40-58,89,102,216,231 | 去 volatile/synchronized 初始化竞态 + 旧池不关线程泄漏；sleep 吞中断；cost.remove 拆箱 NPE；除零 NaN | 恢复同步/可见性/中断补位/判空 |

### 3.3 Netty/MQTT/HTTP（supplement）
| ID | 位置 | 问题 | 修复 |
|---|---|---|---|
| C/H7 | mqtt/netty/MqttPingHandler.java:78 | `!= null` 应为 `== null`（已抽查实锤）→ 超时任务永不注册，死连检测失效客户端假死 | 改回 == null |
| C/H8 | mqtt/netty/MqttChannelHandler.java:233,258 | handlePubrec/handlePubcomp 对 map.remove 结果直接解引用，QoS2 重发/错序 ACK → NPE 杀 event loop；handleConack 去 default → 未知 returnCode 挂死 | 恢复判空 + default completeExceptionally |
| C/H9 | mqtt/netty/MqttClientImpl.java:159,48-55,435-441 | qos2Pending.clear() 前不 release 堆外泄漏；并发容器退回 HashSet/HashMultimap；disconnect 不关自建 eventLoop 线程泄漏 | release 后 clear；恢复并发容器；自建池 shutdownGracefully |
| C/H10 | netty/NettyHttpClient.java:52,72,258-262,77-83,233 | @Value 作用于非 Bean 超时恒 0；无显式端口 createUnresolved(host,-1) 炸；域名分支忽略显式端口；setSsl 不重建 initializer mTLS 失效；SslContextFactory(null,null,null) NPE 包成 Error | 构造传参/端口回退 80/443/重建 pipeline/无证书显式策略 |
| C/H11 | netty/NettyHttpServer.java | CONTENT_LENGTH 写入前取值恒 0 客户端读不到 body；DefaultFileRegion 三参构造 + RandomAccessFile 不关 fd 泄漏；堆栈直接写回客户端外泄 | 写后设长/五参构造/统一错误码 |
| C/H12 | http/VertxHttpConfigure.java webClientHttps | setTrustAll(true) 硬编码（1.x 可配置默认 false）→ HTTPS 中间人无防护 | 恢复配置驱动默认 false |
| C/H13 | http/VertxHttpClient.java | completeExceptionally(e.getCause()) cause=null 时 NPE 丢真实异常；回调 throw BizException 在事件循环线程到不了调用方；去 request.timeout()；headers 已 toLowerCase 但 Content-Type 混合大小写 putIfAbsent/get 永不命中 | cause 判空/completeExceptionally/恢复超时/统一小写 key |
| C/H37 | fieldvaluefinder/FieldValueFindHelp.java:141 | KEEP_SELF 命中 break 跳出整个 dto 循环 → 后续 dto 全部漏注入 | break→continue |
| C/H42 | excel/imp/ExcelReadListener.java:176-179,222-232,175,201 | recordFunction=null 静默 return 整批跳过；onException 吞异常且 errorConverter=null 时无错误记录 → 解析失败仍报 success；空 sheet getTempCellData NPE；merge null NPE | 缺失抛 BizException/无条件记错/判空 |

### 3.4 数据访问（dal/domain，路径已修正 infra）
| ID | 位置 | 问题 | 修复 |
|---|---|---|---|
| C/H14 | infra/dal/mybatis/util/IPageQuery.java:28-33 | (String) 强转 Integer 参数 CCE；无防护 parseLong；MAX_LIMIT=500 上限被删 | 恢复 parsePagingValue + 上限 |
| C/H15 | infra/dal/mybatis/util/PlusPageQuery.java:81 | params.put 无判空（1.x 有 if params!=null）→ getPage(null) NPE（已抽查实锤） | 恢复守卫 |
| C/H16 | domain/shared/Page.java:49,93 | pageSize=0 → ceil Infinity → (int) Integer.MAX_VALUE；sameValueAs 恒 false 违反 ValueObject 契约 | pageSize<=0 守卫 + 逐字段比较 |
| C/H17 | infra/dal/es/model/EsSourceData.java:8 + EsBaseData | @SuperBuilder→@Builder 丢父类字段 → builder 无法设 id，ES 文档 id 恒 null 随机分配，按 id 更新/查询失效 | 恢复 @SuperBuilder + @NoArgsConstructor |
| C/H18 | infra/dal/mybatis/util/SqlFilter.java:19-40（已抽查实锤） | 白名单退回黑名单子串：update_time/is_deleted 等合法列被误杀；删 ^[a-zA-Z0-9_]+$ 与 64 长度上限后注释变形可绕过 | 恢复白名单 + 长度上限 |
| C/H19 | domain/ApplicationContextHelp.java:23-58 | requireContext() 被删 6 方法裸解引用；getBean(Class) catch 空实现静默 | 恢复守卫 + log.warn |

### 3.5 校验（validator）
| ID | 位置 | 问题 | 修复 |
|---|---|---|---|
| C/H20 | annotation/EnumValid.java:56-66 | null 返回 FALSE（JSR-303 约定 null 放行，1.x 有注释）→ 可选字段恒不通过；findCheckMethod 被删精确 getMethod → 父类型参数 NoSuchMethodException | null→TRUE + 恢复方法查找 |
| C/H21 | annotation/IEnumValid.java:39,55 | checkForNull 默认 false→true 静默破坏既有接口；getCode().equals NPE | 默认改回 false + Objects.equals |
| C/H22 | annotation/DBLengthValid.java:59-64 | 10^10 intValue() 截断 1410065408 → 合法 10 位整数误判超长；length>=19 long 回绕 | 全程 BigDecimal compareTo |
| C/H23 | BaseAssert.java:153-172 | notBlank 改名 isBlank（名 is 实为 assertNot）+ trim 判空退回 "".equals → 下游调 notBlank 编译失败；纯空白串放行 | 恢复 notBlank + trim，见裁定#5 |
| C/H24 | CommonValidator.java 整类注释 + config/ValidatorAutoConfigure.java 删除 | 下游 @Autowired CommonValidator 编译失败；仓库内无引用，属对外 API 移除 | 见裁定#5 |

### 3.6 日志（log）
| ID | 位置 | 问题 | 修复 |
|---|---|---|---|
| C/H25 | requestlog/RequestLogAspect.java:55-105,66-67 | 兜底 try-catch 整体删除 → 解析异常炸业务请求；空 List get(0) 越界；非 Web 上下文 requireNonNull(request) NPE | 恢复降级 proceed + 判空 |
| C/H26 | sanitize/LogSanitizeUtil.java 整文件删除 | authorization/cookie/password/token/mobile 明文进日志；报文不再 2KB 截断刷爆日志盘 | 恢复工具类 + 三处调用点接回 |
| C/H27 | catchlog/CatchLogAspect.java:128-129 | 未知异常 e.getMessage() 透前端（SQL/路径外泄）；BaseException 分支被删 | 固定文案 + 恢复分支 |
| C/H28 | catchlog/CatchLogAspect.java:84-87 | 嵌套切面内层 finally 清掉外层 traceId | 恢复 oldTraceId 保存 + tracePutByThisLayer |
| C/H29 | LogInterceptor.java:66 | BEGIN_TIMESTAMP.get() 拆箱 NPE（异步 dispatch/preHandle 未执行） | 判空回退 -1 |
| C/H30 | webfilter/KdlaBodyReaderRequestFilter.java:36-58 | traceId 生成 + finally 清理被删 → Tomcat 线程复用 traceId 串号；GET 排除与 excludes 空判定被删 | 恢复生命周期 + 排除逻辑 |
| C/H31 | common/aspect/mdc/MdcAspect.java + MdcHelp | MdcHelp.clear() 清整个 MDC（宿主 userId/tenantId 一并清空）；restore 方法被删；assert 参数；SpEL 不降级 | 恢复保存-恢复模式 |

### 3.7 缓存/序列化/加密/其他
| ID | 位置 | 问题 | 修复 |
|---|---|---|---|
| C/H32 | cache/SpringRedisHelp.java:195-199,180 | unLock Lua 用裸 getBytes 与 template 序列化器不一致 → 锁永不释放且返回值被忽略；lock 拆箱 NPE | 统一序列化器 + Boolean.TRUE.equals + 失败 log.warn |
| C/H33 | common/utils/ObjectUtil.ObjectToByte | 删 oo.flush() → 块缓冲尾部字节丢失反序列化 StreamCorruptedException | 恢复 flush |
| C/H34 | common/utils/Sm4Util.java:77,95,105 | generateKey 返回 Base64 但 encrypt/decrypt 用 Hex.decode → 标准流程必抛 DecoderException | 统一编码 |
| C/H35 | common/utils/AESUtil.java | 随机 IV+v1: 前缀退回全零 IV → 1.x 存量密文不可解（不可逆）；异常消息带 password 明文 | 恢复 v1 方案 + 异常去密钥，见裁定#6 |
| C/H38 | common/help/LocalDateTimeHelp.java:311 | isMonthNew 只比月不比年 → 跨年同月误判；asLocalDate(long) 非 10/13 位由 null 改抛异常破坏下游判空分支 | YearMonth 比较 + 兼容语义 |
| C/H39 | supplement-job com/xxl/job/core/**（44 文件自 top/kdla/.../job rename） | 下游旧包 import 编译失败；与官方 com.xuxueli:xxl-job-core 同包同名类冲突。内容核实：除新增 DateUtil 外全部只有 package/import 变更 | 见裁定#5 |
| C/H40 | trdplatformcloud 30 处 javax（13 文件 @Resource + 6 VO/5 控制器 javax.validation）；pom:295-308 provided javax 掩盖编译 | Spring 6 只认 jakarta @Resource → 注入 null 首请求 NPE；Hibernate Validator 8 忽略 javax 约束 → 入参校验静默绕过（信任边界失效） | javax→jakarta + pom 换 jakarta 构件 |
| C/H41 | trdplatformcloud/utils/JwtUtil.java:19-38,31 | 注释称"不可篡改"但只 Base64 解码不验签 → 任意伪造用户；签名明文进 INFO 日志；解析失败静默返回 "" | 改 jjwt/nimbus 验签 + 删日志 + 失败抛异常 |
| C/H43 | trdplatformcloud TrdPlatformInfoServiceImpl.java:60、StringUtil.java:60 | Collectors.toMap 无 merge → 重复键 IllegalStateException / null value NPE | 补 merge 函数 / HashMap put |
| B/H1 | 根 pom:362 mybatis-plus-boot-starter；trdplatformcloud/pom.xml:238-248 | Boot2 系 starter 传递 mybatis-spring 2.1.2（m2 pom 实证），trd 未 exclude 且声明顺序胜出 → SqlSessionFactoryBean 引用 Spring 6 已删 NestedIOException → NoClassDefFoundError。dal 已 exclude 安全 | artifactId 改 mybatis-plus-spring-boot3-starter（根 pom + dal + trd） |
| B/H2 | 根 pom:80,366-370 jsqlparser 4.7 pin；dal:84-88、trd:249-253 | MP 3.5.12 jsqlparser 支持已拆 optional 模块（→5.1）；本仓无 InnerInterceptor 使用编译不受影响，但下游引 mybatis-plus-jsqlparser 时被根 DM 降版 4.7 → NoSuchMethodError | 删 pin 与显式声明由 MP 自管，或改 5.1 |

## 4. Medium

| ID | 位置 | 问题 | 修复 |
|---|---|---|---|
| A/M1 | dingding/job/trd 三模块 spring.factories | 残留活跃 EnableAutoConfiguration 死键（Boot 3 忽略；同名类已在各自 .imports 活跃注册，功能无损）；其余 6 模块 spring.factories 键全注释成空文件 | 删 9 个 spring.factories |
| A/M2 | common/pom.xml:441-454 | javax 三件套死依赖（代码已全 jakarta，抽查 LocalIpHelp.java:3 证实） | 删；BUILD-4/H40 完成后同步清根 pom:556-570 与 :108-110 |
| A/M3 | validator 自动配置整体移除 | 一致性删除自洽（仓库内无悬空引用），属功能下线 | 见裁定#5 |
| B/H3 | 根 pom:140-146 | snakeyaml 2.3 直接 compile 依赖被 15 模块继承传递下游，违反 provided 约定，破坏 model/reactor 零第三方依赖约定；Boot 3.4 BOM 本身管理 2.3 | 删整个 dependencies 块 |
| B/M1 | knife4j 4.4.0（基于 springdoc 2.3.0 构建，m2 pom 实证）+ 根 DM springdoc 2.7.0 | trd 使用；注解层兼容，autoconfigure 运行期兼容性无法静态判定 | 需实测：下游启动访问 /doc.html 与 /v3/api-docs |
| B/M2 | easyexcel 3.3.2 → POI 4.1.2（m2 pom 实证） | common/supplement 真实使用；JDK21 基本可用但老旧 | 需实测大文件导出；建议升 FastExcel 1.x 或 easyexcel 3.3.4 |
| B/M3 | 根 pom 无 build 段；job-application/pom.xml:121-139 | 默认 compiler 3.13.0 source/target 未用 release；job-application 自配 3.8.1 + source/target 1.8 与 java.version=21 矛盾；trd 7 个 @Mapper(componentModel=spring) + lombok 无 annotationProcessorPaths/binding → mapstruct 可能生成空映射（JDK23+ 默认禁 classpath 隐式发现） | 根 pom pluginManagement：release 21 + lombok/binding/mapstruct-processor 三 path |
| B/M4 | trdplatformcloud/pom.xml:128-132、supplement-job/pom.xml:103-107 | commons-lang3 重复声明（编译 WARNING 实锤定位） | 各删第二处 |
| B/M5 | TTL 2.14.3（common/supplement，ThreadPoolHelp 6 处 TtlExecutors） | 平台线程兼容；TTL Agent 不支持虚拟线程 | 版本无需升，文档标注限制 |
| B/M6 | common/pom.xml:429-433 camel 4.11.0 | 版本匹配 Boot 3.4.4 但全仓 import 0 命中 → 死依赖 | 删 |
| C/M1 | JEP 400 无 charset 调用点：log/KdlaHttpServletRequestWrapper.java:38,46,72（删了显式 UTF-8）、exception/UnifiedExceptionControllerAdvice.java:173、common/Sm2Util.java:127、trd/JwtUtil.java:27,29；既有：Md5Util:28、GzipUtil:32、SpringRedisHelp:199、job-app LoginService/UserController/GlueFactory | JDK18+ 默认 UTF-8，GBK 平台行为变化 | 补 StandardCharsets.UTF_8 |
| C/M2 | exception/UnifiedExceptionControllerAdvice.java:21,25,26,173（已抽查实锤 import） | snakeyaml DuplicateKeyException 错类型（应 spring.dao）；java.nio.file.AccessDeniedException 当 401（应 403）；javax.security.sasl.AuthenticationException（应 Spring Security）；getCharacterEncoding()=null 时 new String NPE；getMessage 透前端 | 换正确 import + 映射 + UTF-8 回退 |
| C/M3 | model/dto/MultiResponse.java | 删 totalCount 字段/getter 但 buildSuccess(items,totalCount) 仍收参丢弃 → 下游 getTotalCount 编译失败 | 恢复或改签名，见裁定#5 |
| C/M4 | supplement/http/ssl/SslContextFactory.java:75,89 | try-with-resources 被删流泄漏；异常包成 Error 调用方 catch(Exception) 捕不到 | 恢复 + IllegalStateException |
| C/M5 | supplement/http/RestHttpConfigure.java | 删 setConnectionTimeToLive(60s) → 静默断连复用失败 | 恢复 TTL（hc5 ConnectionConfig） |
| C/M6 | common/aspect/watch/StopWatchWrapperAspect.java | cost>error||cost>warn 等价 cost>warn 但文案标 error → 告警误分级 | 分级 if-else |
| C/M7 | monitor/PrometheusAutoConfigure.java:21 | @ConditionalOnClass 改自有类恒真（原第三方 CollectorRegistry/PushGateway）；PushGatewayManager 裸 @Component 缺依赖先炸 | 改回第三方条件 + manager 去裸 @Component |
| C/M8 | supplement/excel/imp/ExcelReadListener.java:234-239 | createTempFile 从不删除 → 磁盘持续增长 | deleteOnExit 或调用方清理 |
| C/M9 | log/catchlog/CatchLogAutoConfigure.java | 删 @ConditionalOnClass(Aspect) 与 kdla.config.catch-log.enabled 开关 → 宿主无法关闭；无用 import EnableAspectJAutoProxy | 恢复条件 + 删 import |
| C/M10 | log/LogInterceptor.java:48-51、CatchLogAspect.java:58-61 | MDC 放 JSON 串而非裸 traceId → %X{traceId} 输出整段 JSON，下游检索/透传失配 | 放裸 traceId |
| C/M11 | supplement/http/HttpContextHelp.java | SSLContext.getInstance("SSL") 过时；@Deprecated 与安全警告被删 | 改 TLS + 恢复警告 |
| C/M12 | common/help/AsyncThreadHelp.java | hutool buildFinalizable() 内部覆写 finalize（JEP 421 废弃待移除） | 改 build() + 显式 shutdown |
| C/M13 | common/help/DateHelp.java DATE_TIME_PATTERN_3/4 | "yyyy-MM-ddTHH:mm:ss.SSS" 字面 T 未引号，joda forPattern 抛 IllegalArgumentException（jshell 实测）；仓库内无引用，下游使用即炸 | 改 'T' 引号 |

## 5. Low（优化建议）

- C/L1 validator/ObjectValidator.java:11 import sun.misc.Unsafe 仅作 Class token（jdk.unsupported 仍导出，编译可过）→ 换 Object.class
- C/L2 DBLengthValid.java:73、EnumValid.java:83 e.printStackTrace() 退回 → 改 slf4j
- C/L3 supplement-job com/xxl/job/core/util/DateUtil.java:27-46 ThreadLocal 不 remove + 不必要全局锁（xxl-job 上游原样代码，非迁移引入）
- C/L4 LocalDateTimeHelp.java:25 DEFAULT_ZONE 声明未用，全部 systemDefault → 容器时区漂移
- C/L5 DateHelp.format() 每次 new ThreadLocal（既有问题）
- C/L6 BCryptUtil.checkPassword 非法 salt 抛 IllegalArgumentException 需调用方捕获
- C/L7 PrometheusPointAspect 未使用 import（MethodSignature/Method）
- C/L8 ExcelReadListener.java:32 `1000_0` 可读性差；:36 raw type
- C/L10 log/pom.xml 删 jackson-databind provided：编译无影响（无直接 import），运行期经 common JacksonUtil 间接需要，Boot Web 宿主必带，风险低
- A/Low 21 个自动配置类用 @Configuration 而非 @AutoConfiguration（功能等效，约定优化）；.imports 缺结尾换行
- B/死依赖清单（三锚点核验：pom 声明 + import 0 命中 + 调用 0 命中）：commons-httpclient 3.1（common:206/supplement:212/dingding:131）、jython 2.7.3（common:410）、itextpdf/xmlworker/itext-asian/flying-saucer（common:330-357）、jtidy（common:359）、commons-configuration 1.10（common:246）、commons-fileupload 1.5（common:222，javax.servlet 基 Boot 3 本不可用）、commons-math3（common:176）、ognl/aviator/luaj（common:398-420）、camel（common:429）→ 建议删（先问再删，见裁定#7）
- 保留确认：jpinyin 1.1.8（PinYinHelp 真实使用，纯 Java 兼容）、groovy 4.0.26（包名 org.apache.groovy 未变，代码 import groovy.lang.* 安全）、fluent-validator 1.0.9（ObjectValidator 仅用核心纯 Java API，jsr303 桥已 exclusion 且未用）
- 文档：.agents/AGENTS.md "仓库尚无 AutoConfiguration.imports 文件"记录已过时（9 模块已有该文件，多数条目被注释）

## 6. 否定性结论（已扫描确认无问题）

- 移除 API：全仓无 JAXB/activation/jws/omg/nashorn/SecurityManager/Thread.stop/finalize 覆写（唯一 sun.misc 命中为 C/L1 token 用法）
- Groovy 4 包名：org.codehaus.groovy 0 命中，编译安全
- 脚本引擎：getEngineByName/ScriptEngineManager 0 命中；jython/aviator/ognl/luaj 代码 0 使用
- setAccessible 20 处：除 AopTargetHelp（C/H1）外全部作用于用户 DTO 或 classpath unnamed module，安全
- MQTT refCnt 稳态平衡：MqttPingHandler:54 retain(+1) 与 MqttChannelHandler:120 手动 release + autoRelease 恰好配对，非双重释放；真实泄漏仅 C/H9 clear()
- reactor 模块零问题（8 文件改动全部 javax→jakarta + 日志占位符）；dingding 零问题
- spring-kafka 2.9.12 pin 无残留；spring.redis 旧配置键 0 命中；springfox/WebSecurityConfigurerAdapter/antMatchers 等移除 API 0 命中
- xxl-job 44 文件 rename 除新增 DateUtil 外无逻辑变更；MpBaseDo 删除仓库内 0 引用
- joda 纯日期 pattern parseLocalDateTime 实测成功，删 containsTimeField 不构成 bug
- jakarta.annotation-api 2.1.1/3.0.0 均含 Nonnull/Nullable，MqttClientConfig 迁移编译安全

## 7. 无法判定项（需用户/下游/网络确认）

| # | 事项 | 所需动作 |
|---|---|---|
| U1 | .imports 注释化是否刻意规避启动冲突（6dc7220 无动机说明；VertxMqttConfigure 无默认值 @Value 是旁证） | 用户确认，裁定#4 |
| U2 | 下游是否 component-scan top.kdla.framework.**（决定 REG 系列实际波及面与 @Component 类注册路径） | 下游确认 |
| U3 | 是否存在 AES/SM2/SM4 存量密文（决定 C/H35/H36 修复方案，不可逆数据风险） | 用户确认，裁定#6 |
| U4 | redisson-spring-data-34 最低引入版本、RedissonRedLock 移除版本（m2 无缓存，无 Nexus 凭证 + http-blocker） | 中央仓可达后 mvn dependency:get 验证 |
| U5 | knife4j 4.4.0 + springdoc 2.7.0 运行期兼容性 | 下游启动实测 /doc.html |
| U6 | POI 4.1.2 JDK21 大文件行为 | 补最小 xlsx 读写用例实测 |
| U7 | KdlaStringHelp.matches(url,null) 行为（C/H30 excludes 空判定删除后） | 补读该方法 |
| U8 | CommonValidator/notBlank/totalCount/xxl-job 旧包名的下游引用面 | 下游确认，裁定#5 |

## 8. 裁定清单（待用户按编号批复）

1. **甲类修复策略**：A) 按文件 `git checkout 1.0.1-SNAPSHOT -- <file>` + 重做 javax→jakarta（推荐，最短 diff、逐文件可控）；B) `git merge 1.0.1-SNAPSHOT`（19 提交 + 65 测试一次带回，但 pom/docs 冲突需裁决、测试需 jakarta 适配）；C) 逐条手写。影响：supplement/log/common/validator/dal/domain ~40 项。
2. **修复范围**：仅 Critical（17 项）/ Critical+High（推荐，安全项 H26/H27/H41/H12 不可延后）/ 全部 65 项 + 依赖项。
3. **job-application 路线**：A) 手工 javax→jakarta 全模块迁移 + 删 pom javax 三件套（推荐，最小 diff）；B) 升级内嵌 xxl-job-admin 至 3.x（jakarta 原生，改动大但免维护分叉）；C) 本轮不动（模块保持不可用）。
4. **自动配置恢复**：取消 6 模块 .imports 注释 + 新建 supplement .imports（5 类）+ 补 log/cache 缺失 5 类 + XxlJobConfig，同时恢复 @ConditionalOnProperty/@ConditionalOnClass 守卫（VertxMqttConfigure 必须加开关，否则未配 mqtt 下游启动炸）。前提：确认注释化非刻意（U1）。
5. **刻意删除项取舍**：CommonValidator/ValidatorAutoConfigure（恢复 or 文档化废弃）、BaseAssert.notBlank（推荐恢复，下游编译兼容）、MultiResponse.totalCount（推荐恢复）、xxl-job 包名 com.xxl.job.core（保留需文档化破坏性变更 + 官方构件冲突警示）。
6. **加密存量兼容**：有存量密文 → AESUtil 恢复 v1 随机 IV + 旧格式兼容分支、SM2/SM4 确定性 KDF 迁移方案；无存量 → 直接安全新方案（随机 IV + KDF）。异常消息去密钥明文无条件执行。
7. **依赖升级包**：redisson 升级 + RedLock 改造、MP starter 换 spring-boot3、jsqlparser 删 pin、snakeyaml compile 块删除、commons-lang3 去重、compiler release 21 + annotationProcessorPaths、死依赖 11 项删除（先问再删已列清单）——整包批准 or 逐项裁定。

## 9. 验证状态

- 已实测：mvn clean compile 15/16 SUCCESS（JDK 21，job-application 待裁定#3）；jshell SecureRandom 确定性、joda pattern；m2 缓存 pom 依赖传递考证
- not measured：全部运行时行为（2.x 无测试，下游启动/Redis/DB/MQTT 路径均未实测）；knife4j/POI 兼容性
- 修复后验证要求：每项修复配回归测试（1.x 有 65 个测试可移植适配），全量 mvn clean compile + test 通过才算完成