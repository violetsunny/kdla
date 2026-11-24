# kdla 网络/CORS/TLS/日志专项安全审计报告（D-network）

- 审计对象：`top.kdla.framework:kdla-components`，分支 `2.0.0-SNAPSHOT`（JDK 21 / Spring Boot 3.4.0）
- 审计范围：`kdla-component-supplement`（cors/http/netty/mqtt）、`kdla-component-log`、`kdla-component-supplement-dingding`
- 审计类别：OWASP A05 Security Misconfiguration / A09 Logging Failures / A10 SSRF
- 审计方式：只读源码审读 + 本地 `~/.m2` 依赖源码核实（hutool 5.8.36、netty-handler 4.1.115.Final、vertx-core 4.5.7 的 sources.jar），未改任何源码/pom，未跑 mvn/git
- 框架上下文分类：(a) 框架真实漏洞 (b) 下游预期可覆盖的不安全默认 (c) 下游责任

## 结论速览

| 严重度 | 数量 | 编号 |
|---|---|---|
| Critical | 0 | — |
| High | 5 | D-01 ~ D-05 |
| Medium | 6 | D-06 ~ D-11 |
| Low | 4 | D-12 ~ D-15 |

已知开放项核实结果：
- **E-03 属实**：`CustomCorsConfig` 硬编码 `setAllowCredentials(true)` + `setAllowedOriginPatterns(ALL)`，无白名单配置项（D-02）。属 (b) 类不安全默认：opt-in 开关 + `@ConditionalOnMissingBean(CorsFilter.class)` 下游可覆盖，但一旦启用即构成真实 CORS 漏洞。
- **E-02 部分属实**：`blade-auth` 已在黑名单，但仍遗漏 `set-cookie`/`proxy-authenticate`/`x-api-key`/`secret`/`credential` 等，且精确匹配不覆盖 `x-access-token` 等变体（D-09）；更大的缺口是请求体/响应体完全没有字段级脱敏（D-10）。
- **前序 SSL 结论核实**：`SslContextFactory` 确无 trust-all（真实 keystore/truststore）；Vertx `trust-all` 默认 false 且 `verifyHost` 默认 true（vertx-core 4.5.7 源码核实）。**但前序结论不完整**：Netty 两套 TLS 路径（NettyHttpClient、SslContextFactory.forClient→MQTT）均缺 hostname 验证（D-04/D-05）；`KdlaHttpClient` 继承 hutool 默认 trust-all + 任意域名（D-01，前序未覆盖）。

---

## High

### D-01 KdlaHttpClient 继承 hutool 默认 trust-all + 任意域名校验，全部 HTTPS 可被中间人攻击

- 严重度：**High**
- 分类：(a) 框架真实漏洞（框架以自有 API 名义暴露，未覆盖底层不安全默认，且无配置项可关）
- 证据：`kdla-component-supplement/src/main/java/top/kdla/framework/supplement/http/KdlaHttpClient.java:19-27`

```java
public class KdlaHttpClient extends HttpUtil {

    public String send(String method, String url, Map<String, String> header, Object req) {
        return createRequest(Method.valueOf(method.toUpperCase(Locale.ROOT)), url)
                .headerMap(header, true)
                .form(JSONUtil.toBean(JSONUtil.toJsonStr(req), new TypeReference<Map<String, Object>>() {}, false))
                .execute()
                .body();
    }
}
```

`createRequest` 未设置任何 SSL 参数。hutool 5.8.36 源码核实（`~/.m2/.../hutool-all-5.8.36-sources.jar`）：
- `cn/hutool/http/HttpConnection.java:279-283`：未显式设置时回退 `DefaultSSLInfo.TRUST_ANY_HOSTNAME_VERIFIER` 与 `DefaultSSLInfo.DEFAULT_SSF`；
- `cn/hutool/http/HttpGlobalConfig.java:36`：`private static boolean trustAnyHost = true;`
- `cn/hutool/http/ssl/TrustAnyHostnameVerifier.java`：`verify(...)` 恒返回 `true`；
- `cn/hutool/core/net/SSLContextBuilder.java`：`trustManagers = {DefaultTrustManager.INSTANCE}`，javadoc 明示"默认信任全部"；`DefaultTrustManager.checkServerTrusted` 全部空实现。

- 攻击场景：下游用 `KdlaHttpClient.send("POST", "https://pay.internal.example/...", ...)` 传敏感数据。网络路径上的攻击者（同网段 ARP 欺骗、恶意 Wi-Fi、被入侵的中间设备）出示任意自签证书即可完整解密/篡改请求与响应，包括 header 里的 token。
- exploitability：高。无需任何配置即生效；只要下游经此客户端走 HTTPS 且网络路径不可信即可利用。与 Vertx 路径不同，此处**没有** `kdla.http.ssl.trust-all` 之类开关可修正。
- 修复方案：在 `send()` 内对 https 请求显式 `.setSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault())` + `.setHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier())`（恢复 JDK 默认信任链与域名校验）；或提供与 Vertx 一致的 `kdla.http.ssl.trust-all` 开关（默认 false）。
- 破坏性：**破坏性**。开启证书/域名校验后，依赖自签证书或证书域名不匹配的下游连接会失败，需裁定并给迁移开关。

