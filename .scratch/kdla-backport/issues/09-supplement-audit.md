# 09: supplement/supplement-cache 聚焦审计（P4 差额补齐）

**What to build:** R openspec proposal 记载 supplement 必改 33 项，但约 20 项行号在审计中截断丢失，仅 32ba8d2 的 13 项有实修记录。派 subagent 对 T 的 kdla-component-supplement 与 kdla-component-supplement-cache 两模块重跑一轮聚焦审计（基准=R openspec proposal 的 33 项范围+R 仓对应代码现状），产出差额清单：哪些项 R 已修而 T 未列入 10/11/12 票、哪些项 R 也未修（登记不修）。审计只读不改代码。

**Blocked by:** None (can start immediately，可与 01-08 并行）

**Status:** done

依据：investigation.md 第 6 节无法判定项 1、P4 裁定。

- [x] subagent 审计报告落盘 .scratch/kdla-backport/audit-supplement.md（每项含证据 路径:行号）
- [x] 差额项清单回填 investigation.md（第 12 节）
- [x] 无需新票：15 项差额全部并入票 10（+8）/11（+2）/12（+4），三票已追加 Comments
- [x] R 也未修的项（Nit S1-S6 核验）无新增，nit-inventory 维持原状

## Comments

- 2026-11-14 完成（subagent ses_e2821e42dffeQid6uDlbR74qzI）。核心结论：32ba8d2 的 13 项实修已被 10/11/12 票全覆盖，「约 20 项丢失差额」实为行号级计数 vs 根因级归并的口径差+更早提交已修+装配缺口被 1fa3289 闭环核销；真正遗漏是 investigation.md 对 bb8022f 欠登记 9 项+R 工作区未提交修复 5 项+RedissonClientConfig 残留 1 项，共 15 项，高优先 5 项（CsvUtil 全废/CacheAutoConfigure @PreDestroy 带参启动炸/DistributeLockedAspect 不加锁/MqttChannelHandler 悬挂/NettyHttpClient setSsl 无效）。
