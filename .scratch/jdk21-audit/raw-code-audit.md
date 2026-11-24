task_id: ses_df96bf76effeVyzWS0SrD6AuVS (for resuming to continue this task if needed)

<task_result>
审计完成。以下是最终报告。

---

# kdla 2.0.0-SNAPSHOT（JDK 21 / Boot 3.4.0）迁移代码审计报告

## 核心结论

`git diff 1.0.1-SNAPSHOT..HEAD` 的方向证明：**HEAD 的业务代码比 1.0.1 更旧**。1.0.1 分支上带中文注释说明的大量 bug 修复，在 2.x 分支被整体回退（最新代码提交 `cbf097f Merge branch '1.0.0-SNAPSHOT' into 2.0.0-SNAPSHOT`）。因此绝大多数问题不是"JDK 21 新语义踩坑"，而是**迁移过程丢失既有修复**型回归。真正由 JDK 21 强封装/默认 charset 触发的只有 3 处（AopTargetHelp、ObjectValidator 递归、SecureRandom 确定性）。

---

## Critical（编译失败 / 启动必炸 / 数据丢失）

### C1. 两个模块 parent 版本错配，整个 reactor 无法构建
`kdla-component-supplement-job-application/pom.xml:9`、`kdla-component-supplement-trdplatformcloud/pom.xml:9` → `<version>1.0.0-SNAPSHOT</version>`，根 pom 为 `2.0.0-SNAPSHOT`（其余 13 模块已同步）。

**已取证**：`~/.m2/repository/top/kdla/framework/kdla-components/1.0.0-SNAPSHOT/` 内**只有** `kdla-components-1.0.0-SNAPSHOT.pom.lastUpdated` 与 `resolver-status.properties`，**无 .pom 实体**；resolver-status 记录 `rdc-snapshots`/`snapshots` 两个仓库均解析失败。叠加本机无 Nexus 凭证 + Maven 3.8+ http-blocker 拦截 `http://nexus.enncloud.cn` → parent 不可解析。

触发条件：任何 `mvn` 命令。Maven 先解析全部模块 POM 才建 reactor，`-pl`/`-am` **无法绕过**。
改法：两个 pom 第 9 行改 `2.0.0-SNAPSHOT`。

### C2. `KdlaExcelWriteHelp.writeSheet` 不关闭 ExcelWriter → 生成的 xlsx 损坏
`kdla-component-supplement/src/main/java/top/kdla/framework/supplement/excel/exp/KdlaExcelWriteHelp.java:61-67`

```java
public static <T> void writeSheet(File file, Map<Class<T>, Collection<T>> lists) {
    ExcelWriter writer = EasyExcel.write(file).build();
    for (...) { writer.write(classMap.getValue(), sheet); }
}   // 无 finish()、无 try-finally
```

1.0.1 有 `finally { writer.finish(); }`，注释原文："旧实现 writer 创建后未关闭，文件句柄/输出流泄漏；finish() 会 flush 并释放底层资源"。
触发条件：调用 `writeSheet` 任意一次。xlsx 是 ZIP 容器，不 finish 则中央目录未写入 → **文件无法打开（数据丢失）+ 句柄泄漏**，正常路径也炸，不只异常路径。
改法：`try (ExcelWriter writer = EasyExcel.write(file).build()) { ... }`（EasyExcel 的 ExcelWriter 实现 Closeable，close 内部调 finish）；或恢复 `try { ... } finally { writer.finish(); }`。

### C3. supplement 模块自动配置全部失效
`kdla-component-supplement/src/main/resources` **整个目录不存在**。1.0.1 的 `META-INF/spring.factories`（7 行）被删，**未补** `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。

原注册 6 项全部不再自动装配：`CustomCorsConfig`、`RestHttpConfigure`、`VertxHttpConfigure`、`VertxConfigure`（类已删）、`VertxMqttConfigure`、`FieldValueFindConfigure`。
触发条件：下游仅依赖构件、不 component-scan `top.kdla.framework.supplement` 包 → CORS/HTTP 客户端/MQTT/字段注入全部静默失效。
改法：新建 `kdla-component-supplement/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，逐行写入上述 5 个仍存在的配置类全限定名（Boot 3 忽略 spring.factories 的 EnableAutoConfiguration 注册，只改 spring.factories 无效）。

### C4. `VertxMqttConfigure` 无默认值 @Value + 条件注解被删
`kdla-component-supplement/.../mqtt/vertx/VertxMqttConfigure.java:51` → `@Value("${kdla.mqtt.topics}")`，同时删掉 `@ConditionalOnClass` 与 `@ConditionalOnProperty(kdla.config.mqtt.enabled)`。
触发条件：C3 修复后（或被 component-scan 命中）且宿主未配 `kdla.mqtt.topics` → `PlaceholderResolutionException`，**启动失败**。
改法：`@Value("${kdla.mqtt.topics:}")` + 恢复 `@ConditionalOnProperty(name = "kdla.config.mqtt.enabled", havingValue = "true")`。

### C5. `NettyHttpServer.start()` bind 成功后立即关闭服务端 channel
`kdla-component-supplement/.../netty/NettyHttpServer.java`，新增 `finally { if (!close.get()) { f.channel().close(); workerGroup.shutdownGracefully(); bossGroup.shutdownGracefully(); } }`。代码内自带 `//TODO 应是一直开着的`。
触发条件：启动即触发，服务端口立刻不可用。
改法：删除该 finally 块；关闭逻辑只保留在显式 `stop()`/`close()` 中。

### C6. `NettyHttpClient` 三个发送方法 finally 立即关连接 + 无超时 get
`kdla-component-supplement/.../netty/NettyHttpClient.java:114-121 / 156-163 / 201-208`，`respPromise.get()`(124)、`future.get()`(166) 无超时（1.0.1 有 requestTimeout=30000）。
触发条件：任意一次 HTTP 调用 → 响应尚未读完连接即关，调用线程**永久挂起**。
改法：finally 只释放请求级资源，连接关闭移到响应完整读取后；`get(timeout, TimeUnit.MILLISECONDS)` 并捕获 TimeoutException 转 BizException。

### C7. `CsvUtil.parseCsv` 无限流
`kdla-component-supplement/.../excel/utils/CsvUtil.java:40` → `Stream.iterate(0, i -> i + 1).map(record::get)`，无 limit（1.0.1 为 `IntStream.range(0, record.size())`）。
触发条件：解析任意非空 CSV → 越界异常。
改法：`IntStream.range(0, record.size()).mapToObj(record::get)`。

### C8. `PlusPageQuery` 基本类型判 nonNull 恒真 → 分页参数归零
`kdla-component-dal/.../mybatis/util/PlusPageQuery.java:70,73` 与 `:117,120`

字段确认是基本类型：`private long pageNum;`(27)、`private long pageSize;`(31)。`Objects.nonNull(this.getPageNum())` 自动装箱 → **恒 true**，默认值 1/10 永不生效。
触发条件：`new PlusPageQuery()` 或 `PlusPageQuery(PageQuery)` 未设分页 → `new Page<>(0, 0)`。MyBatis-Plus 在 `size <= 0` 时关闭分页 limit → **全表查询**，内存/DB 双重风险。
改法：两处均改 `if (this.getPageNum() > 0)` / `if (this.getPageSize() > 0)`（1.0.1 原样）。

### C9. `LogInterceptor` 直接 new wrapper 消费原始输入流
`kdla-component-log/.../LogInterceptor.java:31` → `new KdlaHttpServletRequestWrapper(request)`，未替换下游 request。
1.0.1 注释原文："直接 new wrapper 会消费原始输入流且未替换下游 request，导致 controller 读到空 body"。
触发条件：任何带 body 的请求经过该拦截器 → **@RequestBody 收到空 body**（数据丢失）。叠加 `KdlaHttpServletRequestWrapper` 已删 IOException 时 `setBody("")` 兜底 → 构造/getBody 路径 NPE。
改法：恢复 `String body = request instanceof KdlaHttpServletRequestWrapper ? ((KdlaHttpServletRequestWrapper) request).getBody() : null;`，日志输出 `body == null ? "<body未缓存>" : body`。

