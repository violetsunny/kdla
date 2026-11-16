# 项目结构地图

> KDLA 基础组件框架的目录结构与模块导航。

## 项目信息

| 属性 | 值 |
|------|-----|
| GroupId | `top.kdla.framework` |
| ArtifactId | `kdla-components` |
| 版本 | `1.0.1-SNAPSHOT` |
| 构建工具 | Maven（无 mvnw，用系统 `mvn`） |
| Java 编译目标 | 1.8 |
| 发布仓库 | 公司内部 Nexus（`nexus.enncloud.cn`，settings server id `kdla-nexus`） |
| 模块数 | 13 |

## 目录树

```
kdla/                                   根 POM，管理全局依赖版本与发布配置
├── pom.xml                             父 POM，dependencyManagement 统一版本
├── README.md                           项目入口文档
├── CHANGELOG.md                        变更记录
├── docs/                               技术说明文档
│   ├── project-map.md                  本文件：结构导航
│   ├── architecture.md                 架构说明与设计解释
│   └── nit-inventory.md                上游同源遗留登记（P1-P20 / T1-T3）
│
├── kdla-component-model/               基础数据模型（零内部依赖，除 Lombok）
│   └── src/main/java/top/kdla/framework/dto/
│       ├── Response, SingleResponse,   响应封装
│       ├── MultiResponse, PageResponse
│       ├── BaseModel, Command, Query, PageQuery  DTO 基类
│       ├── IEnum, EnumResponse         枚举抽象
│       └── exception/ErrorCode, ErrorCodeI  错误码体系
│
├── kdla-component-reactor/             响应式编程工具（零内部依赖）
│   └── src/main/java/top/kdla/framework/reactor/
│       ├── trace/                      Mono/Flux 追踪（TraceMono, TraceFlux, TraceHolder）
│       ├── topic/                      Topic 路由（TopicRouter, Router）
│       ├── help/                       Reactors, ServiceContext, 多线程辅助
│       ├── model/                      ConfigKey/Value 抽象
│       └── utils/                      FluxUtil, SerializeUtil, TopicUtil
│
├── kdla-component-common/              通用工具层（依赖 model）
│   └── src/main/java/top/kdla/framework/common/
│       ├── utils/                      加解密（AES/SM2/SM3/SM4）、压缩、序列化工具
│       ├── help/                       线程池、雪花 ID、布隆过滤器等
│       ├── aspect/mdc/                 MDC 追踪切面（@MdcDot, MdcAutoConfigure）
│       ├── aspect/watch/               性能监控切面（@StopWatchWrapper, StopWatchAutoConfigure）
│       ├── constants/                  常量定义
│       └── enums/                      通用枚举
│
├── kdla-component-exception/           异常处理（依赖 model/common）
│   └── src/main/java/top/kdla/framework/exception/
│       ├── BizException, SysException, LockFailException  异常类
│       ├── ExceptionFactory            异常工厂
│       ├── UnifiedExceptionControllerAdvice  全局异常拦截（spring.factories 自动注册）
│       └── ResponseHandler             响应包装
│
├── kdla-component-validator/           参数校验（依赖 common/exception）
│   └── src/main/java/top/kdla/framework/validator/
│       ├── annotation/                 @EnumValid, @DBLengthValid, @IEnumValid
│       ├── group/                      AddGroup, UpdateGroup
│       ├── CommonValidator, ObjectValidator  编程式校验
│       ├── BaseAssert, ValidatorUtils  断言与工具
│       └── ValidatorAutoConfigure      自动配置
│
├── kdla-component-log/                 日志切面（依赖 common/exception）
│   └── src/main/java/top/kdla/framework/log/
│       ├── catchlog/                   @CatchAndLog 方法日志切面
│       ├── requestlog/                 请求日志切面
│       ├── webfilter/                  请求体缓存 Filter
│       └── LogInterceptor              日志拦截器
│
├── kdla-component-domain/              DDD 领域层（依赖 common/exception）
│   └── src/main/java/top/kdla/framework/domain/
│       ├── shared/                     Entity, ValueObject, Specification
│       ├── event/                      DomainEvent, EventPublisher
│       ├── DomainFactory               领域对象工厂
│       └── ApplicationContextHelp      Spring 容器辅助
│
├── kdla-component-dal/                 数据访问层（依赖 domain/common/exception）
│   └── src/main/java/top/kdla/framework/infra/dal/
│       ├── mybatis/common/             BaseDo, MpBaseDo 基类
│       ├── mybatis/convertor/          分页转换器
│       ├── mybatis/util/               SQL 过滤（SqlFilter）、分页工具
│       └── es/model/                   ES 索引模型
│
├── kdla-component-supplement/          业务支撑（依赖 domain/common/exception）
│   └── src/main/java/top/kdla/framework/supplement/
│       ├── http/                       HTTP 客户端（Vertx, Netty, KdlaHttp）
│       ├── mqtt/                       Netty / Vert.x MQTT 客户端
│       ├── netty/                      Netty HTTP Server/Client
│       ├── excel/                      EasyExcel 导入导出
│       ├── cors/                       CORS 配置
│       ├── fieldvaluefinder/           @FieldValueFind 字段值查找
│       └── vertx/                      Vert.x 基础配置
│
├── kdla-component-supplement-cache/    缓存与锁（依赖 common/exception）
│   └── src/main/java/top/kdla/framework/supplement/cache/
│       ├── lock/                       分布式锁（Redisson）
│       │   ├── KdlaBizDisLockService   业务锁服务
│       │   ├── DistributeLockedAspect  @DistributeLocked AOP
│       │   ├── RedissonLockFactory     锁工厂
│       │   └── properties/             锁配置属性
│       ├── cache/                      缓存管理
│       │   ├── SpringRedisHelp         Redis 操作工具
│       │   ├── KdlaCafCache, KdlaGcache  Caffeine + Redis 缓存
│       │   └── MachineIdHelp           机器 ID 生成
│       └── sequence/                   序列号生成
│           ├── SequenceNoGenerator     V1 序列号生成器
│           ├── SequenceNoGeneratorV2   V2 序列号生成器
│           ├── mapper/                 MyBatis Mapper
│           └── model/rules/            生成规则（日期、随机、序列）
│
├── kdla-component-supplement-dingding/ 钉钉告警（依赖 supplement/common/exception）
│   └── src/main/java/top/kdla/framework/supplement/dingding/
│       ├── config/DingAlertConfigure   自动配置（OnClass(Vertx)，无开关）
│       ├── service/DingDingService     告警发送服务
│       ├── utils/DingDingHttpUtil      钉钉 webhook HTTP 工具
│       ├── properties/DingAlertProperties  告警配置属性
│       └── model/                      消息模型（Text/Markdown/At）
│
├── kdla-component-supplement-monitor/  监控埋点（依赖 common/exception）
│   └── src/main/java/top/kdla/framework/supplement/monitor/prometheus/
│       ├── annotation/                 @PrometheusCounter, PrometheusPointAspect, PrometheusAutoConfigure
│       └── PushGatewayManager          Pushgateway 推送管理
│
└── kdla-component-supplement-job/      XXL-JOB 执行器封装（聚合 supplement/cache/dingding/log/domain）
    └── src/main/java/top/kdla/framework/supplement/job/
        ├── executor/                   XxlJobExecutor, XxlJobSpringExecutor
        ├── handler/                    IJobHandler, @XxlJob, Method/Script/Glue Handler
        ├── biz/                        ExecutorBiz, AdminBiz 及客户端
        ├── thread/                     JobThread, 注册/回调/日志清理线程
        ├── glue/                       GlueFactory（Spring GLUE）
        ├── server/EmbedServer          内嵌执行器服务
        ├── context/, log/, util/, enums/
```

