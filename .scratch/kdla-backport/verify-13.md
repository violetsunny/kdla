# 票 13 装配核对说明（T 特有 4 模块，2026-11-16）

依据：investigation.md 第 7 节、第 11.1 节装配矩阵、S2 裁定。

## 1. 改动

| 配置类 | 改动 | 默认开不变 |
|---|---|---|
| dingding DingAlertConfigure | 补 `@ConditionalOnClass(Vertx.class)` | 是（无 @ConditionalOnProperty，同 T 现状）|
| monitor PrometheusAutoConfigure | 方法级恒真 `@ConditionalOnClass({PushGatewayManager.class})`（内部类）替换为真实 `{CollectorRegistry.class, PushGateway.class}` | 是（无开关，同 T 现状）|
| reactor / job | 零改动（均无 spring.factories，实测确认无 META-INF）| — |

### 适配决策（票面前提与代码库事实不符）

1. **DingAlertConfigure 的 OnClass 类**：票面写"钉钉 SDK 类"，实测 dingding 模块无任何钉钉 SDK 依赖（`com.dingtalk`/`com.taobao` 零 import；实际外部依赖为 vertx-core/vertx-web-client/fastjson/hutool/httpclient/jackson，均 provided）。钉钉告警链路 DingDingServiceImpl→VertxHttpClient→WebClient 的真实硬依赖是 vertx，且与 S2 裁定"联动注入保持默认开+真实 OnClass(Vertx)"语境一致 → 采用 `io.vertx.core.Vertx`。
2. **PrometheusAutoConfigure 的 OnClass 类**：矩阵写"prometheus simpleclient"。PrometheusPointAspect 链路真实运行期依赖 = PushGatewayManager（@Autowired 参数），其硬依赖 CollectorRegistry（simpleclient）+ PushGateway（simpleclient_pushgateway，@PostConstruct new）。HTTPServer（simpleclient_httpserver）仅 `prometheus.service.switch=true` 分支使用，不纳入装配条件。故取 `{CollectorRegistry, PushGateway}` 两类。

## 2. VertxHttpConfigure 注册去重核对

- 注册点 1：`kdla-component-supplement/src/main/resources/META-INF/spring.factories`（票 10 新增，自模块注册）
- 注册点 2：`kdla-component-supplement-dingding/src/main/resources/META-INF/spring.factories`（T 原有，跨模块注册，保留不动）
- 去重机制：Spring Boot `AutoConfigurationImportSelector` 经 `removeDuplicates`（LinkedHashSet）按类全名去重，两处注册同一 FQCN 只生效一次；且 VertxHttpConfigure 内 3 个 @Bean 均带 `@ConditionalOnMissingBean(name=...)` 双保险。
- **结论：无双 VertxHttpConfigure 实例、无双 VertxHttpClient/webClient/webClientHttps Bean。**
- Vertx Bean 唯一来源：VertxConfigure（票 10 新增，`@ConditionalOnMissingBean(Vertx.class)`）；VertxHttpConfigure 的 webClient(Vertx) 经方法参数注入获取，跨配置类由容器解析，无顺序问题。无双 Vertx Bean。

## 3. dingding 告警链路完整性结论

链路：DingDingServiceImpl(@Service) → @Resource VertxHttpClient → VertxHttpConfigure(@ConditionalOnClass(WebClient) + matchIfMissing=true 默认开) → webClient/webClientHttps(Vertx) → VertxConfigure(@ConditionalOnClass(Vertx) + matchIfMissing=true 默认开)。

- 下游有 vertx 依赖（T 典型场景）：全链路装配，钉钉告警可用，**链路不断**。
- 下游无 vertx 依赖：VertxConfigure/VertxHttpConfigure/DingAlertConfigure 三者一致不装配（OnClass 联动语义自洽）；DingDingServiceImpl 若被宿主扫描将注入失败——与 T 改动前行为一致（改动前无 vertx 时 VertxHttpClient 同样缺失），非本票引入。
- dingding spring.factories 跨模块注册 VertxHttpConfigure 保留：宿主仅引 dingding 不引 supplement 自动配置扫描时仍能拉起 VertxHttpConfigure（dingding pom 依赖 supplement，类恒在），维持 T 现状语义。

## 4. 只登记不动手（nit-inventory.md T1-T3）

- monitor PrometheusAutoConfigure 死 import：StopWatchWrapperAspect（票面点名）+ EnableAspectJAutoProxy（执行期新发现）
- job pom 7 个内部模块依赖声明代码零使用

## 5. 验证

`mvn -pl kdla-component-supplement-dingding,kdla-component-supplement-monitor,kdla-component-reactor,kdla-component-supplement-job -am clean compile` → 结果见票 13 Comments。