### C10. `CatchLogAspect.around` 吞 Error + 非 Response 返回类型强转
`kdla-component-log/.../catchlog/CatchLogAspect.java:77-78`

```java
} catch (Throwable e) {
    response = handleException(joinPoint, e);   // 无条件转 Response
}
```

1.0.1 有两道守卫被删：`if (e instanceof Error) throw (Error) e;` 与 `if (!Response.class.isAssignableFrom(ms.getReturnType())) throw e;`。
触发条件：① 被切方法抛 OOM/StackOverflowError → 被吞并转成"失败 Response"，故障不可见；② 被切方法返回 `String`/`List`/`void` 等非 Response 类型 → 返回 Response 对象 → 调用方 **ClassCastException**。
改法：恢复这两道守卫，并把方法签名改回 `throws Throwable`。

### C11. `EmailJobAlarm` javax.mail 配 Spring 6 jakarta API
`kdla-component-supplement-job-application/.../core/alarm/impl/EmailJobAlarm.java:15,66,68,74` → `javax.mail.internet.MimeMessage` 传入 `JavaMailSender`/`MimeMessageHelper`（Spring 6 签名为 `jakarta.mail.internet.MimeMessage`）。
触发条件：C1 修复后按 Boot 3 编译 → 类型不兼容，编译失败。
改法：`javax.mail.*` → `jakarta.mail.*`，pom 换 `jakarta.mail-api` + `angus-mail`。
**注意**：当前受 C1 影响，实际编译结果取决于旧 parent，见"无法判定 U1"。

---

## High（特定路径运行时炸 / 数据错误 / 安全）

### 并发（kdla-component-common）

**H1. `AopTargetHelp` 反射 JDK 内部字段 → JDK 17+ 强封装拒绝**
`.../common/help/AopTargetHelp.java:55-56` → `proxy.getClass().getSuperclass().getDeclaredField("h")` + `setAccessible(true)`，目标是 `java.lang.reflect.Proxy` 的私有字段。
触发条件：JDK 21 下对 JDK 动态代理取目标对象 → `InaccessibleObjectException: Unable to make field private ... java.lang.reflect.Proxy.h accessible: module java.base does not "opens java.lang.reflect"`。**两分支都有此代码，非 diff 引入，但 JDK 8→21 升级直接触发**。
改法：`if (proxy instanceof Advised) return ((Advised) proxy).getTargetSource().getTarget();`，彻底不碰 `Proxy.h`。

**H2. `ObjectValidator` 递归防护全丢**
`kdla-component-validator/.../ObjectValidator.java:66-69,107,137-143`
- 无 `MAX_RECURSION_DEPTH`、无 `IdentityHashMap` visited、不跳过 static 字段 → 枚举字段的 `$VALUES` 静态数组自引用 → **StackOverflowError**（文件 133 行还留着"不然会报 StackOverflowError"的旧注释）。
- `getCommonlyClass().contains()` 精确 equals 取代 `isAssignableFrom` → `Instant`/`LocalDateTime` 等 JDK 类型进入递归后 `field.setAccessible(true)`(69) → **InaccessibleObjectException**（java.base 未 open java.time）。
- 只遍历 `getDeclaredFields()` 不含父类 → 父类字段校验注解漏检。
改法：恢复深度上限 + IdentityHashMap visited + `Modifier.isStatic` 跳过 + `isAssignableFrom` 判定 + 沿 `getSuperclass()` 向上遍历。
`import sun.misc.Unsafe`(11) 仅作 Class token 用，JDK 21 `jdk.unsupported` 仍导出该包，**编译可过，不是必炸**；但属待移除 API，建议换 `Object.class`。

**H3. `ThreadPoolHelp` 三处退化**
`.../common/help/ThreadPoolHelp.java`
- `:131` `getSingleExecutorService` 用 `SynchronousQueue` + 默认 AbortPolicy → 第二个并发任务立即 `RejectedExecutionException`。改法：`new LinkedBlockingQueue<>(1)`（保串行语义）或显式 CallerRunsPolicy。
- `:74,163` `DEFAULT_WORK_QUEUE` 是静态共享 `LinkedBlockingQueue(2000)`，被 `getDefaultExecutorService` 多个池共用 → 任务串扰、池间互相偷任务。改法：每次调用 new 独立队列。
- `:200-207` `getExecutorService(...)` 去掉 null 回退 → Builder/无参构造路径 corePoolSize 等包装类型为 null 时**拆箱 NPE**。改法：恢复默认值回退。
TTL 装饰（`TtlExecutors`）全部保留，**无丢失**。

**H4. `MultiThreadInvokeHelp` 新增 timeout 重载形同虚设**
`.../common/help/MultiThreadInvokeHelp.java:74-83,108-117` → 带 timeout 的重载内部调 `executeS`，后者先 `CompletableFuture.allOf(...).join()` **无超时**，timeout 参数从未参与等待。
触发条件：任一任务 hang → 调用线程永久阻塞，超时保护完全失效。`ExcelReadListener:190` 正在用这个 helper。
改法：`allOf(...).get(timeout, TimeUnit.MILLISECONDS)`，超时后 `cancel(true)` 未完成任务并抛 BizException。

**H5. `SelfSnowflakeGeneratorHelp` 序列号 check-then-act**
`.../common/help/SelfSnowflakeGeneratorHelp.java:72-79` → `updateAndGet` 原子更新退回先读后写。
触发条件：并发生成 traceId → **重复 ID**；`getAndIncrement` 可能返回 10000（5 位）破坏 ID 格式。
改法：恢复 `AtomicInteger.updateAndGet(prev -> (prev + 1) % 10000)` 单原子操作。

**H6. `AsyncThreadHelp` 初始化竞态 + 计时 NPE**
`.../common/help/AsyncThreadHelp.java:40-58,89,102,216,231`
- 静态字段去 volatile、`initExecutorService` 去 synchronized 且不关旧池 → 重复初始化时旧池线程泄漏 + 可见性问题。
- `sleep` 吞 `InterruptedException` 不 `Thread.currentThread().interrupt()` → 中断信号丢失，池无法优雅停止。
- `:216` `long totalCost = cost.remove(taskName)` → 重复调用同名任务 **拆箱 NPE**；未 allExecuted 时取到的是起始时间戳；`:231` 除零得 NaN。
改法：恢复 volatile + synchronized + 旧池 shutdown；sleep catch 内补 interrupt；`Long v = cost.remove(...); if (v == null) return;`，除法前判分母 > 0。

### Netty / MQTT（kdla-component-supplement）

**H7. `MqttPingHandler` 判空条件反向 → 死连检测彻底失效**
`.../mqtt/netty/MqttPingHandler.java:78` → `if (this.pingRespTimeout != null)`（1.0.1 为 `== null`）。
触发条件：首次 PINGRESP 超时后该字段仍为 null → 超时任务**永不注册** → 半开连接永远检测不到，客户端假死。
改法：改回 `== null`。

**H8. `MqttChannelHandler` ACK 处理去判空 + CONACK 去 default**
`.../mqtt/netty/MqttChannelHandler.java:233`（handlePubrec）、`:258`（handlePubcomp）→ 直接对 map.remove 结果解引用。
触发条件：broker 重发/错序 ACK（QoS2 正常网络抖动即可出现）→ **NPE 杀 event loop 线程**，该连接后续全部消息停摆。
`handleConack` 去掉 default 分支 → 未知 returnCode 时 `connectFuture` 永不完成，连接调用方挂死。
改法：恢复判空后 return；switch 补 `default: connectFuture.completeExceptionally(...)`。

