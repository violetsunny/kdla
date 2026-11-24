# 核查报告：/Users/kangll13/aiot/java-code/self/gggggggood/kdla/AGENTS.md

核查时间：2026-11-17。核查对象共 17 行、4 条事实声明、4 个路径引用。
结论：**0 FAIL，3 WARN**。事实层全部准确，问题集中在结构（事实重复/易腐状态快照）与表达（中英空格）。

---

## 一、事实核查（逐条，含验证方式）

### 1.1 行 3：「项目规则（分支版本线、模块依赖方向、构建命令、版本与提交约定）见 `.agents/AGENTS.md`」

**判定：准确。**

验证方式：
- `ls -la /Users/kangll13/aiot/java-code/self/gggggggood/kdla/.agents/` → `AGENTS.md` 存在（3549 字节）。
- `grep -n '^#' /Users/kangll13/aiot/java-code/self/gggggggood/kdla/.agents/AGENTS.md` → 括号内四个主题逐一命中标题：
  - 分支版本线 → 行 5 `## 分支 = 技术栈版本线（动手前必查）`
  - 构建命令 → 行 18 `## 常用命令（已验证可跑）`
  - 模块依赖方向 → 行 27 `## 模块依赖方向（改 pom 前看，防成环）`
  - 版本与提交约定 → 行 31 `## 版本与提交约定`
- 括号列举是子集（.agents/AGENTS.md 另有「框架约定」「测试」两节），但原文措辞为「项目规则……见」，未声称穷尽，不构成事实错误。

### 1.2 行 3：「本文件承载 agent-skills 配置」

**判定：准确。** 全文（17 行）除指向 .agents/AGENTS.md 的一句外，仅含 Issue tracker / Triage labels / Domain docs 三节 agent-skills 配置，无项目规则串位。验证方式：通读 /Users/kangll13/aiot/java-code/self/gggggggood/kdla/AGENTS.md 全文。

### 1.3 行 9：「Issues 与 specs 以本地 Markdown 文件跟踪，位于 `.scratch/`。见 `docs/agents/issue-tracker.md`」

**判定：事实准确（但见 WARN-1 重复问题）。**

验证方式：
- `ls /Users/kangll13/aiot/java-code/self/gggggggood/kdla/docs/agents/` → `issue-tracker.md` 存在。
- 对照 docs/agents/issue-tracker.md 行 3：「Issues and specs for this repo live as markdown files in `.scratch/`」——声明一致。
- `ls /Users/kangll13/aiot/java-code/self/gggggggood/kdla/.scratch/` → 目录存在，含 `kdla-backport/`（内有 spec.md、issues/ 等），与 issue-tracker.md 行 7-9 约定的目录结构相符，实际在用。

### 1.4 行 13：「默认五标签词汇表。见 `docs/agents/triage-labels.md`」

**判定：准确。**

验证方式：
- `ls /Users/kangll13/aiot/java-code/self/gggggggood/kdla/docs/agents/` → `triage-labels.md` 存在。
- 对照 docs/agents/triage-labels.md 行 3「five canonical triage roles」及行 7-11 表格恰好 5 行（needs-triage / needs-info / ready-for-agent / ready-for-human / wontfix）——「五标签」数字准确。
- 「默认」措辞与 triage-labels.md 行 15「Edit the "Label in our tracker" column to match whatever vocabulary you actually use」（可改的默认值）一致。

### 1.5 行 17：「Single-context 仓库（根 GLOSSARY.md 与 docs/adr/ 目录，懒创建，当前均未生成）。见 `docs/agents/domain.md`」

**判定：当前时点事实准确（但「当前均未生成」是易腐快照，见 WARN-2）。**

