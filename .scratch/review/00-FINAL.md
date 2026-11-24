# kdla 2.0.0-SNAPSHOT JDK21 迁移修复 — 跨切片终审报告

BASE=`136bf03` → 评审 HEAD=`0fa5ab7`（16 commit）→ 修复 HEAD=`06864e1`（22 commit）→ **复审加固 HEAD=`a805b16`（23 commit）**。
6 路只读评审（A common-log / B infra / C business / D build / E security / F test-quality）+ 主代理亲自复核两个 Critical。

## 总体裁决：GO（2 个 Critical 簇 + 4 必改已修复，复审再加固 F-01 两处残留并复验）

用户裁定「修全部 Critical+必改」（C-1 / F-01 / A-01 / E-02 / E-03 / D1；Nit/Optional/FYI 不动）。
6 个独立 commit 修复完毕后，响应「review，有问题就修复」对本轮 6 提交做批判性复审，发现并修复 F-01 两处残留（第 7 commit `a805b16`）。
**全量 `mvn clean test` EXIT=0，16 模块全 SUCCESS，337 测试 0 失败 0 错误 0 跳过**
（= 评审基线 318 + cache 9 + validator 4 + common 3 + 复审加固 cache 3，精确吻合；log 增断言不增用例数）。

### 修复执行结果（本轮 6 commit，每切片只 stage 本任务文件，禁 add -A/push/amend）

| ID | commit | 修复 | 复验证据 |
|---|---|---|---|
| F-01 | `c1a5ce2` | 工厂委托 properties 消除分叉，全 5 模式真支持（SENTINEL 新实现 + MASTER_SLAVE/CLUSTER bug 修 + 脱敏），测试改打生产路径 | cache 72 绿（原 63，+9）；复审加固后 75（见下） |
| C-1 | `e71b316` | 建 validator `.imports` 注册 ValidatorAutoConfigure + 4 例装配回归测试 | validator 32 绿（原 28，+4） |
| A-01 | `b80e6d6` | 恢复 LocalIpHelp volatile IPv4 缓存字段+短路逻辑 + 3 例回归 | common 78 绿（原 75，+3） |
| E-02 | `b83df83` | LogSanitizeUtil 黑名单加 blade-auth + 回归断言 | log 38 绿 |
| E-03 | `6a684f9` | HttpContextHelp buildCors javadoc 如实说明 CustomCorsConfig 无白名单、需自定义 CorsFilter | 纯注释，全量编译通过 |
| D1 | `06864e1` | README 破坏性变更补登 jsqlparser/redisson/MpBaseDo/死依赖/snakeyaml（事实对提交核实） | 纯文档 |

> 评审时 318 绿**掩盖了两个 Critical**（C-1 测试直接 `new` 不走自动配置；F-01 测试全打死路径）。修复后回归测试改打**生产路径/装配路径**，绿≠正确的盲区已闭合。

### 复审加固（响应「review，有问题就修复」，第 7 commit `a805b16`）

主代理逐一复读本轮 6 提交全部主代码与测试，判定 A-01/C-1/E-02/E-03/D1 无问题、F-01 测试设计扎实（Prove-It 成立、不触网），但发现 **F-01 两处残留**并修复：

| 残留 | 根因 | 修复 | 回归测试（无修复则失败） |
|---|---|---|---|
| `redissons()` 缺 else 兜底 | type 为 null/未知时 5 个 equals 分支全落空→静默返回空 client 列表→`getLock` 构造零节点 `RedissonRedLock`→锁完全失效（正是 F-01 要消灭的 bug 类型却留了尾巴） | 加 `else{ throw new IllegalArgumentException("不支持的 kdla.redisson.type: " + type); }` | `RedissonsTest.nullType_failFast`（7→8） |
| `configMasterSlave`/`configCluster` 漏调 `setNettyThreads` | 用户配 `kdla.redisson.netty-threads` 对主从/集群静默失效（redisson Config 默认 32 恰等于字段默认 32，仅显式配置时暴露）；1.x 忠实拷贝的既有遗漏，F-01 激活这两条生产路径后才真正生效 | 两方法各补 `config.setNettyThreads(nettyThreads)` | `ClusterModeTest.masterSlave_honorsNettyThreads` + `cluster_honorsNettyThreads`（setNettyThreads(64) 断言 config.getNettyThreads()==64，无修复则默认 32≠64 失败）（4→6） |

