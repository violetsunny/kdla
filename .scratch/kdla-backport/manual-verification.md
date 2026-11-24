# 人工验证清单（票 16）— 交用户执行回贴

> 环境标注：[测试]=测试环境/本地宿主执行；[生产]=生产环境执行。全部验证项默认在 [测试] 执行，V8 试点通过后方可推 [生产]。
> 预期值已按 T 装配矩阵调整（T 默认开：Mdc/StopWatch/CatchLog/exception advice/Validator/Domain/Redisson/Vertx/VertxHttp/FieldValueFind/DingAlert/Prometheus；默认关：RequestLog/LogInterceptor/WebFilter/Cache/RestHttp/MQTT/CORS/SequenceNo）。
> **回贴前不宣布回合完成。** 每项回贴格式：`V#: PASS/FAIL + 一行证据（日志/截图/命令输出）`。

## V1-V8（源自 R 审计，预期值按 T 矩阵更新）

| # | 验证项 | 环境 | 步骤（一行可执行） | 预期/判定标准 |
|---|---|---|---|---|
| V1 | Redisson 地址 fail-fast | [测试] | 宿主 classpath 含 redisson、`kdla.cache.redisson.address` 留空或填 `abc`（T 默认 `kdla.cache.redisson.enabled=true` 无需显式开），`java -jar app.jar` 启动 | 启动即抛 `IllegalArgumentException` 且消息指明 address 问题；不静默产出零节点 client |
| V2 | 真实 Redis 锁互斥 | [测试] | 两实例（`app.id` 一致）同时调同一 `@DistributeLocked(lockKey="v2test")` 方法（并发脚本或两终端 curl）；旁证：`redis-cli --scan --pattern '*v2test*'` | 同一时刻仅一实例进入业务；另一实例抛 `LockFailException`；锁 key 带 appId 前缀（app.id 缺失回落 `kdla`） |
| V3 | sequence 并发无重复 | [测试] | 宿主配 `kdla.sequence.enabled=true` + `kdla.sequence.lock.key.prefix`（三键任一 true 即启用，联动 Redisson 装配），≥50 并发调 `getSequenceNo`（JMeter 或 50 线程 main），`SELECT code,value FROM code_generator_cfg` 比对 | 无重复号；溢出/长度段非数字抛 `BizException`，不静默清零重发 |
| V4 | NettyHttpServer 端口占用与异常响应 | [测试] | ① `nc -l 9090` 先占端口再启动 NettyHttpServer(9090) ② `curl http://localhost:9090/<抛异常handler>` | ① log.error + `shutdownGracefully`，进程不僵死，端口释放后可复用 ② 响应固定文案，不含堆栈 |
| V5 | MQTT eventLoop 无线程泄漏 | [测试] | `MqttClientImpl` connect→disconnect 循环 10 次后 `jstack <pid> \| grep -ci eventloop`，与循环前基线比较 | 线程数回落至基线，无累积 |
| V6 | Vertx HTTPS 默认策略 | [测试] | ① 默认（不配 trust-all）用 VertxHttpClient 请求自签证书端点 ② 配 `kdla.http.ssl.trust-all=true` 再请求 | ① 证书校验失败拒绝连接 ② 请求成功且日志出现 MITM 风险 `log.warn` |
| V7 | VertxHttpClient 行为 | [测试] | ① 请求头 `Content-Type: APPLICATION/JSON`（大写）发 POST ② 请求 sleep 35s 的端点 ③ 回调内抛 RuntimeException | ① JSON 正常解析 ② 30s（`kdla.http.request-timeout` 默认 30000ms）超时 future 完成为失败 ③ 异常经 future 传播不吞 |
| V8 | 宿主试点接入 | [测试]→通过后[生产] | 宿主引入 `1.0.1-SNAPSHOT`，**不加任何 kdla.\* 配置**启动，开 `logging.level.org.springframework.boot.autoconfigure=DEBUG` 或 `/actuator/conditions` 查装配报告 | **应装配**：Mdc/StopWatch/CatchLog/exception advice(SERVLET)/Validator/Domain/Redisson(有 redisson 库)/Vertx/VertxHttp(有 vertx 库)/FieldValueFind/DingAlert(有 vertx 库)/Prometheus(有 prometheus 库)；**不应装配**：RequestLog/LogInterceptor/WebFilter/Cache/RestHttp/MQTT/CORS/SequenceNo；启动不炸、无 UnsatisfiedDependency |

## 新增 6 项（A5 裁定）

| # | 验证项 | 环境 | 步骤（一行可执行） | 预期/判定标准 |
|---|---|---|---|---|
| N1 | Sm4/AES `v1:` 前缀混部新旧互解 | [测试] | 新版 `AESUtil.encrypt`/`Sm4Util.encrypt` 产出密文（应带 `v1:` 前缀）交旧版（升级前 jar）`decrypt`；旧版存量密文交新版 `decrypt` | 新密文旧代码**不可解**（明确报错非乱码）——混部窗口必须先升消费方或停写新密文；旧密文新代码兼容可解；Sm4 存量 Hex 编码密钥不可读（需换 Base64 或走 byte[] 重载） |
| N2 | Vertx trust-all 默认 false 自签证书行为 | [测试] | 同 V6（合并执行，回贴 V6 即可） | 同 V6 |
| N3 | request-timeout 30s 生效 | [测试] | 同 V7②（合并执行，回贴 V7 即可） | 同 V7② |
| N4 | Redisson 地址 fail-fast 启动失败 | [测试] | 同 V1（合并执行，回贴 V1 即可） | 同 V1；⚠️ T 默认开 Redisson——宿主有 redisson 库但从未配 address 的，升级后启动即失败，试点（V8）时重点观察 |
| N5 | Response 保留 success 字段序列化不变 | [测试] | `curl -s http://localhost:<port>/<任一正常接口>` 看 JSON；或宿主单测 `new ObjectMapper().writeValueAsString(Response.buildSuccess())` | 输出含 `"success":true` 字段（B1 否决，与 R 构件不一致）；下游反序列化行为与升级前完全一致 |
| N6 | 钉钉告警链路（VertxHttp 联动） | [测试] | 宿主配 `DingAlertProperties`（webhook/secret）后调 `DingDingService` 发文本告警；前提 classpath 有 vertx（DingAlertConfigure OnClass(Vertx) 装配） | 钉钉群收到消息；`DingAlertConfigure` 装配报告 matched（V8 报告中旁证）；无 vertx 库的宿主不装配不报错 |

## 待用户裁定项（非验证，随回贴一并答复）

- **P1**（`MachineIdHelp` 注册失败脏值残留撞号风险）：**已裁定并修复**（2026-11-16 用户选最小修复，commit 30490b8：注册成功才发布静态字段，失败路径保持 null 走随机回退+ERROR 告警；+2 回归测试全绿）。R 侧未修。无需随回贴答复。

## 回贴模板

```
V1: PASS/FAIL — <一行证据>
V2: ...
V3: ...
V4: ...
V5: ...
V6: ...（含 N2）
V7: ...（含 N3）
V8: ...（含 N4 观察）
N1: ...
N5: ...
N6: ...
```