**H9. `MqttClientImpl` 断连泄漏 + 并发容器退化 + 线程泄漏**
`.../mqtt/netty/MqttClientImpl.java`
- `:159` `qos2PendingIncomingPublishes.clear()` 前不 release → 断连时未完成 QoS2 消息 **堆外内存泄漏**。改法：`values().forEach(m -> ReferenceCountUtil.release(m.payload()))` 后再 clear。
- `:48,52,54,55` 并发容器退回 `HashSet`/`HashMultimap` → 调用线程与 event loop 并发读写，丢消息/`ConcurrentModificationException`。改法：恢复 `ConcurrentHashMap.newKeySet()` / 加锁包装。
- `:435-441` `disconnect()` 不再 `shutdownGracefully()` 自建 eventLoop → **NioEventLoopGroup 线程泄漏**，反复重连耗尽线程。改法：仅当 eventLoop 为自建时 shutdownGracefully。

**refCnt 重要澄清（避免误报）**：`MqttPingHandler:54` 的 `ctx.fireChannelRead(ReferenceCountUtil.retain(msg))` 提供 +1，与 `MqttChannelHandler:120` 手动 `payload().release()` + `SimpleChannelInboundHandler` autoRelease 恰好配对。**QoS0/1/2 稳态引用计数平衡，既不是双重释放也不是 use-after-free**。真实泄漏点只有 H9 的 clear()。

**H10. `NettyHttpClient` 连接超时失效 + 地址解析错误 + mTLS 失效**
`.../netty/NettyHttpClient.java`
- `:52,72` `@Value` 作用于非 Spring Bean → `connectTimeout` 恒 0，**无连接超时**。改法：构造器传参或提供 setter 默认值。
- `:258-259` IP 分支用 `uri.getPort()`，无显式端口时为 -1 → `InetSocketAddress.createUnresolved(host, -1)` 抛 `IllegalArgumentException: port out of range:-1`。改法：`uri.getPort() > 0 ? uri.getPort() : (ssl ? 443 : 80)`。
- `:262` 域名分支强制 80/443，**忽略 URL 里的显式端口**。改法：同上取有效端口。
- `:77-83` `setSsl` 不再重建 ChannelInitializer → 构造后再开 SSL/mTLS 配置不生效。改法：setSsl 内重建 pipeline initializer。
- `:233` `SslContextFactory.getClientContext(null, null, null)` → hutool 内部 NPE 被包成 `Error`（`SslContextFactory:75,89`）。改法：无证书时走 `SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)` 或明确抛 IllegalStateException。

**H11. `NettyHttpServer` 响应头恒 0 + fd 泄漏 + 堆栈外泄**
`.../netty/NettyHttpServer.java`
- `CONTENT_LENGTH = response.content().writerIndex()` → 写入前取值恒 **0**，客户端读不到 body。改法：写完内容后再设 `HttpUtil.setContentLength(response, response.content().readableBytes())`。
- `new DefaultFileRegion(raf.getChannel(), 0, raf.length())` 三参构造**不关闭 fd**，且 `RandomAccessFile` 未关 → 文件句柄泄漏。改法：五参构造 `new DefaultFileRegion(file, 0, len)`（传 File，Netty 自行管理）或 try-with-resources + 传输完成后关闭。
- `ExceptionUtils.getStackTrace(e)` 直接写回客户端 → 内部路径/SQL 外泄。改法：返回统一错误码，堆栈只进 log.error。

**H12. `VertxHttpConfigure` 硬编码信任所有证书**
`.../http/VertxHttpConfigure.java` → `webClientHttps` 内 `setTrustAll(true)`（1.0.1 为可配置、默认 false）。
触发条件：任何 HTTPS 调用 → **中间人攻击无防护**（信任边界安全措施被简化掉）。
改法：恢复 `@Value("${kdla.http.ssl.trust-all:false}")` 驱动。

**H13. `VertxHttpClient` 回调异常处理错误 + Content-Type 失效**
`.../http/VertxHttpClient.java`
- `future.completeExceptionally(e.getCause())` → cause 为 null 时 `CompletableFuture` 抛 NPE，真实异常丢失。改法：`completeExceptionally(e.getCause() != null ? e.getCause() : e)`。
- 回调内 `throw BizException` 运行在 Vert.x 事件循环线程 → **无法到达调用方**，只会被 Vert.x 吞掉打日志。改法：改为 `future.completeExceptionally(new BizException(...))`。
- 去掉 `request.timeout()` → 无请求超时。
- `:166` headers key 已 `toLowerCase()`，但 `:170` `putIfAbsent("Content-Type", ...)`、`:192` `get("Content-Type")` 用混合大小写 → **用户设置的 content-type 永远命中不了**，被默认值覆盖。改法：统一用 `"content-type"`。

### 数据访问 / 分页（dal、domain）

**H14. `IPageQuery.mapPage` 强转 + 无防护解析 + 无上限**
`kdla-component-dal/.../mybatis/util/IPageQuery.java:28-33`

```java
curPage = Long.parseLong((String) params.get(DoConstants.PAGE));
limit   = Long.parseLong((String) params.get(DoConstants.LIMIT));
```

触发条件：① 参数值是 Integer（JSON 反序列化常见）→ **ClassCastException**；② 非数字串 → NumberFormatException；③ `MAX_LIMIT = 500` 上限被删 → `limit=999999999` 直接打到 DB。
改法：恢复 1.0.1 的 `parsePagingValue(Object, long)`（`String.valueOf` + try-parse + 负数回退）与 `if (limit > MAX_LIMIT) limit = MAX_LIMIT;`。

**H15. `PlusPageQuery.getPage(Map, ...)` params 为 null 必 NPE**
`.../mybatis/util/PlusPageQuery.java:81` → `params.put(DoConstants.PAGE, page)` 无判空（1.0.1 有 `if (params != null)`）。
触发条件：`getPage((Map<String, Object>) null)` 或 `getPage(null, "col", true)` → NPE。
改法：恢复 `if (params != null)` 守卫。

**H16. `Page` 总页数溢出 + 值对象契约破坏**
`kdla-component-domain/.../shared/Page.java:49` → `this.totalPage = (int) Math.ceil((double) totalCount / pageSize);`
触发条件：`pageSize == 0`（见 C8，默认构造就会是 0）→ `Infinity` → `(int)` 得 **Integer.MAX_VALUE**；totalCount 极大时 int 截断。
改法：`this.totalPage = pageSize <= 0 ? 0 : (long) Math.ceil((double) totalCount / pageSize);`（字段需为 long）。
`:93` `sameValueAs(Page other) { return false; }` → 恒 false，违反 `ValueObject` 契约，任何按值比较的去重/断言全错。改法：恢复逐字段比较 + `Objects.equals(list, other.list)`。

**H17. `EsSourceData` @SuperBuilder → @Builder 丢父类字段**
`kdla-component-dal/.../es/model/EsSourceData.java:8`（`@Builder`）+ `EsBaseData.java`（删 `@SuperBuilder`/`@NoArgsConstructor`）。
`EsSourceData extends EsBaseData` 已确认。`@Builder` **只覆盖本类字段**（timestamp/type/source），父类 `id` 无法通过 builder 设置。
触发条件：`EsSourceData.builder()...build()` 写 ES → **文档 id 恒 null**，ES 自动分配随机 id，按 id 更新/查询全部失效（数据错误）。
改法：两个类都恢复 `@SuperBuilder`（`EsBaseData` 同时恢复 `@NoArgsConstructor`）。

**H18. `SqlFilter` 白名单退回黑名单子串**
`kdla-component-dal/.../mybatis/util/SqlFilter.java:19-40`
- 误杀：`update_time` 含 `update`、`is_deleted` 含 `delete`、`create_by` 不命中但 `select_flag` 含 `select` → **合法排序列被拒**。
- 防护弱化：删掉 `^[a-zA-Z0-9_]+$` 白名单与 64 长度上限后，`id/**/union/**/...`、换行/注释变形可绕过黑名单。
改法：恢复 `ORDER_COLUMN_PATTERN` 白名单 + `MAX_COLUMN_LENGTH`，黑名单只作补充。

