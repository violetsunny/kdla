# Spec 轴评审报告（2026-11-14）

评审范围：T 仓 `git diff 7c0cf81...HEAD`（9b5fff4 票03 / 07ffd02 票17 / 3f1d875 票04，22 文件），规格源=三张票据+investigation.md 第 2/10/11 节，参照 R 仓 HEAD。

## A. 逐字移植保真度

- 票 17：Sm4Util/Sm2Util/AESUtil + 5 测试类（Sm4UtilCbcMigrationTest/Sm4UtilKeyCodecTest/Sm2UtilSecureKeyPairTest/Sm2UtilTest/AESUtilTest）共 8 文件，`tr -d '\r'` 后与 R HEAD 逐字一致。**无发现**。
- 票 04：exception 的 pom.xml、spring.factories、UnifiedExceptionControllerAdvice.java、3 测试类（ResponseHandlerTest/UnifiedExceptionControllerAdviceTest/SecurityAccessDeniedAdviceConditionTest）共 6 文件与 R HEAD 逐字一致。**无发现**。
- 票 03 声明"StopWatchWrapperAspectTest 原样回合"：与 R HEAD 逐字一致，属实。

## B. 分叉适配正确性

逐项核对通过，**无发现**：

- MdcHelp：`TRACE_ID_KEY="traceId"` 保留（MdcHelp.java:10）；restore() 方法体与 R 逐字一致（T 多一段 javadoc，纯注释非偏离）。
- MdcAspect：新增 `BIZ_CODE_KEY="bizCode"`，保存 oldBizCode→finally `MdcHelp.restore(BIZ_CODE_KEY, oldBizCode)` 替代 clear()；`assert params` 改 null→返空串；SpEL try/catch 降级+null→""。符合票 03 "T 写 bizCode 键…保存-恢复对象为 bizCode"。
- StopWatchWrapperAspect：signature.getMethod()+AopUtils.getMostSpecificMethod+AnnotationUtils.findAnnotation+null→proceed() 全落地；logTitle 及 3 处日志均不含 traceId，R trace 日志未引入。符合"保留 T 的 logTitle"。
- Mdc/StopWatchAutoConfigure：均 @ConditionalOnClass(Aspect.class)+@ConditionalOnProperty(kdla.config.mdc.enabled / kdla.config.stop-watch.enabled, matchIfMissing=true)，@Bean 级 COBM 保留；StopWatch 删 EnableAspectJAutoProxy import。符合 B14/S1"默认开，刻意偏离 R 的 false"及矩阵 11.1。
- ResponseHandler：`setSuccess(false)` 保留、仅补 `setMessage(message)`。符合 B1 否决"T 保留 success 字段"。
- CommonConstants、model/Response.java 均不在 diff 中，零改动。符合 A2/B1。

## C. 票据勾选项核对

- 票 04：pom 含 spring-jdbc(provided)、spring-security-core(provided+optional)；spring.factories 已取消注释注册 UnifiedExceptionControllerAdvice。勾选属实。
- 票 17/03 各勾选项对应的代码改动均在 diff 中落地（见 A/B 节）。
- 备注：三票勾选的"compile/test 绿"为执行方声明，本次评审未复跑（禁写约束），归验证轴复核。

## D. 范围蔓延

diff 恰 22 文件 = 票03(7)+票17(8)+票04(7)，与各票 Comments 声明的文件数一致，无清单外文件、无票据外行为改动。**无发现**。

## 结论

三提交忠实实现票据规格，A/B/C/D 四轴均无缺失、部分实现、实现错误或范围蔓延。
