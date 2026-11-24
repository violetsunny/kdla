# 15: 文档/CHANGELOG 回合

**What to build:** CHANGELOG.md 回合 R 的 4 个版本块+补 2026-11-13 审计切片 1-4 版本块+新增本次回合版本块：记录 BREAKING 项（B2-B4/B6/B8-B13 接受、B1 否决 Response 保留 success 字段）、装配偏离（9 类默认开 vs R 全默认关，引用矩阵）、配置键采用 kdla.config.*、下游需知会事项（异常响应格式自动生效、自签证书需显式 trust-all、>30s 慢调用需调 timeout、配置错误 fail-fast 启动失败）。docs/architecture.md 与 docs/project-map.md 改写为 13 模块/enncloud 语境、以 git 为准（R 文档 6 处滞后不继承）。nit-inventory.md（53 项+V1-V8）带入 T 文档体系。README 按需修订。kdla-optimization.md 不回合。

**Blocked by:** 14

**Status:** done

依据：investigation.md 第 4/5 节、A3 裁定、第 11 节矩阵偏离记录。

- [x] CHANGELOG 完整（历史 4 块+切片块+本次块），BREAKING/偏离/知会事项齐全
- [x] architecture/project-map 无 R 语境残留（midea Nexus/9 模块/IntelliJ 说明），模块表=13
- [x] nit-inventory 带入；kdla-optimization.md 未引入
- [x] 独立 commit，不 push

## Comments

- 2026-11-14 review 待办（review-standards-2026-11-14.md judgment call #1）：CHANGELOG/文档需留痕同名配置键 kdla.config.mdc.enabled / kdla.config.stop-watch.enabled 在 T/R 两仓语义相反（T matchIfMissing=true 默认开，R false 默认关），且两仓共享 GAV，防宿主混淆。
- 2026-11-14 review 待办（票 17 Comments）：BREAKING 项——Sm4 新密文（v1: 前缀 CBC）旧代码不可解、存量 Hex 编码密钥不可读（统一 Base64 无兼容分支，B9）。
- 2026-11-16 完成（commit 23a793a，5 文件 +891 行）：
  - `CHANGELOG.md` 新建：2026-11-16 backport 块（BREAKING 全清单/B1 否决/装配契约偏离表+同名键相反警示/下游知会 5 条/收口修复）+ 2026-11-13 切片 1-4 补登块 + R 历史 4 块（10-29 块内联 [T 适配] 标注）。review 待办的同名键语义相反留痕已落实（装配契约节 ⚠️ 条目）。
  - `docs/architecture.md` 新建：13 模块架构图/依赖流向（以各模块 pom 实测为准）/模块表（含 T 装配默认值）/装配矩阵/配置键速查（T 默认值，含 trust-all、request-timeout）/数据流 3 图/已知限制（K1/K3/K5 待处理 + P1 待裁定，K2/K4 已修）/FAQ（异常拦截 T 默认开无需 @Import、SpEL 限制、enncloud 发布）。
  - `docs/project-map.md` 新建：13 模块目录树（含 reactor/dingding/monitor/job）、依赖关系、测试入口（-am 警示）、64 类/171 用例、版本线表。
  - `docs/nit-inventory.md`：自 .scratch 复制带入（53 项 + T1-T3 + P1-P20 + V1-V8）。kdla-optimization.md 未引入。
  - `README.md` 修订：头部补 13 模块/enncloud 一句话、文档导航节、GAV 共享警示、装配默认值速查。
  - 验证：grep midea/rdc-/intellij/openspec 四文件零命中；docs/agents/ 三件套与根 AGENTS.md 未 stage（黑名单/票范围外）；未 push。
  - R 文档 6 处滞后均未继承：Redisson 默认开误述→T 实况矩阵；4 测试类→64/171；K1/K2 状态→按现状；kdla-optimization 路径→不引入；Critical 14 vs 15→CHANGELOG 用 14；缺切片块→已补登。