### D-02 CustomCorsConfig 硬编码 allowCredentials=true + allowedOriginPatterns=ALL，无 origin 白名单（E-03 核实）

- 严重度：**High**（启用 `kdla.config.cors.enabled=true` 时）
- 分类：(b) 下游可覆盖的不安全默认，但启用即真实漏洞
- 证据：`kdla-component-supplement/src/main/java/top/kdla/framework/supplement/cors/CustomCorsConfig.java:18,24-38`

```java
@ConditionalOnProperty(name = "kdla.config.cors.enabled", havingValue = "true", matchIfMissing = false)
public class CustomCorsConfig {
    @Bean
    @ConditionalOnMissingBean({CorsFilter.class})
    public CorsFilter unifyAuthCorsFilter() { ... }

    private CorsConfiguration buildConfig() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedOriginPatterns(Collections.singletonList(CorsConfiguration.ALL));
        corsConfiguration.addAllowedHeader(CorsConfiguration.ALL);
        corsConfiguration.addAllowedMethod(CorsConfiguration.ALL);
        return corsConfiguration;
    }
}
```

已注册自动装配：`kdla-component-supplement/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 第 1 行。

- 攻击场景：`allowedOriginPatterns("*")` + `allowCredentials(true)` 时 Spring 反射请求 Origin 并回 `Access-Control-Allow-Credentials: true`（绕开浏览器对字面量 `*`+credentials 的拦截）。受害者登录下游应用后访问 `evil.com`，页面 JS 向下游 API 发起带 Cookie 的跨域请求并**读取响应**——会话数据批量窃取、以受害者身份执行任意 API（等效 CSRF 但可读返回）。
- exploitability：条件性高。默认关闭（`matchIfMissing=false`）；下游开开关且未自定义 `CorsFilter` Bean 即中招。`@ConditionalOnMissingBean(CorsFilter.class)` 允许下游覆盖，`HttpContextHelp.buildCors` 的 javadoc（HttpContextHelp.java:97-101）已明示此退让路径——但框架未提供白名单配置项，"直接启用"与"安全"不可兼得。
- 修复方案：新增 `kdla.config.cors.allowed-origins` 配置项（默认空=拒绝跨域或仅同源），`allowCredentials` 随之可配；未配置白名单时禁止 `ALL` pattern 与 credentials 组合。
- 破坏性：**破坏性**。改默认 CORS 行为会影响已启用该开关并依赖任意域跨域的下游，需裁定（建议默认拒绝 + 显式白名单）。

### D-03 HttpContextHelp.ignoreSSL() 一次调用污染 JVM 全局 SSL 默认（trust-all + 恒真 HostnameVerifier）

- 严重度：**High**（被调用时）
- 分类：(a)/(b) 之间——框架保留的危险兼容 API，已 `@Deprecated` + 运行时 warn，但仍公开可用
- 证据：`kdla-component-supplement/src/main/java/top/kdla/framework/supplement/http/HttpContextHelp.java:35-45,80-90`

```java
private static void trustAllHttpsCertificates() throws Exception {
    ...
    TrustManager tm = new miTM();          // checkServerTrusted 空实现
    ...
    SSLContext sc = SSLContext.getInstance("TLS");
    sc.init(null, trustAllCerts, null);
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
}
...
@Deprecated
public static void ignoreSSL() throws Exception {
    HostnameVerifier hv = (urlHostName, session) -> { ...; return true; };
    trustAllHttpsCertificates();
    HttpsURLConnection.setDefaultHostnameVerifier(hv);
    ...
}
```

- 攻击场景：下游任一代码（含第三方库初始化路径）调用 `ignoreSSL()` 后，宿主进程内**所有**走 `HttpsURLConnection` 默认工厂的 HTTPS 连接（不限于 kdla 客户端）不再校验证书与域名，全局可被 MITM。
- exploitability：条件性。需下游显式调用；已 Deprecated 且调用即打 warn（前序评审所加），但 API 仍公开、无开关拦截，存量下游调用无法被框架阻止。
- 修复方案：下一 major 版本删除；当前可在方法内改为抛 `UnsupportedOperationException` 或加 `kdla.http.allow-global-ssl-override` 门禁（默认拒绝）。
- 破坏性：**破坏性**（删除/禁用影响仍在调用的下游）。

### D-04 NettyHttpClient TLS 缺 hostname 验证（自定义 truststore 与 JDK 默认信任链两条路径均缺）

- 严重度：**High**
- 分类：(a) 框架真实漏洞
- 证据：`kdla-component-supplement/src/main/java/top/kdla/framework/supplement/netty/NettyHttpClient.java:228-236`

```java
SSLEngine sslEngine;
if (client != null && clienttruststore != null && keyStorePassword != null) {
    sslEngine = SslContextFactory.getClientContext(client, clienttruststore, keyStorePassword).createSSLEngine();
} else {
    //https未提供自定义keystore时走JDK默认信任链；传null给SslContextFactory必NPE
    sslEngine = javax.net.ssl.SSLContext.getDefault().createSSLEngine();
}
sslEngine.setUseClientMode(true);
ch.pipeline().addFirst("ssl", new SslHandler(sslEngine));
```

`createSSLEngine()` 无 peer host/port，且未设 `SSLParameters.setEndpointIdentificationAlgorithm("HTTPS")`。JDK SSLEngine 默认不做端点识别：证书链有效即通过，**证书域名与目标 host 不匹配也接受**。

- 攻击场景：下游连 `https://api.partner.com`，攻击者持任意公共 CA 签发的合法证书（如自己域名 `attacker.com` 的证书）做 MITM，证书链校验通过、域名不校验 → 完整解密篡改。truststore 配置得越认真，越容易误以为安全。
- exploitability：高（使用 NettyHttpClient 走 https 时）。无需 trust-all，任意 CA 合法证书即可。
- 修复方案：`createSSLEngine(peerHost, peerPort)` 并设 `endpointIdentificationAlgorithm("HTTPS")`；或改走 `SslContextBuilder.forClient().endpointIdentificationAlgorithm("HTTPS")`。peer host 可取构造器 URL 的 host。
- 破坏性：**破坏性**。证书域名不匹配的存量下游连接会开始失败（这正是安全目的），需裁定。

