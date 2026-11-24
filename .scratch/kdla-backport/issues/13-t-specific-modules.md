# 13: T 特有 4 模块装配核对（dingding/monitor/reactor/job）

**What to build:** 按 S2 裁定与装配矩阵核对 T 特有 4 模块：dingding 的 DingAlertConfigure 补真实 @ConditionalOnClass（钉钉 SDK 类）保持默认开；dingding spring.factories 跨模块注册的 VertxHttpConfigure 保持默认开，并与票 10 新增的 VertxConfigure/VertxHttpConfigure 自身注册核对去重（Spring 同类自动去重，验证无双 Vertx Bean、钉钉告警链路 DingDingServiceImpl→VertxHttpClient 不断）。monitor 的 PrometheusAutoConfigure 补真实 @ConditionalOnClass（prometheus simpleclient）保持默认开；死 import（StopWatchWrapperAspect）只登记不动手。reactor/job 无 spring.factories 不动；job pom 冗余依赖（声明 7 模块代码零使用）只登记不动手。

**Blocked by:** 10

**Status:** done

依据：investigation.md 第 7 节、第 11.1 节矩阵、S2 裁定、无需裁定第 4 条。

- [x] dingding/monitor 两个 Configure 补真实 OnClass，默认开不变
- [x] VertxHttpConfigure 注册去重核对完成，装配说明落盘 .scratch/kdla-backport/verify-13.md（含 dingding 链路完整性结论）
- [x] reactor/job 零改动；job pom 冗余与 monitor 死 import 登记入 nit-inventory.md
- [x] 4 模块 `mvn -pl <module> -am clean compile` 全绿（LSP Lombok 误报不计）
- [x] 独立 commit，不 push

## Comments（2026-11-16 执行）

- **Commit**: 见 git log `kdla 1.0.1-SNAPSHOT and upgrade supplement-dingding/monitor autoconfigure real OnClass conditions`（仅 2 个 java 文件；.scratch 黑名单不入库）
- **验证**: `mvn -pl kdla-component-supplement-dingding,kdla-component-supplement-monitor,kdla-component-reactor,kdla-component-supplement-job -am clean compile` BUILD SUCCESS（pipefail 严格核验 EXIT=0）
- **适配决策 1（票面前提与事实不符）**: 票面"钉钉 SDK 类"不存在——dingding 模块零钉钉 SDK 依赖（实际外部依赖 vertx/fastjson/hutool/httpclient，均 provided）。按 S2 裁定意图（真实外部类+联动语义）采用 `@ConditionalOnClass(io.vertx.core.Vertx)`：钉钉告警链路 DingDingServiceImpl→VertxHttpClient 的真实硬依赖，与 VertxConfigure/VertxHttpConfigure 的 OnClass 语境一致。默认开不变（未加开关，同 T 现状）。
- **适配决策 2**: monitor 恒真 `OnClass(PushGatewayManager.class)`（内部类）→ 真实 `{CollectorRegistry.class, PushGateway.class}`（simpleclient/simpleclient_pushgateway 均 provided）。HTTPServer（simpleclient_httpserver）仅 service.switch=true 分支使用，不纳入条件。默认开不变。
- **去重结论**: VertxHttpConfigure 双注册点（supplement 自注册 + dingding 跨模块注册）经 AutoConfigurationImportSelector.removeDuplicates 按 FQCN 去重只生效一次，@Bean 均带 COBM(name) 双保险；Vertx bean 唯一来源 VertxConfigure。无双 Bean，dingding 链路不断。详见 verify-13.md。
- **登记不动手**: nit-inventory.md 新增 T 特有模块节 T1（StopWatchWrapperAspect 死 import，票面点名）、T2（EnableAspectJAutoProxy 死 import，执行期新发现）、T3（job pom 7 内部模块依赖零使用）。
- **reactor/job**: 实测均无 META-INF/spring.factories，零改动。
- LSP 误报（DingAlertConfigure Lombok getter 幻影重复、validator 测试 junit/mockito 解析失败）按硬约束 5 忽略，以 mvn 为准。
