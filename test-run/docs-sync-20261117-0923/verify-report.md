# docs-sync 核查汇总报告（verify-report.md）

- 执行时间：2026-11-17
- 技能：robot-aiot-docs-sync
- 验证目录：test-run/docs-sync-20261117-0923/
- 新鲜度头口径：Files scanned 367 = 344 个 .java + 14 个 pom.xml + 9 个 src 下 spring.factories（find 实测）

## 三道门结果

| 门 | 结果 |
|---|---|
| 脚本审计（基线） | exit=1：6 FAIL（路径引用）+ 2 WARN（缺新鲜度头）→ 已全部修复，见 audit-baseline.txt |
| 脚本审计（收口） | exit=0，0 FAIL / 0 WARN，见 audit-final.txt |
| 新鲜度交叉比对 | 5 文件最后修改均 ≤1 天，Flagged 空 |
| fresh-context 事实核查 | 5 个并行 subagent，报告：verify-AGENTS.md / verify-README.md / verify-CHANGELOG.md / verify-project-map.md / verify-architecture.md |
| 二轮 scoped 核查（改动行） | 6 PASS / 0 FAIL，见 verify-round2.md |

## FAIL 修复记录（8 条，全部经主 agent 对照源码/pom 复核属实后修复）

| # | 文件:位置 | 问题 | 修复 |
|---|---|---|---|
| 1 | architecture.md 自动配置机制 | "3 类无开关靠 OnClass 守护"错（DomainAutoConfigure 无条件注册）；"6 类默认关"与自身矩阵（8 类）矛盾 | 改为 9 类带开关默认开 + 3 类无开关（Domain 无条件；DingAlert/Prometheus OnClass 守护）+ 8 类默认关 |
| 2 | architecture.md 已知限制脚注 | K2/K4 日期（2026-08-28/09-04）为参考仓日期，本仓 git log 无对应提交 | 标注"于参考仓修复，本仓经 2026-11-16 backport 回合"；P1 标注本仓直接修复 |
| 3 | architecture.md FAQ 发布节 | server ID `kdla-nexus` 与根 pom 不符 | 改 `maven-releases` / `maven-snapshots`（对照 pom distributionManagement） |
| 4 | project-map.md 项目信息表 | 同上 server id 错误 | 同上 |
| 5 | project-map.md 目录树 | `KdlaHttp` 类不存在 | 改 `KdlaHttpClient`（ls 实测 http/ 目录） |
| 6 | CHANGELOG.md 回合概览 | "13 张实现票"与票目录矛盾（票 17 也是实现票） | 改"14 张实现票（01-13、17）+ 3 张收口票（14/15/16）" |
| 7 | README.md 第 2 行 | 项目名拼写 `Layed` + 残留 `&amp;` 实体，与 architecture.md 矛盾 | 改 "K-DDD-Layered Application & Architect" |
| 8 | AGENTS.md line 17 | `docs/adr/` 反引号路径不存在（懒创建）导致审计 FAIL；"当前均未生成"为易腐快照 | 去反引号 + 删快照短语 |

另：基线审计 6 条路径 FAIL（META-INF/spring.factories ×2、~/.m2 ×2、src/test/java/、docs/adr/）改为 glob 形式（`*/src/main/resources/...`）、`$HOME/.m2` 写法与真实模块前缀路径；2 条新鲜度头 WARN 已补 `<!-- Generated: 2026-11-17 | Files scanned: 367 -->`。

## WARN 处置（28 条：采纳 1，驳回 3，其余呈现给用户不修）

- **采纳**：AGENTS.md line 17 易腐快照（已修，见上表 #8）。
- **驳回 1**：AGENTS.md line 3 "agent-skills配置缺中英空格"——实测该行已有空格，核查员误报。
- **驳回 2**：CHANGELOG "装配矩阵越位复制"——票 15 明确要求 CHANGELOG 记录装配契约偏离（BREAKING 知会的一部分），非越位；矩阵细节仍链接 investigation/architecture。
- **驳回 3**：CHANGELOG ".scratch 死链"——`.scratch/kdla-backport/` 是有意不入库的单一真源，文中为路径说明非 Markdown 链接，脚本 links 检查通过。
- **呈现不修（默认报告不重写，待用户认可）**：
  - README：9 个悬空冒号占位小节（原作者骨架，如 "业务："）；模块清单与 project-map 职责重叠；入口缺 Maven 坐标快速开始段。
  - AGENTS.md：line 9 一句话概述 issue-tracker 约定（入口导读，可接受）。
  - 其余细节见各 verify-*.md 分报告。

## 范围外发现（Flagged，交用户）

`.agents/AGENTS.md`（项目规则真源，不在本技能 5 文件范围）存在 3 处过时：
1. "当前 7 个模块有 spring.factories" → 实际 9 个（backport 补注册 validator/supplement 等后）。
2. "仅 3 个模块有 src/test：common（23 测试类/56 用例）、exception（3）、model（2）" → 实际 9 个模块 65 测试类 / 173 用例（common 56、exception 10、model 6、domain 8、dal 16、validator 28、supplement 7、cache 17、log 25）。
3. 测试段 "kdla-component-common（23 测试类 / 56 用例）" 类数过时（现 24 类含新增）。

## 追加：README 补全更新（2026-11-17，用户认可 WARN 后执行）

- 重写 README.md：首屏三问（是什么/适合谁/怎么开始）；新增安装（Maven 坐标 + provided 说明 + settings server ID）、快速开始（默认装配即生效 + 关/开开关 yaml 示例）、模块概览表（13 行，canonical 链接 project-map）、开发节（编译/测试/-am 警示/deploy/版本线与提交约定）。
- 删除内容（归属裁决）：model/common 两节约 30 个源码文件链接清单与 common spring.factories 代码块——模块清单 canonical owner 为 docs/project-map.md，README 改为索引表+链接；9 个悬空冒号占位小节补全为可溯源职责描述（源自 architecture.md 模块表）。
- scoped 事实核查（verify-README-round2.md）：10 PASS / 0 FAIL / 1 WARN；WARN 已采纳——速查补注"Domain / 钉钉告警 / Prometheus 无开关，不可配置关闭"。
- 收口审计重跑 exit=0。