### D-05 SslContextFactory.forClient 与 Netty MQTT TLS 路径缺 hostname 验证

- 严重度：**High**（启用 TLS 时）
- 分类：(a) 框架真实漏洞
- 证据 1：`kdla-component-supplement/src/main/java/top/kdla/framework/supplement/http/ssl/SslContextFactory.java:58-60`

```java
public static SslContext toNettySslContext(SSLContext javaxSslContext, boolean isClient) {
    return new JdkSslContext(javaxSslContext, isClient, isClient ? ClientAuth.NONE : ClientAuth.REQUIRE);
}
```

证据 2：`kdla-component-supplement/src/main/java/top/kdla/framework/supplement/mqtt/netty/MqttClientImpl.java:591-593`

```java
if (sslContext != null) {
    ch.pipeline().addLast(sslContext.newHandler(ch.alloc(), host, port));
}
```

netty-handler 4.1.115.Final 源码核实：`SslContextBuilder.java:212` `endpointIdentificationAlgorithm` 默认 null；`SslContext.newHandler` javadoc 明示 "the underlying SSLEngine won't have hostname verification enabled by default"，需调用方显式设 `setEndpointIdentificationAlgorithm("HTTPS")`。`JdkSslContext` 三参构造（此处使用的 deprecated 构造）传入的 endpointIdentificationAlgorithm 为 null。因此即使 `newHandler(alloc, host, port)` 带了 peer 信息，**域名校验仍不生效**。

- 攻击场景：下游用 `SslContextFactory.forClient(...)` 构造 `MqttClientConfig(sslContext)` 连 MQTTS broker（8883）。攻击者以任意 CA 合法证书 MITM，窃取 MQTT CONNECT 中的 username/password（`MqttChannelHandler.java:99-106` 明文装载）与全部设备消息。
- exploitability：中高。需下游启用 TLS（`MqttClientConfig` 默认 sslContext=null 走明文，见 D-07）；一旦启用，框架推荐的 `forClient` 产物即缺域名校验。
- 修复方案：`toNettySslContext` 客户端分支改用带 `endpointIdentificationAlgorithm("HTTPS")` 的 JdkSslContext 构造；或在 `MqttClientImpl.initChannel` 对 `sslHandler.engine()` 补设 SSLParameters。
- 破坏性：**破坏性**（同 D-04，证书域名不匹配的连接将失败）。

---

## Medium

### D-06 HttpContextHelp.buildCors 反射任意 Origin + Allow-Credentials:true，且无 Vary: Origin

- 严重度：**Medium**（已 Deprecated + warn，需下游显式调用）
- 分类：(b) 保留的危险兼容 API
- 证据：`kdla-component-supplement/src/main/java/top/kdla/framework/supplement/http/HttpContextHelp.java:104-119`

```java
@Deprecated
public static void buildCors(HttpServletRequest request, HttpServletResponse response) {
    String origin = request.getHeader("Origin");
    if (StringUtils.isBlank(origin)) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Credentials", "false");
    } else {
        ...
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }
    ...
}
```