**H19. `ApplicationContextHelp` 未初始化 NPE + 静默吞异常**
`kdla-component-domain/.../ApplicationContextHelp.java:23-58` → `requireContext()` 被删，6 个方法直接解引用静态 `applicationContext`；`getBean(Class)` 的 catch 块空实现。
触发条件：Bean 未注册/静态初始化早于 Spring 注入 → NPE 而非明确 IllegalStateException；按 type 查失败原因完全不可见。
改法：恢复 `requireContext()`；catch 内 `log.warn`；按 name 查包 `try-catch NoSuchBeanDefinitionException` 以落到统一友好报错。

### 校验（kdla-component-validator）

**H20. `EnumValid` null 判非法 + 方法查找退化**
`.../annotation/EnumValid.java:56-58` → `if (value == null) return Boolean.FALSE;`（1.0.1 返回 TRUE，注释："JSR-303 约定：null 值放行，交由 @NotNull 负责非空校验（此前返回 FALSE 导致可选字段恒不通过）"）。
触发条件：任何可选字段标 `@EnumValid` 且不传值 → **校验恒不通过**。
`:60-66` `findCheckMethod` 被删，退回 `enumClass.getMethod(enumMethod, valueClass)` 精确匹配 → 校验方法参数声明为 `Object`/`CharSequence` 等父类型时 `NoSuchMethodException` → 恒 FALSE；`enumMethod` 为空时走 `getMethod(null)` 同样炸。
改法：null 返回 TRUE；恢复 `findCheckMethod`（按 `isAssignableFrom` 找单参方法，回退无参）；`method.getParameterCount() == 0` 时 `invoke(null)`。

**H21. `IEnumValid` 默认值翻转 + getCode() NPE**
`.../annotation/IEnumValid.java:39` → `boolean checkForNull() default true;`（1.0.1 为 false）。
触发条件：下游未显式写 `checkForNull` 的可选字段 → 行为从"null 放行"变"null 拒绝"，**静默破坏既有接口**。
`:55` `enumConstant.getCode().equals(value)` → 任一枚举 `getCode()` 返回 null 即 **NPE**（1.0.1 用 `Objects.equals`）。
改法：default 改回 false；比较改 `Objects.equals(enumConstant.getCode(), value)`。

**H22. `DBLengthValid` 整数边界溢出**
`.../annotation/DBLengthValid.java:59-64`

```java
if (value instanceof Integer) return (Integer) value <= new BigDecimal(10).pow(this.length).intValue() - 1;
if (value instanceof Long)    return (Long) value    <= new BigDecimal(10).pow(this.length).longValue() - 1;
```

触发条件：`length = 10` → `10^10 = 10000000000`，`intValue()` 截断为 `1410065408`，上界变 `1410065407` → **合法的 10 位整数（如 2000000000）被误判超长**；`length >= 19` 时 `longValue()` 同样回绕。
改法：恢复全程 BigDecimal 比较 `new BigDecimal(value.toString()).compareTo(new BigDecimal(10).pow(length).subtract(BigDecimal.ONE)) <= 0`。

**H23. `BaseAssert.notBlank` → `isBlank` 改名 + 语义退化**
`.../BaseAssert.java:153-172` → 方法名从 `notBlank` 改回 `isBlank`（名 is 实为 assertNot，语义反转），判定从 `str.trim().isEmpty()` 退回 `"".equals(str)`。
触发条件：① 下游调 `notBlank` → **编译失败**（API 移除）；② 纯空白串 `"   "` 通过校验 → 脏数据入库。
改法：恢复 `notBlank` 命名与 trim 判空；若必须保留 `isBlank` 兼容，加 `@Deprecated` 转发。

**H24. `CommonValidator` 整类被注释 + 自动配置删除**
`.../CommonValidator.java`（96 行全部注释）、`.../config/ValidatorAutoConfigure.java`（整文件删除）、`spring.factories` -2 行。
触发条件：下游 `@Autowired CommonValidator` 或 import 该类 → **编译失败**。仓库内无引用，属对外 API 移除。
改法：明确是否为有意废弃；若保留则恢复类体并补 `AutoConfiguration.imports`（注意 `ValidatorAutoConfigure` 原 import 是 `javax.validation.Validator`，恢复时须改 jakarta）。

### 日志（kdla-component-log）

**H25. `RequestLogAspect` 兜底 try-catch 整体删除**
`.../requestlog/RequestLogAspect.java:55-105` → 1.0.1 的 `catch (Exception e) { log.warn(...); return point.proceed(); }` 被删，参数解析全程裸奔。
触发条件：解析期任何异常直接**炸掉业务请求**（日志切面不应影响业务）。具体触发点：
- `:66-67` `if (value instanceof List) value = ((List) value).get(0);` **无 isEmpty 检查** → 空 List 参数 `IndexOutOfBoundsException`。
- `Objects.requireNonNull(request)` → 非 Web 上下文（定时任务/MQ 消费调用被切方法）`getRequestAttributes()` 为 null → **NPE**。
- `BeanUtil.beanToMap(value)` 对循环引用/不可序列化对象抛异常。
改法：恢复外层 try-catch 降级 `return point.proceed()`；`get(0)` 前加 `!list.isEmpty()`；`requireNonNull` 改为 null 时跳过 URI 打印。

**H26. 日志脱敏与截断工具整类删除**
`.../sanitize/LogSanitizeUtil.java`（69 行，整文件删除），`RequestLogAspect`/`CatchLogAspect`/`LogInterceptor` 三处调用点同步移除。
后果：① `authorization`/`cookie`/`password`/`token`/`mobile` 请求头**明文进日志**；② 请求体/响应体不再 2KB 截断 → 大报文刷爆日志盘。
触发条件：任何带认证头的请求、任何大响应。
改法：恢复 `LogSanitizeUtil`（`maskHeader` + `truncate`）并在三处调用点接回。

**H27. `CatchLogAspect` 未知异常 message 透传调用方**
`.../catchlog/CatchLogAspect.java:128-129` → `ResponseHandler.handle(returnType, ErrorCode.UNKNOWN_ERROR.getCode(), e.getMessage())`。
1.0.1 注释原文："未知异常 message 可能含 SQL/内部路径等敏感信息，不外传调用方；详情只进上方 log.error"。
触发条件：任何未归类异常 → SQL 语句/文件路径/堆栈摘要返回前端。
改法：改回固定文案 `"系统繁忙，请稍后重试"`；同时恢复被删的 `if (e instanceof BaseException)` 分支以覆盖框架自定义异常。

**H28. `CatchLogAspect` 嵌套切面清掉外层 traceId**
`.../catchlog/CatchLogAspect.java:84-87` → `if (!LogTraceHolder.get()) { MDC.remove(LOG_TRACE_ID); ... }`，1.0.1 的 `oldTraceId` 保存 + `tracePutByThisLayer` 判定被删。
触发条件：嵌套 `@CatchAndLog`（外层方法调内层被切方法）→ 内层 finally **清掉外层放入的 traceId** → 外层后续日志全部丢失链路标识。
改法：入口保存 `String oldTraceId = MDC.get(...)` 与 `boolean tracePutByThisLayer = !StringUtils.hasText(oldTraceId)`，finally 内 `if (!LogTraceHolder.get() && tracePutByThisLayer) MdcHelp.restore(...)`（需同步恢复 `MdcHelp.restore`，见 H31）。

**H29. `LogInterceptor.afterCompletion` 拆箱 NPE**
`.../LogInterceptor.java:66` → `Long executionTime = DateHelp.now() - BEGIN_TIMESTAMP.get();`
触发条件：异步 dispatch / preHandle 未执行（预检失败、异常路径）→ ThreadLocal 为 null → **NPE**，且发生在 afterCompletion 会污染响应。
改法：`Long begin = BEGIN_TIMESTAMP.get(); long executionTime = begin == null ? -1L : DateHelp.now() - begin;`

