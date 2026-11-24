# kdla 2.0.1-SNAPSHOT 安全审计发现汇总与修复状态

分支 `2.0.0-SNAPSHOT`（分支名未改），版本已升 `2.0.1-SNAPSHOT`（`f08ad3f`，17 处）。
四路专项审计原始报告：`A-crypto.md`（加密，内部编号 C-01~C-13）、`B-auth.md`（鉴权 B-01~B-12）、`C-injection.md`（注入/资源耗尽 C-01~C-11）、`D-network.md`（网络/日志 D-01~D-15）。
注意：A 报告与 C 报告内部都用 `C-xx` 编号，本汇总按报告命名空间区分（A/C-xx = 加密，C/C-xx = 注入）。

## 权威验证

`mvn clean test` 全量：**617 用例，0 失败，0 错误，16/16 模块 BUILD SUCCESS**（日志 `/tmp/kdla_c7_full.log`）。
job-application 模块 39 例（PermissionInterceptor 6 + JobCodeController 1 + LoginService 8 + CookieUtil 4 + UserController 4 + CsrfMethodRestriction 1 + CsrfTokenProtectionTest 10 + CsrfTokenFrontendWiringTest 5）。
common 模块 163 例（含 AESUtilGcmTest 6 + Sm4UtilGcmTest 7 + UrlSecurityUtilTest 33）。
cache 模块 102 例（含 KRedisSetNxLockTest 16 + KRedisSetNxLockAutoConfigureTest 7）。
reactor 模块 9 例（SerializeUtilSecurityTest，本阶段新建 src/test）。
supplement-job 模块 +6（JdkSerializeToolTest 5 + XxlJobRemotingUtilTlsTest 1）。
supplement 模块 +13（VertxMqttConfigureTlsTest 9 + HttpRedirectGuardTest 4）。
trdplatformcloud 模块 +4（GroovyShellCalSandboxTest 新增 .@ / .& / :: 拦截与正常属性访问放行）。
supplement-job 模块 +2（ScriptUtilProcessCleanupTest）。

**未实测项（交用户验证）**：`job-application/Dockerfile` 基础镜像 `eclipse-temurin:21-jre-jammy` 的拉取与 `docker build` —— 本机 Docker daemon 未运行，not measured。

## 修复提交清单（安全阶段）

| commit | 主题 | 覆盖发现 |
|---|---|---|
| f08ad3f | 版本升 2.0.1-SNAPSHOT | — |
| ef8047d | common 加密加固 | A/C-01 反序列化过滤、A/C-03 RSA OAEP、A/C-08 Sm3 恒时、A/C-09 证件全掩码、A/C-10 Gzip 上限、A/C-11 Md5 日志去原文、A/C-02 ECB→CBC+随机IV、A/C-07 低熵派生 @Deprecated |
| 17b089e | cache SecureRandom + password toString 排除 | A/C-05 业务编号随机段、A/C-12 redisson password 泄露面 |
| bde7c2d | log JSON 字段脱敏 + traceId 白名单 | D-09/D-10/D-12 |
| 6b839d2 | supplement TLS hostname + CORS 白名单 | D-01~D-06、D-08 |
| 731b72b | excel 导入行数上限 + 校验 | C/C-03 OOM、C/C-08 空白绕过、C/C-09 长度 NPE |
| ed72978 | LogSanitizeUtil 下沉 common + URL query 脱敏 | D-09/D-11 |
| 2306cff | HTTP 客户端/服务端日志脱敏 + 安全响应头 | D-10/D-11/D-13/D-15 |
| 751438a | 分页上限 + validator fail-closed | C/C-02 分页 DoS、C/C-05/C-06/C-07/C-10 |
| 01f3e57 | trd blade-auth 守卫 + JWT 加固 + Groovy 沙箱 + configJson 脱敏 | B-04、A/C-06、B-12（部分）、D-11 |
| c0ba8c1 | job accessToken fail-closed | B-02/B-03 |
| e71f34b | job admin API 鉴权 + 机器 token | B-01/B-10 |
| b1328b8 | job 数据源凭据外部化 | B-05 |
| e0ba364 | job 会话 Cookie 加固 | B-06 |
| 812510d | job 口令加固 | B-07 |
| e169dae | job CSRF 方法限制 | B-09 |
| 94696be | POI 升 5.4.1 | C/C-04 CVE-2025-31672 |
| 721a0f2 | Sm2Util javadoc 双轨编码 + C1C2C3 模式 | A/C-13（非破坏部分） |
| 03b31df | Sm2Util 双密钥编码 + C1C2C3/C1C3C2 双模式 + 无符号标量修复 | A/C-13（完整） |
| a4c3167 | AES/SM4 CBC→GCM（v2 格式，AEAD 完整性） | A/C-02（完整） |
| e415301 | SET NX PX 分布式锁 + Bean 自动装配 | 用户新增需求（KRedisSetNxLock） |
| 5557a84 | SerializeUtil JEP 290 过滤 + JSON 类白名单 | 范围外线索 reactor/SerializeUtil |
| 10db75a | JdkSerializeTool callbacklog 反序列化过滤 | 范围外线索 job/JdkSerializeTool |
| 5bc13a7 | XxlJobRemotingUtil 恢复 JVM 默认 TLS 信任与主机名校验 | 范围外线索 job/XxlJobRemotingUtil |
| ea3bced | job-application 基础镜像升 temurin 21-jre-jammy | 范围外线索 Dockerfile |
| c8ba952 | SSRF 首跳 URL 防护 helper（UrlSecurityUtil） | D-14（框架侧非破坏部分） |
| 3bce441 | Vertx MQTT truststore + hostname 校验 | D-07（增强项完整） |
| 974099c | Groovy 沙箱拒绝 .@ 直读字段与 .& / :: 方法句柄 | B-04 沙箱残余缺口（完整） |
| 1742285 | HTTP 客户端停止跟随重定向 | D-14（重定向绕过部分） |
| 1280975 | 脚本任务超时/中断强制销毁子进程 | 范围外线索 job/ScriptUtil 残余 |
| 7b6b51a | job admin 会话绑定 CSRF token（零新依赖） | B-09（完整） |

