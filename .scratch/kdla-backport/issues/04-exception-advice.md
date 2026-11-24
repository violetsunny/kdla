# 04: exception 模块回合（advice 全套修复 + 恢复注册默认开）

**What to build:** 下游不加 @Import 即获得统一异常响应（B7 裁定默认开）：UnifiedExceptionControllerAdvice 修复 DuplicateKeyException 误 import（snakeyaml→org.springframework.dao，409 分支真实生效）、删 NIO AccessDeniedException 误 import、认证异常正确绑定 spring-security AuthenticationException 并与 Security 403 一起拆入 @ConditionalOnClass 守护的内层静态 Advice、编码 null 回退 UTF-8、Throwable 兜底不回显 getMessage()、@Order(LOWEST_PRECEDENCE)、@ConditionalOnWebApplication(SERVLET)+@ConditionalOnProperty(kdla.exception.handler.enabled, matchIfMissing=true)；spring.factories 取消注释恢复注册；ResponseHandler 补 setMessage；pom 补 spring-jdbc(provided)+spring-security-core(provided,optional)。

**Blocked by:** 03

**Status:** done

依据：investigation.md 第 3 节 3/16/44/45/70/93/99/101 项、第 11.2 节矩阵、B7 裁定。CHANGELOG 知会项（下游异常响应格式自动改变）由票 15 统一记录。

- [x] advice 全套修复落地，Security/SASL 内层 Advice 有 @ConditionalOnClass 守护
- [x] spring.factories 恢复注册，matchIfMissing=true
- [x] ResponseHandler setMessage；pom 两依赖均 provided（security-core 另加 optional）
- [x] R 的 exception 测试类回合
- [x] `mvn -pl kdla-component-exception -am clean compile` 与 test 绿
- [x] 独立 commit，不 push

## Comments

- 2026-11-14 完成，commit 3f1d875（7 文件，+278/-20）。
- pom/spring.factories/UnifiedExceptionControllerAdvice 三文件 T==R 基线，整文件覆盖移植 R HEAD；3 测试类原样回合（与 B1 否决兼容：assertFalse(isSuccess()) 在 T 保留字段下成立）。
- ResponseHandler 手术式适配：保留 T 的 setSuccess(false)（B1 否决，R 删除了该行），仅补 setMessage(message) 双字段回填。
- 环境坑（已记 MEMORY）：~/.m2 的 model jar 被 R 仓 install 污染（R 版 Response 无 setSuccess(boolean)），`mvn -pl <模块> test` 不带 -am 会 NoSuchMethodError；后续所有票验证一律 `mvn -pl <模块> -am clean test`。
- 验证：`mvn -pl kdla-component-exception -am clean test` reactor 全绿（model+common 56+exception 10 用例）。
