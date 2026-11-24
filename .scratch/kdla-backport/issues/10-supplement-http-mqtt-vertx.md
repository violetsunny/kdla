# 10: supplement 模块 http/mqtt/netty/vertx 回合

**What to build:** VertxHttpConfigure：trust-all 硬编码 true→kdla.http.ssl.trust-all 默认 false（开启时 log.warn MITM，B11）、新增 kdla.http.request-timeout 默认 30s（钉钉四处无超时 futureRes.get() 受益）、content-type 统一小写匹配、回调异常改走 future。新增 VertxConfigure 统一提供 Vertx Bean（OnClass(io.vertx.core.Vertx)+COBM），VertxHttp/Mqtt 复用，与 T dingding 跨模块注册 VertxHttpConfigure 核对去重。NettyHttpClient：端口优先 URL 显式值、缺省按协议 80/443、connectTimeout 默认 3000。NettyHttpServer：启动即关闭修复、bind 失败 shutdownGracefully+close 显式释放、异常响应固定文案不泄漏堆栈。MQTT：MqttPingHandler 心跳超时判断反转、MqttChannelHandler/MqttClientImpl QoS2 ByteBuf retain/release 配对、MqttClientImpl 自建 eventLoop disconnect 时关闭。BaseHttpClient.post headerMap null 安全。FieldValueFindConfigure 注册+FieldValueFindHelp 移除 @Component+break 误用修正。KdlaExcelWrite try/finally finish()。装配：rest-http（NettyHttpServer）/VertxMqttConfigure/CorsConfigure 注册且默认关（矩阵 11.3）；VertxHttpConfigure 保持默认开（矩阵 11.1，dingding 联动关键点）。合并 09 审计归属本票的补项。

**Blocked by:** 09

**Status:** done

依据：investigation.md 第 3 节 18/21/26/58/59/60/61/83/88 项、第 11 节矩阵、B11 裁定、R openspec 无（32ba8d2/bb8022f/9cdbe79/6ff6775/1fa3289）。

- [x] Vertx/Netty/Mqtt/Excel/FieldValue 全部修复落地
- [x] VertxConfigure 新增且与 dingding 注册去重核对（Spring 同类自动去重，验证无双 Vertx Bean）
- [x] 装配矩阵 11.1（VertxHttp 默认开）/11.3（rest-http/mqtt/cors 默认关）落地
- [x] supplement pom 补 test 依赖；R 的 supplement 测试回合+审计补项测试
- [x] `mvn -pl kdla-component-supplement -am clean compile` 与 test 绿
- [x] 独立 commit（可按 http/mqtt 分多个），不 push

## Comments

- 2026-11-14 票 09 审计并入 8 项差额（详见 audit-supplement.md，证据含路径:行号）：
  1.【高】CsvUtil.parseCsv 无限 Stream.iterate，任何输入必抛越界，功能全废
  2.【中】HttpContextHelp trust-all 无告警+废弃 "SSL" 协议+buildCors 任意 Origin 反射无告警+ignoreSSL/buildCors 补 @Deprecated 迁移告警
  3.【中】SslContextFactory 4 处流泄漏+`throw new Error` 绕过宿主异常处理
  4.【低】NettyHttpClient 无 close()+AtomicBoolean 双 shutdown 竞态
  5.【高】MqttChannelHandler CONACK 未知码 connectFuture 永久悬挂+PUBREC/PUBCOMP 不判空 NPE 杀 event loop（R 工作区未提交修复，回合时逐项复核）
  6.【中】MqttClientImpl 4 个 HashSet/HashMultimap 双线程并发读写（CME/丢更新/扩容死循环）（R 工作区未提交）
  7.【高】NettyHttpClient setSsl 事后无效+get() 无超时永久挂起+https 默认路径 NPE（R 工作区未提交）
  8.【中】NettyHttpServer.writeHttpResponseHtml CONTENT_LENGTH 恒 0（keep-alive 协议流错位）+FileRegion fd 泄漏（R 工作区未提交）

- 2026-11-14 完成。commit a459911。验证：`mvn -pl kdla-component-supplement -am clean test` BUILD SUCCESS，supplement 4 测试类 7 用例全绿（上游 model/common/exception/domain 同跑全绿）。
- B 组复核（关键裁定）：R 工作区 `git status` 确认 5 文件未提交修复在位（HttpContextHelp/MqttChannelHandler/MqttClientImpl/NettyHttpClient/NettyHttpServer），移植源一律取 R 工作区现状（非 HEAD）。
- 分叉判定：mqtt/netty 包 9 文件 FORKED，逐一核验分叉全部仅为 import 风格差异（T 通配符 vs R 基线展开式），零语义分叉 → 有 R 改动的 3 文件（MqttChannelHandler/MqttClientImpl/MqttPingHandler）整覆盖 R 工作区；无 R 改动的 6 文件（MqttClientConfig/MqttPending*/MqttSubscription/RetransmissionHandler）不动。其余 14 文件 T==R 基线 → 整覆盖。VertxConfigure+spring.factories+4 测试纯新增。pom 整覆盖（唯一差异=test 块）。
- **netty API 适配（T 4.1.101 vs R 更新版，保持 T 依赖策略不升版本）**：NettyHttpServer.writeHttpResponseHtml 两处——① `headers().setLong(AsciiString,long)` 不存在 → `set(CONTENT_LENGTH, fileLength)`；② `DefaultFileRegion(FileChannel,long,long,boolean closeOnComplete)` 四参构造不存在 → `DefaultFileRegion(File,long,long)` 三参 File 构造（netty deallocate 时自关通道，fd 泄漏修复语义等价）；raf 仅 fail-fast+取长度后立即 close。修复意图（CONTENT_LENGTH 真实值+fd 泄漏）均保留。
- 装配落地：spring.factories 新增注册 6 项；VertxHttpConfigure/VertxConfigure/FieldValueFindConfigure 按矩阵 11.1/11.2 改 matchIfMissing=true（R 均 false，偏离记录票 15 进 CHANGELOG）；RestHttpConfigure/VertxMqttConfigure/CustomCorsConfig 保持默认关（矩阵 11.3）。dingding 跨模块重复注册 VertxHttpConfigure 去重核对：Spring Boot 自动配置按类名去重+全部 @Bean 带 COBM，无双 Vertx/双 client bean 风险。
- 测试回合 4 类零适配（无 nonce 语境，已扫描）：KdlaExcelWriteCloseTest/BaseHttpClientPostNpeTest/MqttChannelHandlerQos2RefCntTest/NettyHttpClientPortTest。
