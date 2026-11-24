# 切片 E：安全横切评审报告

日期：2026-11-23。BASE=`136bf03`，HEAD=`0fa5ab7`（已核实）。分支 `2.0.0-SNAPSHOT`（JDK21 / Boot 3.4.0）。
方法：read 精读 + grep 全仓扫描 + `git diff 136bf03 HEAD` / `git show 1.0.1-SNAPSHOT:<path>` / `git show 136bf03:<path>` 取证。只读，未运行 mvn，未连外部服务。
范围：JwtUtil、AESUtil/Sm4Util/Sm2Util、SslContextFactory/HttpContextHelp/VertxHttpConfigure/CustomCorsConfig/XxlJobRemotingUtil、LogSanitizeUtil+日志切面/过滤器、KRedissonConfigProperties/KRedissonLockFactory、UnifiedExceptionControllerAdvice/CatchLogAspect/NettyHttpServer、charset、SqlFilter、StringUtil。
遵守 COMMON-CONTEXT：C/H36 SHA1PRNG 误报不报；JwtUtil 强制验签为用户裁定破坏性变更不当 bug；LSP 误报忽略；死代码只列不删。

## 一、已验证闭合的安全修复（静态核对通过，无需动作）

1. **JwtUtil fail-closed 闭合**（trdcloud/utils/JwtUtil.java:31-78）：唯一公开方法 `getInfoFromToken` 顺序为 requireSignKey（context=null/密钥 null/空白→抛）→ extractToken（null/格式非法→抛）→ parseToken+setSigner(hs256)（解析异常→抛，不吞）→ `jwt.verify()`（false→抛）→ `jwt.validate(0)`（false→抛）→ 才取 payloads。**验签前无任何 claims 返回路径**。alg=none：signer 显式钉死 HS256（hutool 5.8.36，根 pom:75），verify 用配置 signer 重算 HMAC 与 token 签名比对，none token 空签名必不匹配→拒绝。旧版三处缺陷（只 Base64 解码不验签、header+signature 明文 INFO 日志、解析失败静默返回 ""）全部移除（git diff 实证）。5 个控制器+5 个 VO 调用点均不 catch BizException，异常直达 advice，无吞异常绕过。密钥编码 raw UTF-8 假设已 javadoc 标 not measured（已知裁定，不重复报）。
2. **AESUtil**（common/utils/AESUtil.java:32-95）：`new SecureRandom().nextBytes(iv)` 真随机 IV；`v1:` 前缀 Base64(IV+密文)；旧格式零 IV 兼容分支保留（裁定#6）；两处异常消息仅留长度不留密钥明文（:36,:67）。旧格式兼容不构成降级攻击：剥掉 `v1:` 前缀后字节布局改变（IV+密文 vs 纯密文），零 IV 解密必失败或出垃圾，无法强制得到有意义明文。
3. **Sm4Util**（common/utils/Sm4Util.java:124-152）：ECB→CBC+SecureRandom 随机 IV，`v1:` 格式对齐 AESUtil；旧 ECB 兼容分支注释标明迁移后可移除；`generateSecureKey()` 真随机新增，`generateKey(keySrc)` @Deprecated+低熵风险 javadoc。Sm2Util 同模式（generateSecureKeyPair + @Deprecated 种子派生，Sm2Util.java:68-92），加解密/签名显式 UTF-8（:113,:146,:200,:228）。
4. **SslContextFactory**（supplement/http/ssl/SslContextFactory.java）：**无 trust-all**——KeyManager/TrustManager 均来自调用方 JKS keystore/truststore，标准证书校验；server 侧 ClientAuth.REQUIRE（mTLS）。本轮修复：try-with-resources 关流、`throw new Error`→IllegalStateException（C/M4）。PROTOCOL="TLS" 依赖 JDK21 默认禁用 SSLv3/TLS1.0/1.1（见 E-19）。
5. **VertxHttpConfigure**（supplement/http/VertxHttpConfigure.java:49-50,84-89）：硬编码 `setTrustAll(true)`→`kdla.http.ssl.trust-all:false` 配置驱动默认关闭+开启时 warn（C/H12 已修，VertxHttpConfigureTest 回归覆盖）。
6. **HttpContextHelp**（supplement/http/HttpContextHelp.java）：`SSLContext.getInstance("SSL")`→"TLS"；ignoreSSL/buildCors 恢复 @Deprecated+安全告警（C/M11 已修，HttpContextHelpIgnoreSslTest 回归覆盖）。仓库内 ignoreSSL/buildCors 调用方 0 命中，纯 opt-in 兼容保留。
7. **KRedissonConfigProperties 密码日志去除**（cache/lock/properties/KRedissonConfigProperties.java:169-172）：`ConfigSupport.toJSON(config)`（含 password 明文）→仅 address/codec/threads。**但兄弟路径漏修，见 E-01。**
8. **异常外泄收口**：UnifiedExceptionControllerAdvice.handleThrowable:253-254 兜底固定"系统繁忙，请稍后重试"，getMessage 只进 log.error；CatchLogAspect:182-184 未知异常同方案，且 :119-121 Error 无条件重抛、:123-125 返回类型守卫恢复（C/H27/C/C10 已修）；NettyHttpServer:202,214 固定"服务器内部错误"，exceptionCaught:355-358 只 log+close 不写堆栈回客户端（C/H11 外泄已修）；DuplicateKeyException 改 spring.dao 正确类型+固定 ErrorCode（不再透 ex.getMessage）；AccessDenied/Authentication 改 spring-security 正确类型+403/401。
9. **SqlFilter 白名单**（dal/mybatis/util/SqlFilter.java:19,37-39）：黑名单子串→`^[a-zA-Z0-9_]+$`+长度≤64（C/H18 已修，SqlFilterWhitelistTest 覆盖注入载荷/合法列名/超长）。调用方 PlusPageQuery:86,130 与 IPageQuery:44 经 OrderItem.asc/desc 使用；排序方向仅 ASC/else-desc 二值判定，无方向注入面。dal mapper XML 无 `${}` 拼接（grep 实证，仅 pom 占位符）。
10. **charset 显式 UTF-8**：Md5Util:29（本轮修，1.x 为裸 getBytes）、SHAUtil:87、KdlaHttpServletRequestWrapper:39,48,75（本轮修）、advice:229-233 null 回退 UTF-8、JwtUtil:37、job-app UserController:97,128,167/LoginService:57/GlueFactory:68、VertxHttpClient:215 URLEncoder 显式 UTF-8。残留见 E-07。
11. **LogSanitizeUtil 恢复接线**（C/H26）：RequestLogAspect:170 header 脱敏、LogInterceptor:37 body 截断、CatchLogAspect:130 args/response 截断。无正则→无 ReDoS。覆盖缺口见 E-02/E-12。

