# kdla
K-DDD-Layered Application & Architect

基于 DDD 分层架构的 Java 基础组件框架（13 模块 Maven 库，非可运行应用），发布到公司内部 Nexus（`nexus.enncloud.cn`），供下游业务项目引入后通过 Spring Boot 自动配置获得统一的技术支撑能力。适合公司内 Spring Boot 2.7.x / Java 8 业务应用作为共同依赖基座。

技术基线：Java 8 编译目标 + Spring Boot 2.7.18 + Spring Cloud 2021.0.9。

## 文档导航

- [CHANGELOG.md](./CHANGELOG.md) — 变更记录（2026-11-16 R→T backport 块含 BREAKING 清单与下游知会事项，升级必读）
- [docs/architecture.md](./docs/architecture.md) — 架构说明、装配矩阵与配置键速查、FAQ
- [docs/project-map.md](./docs/project-map.md) — 目录结构、模块清单与导航
- [docs/nit-inventory.md](./docs/nit-inventory.md) — 上游同源遗留登记（只登记不修）

> ⚠️ 本仓与另一裁剪版仓库（9 模块）共享 GAV `top.kdla.framework:*:1.0.1-SNAPSHOT`，同名配置键默认值语义相反（本仓多数默认开）。排障前先确认构件来源。
>
> 装配默认值速查：MDC / StopWatch / CatchLog / 异常拦截 / Validator / Domain / Redisson / Vertx / VertxHttp / FieldValueFind / 钉钉告警 / Prometheus 默认**开**（其中 Domain / 钉钉告警 / Prometheus 无开关，不可配置关闭）；RequestLog / LogInterceptor / WebFilter / Cache / RestHttp / MQTT / CORS / 序列号默认**关**。详见 [docs/architecture.md](./docs/architecture.md)。

## 安装

从公司 Nexus 引入所需模块（按需任选，不必全引）：

```xml
<dependency>
    <groupId>top.kdla.framework</groupId>
    <artifactId>kdla-component-common</artifactId>
    <version>1.0.1-SNAPSHOT</version>
</dependency>
```

注意：

- 几乎所有第三方依赖为 `provided` scope，KDLA 不向下游传递运行时依赖——Spring Boot、MyBatis-Plus、Redisson、Vert.x 等需宿主自行引入
- 仓库凭据配置在 `$HOME/.m2/settings.xml`（server ID `maven-releases` / `maven-snapshots`）

## 快速开始

1. 引入依赖即生效：多数能力经 `spring.factories` 自动装配且默认开启（见上方速查），无需任何 `kdla.*` 配置
2. 关闭不想要的默认能力，例如关方法日志切面：

```yaml
kdla:
  config:
    catch-log:
      enabled: false
```

3. 开启默认关的能力，例如序列号生成（需 Redisson 已装配 + `code_generator_cfg` 表，前置条件见 [docs/architecture.md](./docs/architecture.md) FAQ）：

```yaml
kdla:
  sequence:
    enabled: true
```

全部配置键与默认值：[docs/architecture.md 配置键速查](./docs/architecture.md)。

## 模块概览

模块清单与目录树的 canonical 版本在 [docs/project-map.md](./docs/project-map.md)，此处仅索引：

| 模块 | 职责 |
|---|---|
| kdla-component-model | 基础数据模型：响应封装、DTO 基类、枚举抽象、错误码 |
| kdla-component-reactor | 响应式编程工具：Mono/Flux 追踪、Topic 路由 |
| kdla-component-common | 通用工具：加解密（AES/SM2/SM3/SM4）、线程池、雪花 ID、MDC 与 StopWatch 切面 |
| kdla-component-exception | 异常：BizException / SysException / LockFailException、Controller 全局异常拦截、响应包装 |
| kdla-component-validator | 校验：@EnumValid / @DBLengthValid 注解校验、编程式校验与断言 |
| kdla-component-log | 日志：@CatchAndLog 方法日志、请求日志、请求体缓存 Filter |
| kdla-component-domain | 业务领域：DDD 共享内核（Entity / ValueObject / Specification / 领域事件） |
| kdla-component-dal | 持久化：MyBatis-Plus 与 ES 基类、分页转换、SQL 过滤 |
| kdla-component-supplement | 业务支撑：HTTP 客户端（Vertx/Netty）、MQTT、Excel、CORS、字段值查找 |
| kdla-component-supplement-cache | 业务支撑-缓存：Redis 工具、分布式锁（Redisson）、序列号生成 |
| kdla-component-supplement-dingding | 业务支撑-钉钉：机器人告警 |
| kdla-component-supplement-monitor | 业务支撑-监控：Prometheus 埋点与 Pushgateway 推送 |
| kdla-component-supplement-job | 业务支撑-任务：XXL-JOB 执行器封装 |

## 开发

```bash
# 全量编译（系统默认 JDK 21 可直接编译，target 1.8；或显式用 JDK 8）
mvn clean compile
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home mvn clean compile

# 全量测试（当前 65 测试类 / 173 用例，JUnit 5 + Mockito）
mvn clean test

# 单模块测试：-am 不能省（~/.m2 中同 GAV SNAPSHOT 可能被另一仓 install 污染）
mvn -pl kdla-component-supplement-cache -am clean test

# 发布 SNAPSHOT 到 Nexus（需公司内网/VPN）
mvn deploy
```

- 分支名即版本线：`1.0.1-SNAPSHOT`（当前，JDK 8）、`2.0.0-SNAPSHOT`（JDK 21 / Boot 3.4.0）
- 版本号硬编码在根 pom 与 13 个模块 pom 的 parent，升版本需全部同步
- 提交信息风格：`kdla <version> and upgrade <topic>`
- 有测试设施的模块，bugfix / 非平凡改动须带回归测试