- 攻击场景：与 D-02 相同——任意站点带凭据跨域读取响应。另未设 `Vary: Origin`，共享缓存（CDN/代理）可能把带 `ACAO: evil.com` 的响应缓存给其他 Origin 的请求，放大攻击面。
- exploitability：条件性。需下游在过滤器/拦截器里显式调用；javadoc 已警告并指向自定义 CorsFilter 白名单路径。
- 修复方案：删除或改为白名单驱动；若保留，至少补 `Vary: Origin` 并默认拒绝非白名单 Origin。
- 破坏性：**破坏性**（改变/移除下游在用的跨域行为）。

### D-07 Vertx MQTT 默认明文传输，无 TLS 配置项，"-1" 哨兵值被当作真实凭据发送

- 严重度：**Medium**
- 分类：(b) 不安全默认（`@ConditionalOnMissingBean(MqttClient.class)` 下游可覆盖，但框架未提供任何 TLS 开关）
- 证据：`kdla-component-supplement/src/main/java/top/kdla/framework/supplement/mqtt/vertx/VertxMqttConfigure.java:43-53,83-88`

```java
@Value("${kdla.mqtt.username:-1}")
private String username;
@Value("${kdla.mqtt.password:-1}")
private String password;
...
@Value("${kdla.mqtt.port:1883}")
private int port;
...
MqttClient mqttClient = MqttClient.create(vertx, new MqttClientOptions()
        .setClientId(clientId)
        .setUsername(username)
        .setPassword(password)
        .setAckTimeout(acktimeout)
        .setAutoKeepAlive(true));
```

`MqttClientOptions` 未设 `setSsl(true)`，整个配置类无任何 TLS 属性。Netty 侧同理：`MqttClientConfig.java:49-51` 默认 `sslContext=null`，`MqttClientImpl.connect(host)` 默认 1883 明文。

- 攻击场景：① 同网段攻击者嗅探 1883 端口即得 MQTT 用户名/密码与全部设备消息（IoT 场景消息常含设备控制指令）；② 未配置凭据时客户端把字面量 `"-1"/"-1"` 当用户名密码发给 broker——若 broker 侧恰有弱口令/测试账号匹配即未授权接入，且该行为违背"未配置=不带凭据"的直觉。
- exploitability：中。明文是 MQTT 行业常态默认，但框架连 TLS 开关都不给，下游要 TLS 必须整 Bean 覆盖；`"-1"` 哨兵泄漏是确定性行为。
- 修复方案：新增 `kdla.mqtt.ssl`（默认 false 保持兼容）+ truststore/hostname 校验选项；username/password 默认改空串并在为空时不 set（或 null），消除 `"-1"` 实发。
- 破坏性：加 TLS 配置项与修 `"-1"` 哨兵为**非破坏性**（默认行为不变，`"-1"` 实发本身无人可合理依赖）；若把默认端口/协议改 TLS 则为破坏性，不建议。

### D-08 NettyHttpServer 加载 truststore 却不强制客户端证书（mTLS 静默失效）

- 严重度：**Medium**
- 分类：(a) 框架实现与 API 语义不符
- 证据：`kdla-component-supplement/src/main/java/top/kdla/framework/supplement/netty/NettyHttpServer.java:156-158`

```java
SSLEngine sslEngine = SslContextFactory.getServerContext(server, servertruststore, keyStorePassword).createSSLEngine();
sslEngine.setUseClientMode(false);
ch.pipeline().addFirst("ssl", new SslHandler(sslEngine));
```

对比 `SslContextFactory.java:54-60`：`forServer` 经 `toNettySslContext(..., false)` 设 `ClientAuth.REQUIRE`，但 NettyHttpServer 绕过 `forServer` 直接用裸 `SSLContext.createSSLEngine()`——SSLEngine 默认 `needClientAuth=false`/`wantClientAuth=false`，truststore 加载后从未用于校验客户端证书。`SslContextFactory.getServerContext` javadoc（SslContextFactory.java:27）明示 truststore 用于"验证客户端证书"。

- 攻击场景：下游按 javadoc 语义配置 `setSsl(server, servertruststore, pwd)` 期望 mTLS 保护内部服务，实际任何客户端无需证书即可连接调用 `DispatcherServer` 暴露的全部接口。
- exploitability：中。仅影响依赖该 API 做 mTLS 的下游；属于"安全预期静默落空"，比显式拒绝更危险。
- 修复方案：改用 `SslContextFactory.forServer(...).newHandler(ch.alloc())`（自带 REQUIRE），或对 engine 显式 `setNeedClientAuth(true)`。
- 破坏性：**破坏性**。无证书的存量客户端会被拒连（这正是 mTLS 语义），需裁定。

### D-09 LogSanitizeUtil 敏感头黑名单遗漏（E-02 扩展核实）

- 严重度：**Medium**
- 分类：(a) 框架脱敏不完整
- 证据：`kdla-component-log/src/main/java/top/kdla/framework/log/sanitize/LogSanitizeUtil.java:27-28`