- 注：type=null 经 `@ConfigurationProperties` 绑定非法枚举值会在启动期转换失败，故实际几乎不可达（仅编程式 setType(null)），属防御性收口。
- **实测**：`mvn -pl kdla-component-supplement-cache -am clean test` EXIT=0，cache 75 绿（72+3）；全量复验 337 绿（334+3），日志 `/tmp/kdla_final_full2.log`。
- 取证教训：本轮 D1 文档核实曾误判 MpBaseDo（链式 grep 输出误归属），隔离单命令后澄清——MpBaseDo 1.x 存在、2.x 已删、0 引用，记录正确。

---

## Critical（阻断发布）— 均已修复

### C-1 validator 自动配置未注册，CommonValidator 恢复对下游失效 ✅ 已修复 `e71b316`
- **证据**：`kdla-component-validator/src/main/java/top/kdla/framework/validator/config/ValidatorAutoConfigure.java` 类体已恢复（`@Configuration @ConditionalOnClass(Validator.class) @ConditionalOnProperty(kdla.config.validator.enabled, matchIfMissing=true)` + `@Bean commonValidator()`）。
- 但 validator 模块**无 `src/main/resources` 目录**→无 `.imports`、无 `spring.factories`。全仓 `grep -rl ValidatorAutoConfigure --include=*.imports` **0 命中**。
- 1.x 靠 `spring.factories`（`EnableAutoConfiguration=top.kdla.framework.validator.config.ValidatorAutoConfigure`）注册；本轮删 spring.factories（Boot3 不认）却**漏建 validator 的 .imports**。
- `CommonValidator` 无 `@Component`（纯类）→下游 `@Autowired CommonValidator` 启动即 `NoSuchBeanDefinitionException`。
- **甲类 API 恢复只完成一半**。318 绿暴露不了（`CommonValidatorTest` 直接 `new CommonValidator()`）。
- **修复**：建 `kdla-component-validator/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，内容 `top.kdla.framework.validator.config.ValidatorAutoConfigure`；补装配回归测试（`ApplicationContextRunner` 验证 bean 注册 + `kdla.config.validator.enabled=false` 关闭）。
- **连带 O-1（Optional，未修）**：`CommonValidator` 实例初始化块急建 `Validation.buildDefaultValidatorFactory()`，`@ConditionalOnClass` 守的是 `jakarta.validation.Validator`（API）不是实现。C-1 修复后，只有 validation-api 无 hibernate-validator 的下游会启动炸。建议守实现类或懒加载（factory 未 close 为已知 L8）。
- **✅ 实施 `e71b316`**：建 `.imports`（内容 `top.kdla.framework.validator.config.ValidatorAutoConfigure`，无末尾换行匹配 10 模块约定）；新增 `ValidatorAutoConfigureRegistrationTest` 4 例：①读 classpath `.imports` 资源断言含 FQN（**无文件则 null 失败，Prove-It**）②默认开启注册 CommonValidator ③`enabled=false` 无 bean ④宿主自定义 bean 时 `@ConditionalOnMissingBean` 退让。validator 32 绿。O-1 仍 Optional 未修（用户裁定不动）。

### F-01 簇 cache 锁修复打进死类，活路径 KRedissonLockFactory 漏修（= B-01/B-02/E-01/F-02 同根因）✅ 已修复 `c1a5ce2`
- **活路径**：`KRedissonAutoConfigure.distributeLockFactory():74-88`（`@Bean("distributeLockFactory") @ConditionalOnMissingBean(KDistributeLockFactory.class)`）→ `new KRedissonLockFactory(this.config, prefix)`。这是生产唯一锁工厂。
- **`KRedissonLockFactory.java` 本轮未被触碰**（不在 BASE→HEAD diff，git stat 实证）。其 `redissons():46-67`：
  - SINGLE：`configSingleNode(getAddress(), ...)` 无 trim/空串校验。
  - REPLICATE：`passwords[index]` 无数量校验→密码数<节点数抛 `ArrayIndexOutOfBoundsException`。
  - **MASTER_SLAVE/CLUSTER/SENTINEL：无任何分支→静默返回空 client 列表**→`getLock():136` 构造 `new RedissonRedLock(new RLock[0])` **零节点 RedLock，分布式锁完全失效**。
  - `configSingleNode():99`：`log.info("inti the redisson client with config: {}", config.toYAML())`→**redis 密码明文进 INFO 日志**（影响所有模式，含默认 REPLICATE）。
- **死路径**：本轮 #7a 把全部修复打进了 `KRedissonConfigProperties.redissons():101-139`（splitNodes fail-fast / REPLICATE 密码数量校验 / MASTER_SLAVE/CLUSTER 分支 / SENTINEL UnsupportedOperationException / configSingleNode 只记非敏感项）。但工厂只调 properties 的 **getter**，从不调它的 `redissons()`→**全仓零生产调用方，仅测试用**。
- **测试假绿**：`RedissonsTest`(6) + `KRedissonConfigPropertiesPasswordTest`(3) + `SplitNodesTest`(5) 全打死路径；`LockPrefixTest:20` 注释明言利用该 bug 避免触网。
- **1.x 工厂基线**（`git show 1.0.1-SNAPSHOT:.../lock/RedissonLockFactory.java`，旧名无 K 前缀）：`redissons()` 仅支持 SINGLE/REPLICATE，**其余类型 fail-fast `IllegalArgumentException("仅支持SINGLE/REPLICATE")`**；`configSingleNode` 只记 `address/codec/threads`（无 toYAML）。
- **⚠ 语义冲突 → 用户裁定**：死 `properties.redissons()` 支持 MASTER_SLAVE/CLUSTER（且 configMasterSlave 把同一 address 既设 master 又 addSlave、setFailedSlaveCheckInterval 调两次，本身有 bug），而 1.x 活工厂只支持 SINGLE/REPLICATE 其余 fail-fast。评审推荐方案 A（按 1.x fail-fast）/ 方案 B（委托但继承 bug）。**用户裁定自定义答案：「单个、哨兵、集群都要支持啊」——要求全部 5 种模式真正可用，既非 A 也非 B。**
- **✅ 实施 `c1a5ce2`（按用户裁定，全 5 模式真支持）**：
  - 工厂 `KRedissonLockFactory.redissons()` 改为**委托** `redissonConfig.redissons()`，**消除死/活双实现分叉（根因）**；删工厂孤儿 `configSingleNode`/`getCodecInstance` + 8 import（139→71 行）。原死 properties 升为**唯一生产路径**（不再死代码，无需"先问再删"）。副作用：工厂 `config.toYAML()` 密码明文 INFO 日志自然消除；顺带修掉工厂 `setSubscriptionConnectionPoolSize(getConnectionPoolSize())` 用错字段旧 bug。
  - properties 全 5 模式：新增 `masterName` 字段（键 `kdla.redisson.master-name`，SENTINEL 必填，空白 fail-fast）；**SENTINEL 新实现** `configSentinel()`（`useSentinelServers().setMasterName+addSentinelAddress`，全仓此前从未实现）；**configMasterSlave 修复**（splitNodes 首=master 余=slave，删重复 setFailedSlaveCheckInterval）；**configCluster 修复**（删重复 setFailedSlaveCheckInterval）；所有 config* 日志脱敏（仅节点数/codec，无密码无 toYAML）。
  - 测试改打生产路径：`LockPrefixTest` 重设计为 mock properties（不再依赖零节点 bug 避触网）；新增 `KRedissonLockFactoryTest`（委托+前缀+fail-fast 传播 3 例）、`KRedissonConfigPropertiesClusterModeTest`（反射测 master-slave/cluster/sentinel Config 构建 4 例）；RedissonsTest sentinel fail-fast 拆 blank-master-name+blank-address；PasswordTest +sentinel 例。cache 72 绿（原 63，+9）。
  - redisson 3.52.0 API 实证：`mvn compile` EXIT=0 证 `useSentinelServers().setMasterName/addSentinelAddress` + MasterSlave setter 真实存在；`setFailedSlaveCheckInterval` deprecated 保留单次调用（FYI）。

---

## 必改（强烈建议发布前修，均小 diff）— 均已修复

| ID | 路径:行号 | 问题 | 修复 | 核实 |
|---|---|---|---|---|
| A-01 ✅`b80e6d6` | common/help/LocalIpHelp.java:184-197 | 1.x `localIp4AddressCache` volatile 缓存（traceId 每请求热路径，网卡枚举/socket 探测开销大）被 2.x 删除，本轮未恢复（1.x→HEAD -16 行，本轮 0 触碰） | 恢复 volatile 缓存字段 + 缓存逻辑（jakarta import 已就位）；3 例回归（volatile/哨兵短路/首调填充） | 主代理 diff 实证，common 78 绿 |
| E-02 ✅`b83df83` | log/sanitize/LogSanitizeUtil.java:27-28 | 脱敏黑名单={authorization,cookie,password,token,mobile}，**漏框架自有认证头 `blade-auth`**→RequestLogAspect 全量打印请求头时 JWT 明文进日志可重放 | 黑名单加 `blade-auth`（忽略大小写已就位）+ 回归断言 | 主代理读全文实证，log 38 绿 |
| E-03 ✅`6a684f9` | supplement/http/HttpContextHelp.java:97-99 + cors/CustomCorsConfig.java:34-35 | 本轮新增 javadoc 推荐下游迁移到 CustomCorsConfig"配 origin 白名单"，但该类硬编码 `setAllowCredentials(true)+setAllowedOriginPatterns(ALL)` **无白名单配置项**→误导性安全指引 | 修正 javadoc：如实说明 CustomCorsConfig 同样 ALL+credentials 无白名单项，生产须自定义 CorsFilter bean（@ConditionalOnMissingBean 退让）；未扩 scope 改 CustomCorsConfig | 主代理读两类实证，纯注释 |
| D1 ✅`06864e1` | README.md:91-97 | 破坏性变更章节仅登记 xxl-job/JwtUtil，缺 jsqlparser 自管、redisson 3.20.1→3.52.0、MpBaseDo 删除、14 死依赖移除、snakeyaml compile 块删除 | 补登 5 子节（SENTINEL master-name/MASTER_SLAVE 首=master 并入 redisson 节）；事实对 c1422ae/0fa5ab7/c1a5ce2 + 1.x 基线核实（MpBaseDo 1.x 存在 2.x 删除 0 引用、BaseDo 保留可替代） | 主代理 git 核实，纯文档 |

---

## Nit / Optional / FYI（只列不改，先问再删死代码）

**本轮新引入 Nit**：
- N-1 `exception/UnifiedExceptionControllerAdvice.java:23` 残留未用 `WebRequest` import（bcf5d7a 加入、af9bb6c 恢复后成孤儿）。
- D2 `job/.../XxlJobConfig.java:52` 生效键拼成 `xxl.enabled.xxl`（prefix+name 错配），本轮新注册使该守卫首次入链；2.x 既有源码、not measured，建议 job 切片核对。
- F-05 `UnifiedExceptionControllerAdviceTest:51` nullEncoding 用例只证不抛 NPE，未验 UTF-8 解码内容。
- F-06 `ThreadPoolHelp` 两测试 `remainingCapacity() <= 1024` 弱断言，应 `assertEquals(1024)`。
- F-08 `ObjectValidatorTest:131` 恒真用例（import 是编译期问题，运行时断言必绿）。
- 部分 .imports/JwtUtil/AopTargetHelp 文件末尾缺换行。

**Optional**：
- O-1 CommonValidator 急建 ValidatorFactory + @ConditionalOnClass 守 API 非实现（见 C-1 连带）。
- E-04 trd 5 控制器+5 VO `isNotEmpty(bladeAuth)` 守卫：省略头即跳过验签（BASE 既有，依赖网关鉴权收敛，需下游确认）。
- E-05 `trd/StringUtil.hashString:73-75` 失败静默返回原文（fail-open 哈希，仓库内 0 调用）。
- D3 vertx-codegen 遗留死依赖（根 DM + 4 模块 provided，全仓 0 使用），不在本轮 14 项内，只列不删。
- B O-02 工厂委托 properties 消除分叉——**已随 F-01 `c1a5ce2` 实施**（用户裁定全 5 模式真支持，非原方案 B 的"继承 bug"，bug 已一并修复）。

**FYI（1.x 既有 / 刻意保留 / 已知候选，确认无误）**：
- **RedLock 保留决策正确**：B/E/F 三切片一致确认 `RedissonRedLock` 任何 3.x 未移除仅 deprecated，quorum 语义保留正确，无需改 MultiLock。`failedSlaveCheckInterval` 弃用（建议 failedSlaveNodeDetector）记 FYI。
- **JwtUtil fail-closed 闭合**：E/F 实证 alg=none/空 token/错钥/过期/缺钥全拒，7 用例覆盖，无绕过。密钥编码 raw UTF-8 vs jjwt0.9 base64 仍 not measured（已文档化）。
- F-1 JwtUtil 调用点 blade-auth 头 required=false 省略即匿名放行——1.x 既有设计非本轮引入（与 E-04 同）。
- 已登记候选确认：trd StringUtil.transform2 value null NPE / replaceUrl 空 catch（E 确认 URL 编码后无裸 $/\，降级 Nit）；RequestLogAutoConfigure/PrometheusAutoConfigure 死 import；PrometheusPointAspect 注释反射死代码；dingding DingDingHttpUtil 整文件注释死代码；KRedissonRedDisLock.SHADOW_KEY 未用常量；VertxHttpConfigure 跨 4 模块 .imports 重复注册（Boot 去重无害）；README:3 仍写"依赖JDK1.8"；.agents/AGENTS.md ".imports不存在"/"2.x无测试"记录过时；C/L4 LocalDateTimeHelp.DEFAULT_ZONE 未用；C/L5 DateHelp.format 每次 new ThreadLocal；MqttClientImpl.selfCreatedEventLoop 非 volatile；N-4 IPageQuery limit=0 透传与 PlusPageQuery >0 回退不一致。

**核对通过（无缺陷）**：8 模块 javax 活代码 0 残留（job-application 恰 33 处全改，javax.sql/javax.net.ssl 保留）；md5/token/password 全显式 UTF-8；BaseAssert.notBlank/MultiResponse.totalCount/exception advice/dal/domain 与 1.x 逐行一致；getByPCodes keep-first 正确；SqlFilter 白名单防注入正确；AES/SM4 随机 IV+v1 兼容无降级面；SslContextFactory 无 trust-all、Vertx trust-all 默认 false；异常外泄收口+Error 重抛；15 模块版本全 2.0.0-SNAPSHOT、provided 约定、无成环、compiler release21+binding 顺序正确（trd 7 Impl 静态一致）、MP boot3-starter/redisson-34/jsqlparser 收敛、14 死依赖+javax 三件套 0 残留、easyexcel exclusion 保留、19 auto-config 类全存在守卫齐备、.gitignore 清理正确；约 95 个测试文件真 Prove-It、Netty 坑处理正确、无真实外部服务、trd 不越界、移植保真。

---

## 修复执行完毕 — 剩余开放项（用户裁定不修，只登记）

**已完成**：C-1 / F-01 / A-01 / E-02 / E-03 / D1 共 6 commit（`c1a5ce2` `e71b316` `b80e6d6` `b83df83` `6a684f9` `06864e1`）+ 复审加固 F-01 两处残留 1 commit（`a805b16`），共 7 commit；每切片独立 commit、只 stage 本任务文件、禁 add -A/push/amend；主代理逐切片 mvn 复验 + 审 diff，最终全量 `mvn clean test` EXIT=0、16 模块 SUCCESS、337 绿。

**仍开放（Nit/Optional/FYI，按用户裁定不动）**：
- N-1 孤儿 WebRequest import；D2 `xxl.enabled.xxl` 键错配（not measured）；F-05/F-06/F-08 弱断言/恒真用例；文件末尾缺换行。
- O-1 CommonValidator 急建 ValidatorFactory + @ConditionalOnClass 守 API 非实现（C-1 连带，仍 Optional）。
- E-04 trd blade-auth `required=false` 省略即匿名（1.x 既有，依赖网关收敛）；E-05 StringUtil.hashString fail-open（0 调用）；D3 vertx-codegen 死依赖。
- FYI：`failedSlaveCheckInterval` deprecated（建议 failedSlaveNodeDetector）；JwtUtil 密钥编码 raw UTF-8 vs jjwt0.9 base64 仍 not measured（已文档化，接入前须真实 token 验证）；README:3 仍写"依赖JDK1.8"（D1 scope 外，未触碰）；`.agents/AGENTS.md` ".imports不存在"/"2.x无测试"记录过时；其余死代码候选见上 FYI 段（先问再删）。
- 注：原 B O-02「工厂委托 properties 消除分叉」已随 F-01 实施（用户裁定全模式支持），不再开放。

**发布前下游须知的破坏性变更**已全部登记 README「2.0.0 破坏性变更」（xxl-job 包名 / JwtUtil 强制验签 / jsqlparser 自管 / redisson 升级+SENTINEL master-name+MASTER_SLAVE 首=master / MpBaseDo 删除 / 死依赖+javax 三件套 / snakeyaml compile 块移除）。

**裁决：GO。** 两个 Critical 簇与 4 必改已修复并以生产路径/装配路径回归测试闭合「绿≠正确」盲区；复审再加固 F-01 两处残留（null/未知 type fail-fast + masterSlave/cluster nettyThreads），全量 337 绿。剩余 Nit/Optional/FYI 不阻断发布，按用户裁定登记待挑选。git push 由用户人工执行。