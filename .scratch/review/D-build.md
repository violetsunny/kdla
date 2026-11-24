# 切片 D：构建横切评审报告

范围：根 `pom.xml` + 15 模块 `*/pom.xml` + 10 个 `AutoConfiguration.imports` + 已删 9 个 `spring.factories` + `README.md` + `.gitignore`。
取证：`git diff 136bf03 HEAD`（BASE=136bf03，HEAD=0fa5ab7）+ `git show 1.0.1-SNAPSHOT:` 基线 + grep 全仓实证。只读静态审查，未跑 mvn（主代理已 318 绿）。

## 结论

**GO。Critical = 0。** 构建横切改动整体正确：版本同步、provided 约定、依赖方向、compiler/annotationProcessorPaths、MP/redisson/jsqlparser 收敛、死依赖移除、自动配置注册均核验通过。唯一必改项是 README 破坏性变更章节不完整（jsqlparser 自管等下游运行时影响未登记）。

---

## 发现清单

### D1 / 必改 / README.md:91-97
**问题**：「2.0.0 破坏性变更」章节仅登记 xxl-job 包名、JwtUtil 强制验签 2 项，缺以下对下游有实际影响的变更：
- **jsqlparser 改由 MP 自管**：根 DM 与 dal/trd 的 `jsqlparser` pin 已删，MP 3.5.12 经 optional 模块 `mybatis-plus-jsqlparser` 自管。下游用分页（PaginationInnerInterceptor）须自引 `mybatis-plus-jsqlparser`，否则运行时 NoClassDefFoundError。此项有真实下游运行时破坏，最该登记。
- **redisson 3.20.1 → 3.52.0** 大跨度升级（cache 排除 redisson-spring-data-35、改引 -34）。
- **MpBaseDo 删除**（2.x 仓库内 0 引用未恢复）。
- **14 项死依赖移除**（commons-httpclient/jython/itextpdf 系/xhtmlrenderer/jtidy/commons-configuration/commons-fileupload/commons-math3/ognl/aviator/luaj/camel）。
- **根 snakeyaml compile 块删除**：原全局 compile 强注 2.3 会传递下游；删除后下游若直接用 snakeyaml 须自引（Boot 应用仍经 spring-boot-starter 获得 2.3，见 D9）。
**修复建议**：补全上述 5 项，jsqlparser 自管置顶并给出下游需自引的坐标。
**状态**：本轮文档缺口，建议改。

### D2 / Nit / kdla-component-supplement-job/src/main/java/top/kdla/framework/supplement/timer/config/XxlJobConfig.java:52
**问题**：`@ConditionalOnProperty(prefix = "xxl.enabled", name = "xxl", havingValue = "true")` 拼出的生效键是 `xxl.enabled.xxl`，而非自然的 `xxl.enabled`。本轮 job 的 .imports 新注册了 XxlJobConfig（BASE 仅有 VertxHttpConfigure），使该守卫首次进入自动配置链。下游若配 `xxl.enabled=true` 执行器 Bean 不会创建（无 matchIfMissing，默认禁用）。
**修复建议**：核对下游真实配置键；若意图为 `xxl.enabled`，改 `prefix="xxl", name="enabled"`。属 job 源码切片，本切片仅登记注册面。
**状态**：2.x 既有源码（BASE 已存在、1.x 无此类），非本轮新引入；not measured（未对下游配置实测）。降级 Nit。

### D3 / Optional / pom.xml:284-288；kdla-component-supplement/pom.xml:179；kdla-component-supplement-dingding/pom.xml:156；kdla-component-supplement-job/pom.xml:134；kdla-component-supplement-trdplatformcloud/pom.xml:176
**问题**：`vertx-codegen` 仍在根 DM + 4 模块声明（provided），但全仓 0 import / 0 FQN 使用（grep 实证）。属遗留死依赖，不在本轮移除的 14 项内。provided 不传递下游，无害。另：根 compiler 启用 annotationProcessorPaths 后，classpath 发现的处理器（vertx-codegen 本质是注解处理器）不再运行，但 0 codegen 注解故无影响。
**修复建议**：只列不删；后续 hygiene 轮可连同 DM 一并清。
**状态**：1.x/2.x 既有遗留，刻意未在本轮处理。