```java
private static final Set<String> SENSITIVE_HEADER_NAMES = new HashSet<>(Arrays.asList(
        "authorization", "cookie", "password", "token", "mobile", "blade-auth"));
```

`maskHeader`（:50-55）为**精确匹配**（小写化后 contains）。

遗漏清单（核实存在缺口）：
- `set-cookie`（响应侧会话凭据；NettyHttpClient 会原样打印，见 D-11）
- `proxy-authenticate`、`proxy-authorization`
- `x-api-key`、`api-key`、`apikey`
- `secret`、`credential`、`credentials`
- `access-token`、`refresh-token`、`x-auth-token`、`x-access-token`（精确匹配下 `token` 不命中这些变体）
- `session-id`、`sessionid`、`jsessionid`
- `x-csrf-token`、`x-xsrf-token`

- 攻击场景：下游网关传 `X-Api-Key`/`X-Access-Token` 自定义鉴权头，RequestLogAspect（RequestLogAspect.java:165-174）逐头打印时原样落盘；能读日志者（运维外包、日志平台越权、日志文件泄漏）直接拿到长期凭据。
- exploitability：中。取决于下游实际使用的头名；`authorization`/`cookie`/`blade-auth` 主路径已覆盖。
- 修复方案：黑名单扩充上述条目；匹配策略改为后缀/包含匹配（如 endsWith("-token")、contains("secret")）以覆盖变体，注意控制误杀。
- 破坏性：**非破坏性**（仅日志输出内容变化，安全内部修复）。

### D-10 请求体/请求参数/响应体无字段级脱敏，CatchLogAspect 生产默认开启

- 严重度：**Medium**
- 分类：(a) 框架脱敏缺口（header 有脱敏，body 只有截断）
- 证据 1（生产默认开启）：`kdla-component-log/src/main/java/top/kdla/framework/log/catchlog/CatchLogAspect.java:129-131`

```java
if (log.isInfoEnabled()) {
    log.info("### {} Invoke Finished, requestParam: {}, response: {}, shortSummary:{}", clazzMethodInfo, LogSanitizeUtil.truncate(JSON.toJSONString(args)), LogSanitizeUtil.truncate(JSON.toJSONString(response)), sw.shortSummary());
}
```

`CatchLogAutoConfigure.java:19-20`：`matchIfMissing = true`，无 `@Profile` 限制 → **生产默认生效**。`truncate` 只截 2KB，不做任何字段掩码。

证据 2：`kdla-component-log/src/main/java/top/kdla/framework/log/requestlog/RequestLogAspect.java:98-99,161-162,193`

```java
if (requestBody != null && value != null) {
    paraMap.putAll(BeanUtil.beanToMap(value));
}
...
beforeReqArgs.add(JacksonUtil.toJson(paraMap));
...
afterReqArgs.add(LogSanitizeUtil.truncate(JacksonUtil.toJson(result)));
```

`@RequestBody` 全字段进 paraMap 后 INFO 打印；响应同样只截断不脱敏。缓解因素：`RequestLogAutoConfigure.java:20` `@Profile({"dev","test"})`，生产不装配。

证据 3：`kdla-component-log/src/main/java/top/kdla/framework/log/LogInterceptor.java:37`

```java
log.info("### uri：{}, requestParam: {}, requestBody:{} ", request.getRequestURI(), JacksonUtil.toJson(request.getParameterMap()), body == null ? "<body未缓存>" : LogSanitizeUtil.truncate(body));
```

opt-in（`kdla.config.log-interceptor.enabled`，matchIfMissing=false）；且 Spring Boot 不会自动把 `HandlerInterceptor` Bean 注册进 MVC 拦截链，需下游 `WebMvcConfigurer#addInterceptors` 显式注册才生效。

- 攻击场景：下游登录/改密服务标注 `@CatchAndLog`（框架惯例），`password` 字段、登录响应里的 `token` 全量进 INFO 日志。日志归集平台（ELK/云日志）的宽读权限人群即凭据泄漏面；2KB 截断对短小的登录报文毫无作用。
- exploitability：中高。CatchLogAspect 默认开、INFO 级生产常开；只需下游存在处理敏感字段的被注解方法。
- 修复方案：`LogSanitizeUtil` 增加 JSON 字段级掩码（复用 D-09 扩充后的敏感名集合，对 `password/token/secret/mobile/idCard` 等键值打 `***`），在 CatchLogAspect:130、RequestLogAspect:162/193、LogInterceptor:37 统一套用。
- 破坏性：**非破坏性**（日志内容变化，API/业务行为不变）。

### D-11 HTTP 客户端/服务端 INFO/WARN 全量明文日志（含钉钉 webhook access_token 进 URL 日志）

- 严重度：**Medium**
- 分类：(a) 框架日志卫生缺陷
- 证据（多点，均无脱敏）：