**H30. `KdlaBodyReaderRequestFilter` 删 traceId 生命周期 + GET 排除**
`.../webfilter/KdlaBodyReaderRequestFilter.java:36-58`
- 整段 MDC traceId 生成（含从请求头透传）与 `finally { MDC.remove(LOG_TRACE_ID); }` 被删 → filter 层不再建立链路；且**无 finally 清理**，Tomcat 线程复用导致 **traceId 串号**（上一个请求的 id 泄漏到下一个请求日志）。
- `handleExcludeURL` 删掉 `method.matches(METHOD_GET)` 与 `excludes == null || excludes.isEmpty()` → GET 请求也走 body 缓存包装；`excludes` 未配置时 `KdlaStringHelp.matches(url, null)` 行为未验证（见 U3）。
改法：恢复 traceId 生成 + try-finally 清理；恢复 GET/DELETE 双排除与 excludes 空判定。

**H31. `MdcAspect` 清整个 MDC + assert + SpEL 不降级**
`kdla-component-common/.../aspect/mdc/MdcAspect.java` → finally 用 `MdcHelp.clear()`（清全部 key）取代精确 restore；`MdcHelp.restore` 方法本身也被删；`assert params != null`（-ea 下抛 AssertionError，默认关闭则静默）；SpEL 解析异常不再降级。
触发条件：宿主自己在 MDC 放了 userId/tenantId 等 → 被切面**一并清空**；SpEL 写错 → 炸业务方法。
改法：恢复保存-恢复模式与 `MdcHelp.restore`；assert 改显式判空 + 降级；SpEL 包 try-catch。

### 缓存 / 序列化 / 加密

**H32. `SpringRedisHelp.unLock` 序列化器不一致 → 解锁静默失败**
`kdla-component-supplement-cache/.../cache/SpringRedisHelp.java:195-199` → Lua 参数用裸 `key.getBytes()` / `value.getBytes()`，而写入走 `RedisTemplate` 的序列化器（可能是 Jackson/JDK 序列化，带前缀或二进制头）。
触发条件：template 非 StringRedisSerializer → Lua 内 `get(key)` 与传入 key 不匹配、value 比对永假 → **锁永不释放**，直到 TTL 过期；且返回值被忽略，失败完全不可见。
`:180` `lock()` 直接拆箱 `setIfAbsent(...)` → 连接异常返回 null 时 NPE。
改法：用 `template.getStringSerializer().serialize(key)` 或统一改用 `StringRedisTemplate`；`Boolean ok = setIfAbsent(...); return Boolean.TRUE.equals(ok);`；解锁失败必须 log.warn。

**H33. `ObjectUtil.ObjectToByte` 删 flush → 序列化数据损坏**
`kdla-component-common/.../utils/ObjectUtil.java` → 去掉 `oo.flush()`。
触发条件：`ObjectOutputStream` 有块缓冲，不 flush 直接 `toByteArray()` → **尾部字节丢失**，反序列化 StreamCorruptedException。
改法：`oo.flush();` 后再 `bos.toByteArray()`（或 try-with-resources 关闭 oo）。

**H34. `Sm4Util` 密钥编码不自洽**
`kdla-component-common/.../utils/Sm4Util.java` → `generateKey` 返回 **Base64**（64 字符），但 `encrypt(String,String)`/`decrypt(String,String)` 用 **`Hex.decode`**（`:77,95`）。
触发条件：`generateKey()` → `encrypt(data, key)` 标准流程 → **必抛 DecoderException**。`strKeyToSecretKey`(`:105`) 沦为死代码。
改法：统一编码，`encrypt/decrypt` 内改 `Base64.decode(key)`，或 `generateKey` 改返回 Hex。

**H35. `AESUtil` 零 IV 回退 → 存量密文不可解 + 密钥明文进异常**
`kdla-component-common/.../utils/AESUtil.java` → 随机 IV + `v1:` 前缀方案退回**全零 IV**；异常消息 `"Invalid key size. " + password`。
触发条件：① 1.0.1 加密的数据在 2.x **解密失败**（数据不兼容，不可逆）；② 密钥长度非法时**密钥明文进日志/异常链**。
改法：恢复随机 IV + `v1:` 前缀与旧格式兼容分支；异常消息去掉 password，只报长度数值。

**H36. `Sm2Util`/`Sm4Util` 种子派生密钥在 JDK 21 失效**
`Sm2Util.generateKeyPair(String keySrc)`、`Sm4Util.generateKey(String keySrc)` 依赖 `SecureRandom.getInstance("SHA1PRNG")` + `setSeed(keySrc)` 的确定性。
**jshell 实测（JDK 21）**：`deterministic=false, algo=NativePRNG` → 同 keySrc **不再派生同密钥**。
触发条件：任何用 keySrc 派生密钥加密的存量数据 → **永久无法解密**。
改法：改用确定性 KDF（`MessageDigest.getInstance("SHA-256").digest(keySrc.getBytes(UTF_8))` 取前 16/32 字节），不依赖 SecureRandom 实现细节。

**H37. `FieldValueFindHelp.batchQuery` break 应为 continue**
`kdla-component-supplement/.../fieldvaluefinder/FieldValueFindHelp.java:141` → KEEP_SELF 命中后 `break` 跳出整个 dto 循环。
触发条件：批量注入中任一 dto 命中 KEEP_SELF → **后续所有 dto 全部漏注入**（静默数据缺失）。
改法：`break` → `continue`。

**H38. `LocalDateTimeHelp.isMonthNew` 跨年误判**
`kdla-component-common/.../help/LocalDateTimeHelp.java:311` → 只比 `getMonthValue()` 不比年份（1.0.1 用 `YearMonth`）。
触发条件：2025-11 与 2026-11 被判为"同月" → 月度统计/新月判定逻辑错误。
改法：`YearMonth.from(a).equals(YearMonth.from(b))`。
另：`asLocalDate(long)`/`asLocalDateTime(long)` 对非 10/13 位时间戳由 `return null` 改抛 `IllegalArgumentException` → 下游依赖 null 判空的分支变异常。

**H39. supplement-job xxl-job 包名迁移破坏 API + 与官方构件冲突**
`kdla-component-supplement-job/src/main/java/com/xxl/job/core/**`（44 个文件从 `top/kdla/framework/supplement/job/**` rename）。
触发条件：① 下游 import 旧包 → **编译失败**；② 宿主同时依赖官方 `com.xuxueli:xxl-job-core` → classpath 上**同包同名类重复**，类加载顺序决定用哪份，版本不一致时 `NoSuchMethodError`/`AbstractMethodError`。
改法：保留旧包 deprecated 转发类，或明确文档化为破坏性变更；与官方构件冲突需 shade relocate 或排除官方依赖。
**内容层面已核实**：除新增 `com/xxl/job/core/util/DateUtil.java`（156 行，xxl-job 上游原样代码）外，**44 个文件的改动全部只有 package/import 行**，无逻辑变更。

**H40. supplement-job / trdplatformcloud javax 注解在 Spring 6 下失效**
- `kdla-component-supplement-job/.../timer/manager/impl/LocalJobManager.java:42`、`IotXxlJobManager.java:29` → `javax.annotation.Resource`，且类是 `@Component`。Spring 6 的 `CommonAnnotationBeanPostProcessor` **只认 jakarta.annotation.Resource** → 字段不注入 → 使用时 **NPE**。
- `kdla-component-supplement-trdplatformcloud/**` 大量 `javax.annotation.Resource` + `javax.validation.*`（pom 显式引入 javax.annotation-api / javax.servlet-api / validation-api 2.0.1，**编译可过但 Spring 6 / Hibernate Validator 8 完全忽略**）→ 注入失效 + **所有校验注解静默不生效**（信任边界校验被绕过）。
改法：全量 `javax.annotation` → `jakarta.annotation`、`javax.validation` → `jakarta.validation`、`javax.servlet` → `jakarta.servlet`，pom 同步换 jakarta 构件。

