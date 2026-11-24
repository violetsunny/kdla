# 05: log 模块 CatchLog 链路（分叉重定位 + 装配）

**What to build:** CatchLogAspect 按 T 分叉现状重定位 R 的全部修复：K1 BaseException 兜底分支（下游自定义异常不再落 UNKNOWN_ERROR/error 级）、doBefore 无参方法 args.isEmpty() 守卫（不再越界，直接生成 traceId）、删重复嵌套 log.isWarnEnabled()、doBefore JSON 强解析加降级保护、@Around 仅返回类型可赋值 Response 才转失败响应且 Error 一律 rethrow（修事务不回滚）、嵌套切面保存-恢复外层 traceId、未知异常不外传 e.getMessage() 改通用文案。LogInterceptor：MDC 放裸 traceId 替代整个 JSON 串、printHttpRequestHeaderInfo 不再 new 包装器消费原始输入流（仅已被 Filter 缓存时打印）、afterCompletion BEGIN_TIMESTAMP 判空（-1=未计时）。装配：CatchLogAutoConfigure 补 OnClass(org.aspectj.lang.annotation.Aspect)+@ConditionalOnProperty(kdla.config.catch-log.enabled, matchIfMissing=true 默认开，B14 新键)+COBM。

**Blocked by:** 03

**Status:** done

依据：investigation.md 第 3 节 4/7/11/33/46/47/71/72 项、第 11.1 节矩阵。R openspec spec：traceid-atomicity（doBefore 无参不越界）作验收标准。

- [x] CatchLogAspect 七处修复按 T 分叉现状落地（不引入 R 的 "nonce" 语境）
- [x] LogInterceptor 三处修复落地
- [x] CatchLogAutoConfigure 装配条件落地（默认开）
- [x] R 的 log 相关测试回合，分叉项断言按 T 行为适配
- [x] `mvn -pl kdla-component-log -am clean compile` 与 test 绿
- [x] 独立 commit，不 push

## Comments

- 2026-11-14 完成。commit b65a58e。验证：`mvn -pl kdla-component-log -am clean test` BUILD SUCCESS，log 模块 5 测试类 13 用例全绿（上游 model 6 / common 56 / exception 10 同跑全绿）。
- 分叉判定（硬约束 6 基线 diff 验证）：LogInterceptor、CatchLogAutoConfigure 均 T==R 基线 6d617f2 → 整覆盖 R HEAD；CatchLogAspect 为分叉文件（T doBefore 直接生成 JSON 串 vs R 解析首参）→ 手术式适配。
- 适配决策：
  1. CatchLogAspect 键一律用 T 的 `CommonConstants.LOG_TRACE_ID`（"traceId"，A2 裁定），不引入 R static import 与 "nonce" 语境；doBefore 采用 R 解析首参结构（args.isEmpty() 守卫 + try-catch 降级 + traceObj 判空），同时修掉 T 旧实现 MDC 放 JSON 串的 46 项 T 侧 bug。
  2. around 日志暂不接 LogSanitizeUtil.truncate（票 06 范围），保持 T 现状 JSON.toJSONString；LogInterceptor 整覆盖后同理去除 sanitize 引用（body 直接打印，票 06 再接入三处）。
  3. CatchLogAutoConfigure 按 B14/S1 裁定 matchIfMissing=true（R 为 false）；javadoc 改"默认开启,可显式关闭"，并把 R 原文示例键路径 kdla.log.catch-log 对齐注解实际键 kdla.config.catch-log（R 文档自身与注解不一致，不继承）。
  4. log pom 补 provided jackson-databind（R 同款，无版本号）：LogInterceptor→JacksonUtil 运行期依赖，starter-web exclusions 排除了全部 jackson 导致 LogInterceptorTraceIdTest NoClassDefFoundError；provided 不透传宿主。
  5. 测试回合 5 类：AroundRethrow/NestedMdc/NoArgs/NonJsonArg/LogInterceptorTraceId；LogInterceptorTraceIdTest 与 NonJsonArgTest 注释中 3 处 nonce 语境改 traceId。CatchLogAspectLogTruncateTest 不回合——断言依赖 LogSanitizeUtil 截断行为，归票 06。