## 二、发现清单（按分级排序）

### 必改（3 条）

| ID | 分级 | 路径:行号 | 问题 | 修复建议 | 状态 |
|---|---|---|---|---|---|
| E-01 | 必改 | kdla-component-supplement-cache/src/main/java/top/kdla/framework/supplement/cache/lock/KRedissonLockFactory.java:99 | `log.info("inti the redisson client with config: {}", config.toYAML())` — toYAML 含 **redis password 明文**。默认活跃路径：KRedissonAutoConfigure:87 `new KRedissonLockFactory(...)`，`kdla.cache.redisson.enabled` matchIfMissing=true，SINGLE/REPLICATE 每节点初始化各打一次。与本轮已修的 KRedissonConfigProperties:169-172 同类问题，**兄弟路径漏修**（该文件本轮 0 改动，BASE 既有；COMMON-CONTEXT 已列其为未逐一 diff 项，本条为确认+实锤）。非本轮新引入，但"密码明文日志去除"修复不完整。 | 对齐 KRedissonConfigProperties 方案：仅 log address/codec/threads，删 toYAML 调用与 IOException catch | 待修 |
| E-02 | 必改 | kdla-component-log/src/main/java/top/kdla/framework/log/sanitize/LogSanitizeUtil.java:27-28 | 敏感 Header 黑名单（authorization/cookie/password/token/mobile）**漏 `blade-auth`** —— 本框架 trd 模块自己的认证头（TrdPlatformAPIController:57,81 等 10 处 `@RequestHeader("blade-auth")`）。RequestLogAspect:165-174 遍历打印全部请求头，下游同时启用 log+trd 时 **JWT 凭证明文进 INFO 日志**，有效期内可重放。工具类为本轮新增（C/H26 恢复），对框架已知敏感头覆盖不全。 | 黑名单加 `blade-auth`；建议一并评估 `x-auth-token`/`proxy-authorization`/`set-cookie`；补 LogSanitizeUtilTest 用例 | 待修 |
| E-03 | 必改 | kdla-component-supplement/src/main/java/top/kdla/framework/supplement/http/HttpContextHelp.java:97-99（javadoc，本轮新增）指向 kdla-component-supplement/src/main/java/top/kdla/framework/supplement/cors/CustomCorsConfig.java:34-35 | buildCors 的 @deprecated 指引称"生产请使用 CustomCorsConfig **并配置 origin 白名单**"，但 CustomCorsConfig 硬编码 `setAllowCredentials(true)`+`setAllowedOriginPatterns("*")`，**无任何白名单配置项**。下游照指引迁移即开启"任意 Origin 反射+带凭据"，与被废弃方法同等危险（CSRF/跨域凭据读取）。配置类 BASE 既有（opt-in，matchIfMissing=false），但**误导性安全指引为本轮新引入**。 | 二选一：给 CustomCorsConfig 增加 `kdla.config.cors.allowed-origins` 白名单属性（未配置时拒绝注册或默认同源）；或修正 javadoc 明示其当前同样反射任意 Origin、须宿主自配白名单 | 待修 |