**H41. `JwtUtil` 不验签 + 签名进日志 + 静默返回空串**
`kdla-component-supplement-trdplatformcloud/.../utils/JwtUtil.java:19-38`（新增文件）
- 注释声称"获取用户名的**不可篡改**的信息"，但实现只 Base64 解码 payload，**完全未验证签名** → 任何人可伪造 payload 冒充任意用户（信任边界校验缺失）。
- `:31` `log.info("header {} signature {}", header, signature)` → JWT 签名与完整 header 明文进 INFO 日志。
- `auth.split(" ")[1]` 对 null/无空格输入抛异常，被 catch 吞掉后 `return ""` → 调用方可能把空串当合法用户名。
改法：改用 jjwt/nimbus 验签后再取 claim；删掉 signature 日志；解析失败抛 BizException 而非返回 ""。

**H42. `ExcelReadListener` 静默跳过导入 + 解析失败仍报成功**
`kdla-component-supplement/.../excel/imp/ExcelReadListener.java`（新增 298 行）
- `:176-179` `recordFunction == null` → `log.error` 后 **直接 return**，不设 `importResult.setSuccess(false)`、不调 `resultConsumer` → 调用方拿不到任何结果，**整批导入静默跳过**。
- `:222-232` `onException` 吞异常不上抛；若 `errorConverter == null` 则 `importError` 无记录 → `:199` 的 `hasError()` 判定为 false → **`importResult.setSuccess(true)` 但实际有行解析失败**（数据丢失被报成成功）。
- `:175` `context.readSheetHolder().getTempCellData().getColumnIndex()` → 空 sheet 时 `getTempCellData()` 为 null → NPE。
- `:201` `errors.forEach(error -> importError.merge(error))` → recordFunction 返回 null 时 merge NPE。
改法：recordFunction 缺失时抛 BizException；onException 内无条件 `importError.addError(...)`（errorConverter 为 null 时用默认转换器）；getTempCellData 判空；merge 前过滤 null。

**H43. trdplatformcloud `Collectors.toMap` 无 merge 函数**
`.../server/trd/impl/TrdPlatformInfoServiceImpl.java:60`（重复 platformCode → `IllegalStateException: Duplicate key`）、`.../utils/StringUtil.java:60`（value 为 null → **NPE**，`toMap` 内部走 `map.merge`）。
改法：补第三参 `(a, b) -> a`（或按业务取后者）；value 可能为 null 时改用 `HashMap` + forEach put。

---

## Medium（行为变化 / 资源泄漏 / 可观测性退化）

**M1. JEP 400 默认 charset 缺失调用点**（JDK 18+ 默认 UTF-8，JDK 8 依赖平台默认；评级 Medium，不夸大）
diff 引入或新增的无 charset 调用点：
- `kdla-component-log/.../webfilter/KdlaHttpServletRequestWrapper.java:38,46,72`（同时删掉了显式 UTF-8）
- `kdla-component-exception/.../UnifiedExceptionControllerAdvice.java:173`
- `kdla-component-common/.../utils/Sm2Util.java:127`
- `kdla-component-supplement-trdplatformcloud/.../utils/JwtUtil.java:27,29`（新增）
既有未改的：`Md5Util.java:28`、`GzipUtil.java:32`、`SpringRedisHelp.java:199`、`LoginService.java:31,37,56`、`UserController.java:96,127,166`、`GlueFactory.java:67`。
已显式 UTF-8（安全）：`Sm4Util`、`AESUtil`、`RSAUtil`、`KdlaExcelWriteHelp`。
统一改法：所有 `String.getBytes()` / `new String(byte[])` 补 `StandardCharsets.UTF_8`。
**排除项**：`VertxMqttConfigure:164` 的 `payload.getBytes()` 是 vertx `Buffer` API，与 charset 无关。

**M2. `UnifiedExceptionControllerAdvice` 异常类型映射错误**
`kdla-component-exception/.../UnifiedExceptionControllerAdvice.java`
- `import org.yaml.snakeyaml.constructor.DuplicateKeyException` → **错类型**，捕获的是 SnakeYAML 的重复 key 异常，不是 DB 唯一键冲突（应为 `org.springframework.dao.DuplicateKeyException`）。
- `java.nio.file.AccessDeniedException` 当 401 处理 → 文件系统权限错误被报成"未认证"。
- `javax.security.sasl.AuthenticationException` 当认证异常 → Spring Security 的 `AuthenticationException` 不再被处理。
- `:173` `new String(bytes, requestWrapper.getCharacterEncoding())` → 请求未带 charset 时 `getCharacterEncoding()` 返回 null → **NPE**。
- 兜底把 `exception.getMessage()` 透前端（同 H27）。
改法：换正确 import；`AccessDeniedException` 映射 403；认证异常改 `org.springframework.security.core.AuthenticationException`；编码 null 时回退 UTF-8。

**M3. `MultiResponse` 删 totalCount 但保留入参**
`kdla-component-model/.../dto/MultiResponse.java` → 字段与 getter 删除，`buildSuccess(items, totalCount)` 仍收参并**丢弃**。
触发条件：下游调 `getTotalCount()` → 编译失败；分页总数静默丢失。
改法：恢复字段/getter，或删掉入参并同步改签名（破坏性变更需文档化）。

**M4. `SslContextFactory` 流不关 + 抛 Error**
`kdla-component-supplement/.../http/ssl/SslContextFactory.java:75,89` → try-with-resources 被删（InputStream 泄漏）；异常包装成 `Error` 而非 `IllegalStateException` → 调用方 `catch (Exception)` 捕不到，且 Error 语义暗示 JVM 级故障。
改法：恢复 try-with-resources；改抛 IllegalStateException。

**M5. `RestHttpConfigure` 删连接 TTL**
`kdla-component-supplement/.../http/RestHttpConfigure.java` → 去掉 `setConnectionTimeToLive(60, TimeUnit.SECONDS)` → 连接池长驻连接遇到服务端静默断连后复用失败。hc5 `setConnectTimeout(long, TimeUnit)` 已 deprecated 但可编译。
改法：恢复 TTL；改用 `ConnectionConfig.custom().setConnectTimeout(Timeout.ofSeconds(n))`。

**M6. `StopWatchWrapperAspect` 阈值判定与文案不符**
`kdla-component-common/.../aspect/watch/StopWatchWrapperAspect.java` → `cost > errorTimeOut || cost > warnTimeOut` 等价于 `cost > warnTimeOut`（error 阈值更大时），但日志文案标 `errorTimeOut` → 监控告警误分级。
改法：`if (cost > errorTimeOut) {...} else if (cost > warnTimeOut) {...}`。

**M7. `PrometheusAutoConfigure` @ConditionalOnClass 改自有类 → 条件恒真**
`kdla-component-supplement-monitor/.../annotation/PrometheusAutoConfigure.java:21` → `@ConditionalOnClass({PushGatewayManager.class})`（本模块自有类，恒存在），原为 `{CollectorRegistry.class, PushGateway.class}`（第三方，可缺失）。
触发条件：宿主未引 prometheus simpleclient → 条件保护失效，仍创建 `PrometheusPointAspect` → 类加载 `io.prometheus.client.*` 时 **NoClassDefFoundError**。
**实际暴露点说明**：`PushGatewayManager` 本身带 `@Component`（无条件注册），缺依赖时它先炸，所以此改动的增量影响有限，但确实移除了 autoconfigure 层的保护。
改法：改回 `@ConditionalOnClass({CollectorRegistry.class, PushGateway.class})`，并给 `PushGatewayManager` 加同样条件（去掉裸 `@Component`，改由 autoconfigure 注册）。

