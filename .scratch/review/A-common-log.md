# 切片 A 评审报告：基础 utils（kdla-component-common + kdla-component-log）

- 取证基线：BASE=`136bf03`，HEAD=`0fa5ab7`，1.x 对照=`1.0.1-SNAPSHOT`，merge-base=`320529d`。
- 范围：`kdla-component-common/src/`、`kdla-component-log/src/`（81 文件，+3617/-287）。
- 方式：只读 git diff/show + read/grep；未运行 mvn，未连外部服务。运行时才能定论的项标 not measured。
- 本轮相关提交：`ad0787f`（common 恢复）、`e757332`（log 恢复）、`ae5146f`（LVT 软引用缓存）、`a7f0105`（自动配置注册）。

## 结论（先）

**GO。Critical = 0，必改 = 1。**

甲类恢复总体完整且忠实：AESUtil / Sm4Util / LogSanitizeUtil / CatchLogAspect / KdlaBodyReaderRequestFilter / KdlaHttpServletRequestWrapper / LogInterceptor / MdcAspect（除 LVT 新增）/ BigDecimalHelp / DateHelp / LocalDateTimeHelp / KdlaStringHelp / SelfSnowflakeGeneratorHelp / PinYinHelp / BytesUtil / ThreadPoolHelp / FileChunkTransmitHelp 与 1.x 逐字节一致或仅差 jakarta/LVT/本轮增强。唯一实锤漏修：**LocalIpHelp 的 1.x 热路径缓存未恢复**（A-01，必改）。StopWatchWrapperAspect 注解定位用 @annotation 绑定替代 1.x AopUtils 方案，属等效替代而非漏修（A-11，FYI）。

---

## 必改（1 条）

### A-01【必改｜性能｜甲类漏修（半恢复）】LocalIpHelp 1.x volatile 缓存未恢复，traceId 热路径每请求枚举网卡
- 路径：`kdla-component-common/src/main/java/top/kdla/framework/common/help/LocalIpHelp.java:184-194`（HEAD `getLocalIp4Address`）
- 问题：1.x 提交 `7c0cf81`（属 19 个丢失修复集，已验证 `320529d..1.0.1` 共 19 提交）同时改了 SelfSnowflakeGeneratorHelp（updateAndGet，**已恢复**）和 LocalIpHelp（+17 行 `localIp4AddressCache` volatile 缓存，**未恢复**）。HEAD 每次调用都走 `getLocalIp4AddressFromNetworkInterface()`（网卡枚举）+ 可能的 `getIpBySocket()`（socket 探测）。调用方 `SelfSnowflakeGeneratorHelp.generate():43` 每次生成 traceId 都调它，而 filter/interceptor/CatchLogAspect 每请求至少生成一次 traceId——1.x 注释明言"traceId生成为每请求热路径，解析一次后复用"。BASE 与 merge-base 均无缓存，本轮 diff 未触及该文件，属典型"恢复了一半"。
- 修复建议：按 1.x 恢复 `private static volatile String localIp4AddressCache` 及 getLocalIp4Address 内的缓存读写（`git show 1.0.1-SNAPSHOT:kdla-component-common/src/main/java/top/kdla/framework/common/help/LocalIpHelp.java` 对照），无 jakarta 牵连。
- 状态：待修。非阻断（正确性无损，generate() 有 catch→UUID 兜底），但每请求 syscall 开销明确，且是本轮修复目标（甲类完整性）内的遗漏。

## Optional（5 条）

### A-02【Optional｜结构】LVT 字节码读取器在两模块逐字重复（~55 行 ×2）
- 路径：`kdla-component-common/.../aspect/mdc/MdcAspect.java:127-180`；`kdla-component-log/.../requestlog/RequestLogAspect.java:225-276`
- 问题：`lvtParameterNames` + `readLvtParameterNames` 两份完全相同（含 LVT_CACHE/NO_NAMES 定义）。log 模块已依赖 common（RequestLogAspect import JacksonUtil/KdlaStringHelp），可下沉公共 helper。两份静态缓存各自独立，同一 Method 会被读两次字节码（每模块一次）。
- 修复建议：抽到 common（如 `top.kdla.framework.common.help.LvtParameterNameHelp`），两处调用；缓存随之共享。非缺陷，本轮刻意最小改动可接受，记结构债。
- 状态：只列不改。