验证方式：
- `ls /Users/kangll13/aiot/java-code/self/gggggggood/kdla/docs/agents/` → `domain.md` 存在。
- 对照 docs/agents/domain.md 行 15「Single-context repo (this repo)」——「Single-context 仓库」准确。
- 对照 docs/agents/domain.md 行 11「creates them lazily when terms or decisions actually get resolved」——「懒创建」准确。
- `ls /Users/kangll13/aiot/java-code/self/gggggggood/kdla/GLOSSARY.md /Users/kangll13/aiot/java-code/self/gggggggood/kdla/docs/adr` → 两者均 `No such file or directory`——「当前均未生成」于核查时点为真。

### 1.6 命令核查

本文件不含任何可执行命令（mvn/git 等均无），无需重跑，也不涉及 /tmp/kdla-full-test.log 所覆盖的测试声明。

---

## 二、发现清单（按严重度排序）

FAIL：无。

### WARN-1（结构·事实重复）行 9

> 原行：`Issues 与 specs 以本地 Markdown 文件跟踪，位于 \`.scratch/\`。见 \`docs/agents/issue-tracker.md\`。`

问题：「issues/specs 位于 .scratch/」这一事实的 canonical owner 是 docs/agents/issue-tracker.md 行 3，本行复述了它——若跟踪位置变更需同步改两处。
修法：删去事实复述，改为「Issue/spec 跟踪方式见 `docs/agents/issue-tracker.md`」。

### WARN-2（结构·易腐状态快照）行 17

> 原行：`Single-context 仓库（根 GLOSSARY.md 与 docs/adr/ 目录，懒创建，当前均未生成）。见 \`docs/agents/domain.md\`。`

问题：「当前均未生成」是时点快照，`/domain-modeling` 一旦懒创建这两个文件，本行即成事实错误且无人提醒更新；且 docs/agents/domain.md 行 11 明确态度是「不存在则 proceed silently，不要标记缺失」，入口文件反而固化了缺失状态，与 canonical 文档语义相悖。
修法：删「当前均未生成」，保留「懒创建」即可（如：「Single-context 仓库，根 GLOSSARY.md 与 docs/adr/ 懒创建。见 `docs/agents/domain.md`」）。

### WARN-3（表达·中英空格）行 3

> 原行：`……本文件承载 agent-skills 配置。`

问题：「agent-skills配置」中英文之间缺空格，与同文件行 17「Single-context 仓库」的排版不一致。
修法：改为「agent-skills 配置」。

---

## 三、职责核查（AGENTS = 规则入口）

无串位。根 AGENTS.md 只做两件事：把项目规则指回 `.agents/AGENTS.md`（行 3），承载 agent-skills 三项配置（行 5-17）。构建命令、版本约定、模块依赖等规则内容均未在本文件展开，符合「入口 + 链接」角色。

## 四、表达核查

无营销语、无 AI 腔公式化结构、无无证据断言（「五标签」「Single-context」「懒创建」均可在对应 canonical 文件中找到依据）。唯一表达问题是 WARN-3 的中英空格。

## 五、无法验证的声明

无。本文件全部声明均已通过只读命令或直接文件对照验证。

## 六、已核实清单汇总

| 声明 | 验证方式 | 结果 |
|---|---|---|
| `.agents/AGENTS.md` 存在且含四主题 | ls + grep 标题行 5/18/27/31 | 通过 |
| `docs/agents/issue-tracker.md` 存在且约定 .scratch/ | ls + 对照该文件行 3 | 通过 |
| `.scratch/` 存在且实际在用 | ls（含 kdla-backport/spec.md、issues/） | 通过 |
| `docs/agents/triage-labels.md` 存在且为五标签 | ls + 对照该文件行 3、7-11 | 通过 |
| `docs/agents/domain.md` 存在且为 Single-context、懒创建 | ls + 对照该文件行 11、15 | 通过 |
| 根 GLOSSARY.md、docs/adr/ 当前不存在 | ls 均报 No such file or directory | 通过 |
| 本文件无命令、无测试声明 | 通读全文 17 行 | 通过 |