### D4 / Nit / README.md:3；README.md:47-50
**问题**：:3 仍写「依赖JDK1.8」（已登记候选项，确认过时，实际 JDK21）。:47-50 common 的 .imports 示例只列 StopWatchAutoConfigure，实际文件已含 MdcAutoConfigure，文档与注册漂移。
**修复建议**：:3 改 JDK21；:47-50 示例补 MdcAutoConfigure 或改为指向真实文件。
**状态**：:3 已登记；:47-50 为本轮注册重写后新增漂移。

### D5 / Nit / 8 个 .imports 末尾；pom.xml:482；多模块 pom 末尾
**问题**：重写的 8 个 .imports（common/domain/exception/log/cache/job/monitor/supplement）及根 pom、多模块 pom 文件末尾缺换行（`\ No newline at end of file`）。已登记候选项。
**修复建议**：补尾换行（POSIX 文本文件约定）。纯外观，不影响 Boot 解析。
**状态**：既有风格延续，已登记。

### D6 / FYI / kdla-component-supplement-job-application/pom.xml:109-125
**问题**：job-application 自带 annotationProcessorPaths 顺序为 mapstruct → lombok → binding（binding 不在 lombok 与 mapstruct 之间），与根 pluginManagement（lombok → binding → mapstruct → config-processor）不一致。但 job-application 全仓 0 `org.mapstruct` import、0 lombok 使用（DAO 的 `@Mapper` 是 MyBatis 非 MapStruct），三条 path 均功能惰性，顺序无实际影响。binding 经 ServiceLoader 发现，顺序本非强制。
**修复建议**：无需改；如求一致可对齐根顺序。
**状态**：既有配置延续 + 本轮加 binding，无功能影响。

### D7 / FYI / dingding/job/trd/supplement 的 .imports
**问题**：VertxHttpConfigure 跨 supplement/dingding/job/trd 4 模块重复注册；trd 还跨模块注册 common/exception/log 的 StopWatchAutoConfigure/UnifiedExceptionControllerAdvice/CatchLogAutoConfigure。已登记已知冗余。核验：4 模块均依赖 kdla-component-supplement（dingding:40 / job:40 / trd:41），trd 依赖 common/exception/log，被注册类均在 classpath，Boot 按类名去重，无害。
**状态**：已知刻意冗余，非 bug。

### D8 / FYI / kdla-component-exception/pom.xml:150-156
**问题**：spring-security-core 同时标 `provided` + `optional`。provided 本就不传递，optional 冗余但无害。spring-jdbc/spring-security-core 均 provided，版本经 spring-boot-dependencies BOM 管理（未写版本号），运行时由内部 advice 的 @ConditionalOnClass(AccessDeniedException/DuplicateKeyException) 守卫，正确。
**状态**：冗余无害，可不动。

### D9 / FYI / 覆盖性与 snakeyaml 影响面
- **15 模块全覆盖**：13 模块 pom 本轮改动，domain/reactor pom 未改且合理（domain 无死依赖/MP，reactor 零第三方），无遗漏。
- **snakeyaml 删除影响有限**：kdla 自身 0 snakeyaml 使用；Boot 3.4.0 的 spring-boot-starter 仍传递 snakeyaml 2.3（与原强注同版本），故 Boot 下游版本不变，仅非 Boot 下游或排除 starter snakeyaml 者需自引。登记入 D1 即可。
- **状态**：核验通过，供 D1 佐证。

---

## 核验通过项（无发现）