### A-03【Optional｜安全增强｜1.x 既有设计】LogSanitizeUtil 仅精确匹配 5 个 Header 名，请求/响应 body 无字段级脱敏
- 路径：`kdla-component-log/src/main/java/top/kdla/framework/log/sanitize/LogSanitizeUtil.java:27-28,50-55`；消费方 `RequestLogAspect.java:162,170,193`、`CatchLogAspect.java:130`、`LogInterceptor.java:37`
- 问题：黑名单 exact-match（authorization/cookie/password/token/mobile），`X-Auth-Token`/`X-Api-Key`/`Proxy-Authorization`/`Set-Cookie` 等变体不命中；paraMap（含 @RequestBody 经 BeanUtil.beanToMap 展开的字段）与响应 JSON 仅截断不脱敏——body 内 password/idCard/mobile 明文入日志。无正则，故无 ReDoS 风险。HEAD 与 1.x 逐字节一致，属 1.x 刻意最小设计，非本轮引入、非半恢复。
- 修复建议：如需增强——黑名单改 contains/后缀匹配或可配置；body 脱敏按 JSON key 黑名单（password/secret/idCard/mobile/idNo）做值替换。涉及日志语义变更，建议单独立项。
- 状态：只列不改。

### A-04【Optional｜正确性边界｜本轮新增】ThreadPoolHelp.shutdownGracefully 超时路径不 shutdownNow
- 路径：`kdla-component-common/src/main/java/top/kdla/framework/common/help/ThreadPoolHelp.java:340-349`
- 问题：`awaitTermination` 返回 false（超时）时直接 return false，池内任务继续跑、非 daemon 线程可能阻止 JVM 退出；仅 InterruptedException 分支才 shutdownNow。常见模式是超时后也 shutdownNow 强制中断。
- 修复建议：`boolean terminated = pool.awaitTermination(...); if (!terminated) pool.shutdownNow(); return terminated;`。当前语义（调用方自行决定）也成立，故 Optional。
- 状态：只列不改。

### A-05【Optional｜正确性边界｜1.x 既有】FileChunkTransmitHelp.merge2 不截断已存在目标文件，与 merge() 的 append=false 修复不一致
- 路径：`kdla-component-common/src/main/java/top/kdla/framework/common/help/FileChunkTransmitHelp.java:232-256`
- 问题：本轮把 `merge()` 的 `FileOutputStream(destPath, true)` 修为 false（防旧内容污染），但注释标注"merge2 > merge"（推荐用法）的 `merge2` 用 `RandomAccessFile(destPath, "rw")`——不截断，目标文件已存在且更长时尾部残留旧字节。1.x merge2 同样如此（既有），但同一文件内两个合并入口语义不一致。
- 修复建议：merge2 开头 `writeRaf.setLength(0)`（或 map 完成后 `writeChannel.truncate(beginPos)`）。
- 状态：只列不改。

### A-06【Optional｜安全/健壮性｜1.x 既有】ObjectUtil.byteToObject 未同步 try-with-resources 化，且反序列化无 ObjectInputFilter
- 路径：`kdla-component-common/src/main/java/top/kdla/framework/common/utils/ObjectUtil.java:30-45`
- 问题：本轮 ObjectToByte 改 try-with-resources + flush（正确），byteToObject 仍手动 close 且不在 finally（异常路径泄漏，BAIS/OIS 泄漏危害小）；更重要的是 `ObjectInputStream.readObject` 无 JEP 290 过滤器，下游若传入不可信字节存在反序列化 gadget 风险。1.x 既有，非本轮引入。
- 修复建议：byteToObject 同步 try-with-resources；评估 `ObjectInputFilter.Config.setSerialFilter` 或方法级 filter（框架库设全局过滤器影响下游，需谨慎，建议 javadoc 警示 + 下游自配）。
- 状态：只列不改。

## Nit（4 条）

### A-07【Nit｜正确性/文档｜本轮新增】MultiThreadInvokeHelp.awaitAll 的 cancel(true) 不会中断正在执行的任务
- 路径：`kdla-component-common/src/main/java/top/kdla/framework/common/help/MultiThreadInvokeHelp.java:120-128`
- 问题：总预算超时修复本身正确（修掉 N×timeout 放大）。但 `CompletableFuture.cancel(true)` 忽略 mayInterruptIfRunning——已在跑的任务继续占用池线程直到自然结束，注释"超时不取消剩余任务"的修复表述 overstated：取消的只是 future 状态，不是任务执行。
- 修复建议：注释补一句"cancel 仅标记 future，运行中任务不被中断"；若需真中断，改用 ExecutorService.submit 拿 Future 再 cancel(true)。
- 状态：只列不改。