### Optional（4 条）

| ID | 分级 | 路径:行号 | 问题 | 修复建议 | 状态 |
|---|---|---|---|---|---|
| E-04 | Optional | trdcloud/controller/TrdPlatformAPIController.java:71,87（另 4 控制器同模式）+ trdcloud/vo/TrdPlatformModelRefAddVo.java:78,93（另 4 VO 同模式） | `if (StringUtils.isNotEmpty(bladeAuth))` 守卫：**省略 blade-auth 头即完全跳过验签**，save/update 照常执行且 createUser/updateUser 为空。JwtUtil fail-closed 只覆盖"头存在"路径。BASE 既有（git show 136bf03 实证，本轮控制器仅 javax→jakarta）；若端点经 BladeX 网关强制鉴权则风险被上游收敛（not measured，部署依赖），若直接暴露则构成鉴权绕过+审计缺失。 | 下游确认网关强制鉴权；或头改 required=true、缺失即 fail-closed；至少在 README 标明"本模块不自带鉴权，依赖网关" | 待修/待确认 |
| E-05 | Optional | trdcloud/utils/StringUtil.java:73-75 | `hashString` catch Exception → **return message（原文）**：算法名拼错/不可用时把明文当"哈希"静默返回（fail-open）。若下游用于口令/令牌摘要即明文落库。BASE 既有（trd 模块 2.x 新增，1.0.1 无此文件），仓库内调用方 0 命中，纯下游 API。对照 SHAUtil.getSHA:94-95 失败抛异常是正确姿势。 | 失败抛 RuntimeException（对齐 SHAUtil），删除 return message 降级 | 待修 |
| E-06 | Optional | exception/UnifiedExceptionControllerAdvice.java:235-236 | handleHttpMessageNotReadableException：①`ex.getMessage()` 透前端（JSON 解析异常消息可含内部类名/报文片段）；②raw requestBody 无脱敏进 ERROR 日志（可含密码字段；KdlaBodyReaderRequestFilter excludes 默认 `/config/**,/ping` 非空，POST JSON body 缓存默认开启）。BASE 既有（本轮仅修 charset NPE）。 | 返回固定文案（对齐 handleThrowable 方案）；body 日志接 LogSanitizeUtil 截断+字段脱敏 | 待修 |
| E-07 | Optional | exception/UnifiedExceptionControllerAdvice.java:242-255 | handleThrowable(Throwable) 无显式 Error 重抛。标准 MVC 流程 Error 不进 advice（DispatcherServlet.doDispatch 只 catch Exception，FrameworkServlet 将 Error 包成 NestedServletException 上抛容器），实际吞 Error 路径 not measured；但签名收 Throwable 即存在被包装 Error 命中的理论面。CatchLogAspect:119-121 已做对。 | 防御性补 `if (exception instanceof Error) throw (Error) exception;` 与 CatchLogAspect 对齐 | 待修 |

### Nit（4 条）

