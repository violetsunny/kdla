# kdla
K-DDD-Layed-Application&amp;Architect
### 依赖JDK1.8

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
org.springframework.boot.autoconfigure.AutoConfiguration.imports
```
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
- [ExcelReadListener](./kdla-component-supplement/src/main/java/top/kdla/framework/supplement/excel/imp/ExcelReadListener.java)

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

## 2.0.0 破坏性变更

### xxl-job 内嵌源码包名（kdla-component-supplement-job / kdla-component-supplement-job-application）
2.x 内嵌的 xxl-job 源码包名为 `com.xxl.job.core` / `com.xxl.job.admin`，与官方 `com.xuxueli:xxl-job-core` 的 `com.xxl.job.core` 同包同名类冲突。下游应用不得同时依赖本模块与官方 xxl-job-core；执行器注册、组件扫描路径须按内嵌包名配置。

### JwtUtil 强制验签（kdla-component-supplement-trdplatformcloud）
`JwtUtil.getInfoFromToken` 由"仅解码不验签"改为强制 HS256 验签 + exp 过期校验，任何失败抛 BizException，不再返回空串。下游必须配置 `kdla.trd.jwt.sign-key`，未配置或验签失败时依赖该工具的 trd 接口立即不可用。密钥按 raw UTF-8 bytes 参与签名（非 base64 解码），此假设未对真实 BladeX token 实测，接入前须用真实 token 验证。

### MyBatis-Plus 分页 jsqlparser 自管（kdla-component-dal / kdla-component-supplement-trdplatformcloud）
2.x 升级到 `mybatis-plus-spring-boot3-starter` 3.5.12，并移除根 pom 对 jsqlparser 4.7 的强制 pin。MP 3.5.12 将 jsqlparser 拆为可选模块自管，下游使用分页（PaginationInnerInterceptor）须自行引入 `com.baomidou:mybatis-plus-jsqlparser`（版本与 MP 对齐），否则分页运行期报 jsqlparser 类缺失。

### Redisson 3.20.1 → 3.52.0（kdla-component-supplement-cache）
- 排除传递依赖 `redisson-spring-data-35`，改引 `redisson-spring-data-34`（对齐 Spring Data Redis 3.4 / Boot 3.4，修复下游 RedissonAutoConfiguration AbstractMethodError）。
- 分布式锁工厂 `KRedissonLockFactory` 改为委托 `KRedissonConfigProperties` 统一构建，支持全部 5 种 `RedisClusterType`（SINGLE/REPLICATE/MASTER_SLAVE/CLUSTER/SENTINEL）；旧工厂仅 SINGLE/REPLICATE 可用，其余静默返回空 client 列表导致锁不可用。
- **SENTINEL 模式新实现**：必须配置 `kdla.redisson.master-name`（Redis 主节点名），缺失即 fail-fast。
- **MASTER_SLAVE 模式 address 约定**：逗号分隔地址首节点为 master，其余为 slave（旧实现把整串地址同时当 master 与 slave，属 bug，已修）。
- `RedissonRedLock` 保留（3.x 仅 deprecated 未移除），RedLock 多数派 quorum 语义不变。

### MpBaseDo 删除（kdla-component-dal）
2.x 删除基类 `top.kdla.framework.infra.dal.mybatis.common.MpBaseDo`（带 `@TableLogic` 逻辑删除标志，仓库内 0 引用）。下游若曾继承该类须自行迁移：无需逻辑删除可改用同包 `BaseDo`，否则迁移到 MyBatis-Plus 原生基类或自定义基类。

### 死依赖与 javax 三件套清理（多模块）
移除 14 个未使用依赖，以及 common/supplement/dingding 与根 dependencyManagement/properties 中的 javax 三件套（javax.annotation / javax.validation / javax.servlet）。2.x 全面迁移 jakarta，下游若曾传递依赖这些 javax 库须自行显式引入或迁移到 jakarta。

### snakeyaml compile 块移除（根 pom）
移除根 pom 向所有模块强注的 snakeyaml 2.3 compile 依赖块，回归 provided 约定。下游如需 snakeyaml 须自行引入（Spring Boot 3.4 经 spring-boot-starter 传递 snakeyaml 2.x）。