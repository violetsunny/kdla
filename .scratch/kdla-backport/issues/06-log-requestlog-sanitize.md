# 06: log 模块 RequestLog 重构 + LogSanitizeUtil 脱敏 + Filter/Wrapper

**What to build:** RequestLogAspect 整体可靠性重构（165 行，null 安全）。新增 LogSanitizeUtil（Header 黑名单脱敏 authorization/cookie/password/token/mobile + 2KB 截断）并接入 LogInterceptor/CatchLogAspect/RequestLogAspect 三处，请求日志不再泄漏凭证。KdlaBodyReaderRequestFilter：finally 清 MDC（线程复用不串 traceId）+ 按 T 的 List<String> excludes 现状适配 R 修复 + GET 排除对齐注释。KdlaHttpServletRequestWrapper：body 空兜底 + UTF-8 显式编码。装配：RequestLogAutoConfigure 新注册（OnClass(Aspect)+@ConditionalOnProperty kdla.config.request-log.enabled 默认关+COBM，矩阵 11.3——全量请求日志有日志量/隐私副作用且 T 现状未开启）。

**Blocked by:** 05

**Status:** done

依据：investigation.md 第 3 节 20/22/34/84/97 项、第 11.3 节矩阵。R 参考：LogSanitizeUtil 位于 R log 模块 sanitize 包。

- [x] RequestLogAspect 重构落地，行为与 R bb8022f 一致
- [x] LogSanitizeUtil 新增并接入三处切面/拦截器
- [x] Filter 按 T excludes 现状适配 + finally 清 MDC；Wrapper 空兜底+UTF-8
- [x] RequestLogAutoConfigure 注册且默认关
- [x] R 相关测试回合
- [x] `mvn -pl kdla-component-log -am clean compile` 与 test 绿
- [x] 独立 commit，不 push

## Comments

- 2026-11-14 完成。commit ba03d00。验证：`mvn -pl kdla-component-log -am clean test` BUILD SUCCESS，log 模块 10 测试类 25 用例全绿（本票新增 5 类 12 用例；上游 model 6 / common 56 / exception 10 同跑全绿）。
- **发现拆票遗漏（同票 02→17 模式）**：R HEAD 的 log spring.factories 注册 4 项，其中 LogInterceptorAutoConfigure（kdla.config.log-interceptor.enabled 默认关）与 WebFilterAutoConfigure（kdla.config.web-filter.enabled 默认关）在 investigation/spec/全部票据零命中。处置：并入本票（同模块同 spring.factories 文件，单独立票会冲突）。两者 R HEAD 均 OnWebApplication(SERVLET)+OnClass+OnProperty 默认关+COBM；T 现状两类存在但未注册=关，注册后默认关=仍关，零行为变化，符合 S1（有副作用且 T 现状关→默认关）。
- 分叉判定（硬约束 6 基线 diff 验证）：仅 KdlaBodyReaderRequestFilter FORKED（R String exclude vs T List<String> excludes）→ 手术式适配；RequestLogAspect/RequestLogAutoConfigure/Wrapper/WebFilterAutoConfigure/LogInterceptorAutoConfigure 均 T==R 基线 6d617f2 → 整覆盖 R HEAD（7 文件覆盖后与 R HEAD 逐字一致，已抽查核验）。
- 适配决策：
  1. Filter 手术：移植 R 的 traceId 获取（header "traceId" 透传或生成）+ finally MDC.remove（20 项）；handleExcludeURL 补 GET 排除（对齐注释）+ 空配置守卫按 T List 语义适配为 excludes==null||isEmpty()（R 为 !hasText(String)，行为语义一致：空配置=全排除不包装）。
  2. LogSanitizeUtil 新增（R 逐字），接入三处：LogInterceptor（恢复票 05 暂去除的 truncate）、CatchLogAspect around 日志、RequestLogAspect（R HEAD 自带）。
  3. CatchLogAspectLogTruncateTest 随本票回合（票 05 遗留，依赖 truncate 行为）。
  4. GetMethodTest 反射赋值适配 T 分叉：字段 exclude(String)→excludes(List)，Arrays.asList("/config/**","/ping")。
  5. RequestLogAutoConfigure 默认关（matchIfMissing=false 同 R，矩阵 11.3）。