## 模块依赖关系

```
model / reactor（最底层，零内部依赖）
  ↑
common（工具层，依赖 model）
  ↑
exception（依赖 model/common）
  ↑
domain（依赖 exception 及以下）
  ↑
dal / log / validator / supplement / supplement-cache / supplement-monitor（横切与支撑层）
  ↑
supplement-dingding（依赖 supplement）
  ↑
supplement-job（聚合最多：supplement / cache / dingding / log / domain）
```

依赖原则：
- 下层模块不能依赖上层模块，给底层模块加依赖会成环（改 pom 前先看本流向）
- `model`、`reactor` 保持纯净，不引入任何第三方重依赖
- 所有模块的外部依赖默认使用 `provided` scope
- 模块间依赖版本由根 pom `dependencyManagement` 统一管理（`${project.version}`），模块 pom 内不写版本号

## 启动入口

KDLA 是库项目，无可运行入口。下游应用通过 Maven 引入依赖后，通过 Spring Boot 自动配置生效（各模块 `META-INF/spring.factories`，装配矩阵见 `docs/architecture.md`）。

## 配置入口

- 自动配置：各模块 `META-INF/spring.factories`（当前 7 个模块有）
- 配置前缀：`kdla.*`（键与默认值速查见 `docs/architecture.md`）

## 测试入口

```bash
# 全量测试
mvn clean test

# 单模块测试（-am 不能省：~/.m2 中同 GAV SNAPSHOT 可能被另一仓 install 污染）
mvn -pl kdla-component-supplement-cache -am clean test
```

测试文件位于各模块 `src/test/java/` 下，当前 **64 个测试类 / 171 个用例**（JUnit 5 + Mockito，经 provided 作用域 `spring-boot-starter-test` 引入）。

## 版本线

| 分支 | JDK | Spring Boot | Spring Cloud |
|---|---|---|---|
| `1.0.1-SNAPSHOT`（当前） | 8 | 2.7.18 | 2021.0.9 |
| `2.0.0-SNAPSHOT` | 21 | 3.4.0 | 2024.0.1 |

分支名即版本线，版本开发在同名版本分支上进行。
