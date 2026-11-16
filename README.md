# kdla
K-DDD-Layed-Application&amp;Architect

13 模块 Maven 框架库（非应用），发布到公司内部 Nexus（`nexus.enncloud.cn`）。Java 8 编译目标 + Spring Boot 2.7.18 + Spring Cloud 2021.0.9。

## 文档导航

- [CHANGELOG.md](./CHANGELOG.md) — 变更记录（2026-11-16 R→T backport 块含 BREAKING 清单与下游知会事项，升级必读）
- [docs/architecture.md](./docs/architecture.md) — 架构说明、装配矩阵与配置键速查
- [docs/project-map.md](./docs/project-map.md) — 目录结构与模块导航
- [docs/nit-inventory.md](./docs/nit-inventory.md) — 上游同源遗留登记（只登记不修）

> ⚠️ 本仓与另一裁剪版仓库（9 模块）共享 GAV `top.kdla.framework:*:1.0.1-SNAPSHOT`，同名配置键默认值语义相反（本仓多数默认开）。排障前先确认构件来源。
>
> 装配默认值速查：MDC / StopWatch / CatchLog / 异常拦截 / Validator / Domain / Redisson / Vertx / VertxHttp / FieldValueFind / 钉钉告警 / Prometheus 默认**开**；RequestLog / LogInterceptor / WebFilter / Cache / RestHttp / MQTT / CORS / 序列号默认**关**。详见 [docs/architecture.md](./docs/architecture.md)。

## kdla-component-model
基础数据模型：实体类，枚举类，异常类

- [Query](./kdla-component-model/src/main/java/top/kdla/framework/dto/Query.java)
- [SingleResponse](./kdla-component-model/src/main/java/top/kdla/framework/dto/SingleResponse.java)
- [MultiResponse](./kdla-component-model/src/main/java/top/kdla/framework/dto/MultiResponse.java)
- [PageQuery](./kdla-component-model/src/main/java/top/kdla/framework/dto/PageQuery.java)
- [PageResponse](./kdla-component-model/src/main/java/top/kdla/framework/dto/PageResponse.java)
- [IEnum](./kdla-component-model/src/main/java/top/kdla/framework/dto/IEnum.java)
- [ErrorCodeI](./kdla-component-model/src/main/java/top/kdla/framework/dto/exception/ErrorCodeI.java)
- [ErrorCodeI](./kdla-component-model/src/main/java/top/kdla/framework/dto/exception/ErrorCode.java)

## kdla-component-common
通用工具：工具类，help类，stopwatch，mdc

- [@MdcDot](./kdla-component-common/src/main/java/top/kdla/framework/common/aspect/mdc/MdcDot.java)
- [@StopWatchWrapper](./kdla-component-common/src/main/java/top/kdla/framework/common/aspect/watch/StopWatchWrapper.java)
***
- [AopTargetHelp](./kdla-component-common/src/main/java/top/kdla/framework/common/help/AopTargetHelp.java)
- [BigDecimalHelp](./kdla-component-common/src/main/java/top/kdla/framework/common/help/BigDecimalHelp.java)
- [BloomFilterHelp](./kdla-component-common/src/main/java/top/kdla/framework/common/help/BloomFilterHelp.java)
- [DateHelp](./kdla-component-common/src/main/java/top/kdla/framework/common/help/DateHelp.java)
- [FileChunkTransmitHelp](./kdla-component-common/src/main/java/top/kdla/framework/common/help/FileChunkTransmitHelp.java)
- [KdlaStringHelp](./kdla-component-common/src/main/java/top/kdla/framework/common/help/KdlaStringHelp.java)
- [LocalDateTimeHelp](./kdla-component-common/src/main/java/top/kdla/framework/common/help/LocalDateTimeHelp.java)
- [MultiThreadInvokeHelp](./kdla-component-common/src/main/java/top/kdla/framework/common/help/MultiThreadInvokeHelp.java)
- [RegexHelp](./kdla-component-common/src/main/java/top/kdla/framework/common/help/RegexHelp.java)
- [SelfSnowflakeGeneratorHelp](./kdla-component-common/src/main/java/top/kdla/framework/common/help/SelfSnowflakeGeneratorHelp.java)
- [ThreadPoolHelp](./kdla-component-common/src/main/java/top/kdla/framework/common/help/ThreadPoolHelp.java)
***
- [AESUtil](./kdla-component-common/src/main/java/top/kdla/framework/common/utils/AESUtil.java)
- [BytesUtil](./kdla-component-common/src/main/java/top/kdla/framework/common/utils/BytesUtil.java)
- [CharUtil](./kdla-component-common/src/main/java/top/kdla/framework/common/utils/CharUtil.java)
- [GzipUtil](./kdla-component-common/src/main/java/top/kdla/framework/common/utils/GzipUtil.java)
- [Md5Util](./kdla-component-common/src/main/java/top/kdla/framework/common/utils/Md5Util.java)
- [ObjectUtil](./kdla-component-common/src/main/java/top/kdla/framework/common/utils/ObjectUtil.java)
- [SHAUtil](./kdla-component-common/src/main/java/top/kdla/framework/common/utils/SHAUtil.java)
- [Sm2Util](./kdla-component-common/src/main/java/top/kdla/framework/common/utils/Sm2Util.java)
- [Sm3Util](./kdla-component-common/src/main/java/top/kdla/framework/common/utils/Sm3Util.java)
- [Sm4Util](./kdla-component-common/src/main/java/top/kdla/framework/common/utils/Sm4Util.java)

***
spring.factories 
```
org.springframework.boot.autoconfigure.EnableAutoConfiguration = \
top.kdla.framework.common.aspect.mdc.MdcAutoConfigure,\
top.kdla.framework.common.aspect.watch.StopWatchAutoConfigure
```

## kdla-component-exception
异常：
- BizException 通用业务异常
- LockFailException 锁异常
- SysException 系统异常
- UnifiedExceptionControllerAdvice Controller异常拦截
- ResponseHandler 返回包装

## kdla-component-domain
业务：

## kdla-component-dal
持久化：ES Mybatis

## kdla-component-log
日志：

## kdla-component-validator
校验：

## kdla-component-reactor
响应式编程工具：

## kdla-component-supplement
业务支撑：

## kdla-component-supplement-cache
业务支撑-缓存：

## kdla-component-supplement-dingding
业务支撑-钉钉：

## kdla-component-supplement-monitor
业务支撑-监控：

## kdla-component-supplement-job
业务支撑-任务：