# Standards 轴评审：kdla T 仓 7c0cf81...HEAD（9b5fff4/07ffd02/3f1d875）

范围：R→T backport 3 提交，22 文件。标准：项目 AGENTS.md + 全局 AGENTS.md + Fowler smell 基线。已与 R HEAD 逐字比对区分移植/分叉内容。

## 硬违规

无发现。逐项核验通过：
- JDK9+ 语法：全 diff 新增行扫描 var/record/text blocks/List.of/switch-> 等，零命中，JDK8 合规。
- 模块 pom 版本号：exception pom 新增 spring-jdbc/spring-security-core/spring-boot-starter-test 均无 `<version>`（根 pom L215 spring-boot-dependencies BOM 管理），合规。
- provided 约定：spring-jdbc、spring-security-core（+optional）均 provided，合规。
- spring.factories：UnifiedExceptionControllerAdvice 恢复登记；common 的 Mdc/StopWatch AutoConfigure 登记与 R 一致，合规。
- StopWatchAutoConfigure 删除的 EnableAspectJAutoProxy import 经核验为未使用 import（旧版即无注解使用），清理正当，符合外科手术式修改。

## Judgment call

1. **分叉默认值反转**：MdcAutoConfigure/StopWatchAutoConfigure 的 `matchIfMissing` T=true、R=false，同名配置键 `kdla.config.mdc.enabled`/`kdla.config.stop-watch.enabled` 在共享 GAV（top.kdla.framework:*:1.0.1-SNAPSHOT）的两仓语义相反。属刻意分叉，建议在 AGENTS.md 或提交说明留痕，防宿主混淆。
2. **test 依赖用 provided 而非 test scope**（exception pom，与 R 逐字一致；common pom 既有同模式）：provided 会进主代码编译 classpath，标准做法是 `<scope>test</scope>`（同样不传递）。convention beats novelty，可维持，但属次优。
3. **Duplicated Code（基线）**：AESUtil 与 Sm4Util 的 `v1:Base64(IV+密文)` 拼接/解析形状重复 4 处（AESUtil.encrypt/decrypt 两分支、Sm4Util.encryptV1/decryptCompat），可提取共享格式 helper。两文件均与 R 逐字一致，改动决策权在 R 仓，T 侧不单独修。
4. **同形 handler**（基线 Duplicated Code，轻微）：内层 SecurityAccessDeniedExceptionAdvice 两个 handler 均为 log.warn+buildFailure(UNAUTHORIZED)；且 403 状态码配 UNAUTHORIZED 错误码语义不一致。R 逐字内容，转 Spec 轴关注。
5. exception pom 文件末尾无换行符，trivial。

## 结论

T 适配段（MdcAspect bizCode 键、MdcHelp.restore、StopWatchWrapperAspect 注解定位、ResponseHandler.setMessage、MdcAspectTest 改写）均无规范问题；移植段按规格视为符合要求。