**M8. `ExcelReadListener` 临时报告文件从不删除**
`.../excel/imp/ExcelReadListener.java:234-239` → `File.createTempFile(...)` 无 `deleteOnExit()`、无后续清理 → 每次失败导入留一个 xlsx，磁盘持续增长。
改法：`tempFile.deleteOnExit()` 或由调用方在消费后删除并文档化。

**M9. `CatchLogAutoConfigure` / `PrometheusAutoConfigure` 删开关条件**
`kdla-component-log/.../catchlog/CatchLogAutoConfigure.java` → 删 `@ConditionalOnClass(Aspect.class)` 与 `@ConditionalOnProperty("kdla.config.catch-log.enabled")` → 宿主**无法关闭**日志切面；新增 `import EnableAspectJAutoProxy` 但未使用。
改法：恢复两个条件注解；删无用 import。

**M10. `LogInterceptor` / `CatchLogAspect` MDC 放 JSON 串而非裸 traceId**
`LogInterceptor.java:48-51`、`CatchLogAspect.java:58-61` → `MDC.put(LOG_TRACE_ID, traceIdInfo.toJSONString())`，值为 `{"traceId":"123..."}`。
触发条件：日志 pattern 里 `%X{traceId}` 输出整个 JSON 串；下游按 traceId 精确检索/透传全部失配。1.0.1 注释明确"放裸 traceId 字符串，与 KdlaBodyReaderRequestFilter/CatchLogAspect 格式一致"。
改法：`MDC.put(LOG_TRACE_ID, SelfSnowflakeGeneratorHelp.generate())`。

**M11. `HttpContextHelp` 用 SSL 协议 + 删安全警告**
`kdla-component-supplement/.../http/HttpContextHelp.java` → `SSLContext.getInstance("SSL")`（JDK 21 仍可用但协议过时），`@Deprecated` 与安全警告注释被删。
改法：改 `getInstance("TLS")`，恢复警告。

**M12. `AsyncThreadHelp` 依赖 hutool buildFinalizable()**
`.../common/help/AsyncThreadHelp.java` → 用 hutool `ThreadFactoryBuilder.buildFinalizable()`，其内部覆写 `finalize()`。JDK 18+ finalization 已废弃待移除（JEP 421），未来 JDK 会失效。
改法：改 `build()`，线程池生命周期由显式 shutdown 管理。

**M13. `DateHelp.DATE_TIME_PATTERN_3` 是非法 joda pattern**
`kdla-component-common/.../help/DateHelp.java` → `"yyyy-MM-ddTHH:mm:ss.SSS"`，字面 `T` 未加引号。
**jshell 实测**：joda `DateTimeFormat.forPattern` 抛 `IllegalArgumentException: Illegal pattern component: T`。
**仓库内无任何引用**，故当前不触发；下游若使用即炸。新增的 `DATE_TIME_PATTERN_4` 同样无引用。
改法：改 `"yyyy-MM-dd'T'HH:mm:ss.SSS"`。

---

## Low（优化 / 风格 / 潜在风险）

- **L1** `kdla-component-validator/.../ObjectValidator.java:11` `import sun.misc.Unsafe` 仅作 Class token，JDK 21 可编译，属待移除 API → 换 `Object.class`。
- **L2** `DBLengthValid.java:73`、`EnumValid.java:83` `log.warn` 退回 `e.printStackTrace()` → 绕过日志框架，生产不可采集。改回 slf4j（注意 validator pom 已删 slf4j-api 依赖，需一并恢复或经 logback 传递）。
- **L3** `kdla-component-supplement-job/.../com/xxl/job/core/util/DateUtil.java:27-46` `ThreadLocal<Map<String,DateFormat>>` 从不 remove → 线程池场景 ClassLoader 泄漏；`synchronized(dateFormatThreadLocal)` 是不必要的全局锁（ThreadLocal 本身线程隔离）。**SimpleDateFormat 在 ThreadLocal 内，线程安全无问题**。属 xxl-job 上游原样代码，非迁移引入。
- **L4** `kdla-component-common/.../help/LocalDateTimeHelp.java:25` `DEFAULT_ZONE` 声明但从未使用，所有方法仍 `ZoneId.systemDefault()` → 容器时区变化即行为漂移。
- **L5** `kdla-component-common/.../help/DateHelp.java` `format()` 每次 new ThreadLocal（既有问题，非 diff 引入）。
- **L6** `kdla-component-common/.../utils/BCryptUtil.java`（新增，jbcrypt 0.4）`checkPassword` 对非法 salt 格式抛 `IllegalArgumentException`，调用方需捕获。
- **L7** `kdla-component-supplement-monitor/.../PrometheusPointAspect.java` pointcut 改注解参数绑定是**正向改进**（避免 `getMethod` 反射在桥接方法/代理上失败），但 `MethodSignature`、`Method` import 随注释代码变为未使用。
- **L8** `kdla-component-supplement/.../excel/imp/ExcelReadListener.java:32` `DEFAULT_MAX_ROW = 1000_0` 值为 10000（合法字面量，非 bug），可读性差；`:36` `new ExcelImportError<>()` 赋给 raw type。
- **L9** `KdlaExcelWriteHelp.java:57` `list.stream().findFirst().get()` 前已判 isEmpty，安全；但 `.get()` 无 orElse 不如 `iterator().next()` 直观。
- **L10** `kdla-component-log/pom.xml` 删 `jackson-databind` provided 依赖。**已核实 log 模块无直接 `com.fasterxml.jackson` import**，编译不受影响；运行期经 `JacksonUtil`（common）间接需要 jackson，provided 不透传 → 宿主未引 jackson 时 NoClassDefFoundError。Spring Boot Web 宿主必带 jackson，实际风险低。

---

## 否定性结论（已扫描，确认无问题）