### A-08【Nit｜性能微优化｜本轮新增】RequestLogAspect 每请求 new DefaultParameterNameDiscoverer + 匿名 discoverer
- 路径：`kdla-component-log/src/main/java/top/kdla/framework/log/requestlog/RequestLogAspect.java:75-87`
- 问题：LVT_CACHE 已静态，但 discoverer 链每请求重建（小对象分配，热路径）。discoverer 无状态，可 static final 复用。
- 修复建议：提为 `private static final DefaultParameterNameDiscoverer NAME_DISCOVERER`（static 块 addDiscoverer）。
- 状态：只列不改。

### A-09【Nit｜边界｜1.x 既有（本轮恢复原样）】BytesUtil.reverse 边界校验 offset+length 可 int 溢出
- 路径：`kdla-component-common/src/main/java/top/kdla/framework/common/utils/BytesUtil.java:440`
- 问题：`offset + length > data.length` 在 offset 接近 Integer.MAX_VALUE 时溢出为负，绕过校验（随后循环仍会 AIOOBE，不产生静默错误数据，故仅 Nit）。
- 修复建议：改 `length > data.length - offset`（offset 已先判非负）。
- 状态：只列不改。

### A-10【Nit｜charset/边界｜1.x 既有（逐字节恢复）】AESUtil.decrypt Base64 串 getBytes() 平台默认 charset；null content NPE 在 try 外
- 路径：`kdla-component-common/src/main/java/top/kdla/framework/common/utils/AESUtil.java:71,84`
- 问题：`content.substring(3).getBytes()` / `content.getBytes()` 未显式 charset。Base64 字符集为 ASCII 子集，任何常规平台 charset 下行为一致，无实际危害，但违反"全部显式 UTF-8"约定。另 `content.startsWith(V1_PREFIX)` 在 try 外，null content 抛裸 NPE（旧版 decode 在 try 内会包成 RuntimeException("decrypt exception")）。HEAD 与 1.x 完全一致，属既有。
- 修复建议：`getBytes(StandardCharsets.US_ASCII)`；null 入参前置校验或纳入 try。
- 状态：只列不改。

## FYI（14 条）

### A-11【FYI｜等效替代，非漏修】StopWatchWrapperAspect 注解定位：HEAD @annotation 绑定 vs 1.x AopUtils.getMostSpecificMethod
- 路径：`kdla-component-common/src/main/java/top/kdla/framework/common/aspect/watch/StopWatchWrapperAspect.java:35-52`
- 说明：merge-base 是 `classTarget.getMethod(...)` 反射重取（会 NoSuchMethodException/注解 null）；1.x `9b5fff4` 改 AopUtils 方案；2.x BASE 独立改为 pointcut 参数绑定（`@annotation(stopWatchWrapper)` + argNames），本轮仅恢复阈值分级 if-else（与 1.x 一致）。绑定方案构造上保证注解非空，Spring 匹配走 most-specific method，接口代理场景理论等效；测试用 AspectJProxyFactory（CGLIB）覆盖有/无注解与阈值分级，**接口代理场景 not measured**。around 内保留 4 行注释掉的旧反射死代码（1.x 已删，BASE 遗留）——只列不删。error 阈值分支仍 log.warn（1.x 亦然，刻意）。
- 状态：确认即可，无需改。

### A-12【FYI｜1.x 既有死代码/边界】FileChunkTransmitHelp 其余遗留
- 路径：`kdla-component-common/.../help/FileChunkTransmitHelp.java:145-161,261-271,91-95`
- 说明：`splitDetail2` private 且 0 调用（死代码），仍 log.error 吞 IOException——与本轮"失败显性化"修复不一致，但死代码只列不删；`delFileBySrcPath` 用 deleteOnExit（JVM 退出才删，DeleteOnExitHook 列表累积）；merge2/splitDetail2 的 MappedByteBuffer 无显式 unmap（Windows 下文件句柄延迟释放，JDK21 无公开 unmap，属平台限制）；split() 的 `beginPos = i * blockSize` 为 int，>2GB 文件溢出（1.x 既有）。
- 状态：只列不改。