1. `kdla-component-supplement/src/main/java/top/kdla/framework/supplement/http/BaseHttpClient.java:41-43,79-81`

```java
log.info("BaseHttpClient.get:URL:{};params:{}", url, JSON.toJSONString(params));
...
log.info("BaseHttpClient.post URL:{} body:{}", url, JSON.toJSONString(body));
```

INFO 级全量 URL+参数/请求体；:59/:74/:98 错误路径再打印响应体。

2. `kdla-component-supplement/src/main/java/top/kdla/framework/supplement/http/VertxHttpClient.java:117,123,127`

```java
log.debug("VertxHttpClient-send url:{} req:{} result:{}", url, JSON.toJSONString(req), result);
...
log.warn("VertxHttpClient-send url:{} req:{} \n result:{}", url, JSON.toJSONString(req), response.bodyAsString());
```

WARN 路径（响应解析失败即触发，生产可达）打印完整 URL+请求+响应。具体凭据泄漏链：`kdla-component-supplement-dingding/.../config/DingAlertConfigure.java:33-37`

```java
private String baseDingUrl = "https://oapi.dingtalk.com/robot/send?access_token=%s";
public String getDingUrl() {
    return String.format(baseDingUrl, dingAlertProperties.getAccessToken());
}
```

钉钉机器人 `access_token` 在 URL query 里，经 `DingDingServiceImpl.java:54,86,128,160` 传入 VertxHttpClient——响应解析一旦失败，**webhook token 随 URL 进 WARN 日志**，拿到 token 者可冒用机器人向企业群发任意消息（钓鱼）。

3. `kdla-component-supplement/src/main/java/top/kdla/framework/supplement/netty/NettyHttpClient.java:354-361,369-371`（Promise/Async 三个 handler 同构：:393-400,:408-410,:439-446,:454-456）

```java
for (String name : msg.headers().names()) {
    for (String value : msg.headers().getAll(name)) {
        if (log.isInfoEnabled()) {
            log.info("HEADER: {} = {}", name, value);
        }
    }
}
...
log.info("content:{}", buf.toString(CharsetUtil.UTF_8));
```

INFO 级打印**全部响应头**（含 `Set-Cookie` 会话凭据）与完整响应体，完全绕过 LogSanitizeUtil。

4. `kdla-component-supplement/src/main/java/top/kdla/framework/supplement/netty/NettyHttpServer.java:182-185,190-192`

```java
log.info("客服端地址:{}", ctx.channel().remoteAddress());
log.info("请求数据:{}", JSONObject.toJSONString(msg));
...
log.info("Uri:{} method:{} headValue:{}", uri, method, headers.toString());
```

INFO 级序列化整个 FullHttpRequest（含 Authorization/Cookie 头与 body）。

- 攻击场景：日志读取权限即凭据读取权限；Set-Cookie/access_token/请求体密码三类凭据均可从日志直接回收。
- exploitability：高（INFO 生产常开；Netty 客户端/服务端无条件打印；钉钉 token 链确定存在）。
- 修复方案：统一接入 `LogSanitizeUtil.maskHeader`（头）与 D-10 字段掩码（体）；URL 日志对 query 中 `access_token/token/key` 类参数打码；Netty 响应头打印改 DEBUG 或删减为白名单头。
- 破坏性：**非破坏性**（仅日志输出变化）。

---

## Low

### D-12 请求头 traceId 未校验直接进 MDC，可注入伪造日志行

- 严重度：**Low**
- 分类：(a) A09 日志注入
- 证据：`kdla-component-log/src/main/java/top/kdla/framework/log/webfilter/KdlaBodyReaderRequestFilter.java:46-54`

```java
if (!StringUtils.hasText(MDC.get(LOG_TRACE_ID))) {
    String traceId = request.getHeader(LOG_TRACE_ID);   // LOG_TRACE_ID = "traceId"（CommonConstants.java:13）
    if (StringUtils.hasText(traceId)) {
        MDC.put(LOG_TRACE_ID, traceId);
    } else {
        MDC.put(LOG_TRACE_ID, SelfSnowflakeGeneratorHelp.generate());
    }
}
```

- 攻击场景：攻击者发送 `traceId: abc%0d%0a2026-11-24 INFO ...伪造日志`（头值含 CRLF），若日志 pattern 输出 MDC traceId 且编码器未转义换行，可在日志中伪造任意行，干扰审计溯源/掩盖攻击痕迹；超长值亦可刷爆日志。
- exploitability：低。需 web-filter 开关开启（opt-in）+ 日志 pattern 含 traceId + 编码器未转义；多数现代编码器会转义。
- 修复方案：对外来 traceId 做格式白名单（如 `[A-Za-z0-9-]{1,64}`），不合法即自生成。
- 破坏性：**非破坏性**。

### D-13 钉钉告警失败日志打印 atMobiles 手机号与告警正文