| 判定项 | grep 模式 | 扫描范围 | 结果 |
|---|---|---|---|
| 移除 API | `sun\.misc\|com\.sun\.\|javax\.xml\.bind\|javax\.activation\|javax\.jws\|org\.omg\|nashorn\|SecurityManager\|runFinalizersOnExit\|protected void finalize` | 全仓 `*.java` | **仅 ObjectValidator:11 命中**（L1）。无 JAXB / activation / jws / omg / nashorn / SecurityManager / finalize 覆写 |
| 脚本引擎查找 | `getEngineByName\|ScriptEngineManager` | 全仓 `*.java` | **0 命中**，无 Nashorn 依赖 |
| Thread 危险方法 | `\.stop\(\)\|\.suspend\(\)\|\.resume\(\)\|void finalize\(` | 全仓 `*.java` | 15 命中全为 Stopwatch / EmbedServer / RetransmissionHandler 等**自有对象**，无 Thread.stop/suspend/resume |
| Groovy 4 包名 | `org\.codehaus\.groovy` | 全仓 `*.java` | **0 命中**。仅 `groovy.lang.Binding`/`GroovyShell`（TrdPlatformCloudServerImpl:12,13,515）、`GroovyClassLoader`（GlueFactory:5,36），Groovy 4 包名未变，**编译安全** |
| 其他脚本引擎 | `jython\|aviator\|ognl\|luaj\|nashorn\|javascript` | 全仓 `*.java` | **0 命中**。仅 `kdla-component-common/pom.xml` 声明 jython 2.7.3 依赖，**无任何使用点** |
| setAccessible 目标 | `setAccessible` | 全仓 `*.java` | 20 命中。非本仓库类仅 `AopTargetHelp` 的 `Proxy.h`（H1，会炸）与 Spring CGLIB/内部类（classpath unnamed module，**可访问**）；其余全部作用于用户 DTO 字段，安全 |
| joda 纯日期 pattern | jshell 实测 | `DateHelp` | `DateTimeFormat.forPattern("yyyy-MM-dd").parseLocalDateTime("2024-01-01")` → **成功返回 2024-01-01T00:00**。故删除 `containsTimeField` 判定**不构成 bug** |
| jakarta.annotation 内容 | jar 内容核实 | `~/.m2` | `jakarta.annotation-api` **2.1.1 与 3.0.0 均含 Nonnull.class / Nullable.class** → `MqttClientConfig` 的 javax→jakarta 迁移编译安全 |
| reactor 模块 | 逐文件 diff | `kdla-component-reactor/src/main/java`（8 文件） | 改动**全部是 javax.validation → jakarta.validation + 日志占位符改进**，零逻辑变更。pom 有 `spring-boot-starter-validation` 提供 jakarta.validation；`org.apache.commons.lang.`（2.x）grep **0 命中** → 删 commons-lang 依赖安全。**reactor 模块零问题** |
| validator 删 slf4j 依赖 | `org\.slf4j` | `kdla-component-validator/src/main/java` | **0 命中** → 删 slf4j-api provided 依赖**不影响编译** |
| fluent-validator 版本 | 根 pom 核查 | `pom.xml:528-529` | 根 pom `dependencyManagement` **确实管理** `fluent-validator-spring` 版本（`${fluent-validator-spring.version}` = 1.0.9）→ validator pom 去掉 `<version>` **安全**，不会报 version missing |
| xxl-job 内容变更 | `git diff -M` + 过滤 import/package | `kdla-component-supplement-job/src/main/java`（44 文件） | 除新增 `DateUtil.java` 外，**全部改动只有 package/import 行**，无逻辑变更 |
| MQTT refCnt | 逐路径引用计数核算 | `MqttPingHandler` / `MqttChannelHandler` | `retain(+1)` 与手动 `release` + autoRelease **恰好配对**，QoS0/1/2 稳态平衡，**不是双重释放也不是 use-after-free** |
| KdlaExcelWriteHelp charset | 逐行 diff | `excel/exp/KdlaExcelWriteHelp.java` | `URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())`、`response.setCharacterEncoding(StandardCharsets.UTF_8.name())` → **JEP 400 安全，正向改进** |
| PageConvertor 泛型 | diff | `dal/.../convertor/PageConvertor.java` | raw type → `<T> Page<T> toPage(IPage<T>)`，**正向改进** |
| MpBaseDo 删除 | `MpBaseDo` | 全仓 `*.java` | **0 引用**，删除不影响仓库内编译（对外 API 移除需文档化） |

---

## 无法判定（标注原因，不猜）

**U1. job-application / trdplatformcloud 的实际编译结果**
取决于 parent `1.0.0-SNAPSHOT` 能否解析及其 `spring.boot.version`/`java.version`。已确认本地**无 pom 实体、只有失败标记**，远程被 http-blocker 拦截 → 当前状态下 C1 先炸，C11/H40 的 javax 问题**根本没机会暴露**。若旧 parent 意外可解析且指向 Boot 2.7，则 javax 代码编译可过但 artifact 版本错为 1.0.0。**需先修 C1 再复验**。

**U2. supplement-job 的 `javax.annotation.Resource` 编译来源**
`kdla-component-supplement-job/pom.xml` **未声明** javax.annotation-api。是否靠传递依赖（如官方 xxl-job-core 或 spring-boot-starter）编译通过未确认。provided 依赖不传递，需修 C1 后看实际 classpath。

**U3. `KdlaStringHelp.matches(url, null)` 行为**
`KdlaBodyReaderRequestFilter.handleExcludeURL` 删掉 `excludes == null || excludes.isEmpty()` 守卫后，excludes 未配置时会以 null 调用该方法。未读 `KdlaStringHelp.matches` 实现，无法判定是 NPE 还是返回 false。**需补读该方法**。

**U4. 下游是否 component-scan `top.kdla.framework.supplement` 包**
决定 C3（自动配置全丢）的实际影响面：若下游已显式扫描该包，配置类仍会注册，C3 降为 Medium；若仅依赖构件自动装配，则 C3 为 Critical。**本仓库无法判定，需下游确认**。

**U5. fluent-validator 1.0.9 在 JDK 21 + jakarta 下的兼容性**
`ObjectValidator.java:6,7,36` 确实 `extends ValidatorHandler<T> implements Validator<T>`（`com.baidu.unbiz.fluentvalidator`）。该库 1.0.9 基于 `javax.validation` + 旧 hibernate-validator，而 validator pom **排除了 hibernate-validator** 且代码已迁 jakarta。根 pom:103 自带注释"该库较旧，未明确支持 JDK 21，需测试"。jar 在 `~/.m2` 存在（fluent-validator / -jsr303 / -spring / -parent 均 1.0.9），但**未反编译核实其内部 javax 引用与字节码版本**，无法判定是编译期不兼容还是运行期 NoClassDefFoundError。**需 javap 或实际编译验证**。

**U6. 全部编译级判定均为静态推断**
遵守禁令**未运行 mvn**。C1/C11/H20-H24/H39/H40 的"编译失败"结论基于源码与 pom 静态分析，需主 agent 编译结果交叉验证。

---

## 按模块汇总

| 模块 | Critical | High | Medium | Low | 合计 |
|---|---|---|---|---|---|
| kdla-component-supplement | C2 C3 C4 C5 C6 C7 | H7 H8 H9 H10 H11 H12 H13 H37 H42 | M4 M5 M8 M11 | L8 L9 | **24** |
| kdla-component-log | C9 C10 | H25 H26 H27 H28 H29 H30 | M9 M10 | L10 | **12** |
| kdla-component-common | — | H1 H3 H4 H5 H6 H31 H33 H34 H35 H36 H38 | M1 M6 M12 M13 | L4 L5 L6 | **18** |
| kdla-component-validator | — | H2 H20 H21 H22 H23 H24 | — | L1 L2 | **8** |
| kdla-component-dal | C8 | H14 H15 H17 H18 | — | — | **5** |
| kdla-component-supplement-job | — | H39 H40 | — | L3 | **3** |
| kdla-component-supplement-trdplatformcloud | — | H40 H41 H43 | M1 | — | **4** |
| kdla-component-supplement-job-application | C1 C11 | — | — | — | **2** |
| kdla-component-domain | — | H16 H19 | — | — | **2** |
| kdla-component-exception | — | — | M2 | — | **1** |
| kdla-component-supplement-cache | — | H32 | — | — | **1** |
| kdla-component-supplement-monitor | — | — | M7 | L7 | **2** |
| kdla-component-model | — | — | M3 | — | **1** |
| kdla-component-reactor | — | — | — | — | **0** |
| kdla-component-supplement-dingding | — | — | — | — | **0** |
| **合计** | **11** | **31** | **13** | **10** | **65** |

注：C1 同时归属 job-application 与 trdplatformcloud（两个 pom），表中只计一次于 job-application；H40 跨 supplement-job 与 trdplatformcloud，两处各计一次。M1（charset）为跨模块清单，计入 common 与 trdplatformcloud。

---

## 修复优先级建议

1. **先修 C1**（两行 pom 版本号）——否则任何编译验证都无法进行，其余结论全部悬空。
2. **再修 C2 / C9 / C10**——三处静默数据丢失/损坏，正常路径即触发，不需要特殊条件。
3. **补 C3 的 AutoConfiguration.imports**——否则 supplement 整个模块对下游等于不存在。
4. **H1 / H2 / H36**——真正的 JDK 21 语义问题（强封装 + SecureRandom 确定性），其中 H36 涉及**存量数据不可逆解密失败**，需在发版前确认是否有历史密文。
5. **H26 / H27 / H41 / H12**——安全类（脱敏失效、异常信息外泄、JWT 不验签、trustAll），按用户规则"永不简化掉安全措施"，不可延后。
6. 其余 High 按模块批量恢复 1.0.1 实现——**多数问题的最短 diff 就是 `git checkout 1.0.1-SNAPSHOT -- <file>` 后重做 javax→jakarta 迁移**，比逐条手写修复更小更可靠。
</task_result>