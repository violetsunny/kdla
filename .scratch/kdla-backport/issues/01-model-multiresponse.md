# 01: model 模块回合（MultiResponse totalCount + 测试设施）

**What to build:** 下游调用 MultiResponse.buildSuccess(items, totalCount) 时 totalCount 不再被静默丢弃：新增 totalCount 字段+getter/setter 并在 buildSuccess 回填；单参重载默认 items.size() 行为不变；JSON 出参新增字段向前兼容。model 模块从零测试变为有测试锁定。Response.java 不动（B1 否决，保留 success 字段）。

**Blocked by:** None (can start immediately)

**Status:** done

依据：investigation.md 第 3 节 76/87 项（e3a4acd）、A1 裁定。R 参考：R/kdla-component-model MultiResponse 及其测试。

- [x] MultiResponse 新增 totalCount 字段，buildSuccess(items,totalCount) 回填，单参重载行为不变
- [x] Response.java 零改动（B1 否决）
- [x] model pom 补 spring-boot-starter-test(provided) + mockito-junit-jupiter（实际按 R model pom 镜像：仅 starter-test provided+排除 slf4j-api，R model 无 mockito，不多加）
- [x] R 的 model 测试类回合；Response 相关断言按 T 现状适配（删 successFieldAndBooleanSetter_removed 用例，余 4 用例保留）
- [x] `mvn -pl kdla-component-model -am clean compile` 与 `mvn -pl kdla-component-model test` 绿（Tests run: 6, Failures: 0, Errors: 0）
- [x] 独立 commit fe53887，只 stage 本票 4 文件，未 push

## Comments

- 2026-11-14 完成。buildFailure 保留 T 的 setSuccess(false)（R 侧该行随 B1 删除，T 不跟随）。LSP 的 Lombok/junit 报错为误报，mvn 实测绿。