### A-13【FYI｜1.x 既有】GzipUtil.unZip 流关闭不在 finally；注释死代码
- 路径：`kdla-component-common/src/main/java/top/kdla/framework/common/utils/GzipUtil.java:53-71,38-39`
- 说明：read 抛异常时 GZIPInputStream（持 Inflater native 资源）/bos 不关闭；compression 的 finally 关闭正确。本轮仅加 UTF-8（正确，1.x 反而是平台默认）。BASE64Decoder 注释块为死代码。均 1.x 既有。
- 状态：只列不改。

### A-14【FYI｜1.x 既有语义】KdlaBodyReaderRequestFilter 生命周期/排除语义
- 路径：`kdla-component-log/src/main/java/top/kdla/framework/log/webfilter/KdlaBodyReaderRequestFilter.java:46-72,75-84`
- 说明：HEAD 与 1.x 仅差 jakarta。① `excludes == null || isEmpty()` → 跳过所有 body 包装（默认值 `/config/**,/ping` 非空，正常不触发；用户显式配空列表时语义为"全不包装"，1.x 即如此）；② finally 无条件 `MDC.remove(LOG_TRACE_ID)`——即使 traceId 是进入前已存在的（非本 filter 放入）也移除，与切面 save-restore 模式不一致，但 servlet 场景 filter 是最外层 owner，实际无害；③ async dispatch：finally 在初始线程返回时即清 MDC，异步续跑线程无 traceId（1.x 设计）；④ @WebFilter + @Bean 双注册风险：下游若开 @ServletComponentScan，容器实例不经 Spring 注入，excludes=null → 全跳过包装（与自动配置实例并存）。GET/DELETE 排除修复（BASE 漏判 GET）已正确恢复。
- 状态：确认即可。

### A-15【FYI｜1.x 既有】KdlaHttpServletRequestWrapper 的 ServletInputStream 异步语义不正确
- 路径：`kdla-component-log/src/main/java/top/kdla/framework/log/webfilter/KdlaHttpServletRequestWrapper.java:50-69`
- 说明：`isFinished()` 恒 false、`isReady()` 恒 false、`setReadListener` 空实现——阻塞读（@RequestBody 常规路径）可用，非阻塞/异步读会挂起或误判；仅覆写单字节 read()，read(byte[],off,len) 走 InputStream 默认逐字节循环（性能）。UTF-8 显式化与 body="" 兜底为本轮正确恢复。1.x 既有。
- 状态：只列不改。

### A-16【FYI｜1.x 既有设计】MDC 不跨 TTL 包装线程池传递
- 路径：`kdla-component-common/.../help/ThreadPoolHelp.java`（全部 TtlExecutors.getTtlExecutorService 包装）、`AsyncThreadHelp.java`
- 说明：TTL 只传递 TransmittableThreadLocal；logback MDC 是普通 ThreadLocal，异步任务内 traceId/bizCode 丢失。1.x 即如此，非本轮引入。如需跨线程 traceId，须 TTL agent 或任务装饰器手动 copy MDC——属新特性，不在本轮范围。
- 状态：确认即可。

### A-17【FYI｜1.x 既有（本轮仅 buildFinalizable→build）】AsyncThreadHelp 双 volatile 字段非原子赋值
- 路径：`kdla-component-common/src/main/java/top/kdla/framework/common/help/AsyncThreadHelp.java:41-42,59-74`
- 说明：initExecutorService 已 synchronized + 旧池 shutdown（正确），但 executorService 与 simpleTimeLimiter 两次 volatile 写非原子——重建窗口内读线程可能拿旧池 + 新 limiter。重建是低频运维操作，窗口极小。daemon 线程工厂已确认（:24-38），build() 替代 buildFinalizable 规避 JEP 421，正确。sleep 中断恢复、prettyPrint 判空/除零防护均与 1.x 一致。
- 状态：确认即可。

### A-18【FYI｜恢复正确性确认】Sm4Util String 密钥 API Hex→Base64 属恢复 1.x 语义
- 路径：`kdla-component-common/src/main/java/top/kdla/framework/common/utils/Sm4Util.java:101-122,154-158`
- 说明：2.x BASE 的 String 密钥 API 用 Hex.decode，1.x 用 Base64（generateKey/generateSecureKey 均输出 Base64）。HEAD 恢复 Base64 后密钥生成/加解密自洽；对未发布的 2.x BASE Hex 格式是破坏性变更，但 2.0.0 未发版，恢复 1.x 语义正确。旧 ECB 无前缀兼容分支保留（刻意，注释标明迁移后可移除）。`import java.security.Key` 与 `log` 字段未使用（1.x 既有死代码，只列不删）。
- 状态：确认即可。