- 严重度：**Low**
- 分类：(a) PII 进日志
- 证据：`kdla-component-supplement-dingding/src/main/java/top/kdla/framework/supplement/dingding/service/impl/DingDingServiceImpl.java:92,166`

```java
log.info("DingDingServiceImpl.sendMessage failed#msg:{},atMobiles:{}, rest:{}", msg, atMobiles, JSON.toJSONString(result));
...
log.info("DingDingServiceImpl.sendMessage failed#title:{}, msg:{},atMobiles:{}, rest:{}", title, msg, atMobiles, JSON.toJSONString(result));
```

（:60/:134 同构打印 msg 正文。）

- 攻击场景：`atMobiles` 为员工手机号（PII），告警 msg 常含异常上下文/内部信息；失败路径 INFO 落盘后随日志平台扩散。框架在 header 侧把 `mobile` 列为敏感（LogSanitizeUtil.java:28），此处却明文打印，策略不一致。
- exploitability：低。仅失败路径，且内容为下游传入的告警数据。
- 修复方案：atMobiles 打码（保留前 3 后 4）；msg 截断。
- 破坏性：**非破坏性**。

### D-14 全部 HTTP 客户端接受任意 URL，无内网防护；Vertx/HttpClient5 默认跟随重定向（SSRF，下游责任）

- 严重度：**Low**（框架上下文）
- 分类：(c) 下游责任——通用 HTTP 客户端库接受任意 URL 是设计使然；SSRF 成立前提是下游把用户可控输入传进 url
- 证据：
  - `VertxHttpClient.java:78,93,185-193`：`getAbs(url)/postAbs(url)` 等直接接受任意绝对 URL，仅校验中文字符（:169-171），无 scheme/host 白名单、无内网地址防护。
  - `BaseHttpClient.java:44,65,82`、`KdlaHttpClient.java:22`、`ProtoHttpClient.java:56,70`、`NettyHttpClient.java:246-267`（`InetAddress.getByName(host)` 任意解析）同。
  - 重定向默认值（依赖源码核实）：vertx-core 4.5.7 `HttpClientOptions.java:173` `DEFAULT_MAX_REDIRECTS = 16`（VertxHttpClient 未覆盖 → 默认跟随最多 16 跳）；Apache HttpClient5 `HttpClientBuilder.create()` 默认启用重定向策略（RestHttpConfigure.java:93-96 未 disable）；hutool 默认 `maxRedirectCount = 0`、java.net.http 默认 NEVER（这两条路径无重定向放大）。
- 攻击场景：下游若提供"URL 预览/webhook 回调/图片抓取"类功能并通过这些客户端请求用户传入的 URL，攻击者指向 `http://169.254.169.254/`（云元数据）或内网服务；即使下游做了首跳白名单，Vertx/HttpClient5 的默认重定向跟随可经外部 302 跳入内网绕过。
- exploitability：框架本身不可被远程直接利用；风险完全取决于下游用法。列为 Low 提示而非框架漏洞。
- 修复方案（可选增强，非必须）：提供可选的 URL 校验 helper（scheme 白名单 + 解析后 IP 私网/链路本地/元数据地址拒绝）；文档明示"用户可控 URL 必须下游校验"，并提示 Vertx 默认 16 跳重定向。
- 破坏性：**非破坏性**（新增可选工具不改现有默认）。

### D-15 NettyHttpServer 响应缺安全响应头

- 严重度：**Low**
- 分类：(b) 框架服务端默认未加固
- 证据：`kdla-component-supplement/src/main/java/top/kdla/framework/supplement/netty/NettyHttpServer.java:246-249`（json 分支，其余 html/plain/image 分支 :265-278,:289-295,:313-316,:333-336 同构）

```java
FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.wrappedBuffer(res.getBytes(Charsets.UTF_8)));
response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
response.headers().setInt(HttpHeaderNames.EXPIRES, 0);
```

无 `X-Content-Type-Options: nosniff`、`Cache-Control: no-store`（仅 Expires:0）、`X-Frame-Options` 等。html 分支（writeHttpResponseHtml2）把 dispatcher 结果直接以 `text/html` 返回，若下游 dispatcher 回显用户输入则叠加存储型/反射型 XSS 风险（XSS 本身属下游 dispatcher 责任）。

- 攻击场景：MIME 嗅探把 JSON 响应当脚本执行（老旧浏览器）、敏感 JSON 被中间缓存留存、html 分支被嵌入 iframe 点击劫持。
- exploitability：低。该服务器多用于内部/设备侧接口，非浏览器主站。
- 修复方案：统一补 `X-Content-Type-Options: nosniff`、`Cache-Control: no-store`；html 分支补 `X-Frame-Options: DENY`。
- 破坏性：**非破坏性**（新增响应头，极小概率影响依赖嗅探行为的下游，可忽略）。

---

