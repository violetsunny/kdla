# 03: common MDC/StopWatch 链路分叉重定位 + 装配契约

**What to build:** 3 个分叉文件按 T 现状（MDC 键 "traceId"）重定位 R 的修复：MdcHelp（保留 T 的 TRACE_ID_KEY="traceId"）、MdcAspect（保存-恢复旧值替代 MDC.clear() 修嵌套丢外层 traceId + SpEL 解析降级 + 空值防护）、StopWatchWrapperAspect（signature.getMethod()+AnnotationUtils 判空）。CommonConstants 不动（T 保持 "traceId"，A2 裁定）。装配：MdcAutoConfigure/StopWatchAutoConfigure 的恒真 @ConditionalOnClass（同 jar 内部类）改指 org.aspectj.lang.annotation.Aspect，补 @ConditionalOnProperty（kdla.config.mdc.enabled / kdla.config.stop-watch.enabled，matchIfMissing=true 默认开，B14 新键），@Bean 一律 COBM——落地装配矩阵 11.1。

**Blocked by:** 02

**Status:** done

依据：investigation.md 第 2 节分叉表、第 3 节 1/2/39/40/64 项、第 11.1 节矩阵、A2/B14 裁定。

- [x] MdcHelp/MdcAspect/StopWatchWrapperAspect 按 T "traceId" 重定位，R 的 "nonce" 语境不引入
- [x] CommonConstants 零改动
- [x] 两个 AutoConfigure：OnClass(Aspect) + 新键 matchIfMissing=true + COBM
- [x] R 相关测试回合并按 T 键名适配
- [x] `mvn -pl kdla-component-common -am clean compile` 与 test 绿
- [x] 独立 commit，不 push

## Comments

- 2026-11-14 完成，commit 9b5fff4（7 文件，+237/-12）。
- MdcHelp：原样移植 R 的 restore(key, oldVal)；TRACE_ID_KEY="traceId" 保留。
- MdcAspect 分叉重定位：T 写 "bizCode" 键（R 写 LOG_TRACE_ID），故保存-恢复对象为 "bizCode"（新增私有常量 BIZ_CODE_KEY），finally 用 MdcHelp.restore 替代 clear()；assert params→null 返空串；SpEL try/catch 降级+null→""。
- StopWatchWrapperAspect：移植 signature.getMethod()+AopUtils.getMostSpecificMethod+AnnotationUtils.findAnnotation+null→proceed()；保留 T 的 logTitle（不带 traceId，R 的 trace 日志语境不引入）。
- 两个 AutoConfigure：@ConditionalOnClass(Aspect.class)+@ConditionalOnProperty(matchIfMissing=true，偏离 R 的 false，B14/S1 裁定默认开)；StopWatch 删未用 EnableAspectJAutoProxy import，javadoc 改"默认开启,可显式关闭"。
- 测试：StopWatchWrapperAspectTest 原样回合（2 用例）；MdcAspectTest 按 T 语义重写（嵌套 bizCode 保存-恢复、traceId 等其他键不被误清、无旧值退出移除、SpEL 降级，3 用例）。
- 验证：compile 绿；`mvn -pl kdla-component-common test` 46 用例全绿（41 旧+5 新）。