### A-19【FYI｜安全理论面｜1.x 既有】AES/SM4 decrypt 异常 cause 链保留 BadPaddingException
- 路径：`AESUtil.java:78-80,88-90`；`Sm4Util.java:137-152`
- 说明：异常消息已去密钥明文（"Invalid key size, length N no 16 size"/"decrypt exception"，本轮恢复正确，测试覆盖）；但 cause 链保留底层 BadPaddingException，调用方若检查 cause 类型理论上可区分 padding 错误（padding oracle 表面）。库层面无网络响应路径，未证明可利用，not measured。IV 真随机（SecureRandom.nextBytes，测试验证同明文两次密文不同）；v1 前缀 + 旧格式兼容分支逻辑正确（combined.length<16 时 NegativeArraySize 被 catch 包装）。
- 状态：确认即可。

### A-20【FYI｜1.x 既有链路】RequestLogAspect 参数名不可解析时参数日志静默丢失
- 路径：`kdla-component-log/.../requestlog/RequestLogAspect.java:138-141`；`kdla-component-common/.../utils/JacksonUtil.java:48-59`
- 说明：LVT 回退也拿不到名（如 JDK 代理接口方法、-g:none 编译）时 paraName=null → HashMap null key → JacksonUtil.toJson 默认 null-key 序列化抛 JsonProcessingException 被吞返回 "" → 日志输出 `Parameters: {}` 空串，无 warn。降级不炸业务（正确），但参数内容静默丢失。1.x 既有链路。
- 状态：确认即可。

### A-21【FYI｜1.x 既有】CatchLogAspect finally 内 fastjson 序列化可能抛异常掩盖业务结果
- 路径：`kdla-component-log/src/main/java/top/kdla/framework/log/catchlog/CatchLogAspect.java:130`
- 说明：`JSON.toJSONString(args/response)` 在 finally 内，若参数 getter 抛异常或序列化失败，RuntimeException 从 finally 传播，替换业务返回值/原始异常。fastjson 2.0.57（compat 包，autoType 默认关，doBefore 首参解析安全）；filterArgs 已排除 servlet/multipart 但未排除 InputStream 等。1.x 既有（本轮仅加 truncate 包装）。Error 重抛、Response 返回类型守卫、嵌套 traceId save-restore、未知异常固定文案"系统繁忙，请稍后重试"均与 1.x 逐字节一致，恢复完整。
- 状态：确认即可。

### A-22【FYI｜LVT 缓存语义确认（特别核对项 2）】
- 路径：`MdcAspect.java:44-46,127-180`；`RequestLogAspect.java:55-57,225-276`
- 说明：① 两处实现逐字一致（缓存结构、哨兵、slot 计算、ASM 访问器）；② ConcurrentReferenceHashMap 默认软引用——仅内存压力时回收条目触发重读，正常工况每 Method 读一次字节码，不会频繁重读（Spring 自身同款模式）；③ Method 键被软引用包裹，无硬 ClassLoader 泄漏；④ NO_NAMES 哨兵用 `==` 身份比较，语义正确（computeIfAbsent 原子，线程安全）；⑤ slot 计算含 static/this 偏移与 long/double 双槽，正确；⑥ 返回的缓存数组调用方只读不写，无缓存污染；⑦ 接口/抽象方法无 Code/LVT → NO_NAMES → MdcAspect bizCode 降级 ""、RequestLogAspect 走 A-20 链路——JDK 代理接口方法场景 not measured；⑧ 编译器 LVT 条目顺序异常（参数槽被局部变量复用且条目乱序）时取首个匹配，理论可误取——javac 常规输出参数条目在前，not measured。MdcAspectTest 在无 -parameters 的测试编译下实际走 LVT 回退并覆盖缓存复用/嵌套恢复/SpEL 降级。
- 状态：确认即可。