## 核实无问题项（前序结论复核 + 本次扩展检查）

1. **SslContextFactory 无 trust-all**：`getServerContext/getClientContext`（SslContextFactory.java:31-52,68-89）均加载真实 JKS keystore/truststore，`TrustManagerFactory.getDefaultAlgorithm()` 标准校验链，无 InsecureTrustManagerFactory（仅 NettyHttpClient.java:225、NettyHttpServer.java:153 的**注释**里出现，未启用）。
2. **Vertx HTTPS 默认安全**：`VertxHttpConfigure.java:49-50` `kdla.http.ssl.trust-all` 默认 false，开启时打 warn（:84-86）；vertx-core 4.5.7 `HttpClientOptions.DEFAULT_VERIFY_HOST = true`（源码核实），trustAll=false 时证书链+域名双重校验。前序结论成立。
3. **弱协议**：`SslContextFactory.PROTOCOL="TLS"`（:23）在 JDK 21 上默认协商 TLSv1.3/1.2；netty `JdkSslContext.defaultProtocols` 尊重 `context.getDefaultSSLParameters()`（源码核实），TLSv1/1.1 已被 JDK `jdk.tls.disabledAlgorithms` 禁用。无弱协议暴露。
4. **RestHttpConfigure TLS**：`SSLConnectionSocketFactory.getSocketFactory()`（RestHttpConfigure.java:77）走系统默认信任链+默认域名校验；`java.net.http.HttpClient`（:101-106）JDK 默认 TLS。无 trust-all。
5. **DingDingHttpUtil**：`DingDingHttpUtil.java` 全文件 199 行**整体被注释**，无活跃代码，不构成攻击面（实际发送走 VertxHttpClient，见 D-11）。
6. **Netty MQTT 凭据不落日志**：`MqttChannelHandler.java:84-106` CONNECT 装载 username/password 但无日志输出；`MqttClientImpl` 日志仅 host/port/clientId。
7. **RequestLogAspect header 脱敏已生效**：RequestLogAspect.java:170 经 `LogSanitizeUtil.maskHeader`，`blade-auth` 已覆盖（E-02 前序修复属实）。
8. **异常响应不外泄内部细节**：NettyHttpServer.java:200-202,212-214 与 CatchLogAspect.java:182-184 均只回通用文案，堆栈仅进服务端日志。

## 范围外相邻观察（不在本组处置）

- `kdla-component-supplement-job/src/main/java/com/xxl/job/core/util/XxlJobRemotingUtil.java:25-42,79`：内嵌 xxl-job 的 `trustAllHosts` trust-all 实现（上游 xxl-job 已知问题），属 supplement-job 模块，建议另行审计。

---

## 处置分组

### 非破坏性可直接修（安全内部修复，不改对外行为默认）

| 编号 | 修复内容 |
|---|---|
| D-09 | LogSanitizeUtil 黑名单扩充 + 变体匹配策略 |
| D-10 | 增加 JSON 字段级掩码并套用到 CatchLogAspect/RequestLogAspect/LogInterceptor |
| D-11 | 客户端/服务端日志接入 maskHeader/字段掩码；URL query 凭据打码；Netty 响应头日志降级 |
| D-12 | 外来 traceId 格式白名单校验 |
| D-13 | atMobiles 打码、告警 msg 截断 |
| D-14 | （可选）新增 URL 内网防护 helper + 文档提示，不改现有默认 |
| D-15 | NettyHttpServer 补 nosniff/no-store/X-Frame-Options |
| D-07（部分） | 新增 `kdla.mqtt.ssl` 配置项（默认 false）；username/password 空值时不发送，消除 "-1" 实发 |

### 破坏性需裁定（改 CORS/TLS/HTTP 客户端默认行为，影响下游）

| 编号 | 修复内容 | 破坏点 |
|---|---|---|
| D-01 | KdlaHttpClient 恢复 JDK 默认证书/域名校验（或加 trust-all 开关默认 false） | 依赖自签证书的下游 HTTPS 连接失败 |
| D-02 | CustomCorsConfig 增加 origin 白名单配置，禁止 ALL+credentials 组合 | 已启用开关且依赖任意域跨域的下游被拒 |
| D-03 | 删除/禁用 HttpContextHelp.ignoreSSL | 仍在调用的下游编译/运行失败 |
| D-04 | NettyHttpClient 补 hostname 验证 | 证书域名不匹配的下游连接失败 |
| D-05 | SslContextFactory.forClient/MQTT TLS 补 hostname 验证 | 同上 |
| D-06 | 删除/白名单化 HttpContextHelp.buildCors | 在用的下游跨域行为改变 |
| D-07（部分） | 若改 MQTT 默认协议/端口为 TLS | 明文 broker 下游断连（不建议，保持默认 1883） |
| D-08 | NettyHttpServer 强制 needClientAuth（mTLS 语义兑现） | 无证书的存量客户端被拒 |