1. **版本硬编码同步**：根 `<version>` + `kdla.framework.version` + 15 模块 parent 全 = 2.0.0-SNAPSHOT；曾钉 1.0.0 的 trd/job-application 已修（bcf5d7a）。
2. **第三方版本集中**：13 模块 pom 内 0 硬编码第三方依赖版本（job-application 例外，仅插件/处理器 path 版本 + 本地 mysql property）；新增 spring-jdbc/spring-security-core/jackson-databind/redisson-spring-data-34/spring-boot-starter-test 版本均经 BOM/根 DM。
3. **provided 约定**：新增/恢复依赖全 provided（test 依赖 provided 不传递）；job-application 应用例外 compile；model/reactor 无传递性第三方（model lombok optional + test provided）。
4. **依赖方向防成环**：本轮未新增任何内部依赖；model/reactor 零内部 → common → exception → domain → dal/log/validator/supplement-* → job-application，无环。trd 8 内部依赖、job 7、dingding 4 均向下。
5. **compiler**：根 pluginManagement maven-compiler-plugin 3.13.0 + `<release>${java.version}</release>`(21)；4 path 版本与 properties 一致（lombok 1.18.36 / binding 0.2.0 / mapstruct 1.6.3 / config-processor 3.4.0），binding 居 lombok 与 mapstruct 之间。仅根与 job-application 声明 compiler 插件，其余 13 模块继承根配置。job-application 3.8.1→3.13.0、source/target 1.8→release 21 正确。
6. **mapstruct binding 有效性（静态）**：MapStruct 仅 trd 使用，trd 继承根正确顺序配置 → 7 个 Impl 生成非空与配置一致（运行时已实证，本切片静态核对配置正确）。
7. **MP 收敛**：dal/trd `mybatis-plus-boot-starter` → `mybatis-plus-spring-boot3-starter`（3.5.12，mybatis-spring 3.0.4 兼容 Spring6）；dal 对 MP starter 的 mybatis/mybatis-spring/spring-* 排除保留，经 mybatis-spring-boot-starter 3.0.4 控版本；jsqlparser pin 根 DM + dal/trd 全删。
8. **redisson**：3.20.1→3.52.0；cache 排除 redisson-spring-data-35 + 新增 -34 provided，根 DM 补 -34（同版本 3.52.0），对齐 Spring Data Redis 3.4 / Boot 3.4。
9. **commons-lang3 去重**：trd/job 各删一处重复声明，各保留一处（trd:101 / job:75，provided），编译不受影响。
10. **死依赖移除安全**：14 项（commons-math3/commons-httpclient/commons-fileupload/commons-configuration/itextpdf/xmlworker/itext-asian/flying-saucer-pdf-itext5/jtidy/ognl/aviator/jython/luaj-jse/camel-spring-boot-starter）从 common 删除 + 根 DM/properties 同步删；javax 三件套从 common/trd/job-application 删 + 根 DM/properties 删。全仓 0 import + 0 FQN 使用实证，无误删仍在用依赖。
11. **exclusion 保留**：common easyexcel 对 commons-math3 的 exclusion（:255-258）完整保留，未因 DM 删除而误删整块；exclusion 不需 DM 支撑。
12. **javax 残留**：仅 reactor/validator 注释行含 javax（规则不动注释），0 活跃 javax 依赖/import；javax.sql/javax.net JDK 包未动。
13. **自动配置注册正确性**：10 个 .imports 列出的 19 个唯一 auto-config 类全限定名全部真实存在、拼写正确；9 个 spring.factories 删除（Boot3 忽略其 EnableAutoConfiguration 注册，删除正确，注册统一走 .imports）。守卫恢复完整：common Mdc/StopWatch @ConditionalOnClass(Aspect)+OnProperty+OnMissingBean；log CatchLog/RequestLog/LogInterceptor/WebFilter 齐备（含 OnWebApplication SERVLET）；cache KRedisson/Cache/SequenceNo 齐备；dingding DingAlert @ConditionalOnClass(Vertx)；monitor Prometheus 方法级 @ConditionalOnClass(CollectorRegistry/PushGateway)（与 1.x 同模式）；supplement VertxHttp @ConditionalOnClass(WebClient)、RestHttp @ConditionalOnClass(httpclient5 HttpClient)（pom 有 httpclient5:208）、VertxMqtt @ConditionalOnClass(io.vertx.mqtt.MqttClient)（pom 有 vertx-mqtt:174）、Cors/FieldValueFind 齐备；exception UnifiedExceptionControllerAdvice @ConditionalOnWebApplication+OnProperty+内部 advice @ConditionalOnClass 守卫。
14. **.gitignore**：新增 .project/.classpath/.factorypath/.settings（Eclipse/jdt.ls）+ .DS_Store；f8cb918 已 untrack 两个误提交 .DS_Store；当前 0  tracked .DS_Store/Eclipse 元数据。正确。

---

**GO / NO-GO：GO**
**Critical 计数：0**（必改 1 / Nit 3 / Optional 1 / FYI 4）