## B 系列（鉴权）逐项状态

- **B-01 [Critical] 任务管理 8 端点匿名开放 → RCE**：已修（e71f34b）。删除 8 处 `@PermissionLimit(limit=false)`，pageList/add/update 按 jobGroup 鉴权，remove/pause/start/trigger 按 id 查 group 鉴权。
- **B-02 [High] admin /api accessToken 空值 fail-open + default_token**：已修（c0ba8c1）。`XxlJobRemotingUtil.validAccessToken` 恒时比对，空配置/空请求一律拒。
- **B-03 [Critical] 执行器 accessToken 默认 default_token → RCE**：已修（c0ba8c1）。默认值改空，`XxlJobExecutor.start()` 首行空 token 抛 IllegalStateException 阻断启动（fail-closed）。
- **B-04 [High] trd blade-auth required=false 省略头匿名写 + Groovy RCE 链**：已修（01f3e57）。守卫 fail-closed + Groovy SecureASTCustomizer 沙箱。
- **B-05 [High] 生产 DB 口令硬编码 + JDBC 明文**：已修（b1328b8）。`application.properties` 改 `${DB_URL:}/${DB_USERNAME:}/${DB_PASSWORD:}/${DB_JDBC_URL:}`，移除 `sslMode=disabled` 硬编码。**口令轮换与 git 历史清理经用户裁定不做（风险接受），见下方运维动作章节。**
- **B-06 [High] 会话令牌无签名/无过期/内嵌 MD5 + Cookie 无 Secure/SameSite + 记住我 68 年**：已修（e0ba364）。token = hex(payload)+ "." +hex(HMAC-SHA256)，2h 过期；Cookie HttpOnly+SameSite=Lax+Secure(HTTPS)+2h Max-Age，会话 Cookie 不写 Max-Age。
- **B-07 [Medium] 无盐 MD5 + 最短 4 位 + 改密不验旧 + 无限速**：已修（812510d）。BCrypt（复用 common BCryptUtil）+ 旧 MD5 登录懒迁移；策略 8-100；updatePwd 验旧口令；登录 5 次失败锁 15 分钟。
- **B-08 [Medium] 默认 admin/123456**：**仓库内无法证实**（上游 xxl-job 初始化脚本默认值）。**用户裁定不做生产库核实，风险接受**，开放项关闭。
- **B-09 [Medium] 状态变更接受 GET + 无 CSRF 防护**：**已完整闭合（e169dae + 7b6b51a）**。①16 个变更端点限 POST + SameSite=Lax（e169dae，前端均已 POST，兼容）；②会话绑定 CSRF token（7b6b51a，零新依赖，未引 spring-security）。详见下方实施记录。
- **B-10 [Medium] /jobcode/save 缺 validPermission 横向越权 → RCE**：已修（e71f34b）。save 按 exists_jobInfo.jobGroup 鉴权。
- **B-11 [Low] xxl.enabled.xxl 键名语义**：**本就 fail-closed**（`@ConditionalOnProperty` matchIfMissing 默认 false），是文档/键名语义问题非鉴权绕过。**仅记录，不改代码。**
- **B-12 [Low] JwtUtil 残留：无 exp 永久有效 / 验签失败 HTTP 200 / 不校验 Bearer 前缀**：**三点均已闭合（本会话复核）**。①缺 exp 抛 BizException（JwtUtil:46-50，测试 missingExpRejected）；③强制 Bearer 前缀（JwtUtil:81，测试 nonBearerSchemeRejected/lowercaseBearerAccepted）；②验签失败 HTTP 200 已被 TrdBladeAuthFilter 闭合——过滤器 catch 全部 JwtUtil 异常直接返回 401（TrdBladeAuthFilter:33-38），注册于 /trd/* order 0 默认开启（TrdAuthAutoConfigure:23-28），控制器/VO 内联调用仅在放行后执行，BizException→200 路径对 trd 不可达。另密钥<32 字节拒绝（JwtUtil:70-72）。残留：UnifiedExceptionControllerAdvice.handleBizException:142-153 仍无 @ResponseStatus（框架级业务异常 200 约定，非 JWT 专属），仅下游显式 kdla.trd.auth.enabled=false 且裸调 JwtUtil 时才可能落 200，属 opt-out。

## A 系列（加密）残留

- **A/C-02 AES/SM4 CBC 无认证**：**已修（a4c3167；用户裁定无生产调用方、直接修复）**。ECB→CBC+随机 IV（ef8047d）后再升 GCM，机密性与完整性（AEAD）均达标。详见下方实施记录。
- **A/C-04 StringUtil.hashString fail-open**：**已修（本会话复核）**。catch 改抛 BizException 不再返回原文（StringUtil:71-79）；回归测试 StringUtilTest.unknownAlgorithmThrowsInsteadOfReturningPlaintext/nullMessageThrows + md5/sha256 已知向量。
- **A/C-13 Sm2Util 密钥双轨 + C1C2C3 [Low]**：**已修（03b31df；用户裁定无生产调用方、直接双支持）**。①编码兼容：createPublicKey/createPrivateKey 自动识别 hex 裸公钥点(130/66 hex)/裸私钥 D(64 hex) 与 Base64(X509/PKCS8)，encrypt/decrypt/sign/verify 四操作均可混用两种编码；②双模式：encrypt 默认 C1C3C2（现行 GB/T 32918）+ 重载指定 C1C2C3，decrypt 依 C3 SM3 校验自动识别两种模式。附带修复潜在 bug：hex 私钥 new BigInteger(byte[]) 最高位为 1 时解成负数致 "Scalar not in [1,n-1]"，改 signum=1。回归测试 Sm2UtilDualEncodingTest 8 例（含高位 D 确定性用例）。既有方法签名全保留，调用方零改动。

## C 系列（注入）残留

- **C/C-04 POI CVE-2025-31672**：已修（94696be）。EasyExcel 3.3.2→3.3.4，根 pom 钉 poi/poi-ooxml/poi-ooxml-lite 5.4.1，supplement/common 排除 poi-ooxml-schemas。本地无 EasyExcel 4.x，走 POI override。dependency:tree 确认 5.4.1 无 4.1.2；excel 读写测试全绿。
- **C/C-11 FieldValueFindHelp 敏感字段入日志**：log 脱敏（bde7c2d/2306cff）覆盖，FieldValueFindHelpLogSanitizeTest 绿。

## D 系列（网络）残留

- **D-07 Vertx MQTT 明文 + "-1" 哨兵**：**Vertx 侧已修（本会话复核）**。username/password 默认改空（VertxMqttConfigure:45-49），新增 kdla.mqtt.ssl:false（:51-52），buildClientOptions:96-109 setSsl + 空白/"-1" 一律不下发凭据；回归测试 VertxMqttConfigureSecurityTest 6 例。**Netty 侧**：hostname 校验已修（MqttClientImpl:595-598 setEndpointIdentificationAlgorithm("HTTPS")，测试 MqttClientImplTlsHostnameTest，属 D-05）；默认 sslContext=null（MqttClientConfig:49-51）+ connect(host)→1883（MqttClientImpl:116）仍明文，但 Netty 为裸客户端库 API（非 Spring 托管，无 @Value 配置面），TLS 由调用方显式 MqttClientConfig(SslContext) opt-in，框架层无法强制而不破坏连明文 broker 能力。**增强项已修（3bce441）**：新增 `kdla.mqtt.ssl.truststore`（JKS 路径，留空用 JVM 默认信任库）、`kdla.mqtt.ssl.truststore-password`、`kdla.mqtt.ssl.verify-host`（默认 true）。回归测试 VertxMqttConfigureTlsTest 9 例。
**本轮实证纠正审计结论**：Vertx `NetClientOptions.DEFAULT_HOSTNAME_VERIFICATION_ALGORITHM` 实为 **null**，即 net/MQTT 客户端**默认不做 hostname 校验**（审计「verifyHost 默认 true」只适用于 HttpClientOptions）。故本修复显式置 `"HTTPS"`，属实质补强而非冗余；`trustAll` 始终 false，有测试钉住。
- **D-14 SSRF（任意 URL 无内网防护 + 跟随重定向）**：**框架侧已修（c8ba952）**。新增 `common/utils/UrlSecurityUtil.isPublicHttpUrl(String)`：仅放行 http/https，拒绝 loopback/any-local/link-local（含 169.254.169.254 云元数据）/site-local（RFC1918）/multicast/IPv6 fc00::/7，拒绝反斜杠与 userinfo 指向内网、解析失败 fail-closed。回归测试 UrlSecurityUtilTest 33 例（全 IP 字面量，不触网），变异校验 14 例失败。
  **重定向绕过已修（1742285）**：`VertxHttpConfigure` 两个 WebClient 均 `setMaxRedirects(0)`（原默认 16 跳），`RestHttpConfigure` 的 HttpClient5 `disableRedirectHandling()`（原默认跟随）。3xx 原样返回调用方，重定向决策权交回调用方（可先校验 Location 再自行发起）。ProtoHttpClient（JDK `Redirect.NEVER`）、KdlaHttpClient（hutool `maxRedirectCount=0`）、NettyHttpClient（无重定向逻辑）本就不跟随，未改。回归测试 HttpRedirectGuardTest 4 例（JDK HttpServer 返回 302，断言不跟进 + 正常 200 不受影响）；Prove-It 反证 3 例失败（Vertx 拿到 200 "followed"、maxRedirects=16、BaseHttpClient 未抛异常）。
  **首跳内网拦截仍不默认强制**：5 个出站客户端主用途是调内网服务，且现有 13 个 HTTP 测试全部用 loopback 服务端；默认强制 `isPublicHttpUrl` 会废掉主用途并破坏全部测试。`UrlSecurityUtil` 保持 opt-in，由下游在公网出口场景显式调用。
  **残余仍属下游责任**：DNS rebinding（校验后重新解析）；升级路径=连接期绑定已校验 IP，或改用固定出口代理。

## AES/SM4 CBC→GCM 实施记录（A/C-02 完整性，已修 a4c3167）

原缺口：CBC 无认证 → 密文可篡改（malleability），理论 padding-oracle 风险取决于调用方是否暴露可区分的解密错误。
暴露面：**框架内 AESUtil/Sm4Util 无生产调用方**（grep 实证仅测试引用），属下游工具类，框架自身无解密 oracle。
用户裁定「没有生产调用方，所有的调用方修改都不用改」→ 原「混合版本滚动升级」顾虑不成立，直接实施。

实施内容：
- 新格式 **`v2:Base64(12字节nonce + GCM密文含16字节tag)`**；`AES/GCM/NoPadding`（JCE）、`SM4/GCM/NoPadding`（显式 BC provider）。
- encrypt 默认产 v2，`SecureRandom` 12 字节 nonce，`GCMParameterSpec(128, nonce)`。
- decrypt 三分支：**v2→GCM；v1→CBC+随机IV；无前缀→旧格式**（AES 全零 IV CBC / SM4 ECB），存量数据零丢失。
- **既有 public 签名全保留**（AESUtil 2 个、Sm4Util 4 个加解密 + 2 个 generateKey），调用方零改动。
- AESUtil 异常消息仍不含密钥明文；Sm4Util 改动前即存在的未用 `log` 字段保留不动。

验证：
- 新增 `AESUtilGcmTest` 6 例 + `Sm4UtilGcmTest` 7 例，共 +13（common 117→130，全量 499→512）。
- **AEAD 完整性**：篡改密文首字节 / 篡改 tag 末字节均必抛异常（CBC 时代无此保证）。
- **标准性证明**：AES 侧匹配 NIST GCM Test Case 2 已知答案向量（K=0^128, IV=0^96, C=0388dace…fe78, T=ab6e47d4…bddf）；SM4 侧 v2 密文可被原生 BC `SM4/GCM/NoPadding` Cipher 直接解开，证明非私有封装。
- **存量兼容**：手工构造 v1 CBC 密文（AES/SM4）与旧格式密文均仍可解。
- **Prove-It 反证**：`git stash` 回退两个主源码后跑新测试 → 3 断言失败 + 2 错误（`/tmp/kdla_gcm_proveit2.log`），恢复后全绿。
- 既有 `AESUtilTest`/`Sm4UtilCbcMigrationTest` 中 `startsWith("v1:")` 断言同步改为 v2（格式升级所致），随机 nonce/round-trip/旧格式兼容等核心断言保留。

## 运维动作（用户已裁定：1/2/3 不做，风险接受）

**用户裁定（本轮）：「B-08 默认管理员核实、DB 口令轮换 + git 历史清理 都不做。其他照做。」**
以下 1/2/3 项**全部取消，不再提议、不再执行**。风险事实保留备查，状态为「用户已知悉并接受」。

1. ~~轮换生产 DB 口令~~ **【用户裁定不做】**：`adm_airecon` 口令 `adm2o3rhU$2i`（主机 10.39.68.62）曾明文入库。b1328b8 已从工作区清除，但口令仍存在于 git 历史与 GitHub 远端（`origin = https://github.com/violetsunny/kdla.git`）。`git log --all -S` 实证仅 `4af99c0`（引入）与 `b1328b8`（移除）两个提交命中，全仓 153 commits。**风险由用户接受。**
2. ~~清理 git 历史~~ **【用户裁定不做】**：属不可逆操作，本会话未触碰历史；`git-filter-repo` 本机未安装。**风险由用户接受。**
3. ~~核实默认管理员（B-08）~~ **【用户裁定不做】**：仓库内无法证实是否存在 admin/123456（上游 xxl-job 初始化脚本默认值）。**风险由用户接受。**
4. **配置注入（仍须执行）**：job-application 启动须注入 `DB_URL/DB_USERNAME/DB_PASSWORD/DB_JDBC_URL` 与 `XXL_JOB_ACCESS_TOKEN`；多实例须配置共享 `xxl.job.login.tokenSecret`（否则重启会话失效、实例间 token 不互认）。

## 范围外线索（用户裁定「2 都进行修复」→ 本阶段全部闭合）

- `reactor/SerializeUtil` readObject 无过滤：**已修（5557a84）**。详见下方实施记录。
- `job/JdkSerializeTool:58-59` 原生反序列化：**已修（10db75a）**。详见下方实施记录。
- `job/ScriptUtil:76` Runtime.exec：**已评估，无注入面，不改代码**。取证：唯一调用方 `ScriptJobHandler:55,81`；`cmd = glueType.getCmd()` 来自 `GlueTypeEnum` 硬编码常量（bash/python/php/node/powershell），`GlueTypeEnum.match()` 只按枚举名匹配，无法注入任意解释器；`Runtime.exec(String[])` 为 argv 分离**不经 shell**，故 `XxlJobHelper.getJobParam()` 中的 `;`/`|`/`$()` 不被解释；`scriptFileName` 由 int jobId + long glueUpdatetime + 枚举 suffix 拼接，无路径穿越。执行 GLUE 脚本是 xxl-job 设计功能，其来源鉴权已由 B-01/B-02/B-09/B-10 加固闭合。**残余已修（1280975）**：`JobThread:135-163` 超时仅 `futureThread.interrupt()`，`process.waitFor()` 抛 InterruptedException 后直接 return -1，**子进程从不被销毁**，成为孤儿继续占用执行机。修复：waitFor/join 外包 try-finally，退出前 `process.destroyForcibly()`（正常结束时进程已退出，无副作用）。超时上限仍由既有 `executorTimeout` 控制（0=不限），未新造配置。回归测试 ScriptUtilProcessCleanupTest 2 例（`echo $$ > pid; exec sleep 30` + 中断 + `ProcessHandle.isAlive()` 断言已死；正常脚本 exit 0 与日志落盘不受影响）；Prove-It 反证：修复前子进程存活 5s 超时失败。
- `job/XxlJobRemotingUtil:25-53` trust-all SSL：**已修（5bc13a7）**。删除 `trustAllHosts`/`trustAllCerts` 与全放行 HostnameVerifier，https 走 JVM 默认信任库 + 默认主机名校验；自签证书须导入 `javax.net.ssl.trustStore`。回归测试 XxlJobRemotingUtilTlsTest 用 keytool 生成临时自签证书 + 本地 HttpsServer，断言握手失败（FAIL_CODE）；`git stash` 回退后实测返回 200（反证成立）。
- `job-application/Dockerfile:1` openjdk:8-jre-slim：**已修（ea3bced）**，改 `eclipse-temurin:21-jre-jammy`（`java.version=21`，pom:19）。`MAINTAINER`/`TZ`/`ENTRYPOINT` 等无关行未动。**镜像构建 not measured**（Docker daemon 未运行）。

## 刻意简化（ponytail 标注）

- 登录限速：单实例内存 ConcurrentHashMap，重启清零、多实例不共享；升级路径=Redis 集中计数。
- CSRF：token 会话期内固定，非一次性；一次性 token 需服务端存 nonce，与现有无状态会话冲突（spring-security `CookieCsrfTokenRepository` 同为会话级语义）。
- `SerializeUtil` OBJECT 分支：改长度前缀字节块 + 自建过滤流，**线格式变更**（旧数据不可读）、失去跨对象引用共享；升级路径=下游统一改用 JSON 传输。
- `SerializeUtil`/`JdkSerializeTool` 默认白名单：仅 java.base + top.kdla（或 com.xxl.job）+ 调用方声明类型；下游自有类型须显式放宽。
- `UrlSecurityUtil`：仅校验首跳且 opt-in，DNS rebinding 仍可绕过；升级路径=连接期绑定已校验 IP。重定向绕过已由 1742285 闭合。
- `ScriptUtil`：孙进程持有管道时 `inputThread.join()` 仍可能长阻塞（直接子进程已销毁）；升级路径=join 带超时 + 进程组销毁。
- `VertxMqttConfigure` 信任库：仅支持 JKS（与 SslContextFactory 一致）；升级路径=PEM/PKCS12 走 setPemTrustOptions/setPfxTrustOptions。
- `KRedisSetNxLock`：轮询 100ms 非 Redis pub/sub；单节点非 RedLock，主从异步复制故障切换可能丢锁。

## SETNX 锁与 backlog 全量修复实施记录（本阶段）

用户裁定：「2 都进行修复。KRedisSetNxLock 也加一个 Bean 注入配置。」→ 上述 7 项 backlog + Bean 装配全部执行。

### KRedisSetNxLock（e415301）

复用既有 `SpringRedisHelp.lock`（SET NX PX，毫秒）与 `unLock`（Lua 比对 token 后 del，已统一走 template 序列化器），未新造 Redis 交互。
API：`tryLock(String)`（默认 10s 等待 / 3600s 租约）、`tryLock(String,TimeUnit,long,long)` 返回 token（null=未获锁）、`unlock(String,String)` 返回 boolean。
**不实现 `KDistributeLock` 接口**：该接口为 Redisson `Lock` 形状，SETNX 硬套需 Lock 适配器，判定过度工程；`KDistributeLockFactory.getLock` 返回 `RLock`、`KdlaBizDisLockService` 构造器钉死 `KRedissonRedDisLock`，均无法复用。
`KRedisSetNxLockAutoConfigure`：`@ConditionalOnClass(RedisTemplate)` + `@ConditionalOnBean(SpringRedisHelp)` + `@AutoConfigureAfter(CacheAutoConfigure)` + `kdla.cache.setnx-lock.enabled`（matchIfMissing=true），**不依赖 Redisson**；已登记进 cache 模块 `AutoConfiguration.imports`（第 4 行），并有测试读该资源文件断言类名在册防漏登记。
类 javadoc 明列与 RLock 5 点差异（不可重入 / 无看门狗 / 轮询 / 单节点 / 无公平锁）。测试 16+7 例。

### SerializeUtil（5557a84）

**关键技术障碍（本轮实证）**：`ObjectInput` 是接口无法直接设 filter；而唯一入口 `Externalizable.readExternal` 必然处于外层流读取中，JDK 21 `ObjectInputStream.setObjectInputFilter` 抛 `IllegalStateException: filter can not be set after an object has been read`。故「借用外层流过滤」方案不可行。
**根治方案**：OBJECT 分支改为长度前缀字节块，在**自建** `ObjectInputStream` 上强制施加 JEP 290 filter（`maxdepth=20;maxarray=10000;maxbytes=1048576;maxrefs=10000;java.base/*;top.kdla.**;!*`），与外层流状态无关。
**第二处 sink（审计未标，本轮新发现）**：JSON 分支 `input.readUTF()` 取线上可控类名 → `loadClass` → fastjson `parseObject`。已加包前缀白名单（默认 `top.kdla.`），且**校验前移到 `clazzCache.computeIfAbsent` 之前**——原设计放在 `loadClass` 内会被缓存预热绕过（本轮实测暴露）。
**纠正旧记录**：fastjson 实为 **2.0.57**（fastjson2 兼容包），非 1.x。
可配置：`setObjectInputFilter`（置 null 关闭，仅限可信字节）、`setJsonClassPrefixes`。既有 public 签名全保留。
reactor 模块原无 src/test 与测试依赖，本阶段按仓库范式新增 `spring-boot-starter-test`（provided，排除 slf4j-api）。
测试 SerializeUtilSecurityTest 9 例（含 gadget fixture 置标志位证明未实例化、Externalizable 真实路径、缓存预热绕过、外层流放行仍被内层过滤）。Prove-It：关闭防护后 5 例失败（`/tmp/kdla_s2_proveit2.log`）。

### JdkSerializeTool（10db75a）

唯一调用方 `TriggerCallbackThread:214,252`：把 `List<HandleCallbackParam>` 序列化到 `callbacklog/xxl-job-callback-*.log`，重试时读回——**能写该文件即可 RCE**。
原 `deserialize(byte[], Class<T>)` 的 `clazz` 参数**完全未被使用**，正好作为白名单抓手：默认 filter = `java.base/*` + `clazz.getName()` + `com.xxl.job.**` + 深度/数组/字节/引用上限，其余拒绝。
新增带 `ObjectInputFilter` 的重载（对齐 `ObjectUtil` 既有范式），原签名保留；`deserialize` 改 try-with-resources（消除原 finally 裸 close 的 NPE 隐患），拒绝时仍返回 null（fail-closed）。`serialize` 未触碰。
测试 JdkSerializeToolTest 5 例（真实回调列表 round-trip 非回归 + 类型不符拒绝 + 列表内嵌 gadget 拒绝 + 显式 filter 覆盖 + null filter 保留旧行为）。Prove-It：默认 filter 置 null 后 2 例失败。
## 缺口闭合实施记录（本轮：用户「没有上下游需要跟进修改，可以直接修复代码」）

前序阶段刻意留给「下游责任 / 升级路径」的 3 处缺口，本轮全部在框架内闭合，无需下游改动。

### Groovy 沙箱三类表达式绕过（974099c）

`SecureASTCustomizer` 的 `addExpressionCheckers` 原只拦 `ConstructorCallExpression` 与 `MethodCallExpression`，实证三类绕过成立：
- `bean.@secret`（`AttributeExpression`）直读字段，绕过 getter 与接收者黑名单，返回 `"classified"`。
- `('x'.&getClass)()`（`MethodPointerExpression`）取方法句柄后调用，绕过方法名黑名单。
- `('x'::getClass)()`（`MethodReferenceExpression`，Groovy 3+ 语法）同上。

修复：三类表达式一律 `return false`。`AttributeExpression extends PropertyExpression`，但正常 `bean.secret` 是 `PropertyExpression`（走 getter），拒绝 `AttributeExpression` 不误伤正常属性访问（`legitPropertyAccessStillWorks` 钉住）。
测试 12→13 例；Prove-It 反证：修复前 3 例均「无异常抛出」= 逃逸成功（`/tmp/kdla_g2.log`）。

### HTTP 重定向绕过（1742285）

见上方 D-14 章节。要点：框架内可闭合的是**重定向那一段**；首跳内网拦截因与内网调用主用途冲突，保持 opt-in。

### 脚本子进程泄漏（1280975）

见上方范围外线索章节。要点：修的是**孤儿进程泄漏**（根因），不是新造超时配置。

### 本轮未改项（设计天花板 / 需用户裁定的结构性选择）

- **登录限速单实例内存**：升级 Redis 集中计数需给 job-application 引入 Redis 依赖，**用户裁定不做**。
- **B-12 残留 `UnifiedExceptionControllerAdvice.handleBizException:142-153` 无 `@ResponseStatus`**：框架级「业务异常 HTTP 200」约定，非缺陷，opt-out 场景才可达。
- `KRedisSetNxLock` 轮询/单节点、`SerializeUtil` 白名单窄、MQTT 仅 JKS：均为设计天花板，非缺陷。

## CSRF 会话绑定 token 实施记录（B-09 完整闭合，7b6b51a）

用户问「引 spring-security 会带来什么问题？可直接用么？能用 JDK 21 么？」→ 取证结论：**JDK 21 兼容无问题**（Boot 3.4.0 BOM 隐式管 Security 6.4.x，基线 JDK 17），但**默认链不可直接用**，4 个冲突：

1. **机器端点全断**：`/api/callback`、`/api/registry`、`/api/registryRemove`（`JobApiController:25,38-54`）+ `PermissionInterceptor:48-58` 的 jobinfo 机器兜底靠 `XXL-JOB-ACCESS-TOKEN` 头鉴权、无会话 cookie；默认认证拦成 302/401，默认 CSRF 过滤也拦掉机器 POST（执行器不可能带 token）→ 执行器注册与回调全断。
2. **默认 CSRF 仓库不可用**：现有会话是自研 HMAC token + Cookie，**完全无状态、无 HttpSession**（`LoginService:63-112`），`HttpSessionCsrfTokenRepository` 直接失效。
3. **双鉴权体系并存**：FilterChainProxy 跑在 Interceptor 之前，未认证请求到不了 `PermissionInterceptor`，`@PermissionLimit`、302→`/toLogin`、adminuser 角色判断、request attribute 注入均需重做。
4. **actuator 独立上下文**（`application.properties:6`），主 security 链不覆盖。

**故未引 spring-security，用 JDK 原生 `javax.crypto.Mac` 实施，零新依赖。**

### 服务端

- `LoginService`：新增 `CSRF_HEADER = "X-CSRF-TOKEN"`、`makeCsrfToken(String)` = `hex(HMAC("csrf|" + 会话token))`（域分隔前缀避免与会话签名混用）、`validCsrfToken(String,String)`（`MessageDigest.isEqual` 恒时比对，null/空 fail-closed）。复用既有 private `hmac(byte[])`，未新造密码学代码。
- `PermissionInterceptor`：鉴权块后新增校验，`machineAuth` 标志仅在 accessToken **校验通过**时置位。**伪造 token 头不能绕过**——此时仍走 cookie 鉴权并要求 CSRF（有专门测试钉住）。
- 豁免面：安全方法（GET/HEAD/OPTIONS/TRACE，即非 POST/PUT/PATCH/DELETE）、无会话 cookie 的匿名请求（login）、机器调用。`logout` 虽 `@PermissionLimit(limit=false)` 但带 cookie，**同样要求 token**。
- 失败响应 `403`（与既有 302 风格一致，不写 body）。
- 新增 `CsrfTokenAdvice`（`@ControllerAdvice` + `@ModelAttribute("csrfToken")`），无 cookie 返回空串。

### 前端（仅 2 处单点，覆盖全部 26 个 POST 调用点）

- `templates/common/common.macro.ftl:63` commonScript 宏内联脚本新增 `var csrf_token = '${csrfToken!''}';`。9 个页面全部 import 该宏。
- `static/js/common.1.js:1-9` 顶层 `$.ajaxSetup({beforeSend})` 注入 `X-CSRF-TOKEN` 头。26 个调用点（11 `$.post` + 11 `$.ajax` + 4 DataTables）全部走 `jQuery.ajax`，一处覆盖；全仓原无 `$.ajaxSetup`/`beforeSend`，无覆盖冲突。
- token 为 hex（安全字符集），内联 JS 无 XSS 注入面。

### 验证

- `CsrfTokenProtectionTest` 10 例：无 token 头 403、正确 token 放行、伪造 token 403、**他会话 token 403**、GET 不校验、机器 token 豁免、**伪造机器 token 不绕过**、匿名 login 豁免、logout 要求 token、token 确定性/域分隔/null fail-closed。
- `CsrfTokenFrontendWiringTest` 5 例：advice 无 cookie 返空、有 cookie 返会话绑定 token、宏注入点在册、ajaxSetup 头名与 `LoginService.CSRF_HEADER` 一致（防漂移）、**FTL 模板 Freemarker 解析通过**（语法校验）。
- **Prove-It 反证**：把校验条件中和为 `if (false && ...)` 后 5 例失败（`/tmp/kdla_c3_proveit.log`），还原后全绿。
- 全量 617 例，16/16 SUCCESS，既有 job-application 24 例无回归。

**未实测项**：应用真实启动与浏览器端到端点击（需 DB，本地不连库）；`@ControllerAdvice` 与 Freemarker 视图渲染的运行时装配仅由模板解析测试与 advice 单测覆盖，not measured end-to-end。
