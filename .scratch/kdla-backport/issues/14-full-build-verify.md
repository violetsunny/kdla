# 14: 全量收口验证（compile + test 全绿）

**What to build:** 全部实现票完成后，全仓收口验证：`mvn clean compile` 与 `mvn test` 全绿（JDK8 target 1.8，系统 JDK21 编译）；跨模块回归 162 用例+适配项；核对 git log 每票独立 commit、无无关文件被 stage、无 push 发生；修复收口发现的残留问题（仅限本次改动引入的问题，历史遗留登记不修）。

**Blocked by:** 01-13 全部

**Status:** done

依据：spec.md Testing Decisions 接缝②。

- [x] `mvn clean compile` 全绿，输出摘要落盘 .scratch/kdla-backport/verify-14.md
- [x] `mvn test` 全绿（失败数=0，跳过数记录在案），输出摘要落盘同文件
- [x] git log 核对：每票独立 commit、提交信息风格一致、工作区干净
- [x] 残留问题修复或登记，无静默跳过（失败可见原则）

## Comments（2026-11-16 执行）

- **验证**: 全仓 `mvn clean compile` 14/14 SUCCESS；`mvn clean test` 171 用例全绿（0 失败 0 错误 0 跳过）；修复后最终复核再次全绿。摘要落盘 verify-14.md。
- **git 核对**: fe53887^..HEAD 每票独立 commit、风格一致、未 push、工作区干净。
- **残留问题（失败可见，全部处置）**:
  1. 黑名单混入: 票 08/10/11 误 stage 20 个 IDE 噪音文件 → 清理 commit 6b7bdb9（git rm --cached + .gitignore 加固），不改写历史（非破坏性，hash 引用保持有效）
  2. Spec 遗漏: CacheAutoConfigure 缺 OnBean(RedisTemplate)（矩阵 11.3）+ DistributeLockedAspect 缺 OnClass(Aspect)（矩阵 11.2）+ SqlFilter 缩进 → 修复 commit fb4cff4
  3. 组合放大型（本次注册/默认开 × R 代码）: advice DuplicateKey 移入 OnClass 守护内层 + CacheAutoConfigure ObjectProvider → 修复 commit 8f6af69
  4. R 上游同源 20 项: 逐项 diff R 核验 SAME 后登记 nit-inventory.md P1-P20（不单边修，保 T/R 一致）；其中 P1（MachineIdHelp 脏值撞号，数据正确性）高亮交底 16 报告建议 R/T 同步修交用户裁定；P11-P14（BREAKING）交底 15 CHANGELOG
- **质量校验层**: code-review（Standards 零硬违规/Spec 零 creep）+ java-code-review（Critical 零）+ ponytail-review 三技能 sub-agent 并行执行完毕，裁定明细见 verify-14.md 第 5 节。
- **口径修正**: 用例数以实际 171 为准（spec 预估 162），票 15 文档采用。
- 本票 3 个 commit: 6b7bdb9 / fb4cff4 / 8f6af69。