| ID | 分级 | 路径:行号 | 问题 | 修复建议 | 状态 |
|---|---|---|---|---|---|
| E-08 | Nit | trdcloud/utils/StringUtil.java:37-42 | 已登记候选确认+降级：`appendReplacement(sb, pram)` 的 pram 先经 `URLEncoder.encode(pram,"UTF-8")`，输出仅含 `[A-Za-z0-9._*+% -]`，**无裸 `$`/`\`**，成功路径不会触发未转义元字符问题；UTF-8 恒被支持，:39-41 空 catch 为死分支。实际风险低于候选描述。 | 防御性 `Matcher.quoteReplacement(pram)`；空 catch 删除或改 log（死代码只列不删，先问） | 待修 |
| E-09 | Nit | common/utils/AESUtil.java:71,85 | `content.getBytes()` 无 charset（平台默认）。Base64 字母表纯 ASCII 且 JDK21 JEP400 默认 UTF-8，无实际跨环境风险；与同文件其余显式 UTF-8 不一致。 | 改 `StandardCharsets.UTF_8`（或 US_ASCII） | 待修 |
| E-10 | Nit | dal/mybatis/util/SqlFilter.java:42 | `str.toLowerCase()` 无 Locale：Turkish locale 下 `I`→`ı`（无点 i），含大写 I 的合法列名转小写后与 DB 列名失配（正确性问题，非注入——白名单已挡注入）。 | `toLowerCase(Locale.ROOT)` | 待修 |
| E-11 | Nit | common/utils/Md5Util.java:37 | `log.error("md5()-->input{},...", input, ...)` 把输入明文（可能是口令/token）写日志。MD5 为 JDK 必备算法，NoSuchAlgorithmException 分支实际不可达；1.x 既有（git show 实证本轮仅修 charset）。 | 日志去掉 input 参数 | 待修 |

### FYI（8 条）

| ID | 分级 | 路径:行号 | 问题 | 修复建议 | 状态 |
|---|---|---|---|---|---|
| E-12 | FYI | log/sanitize/LogSanitizeUtil.java:50-68；LogInterceptor.java:37；CatchLogAspect.java:130；RequestLogAspect.java:162 | 脱敏仅覆盖 Header 名黑名单+2KB 截断，**body/query 参数/方法入参无字段级脱敏**：JSON body 或 `?password=` 中的口令、身份证、手机号原样进日志（截断不脱敏）。1.x 即此设计，本轮新增截断属改善。无正则故无 ReDoS。 | 后续增加 JSON 字段黑名单/正则脱敏（password/idCard/mobile 等），注意正则线性复杂度防 ReDoS | 设计限制，记录 |
| E-13 | FYI | supplement-job/src/main/java/com/xxl/job/core/util/XxlJobRemotingUtil.java:25-41,76-80 | xxl-job 上游 vendored 代码：HTTPS 无条件 trustAllHosts+HostnameVerifier 恒 true（连接级，非 JVM 全局），executor↔admin 通信可被 MITM，XXL-JOB-ACCESS-TOKEN 亦经该连接发送。1.x 同款（1.0.1 路径 top/kdla/framework/supplement/job/util/XxlJobRemotingUtil.java:25-79 实证），非迁移引入。 | 上游行为，改动即分叉；文档标注风险或加 trust-all 开关（默认校验） | 既有/上游，记录 |
| E-14 | FYI | supplement/http/HttpContextHelp.java:35-90 | ignoreSSL：JVM 全局 trust-all+hostname 绕过，一次调用污染宿主全部 HttpsURLConnection。本轮已加 @Deprecated+双 warn+迁移指引（指向连接级 kdla.http.ssl.trust-all 默认 false），opt-in 且仓库内 0 调用方。刻意兼容保留，处置得当。 | 无需动作；下游迁移完成后可移除 | 已处置，记录 |
| E-15 | FYI | common/utils/AESUtil.java:20；Sm4Util.java:37 | CBC+PKCS5 **无完整性校验**（非 AEAD），密文可篡改（bit-flipping）；decrypt 失败统一 RuntimeException("decrypt exception") 不区分 padding 错误，客观上收敛 padding oracle 面。1.x 设计延续，本轮已从全零 IV/ECB 改善到随机 IV。 | 升级路径：AES-GCM / SM4-GCM（需新格式前缀 v2 与存量迁移方案） | 设计限制，记录 |
| E-16 | FYI | common/utils/Sm4Util.java:101-122,154-158 | 密钥文本编码 Hex→Base64（C/H34 修复，对齐 generateKey/generateSecureKey 的 Base64 输出，消除旧版"生成 Base64 却按 Hex 解码"的自相矛盾）。**破坏性变更**：下游直传 hex 密钥文本的调用会 Base64 解码失败或派生错误密钥（fail-visible，非静默）。 | 确认 README/发布说明已文档化该破坏性变更（正确性切片可能已覆盖，此处仅安全视角备注） | 已修，兼容性待文档确认 |
| E-17 | FYI | trdcloud/utils/JwtUtil.java:46 | hutool `validate(0)` 仅校验**存在的**时间声明：无 exp 声明的 token 会通过过期检查（验签仍强制）。BladeX token 通常含 exp，not measured。 | 如需强制 exp 存在，补 `payloads.get("exp") != null` 显式检查 | 记录 |
| E-18 | FYI | supplement-job-application/.../UserController.java:97,128,167；LoginService.java:57 | vendored xxl-job-admin：口令**无盐 MD5** 哈希（上游原样），charset 已显式 UTF-8。彩虹表可破弱口令。 | 长期改 BCrypt（仓库已有 BCryptUtil）；属上游分叉改造，非本轮范围 | 既有/上游，记录 |
| E-19 | FYI | supplement/http/ssl/SslContextFactory.java:23,59 | `SSLContext.getInstance("TLS")`+JdkSslContext 未显式限定启用协议，依赖 JDK21 `jdk.tls.disabledAlgorithms` 默认禁用 SSLv3/TLS1.0/1.1（默认安全）；:23 TODO 注释仍在。证书校验本身完整（truststore TM，无 trust-all）。 | 如需显式强制 TLS1.2+，在 JdkSslContext 构造指定 protocols；删 TODO 先问 | 记录 |

## 三、扫描否定性结论（已查无问题）

- `SHA1PRNG` 全仓 0 命中（C/H36 误报按 COMMON-CONTEXT 不报；Sm4Util.generateKey/Sm2Util.generateKeyPair 的 `new SecureRandom(seed)` 为裁定保留的兼容派生，已 @Deprecated）。
- `InsecureTrustManagerFactory` 仅 NettyHttpServer:153 / NettyHttpClient:225 **注释行**，无活跃 trust-all（活跃 trust-all 仅 E-13 上游 vendored 与 E-14 已处置 opt-in）。
- `printStackTrace` 仅 supplement-job 3 处 `e.printStackTrace(new PrintWriter(stringWriter))`（xxl-job 上游 ThrowableUtil/JobThread/XxlJobHelper，转字符串进日志非 stderr，既有）。
- 全仓 `log.*(password|secret|signKey)` 模式 0 命中（E-01 的 toYAML 为间接泄露，模式扫不到，已人工实锤）；JwtUtil 旧版 header/signature 明文日志已删。
- mapper XML `${}` 拼接 0 命中；`Statement` 裸拼接 0 命中；URLEncoder/URLDecoder 单参（平台默认 charset）0 命中。
- `VertxMqttConfigure.java:171 payload.getBytes()` 为 **io.vertx Buffer.getBytes()**（取原始字节，无 charset 语义），grep 误报排除。
- AESUtil 旧格式兼容分支无降级攻击面（见一.2）；JwtUtil 无 alg=none/空 token/吞异常/验签前返回 claims 路径（见一.1）。

## 四、结论

- **Critical 计数：0**
- 必改 3（E-01 redisson 密码日志漏修兄弟路径 / E-02 blade-auth 脱敏缺口 / E-03 CORS 迁移指引误导）；Optional 4；Nit 4；FYI 8。
- 三条必改均为小 diff（一行日志替换 / 一个黑名单条目 / 一段 javadoc 或一个白名单属性），不阻断编译与测试，建议发布前补齐。
- 本轮安全修复主线（JWT fail-closed、随机 IV、trust-all 默认关闭、异常外泄收口、SQL 白名单、charset）**静态核对全部闭合**，无新引入 Critical 安全风险。

**本切片结论：GO**（无 Critical；E-01/E-02/E-03 建议发布前修复，E-04 需下游确认网关鉴权部署模型）。