### A-23【FYI｜已登记候选项确认】
- `RequestLogAutoConfigure.java:10` 残留未用 `EnableAspectJAutoProxy` import——确认存在。
- `AopTargetHelp.java`、`ThreadPoolHelp.java`、common/log 两个 `.imports` 文件末尾缺换行——确认存在。
- `DateHelp.format:85-95` 每次调用 new ThreadLocal（C/L5）——确认；补充：方法返回后 ThreadLocal 键不可达但 Entry 留在调用线程 ThreadLocalMap（stale entry，值 SimpleDateFormat 滞留至 expungeStaleEntries），长活池线程反复调用会累积，且无 remove()。1.x 既有。
- `LocalDateTimeHelp.DEFAULT_ZONE:25` 未使用（C/L4）——确认存在。
- 状态：只列不删。

### A-24【FYI｜1.x 既有边界】DateHelp.containsTimeField 引号字面量误判 + 遗漏 joda 'A'/'n'/'N'
- 路径：`kdla-component-common/src/main/java/top/kdla/framework/common/help/DateHelp.java:117-124`
- 说明：pattern 含引号内字面 'm'/'s' 等（如 `yyyy-MM-dd 'min'`）会被误判为时间 pattern → parseLocalDateTime 对纯日期串抛异常；joda 毫秒OfDay 'A'、纳秒 'n'/'N' 未纳入判断。常规 pattern（含 DATE_TIME_PATTERN_3 引号 T 修复，已正确恢复）不受影响。1.x 既有。另 RegexHelp（本轮无 diff）：2.x BASE 的 `DOUBLE="^[-+]?\d*(\.\d+)?$"` 允许空串匹配（isDouble("")=true），1.x `[0-9]+` 不允许——2.x BASE 既有语义差异，非本轮引入，非丢失修复（320529d..1.0.1 无 RegexHelp 提交，已验证）。
- 状态：确认即可。

### A-25【FYI｜1.x 既有】Sm2Util formatKeyPair 平台 charset + generateKeyPair 每次 new BouncyCastleProvider
- 路径：`kdla-component-common/src/main/java/top/kdla/framework/common/utils/Sm2Util.java:92-101,84-89`
- 说明：`new String(Base64.getEncoder().encode(...))` 无显式 charset（Base64 ASCII 子集，实际无害）；generateKeyPair 用 `new BouncyCastleProvider()` 而非静态 PROVIDER（每次新建实例，static 块防重复注册的意图被绕过）。decrypt 侧 UTF-8 修复与 generateSecureKeyPair 新增均正确（本轮）。1.x 既有。
- 状态：只列不改。

---

## 特别核对项逐项回应

1. **甲类恢复完整性**：逐文件 HEAD vs 1.x diff 核对。唯一漏修 = A-01（LocalIpHelp 缓存，7c0cf81 半恢复）。StopWatchWrapperAspect 注解定位为等效替代（A-11）。其余恢复文件与 1.x 一致或为超集（新增 null 守卫/方法），jakarta 重做无漏网（log 模块 5 个 servlet 相关文件 import 全部 jakarta，common 的 LocalIpHelp 亦 jakarta）。
2. **LVT 两处一致性/线程安全/软引用**：见 A-22，一致、线程安全、无频繁重读、无硬泄漏；重复代码记 A-02。
3. **charset 显式 UTF-8**：本轮新增/恢复的 charset 修复全部正确（GzipUtil/Md5Util/Sm2Util.decrypt/Wrapper/LogInterceptor 链路）。残留平台默认仅 1.x 既有的 Base64 ASCII 场景（A-10/A-25），无实际危害。
4. **并发**：ThreadLocal remove——LogInterceptor.afterCompletion 三件全清（MDC/LogTraceHolder/BEGIN_TIMESTAMP），LogTraceHolder.get() null 安全（置 FALSE）；CatchLogAspect/MdcAspect 嵌套 save-restore 正确；DateHelp.format 的 ThreadLocal 无 remove（A-23，1.x 既有）。线程池关闭——AsyncThreadHelp 旧池 shutdown + daemon、ThreadPoolHelp.shutdownGracefully 超时路径可商榷（A-04）。MDC 跨线程——TTL 不传 logback MDC（A-16，1.x 设计）。
5. **加密**：IV 真随机（SecureRandom，测试覆盖密文差异性）；v1 前缀/旧格式兼容分支正确；异常消息无密钥明文（测试覆盖）；cause 链理论 padding oracle 面记 A-19（not measured）；Sm4 密钥编码恢复 1.x Base64 自洽（A-18）。

## GO/NO-GO

**GO**（Critical = 0）。必改 1 条（A-01 LocalIpHelp 缓存恢复），建议随下一提交补上；Optional 5 / Nit 4 / FYI 14 均不阻断。