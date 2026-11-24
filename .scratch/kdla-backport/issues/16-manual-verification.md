# 16: 人工验证清单交付（V1-V8 + 新增项）

**What to build:** 整理人工验证清单落盘 .scratch/kdla-backport/manual-verification.md 并交用户执行回贴：V1-V8（Redisson fail-fast/真实 Redis 锁互斥/sequence 并发/NettyHttpServer 端口/MQTT 线程泄漏/Vertx HTTPS 默认策略/VertxHttpClient 行为/宿主试点接入）预期值按装配矩阵调整（默认启动应装配 Mdc/StopWatch/CatchLog/Redisson(有 redisson 库)/VertxHttp(有 vertx 库)/exception advice；不应装配 RequestLog/Cache/rest-http/mqtt/cors）；新增项：Sm4/AES v1: 前缀混部新旧互解、Vertx trust-all 默认 false 下自签证书行为、request-timeout 30s 生效、Redisson 地址 fail-fast 启动失败、Response 保留 success 字段的序列化输出不变、钉钉告警链路（VertxHttp 联动注入后仍可用）。每项含[生产]/[测试]环境标注与一行可执行命令/步骤。用户回贴前不宣布回合完成。

**Blocked by:** 14

**Status:** waiting-user-results（清单已交付，用户回贴 PASS/FAIL 前不宣布回合完成、不置 done）

依据：investigation.md A5 裁定、nit-inventory.md V1-V8 附表、第 11 节矩阵 V8 预期值联动。

- [x] manual-verification.md 落盘，V1-V8 预期值按矩阵更新
- [x] 新增 6 项验证步骤完整（环境标注+可执行步骤+预期结果）
- [x] 清单交用户，明确「回贴前不宣布完成」

## Comments

- 2026-11-16 落盘 `.scratch/kdla-backport/manual-verification.md` 并交用户：V1-V8 预期值按 T 装配矩阵更新（V8 应装配 12 类/不应装配 8 类）；新增 N1-N6（N2/N3/N4 与 V6/V7②/V1 重叠，标注合并执行）；附 P1 裁定项与回贴模板。V3 键名按 T 实况修正为 `kdla.sequence.enabled`（nit 原文 `kdla.cache.sequence.enabled` 为旧键名）。N4 附加警示：T 默认开 Redisson，宿主有 redisson 库但未配 address 的升级后启动即失败。
- 待用户回贴后：勾 PASS 项、FAIL 项开修复票，全部 PASS + P1 裁定落定后方可置 done 并宣布回合完成。
- 2026-11-16 P1 裁定落定：用户选**最小修复**（否决 fail-fast 方案）。commit 30490b8：`MachineIdHelp.init` 改 SETNX 注册成功才发布静态字段，失败路径（Redis 异常/1024 槽耗尽）保持 null → getGenerator 走随机回退+ERROR 告警；槽耗尽补 ERROR 日志；+2 回归测试（MachineIdHelpFailPathTest，反射重置静态字段防跨用例污染）。`mvn -pl kdla-component-supplement-cache -am clean test` 17 用例全绿（全仓 173）。同步更新 docs/nit-inventory.md、docs/architecture.md（P1 移入已修脚注）、CHANGELOG.md、manual-verification.md（P1 项关闭）。R 侧未修，两仓该类出现 diff（已在 CHANGELOG/nit 登记）。剩余待办：仅 V1-V8/N1/N5/N6 用户回贴。
