# 12: supplement-cache Sequence/MachineId/本地缓存回合

**What to build:** SequenceNoGenerator/V2：规则 split 长度校验抛 BizException、异常时 noCacheMap.remove(code) 替代 clear()（防雪崩）、new Long()→Long.valueOf、StringBuffer→StringBuilder、V2 非缓存模式补 updateById() DB 写回（修重复流水号）、双锁单解死锁删除+isHeldByCurrentThread 守卫、溢出抛 BizException 禁止清零重号（B12）、日期翻越重置先于溢出检查、长度段非数字显式抛 BizException、异常根因透传、V2 generateSequenceNo 补 cfg 判空。SequenceNoConfigure：注册闭环（真实 OnClass(RedissonClient)+AutoConfigureAfter(RedissonAutoConfigure)+OnBean(RedissonRedDisLock)，联动注入）+3 配置键 @ConditionalOnExpression OR 语义。MachineIdHelp：Redis SETNX 原子注册（修雪花 machineId 竞态）、destroy SETNX 覆盖改 del+判空（B13，消除 1 秒阻塞窗口）、随机回退取号 INFO→ERROR+localIp 字段赋值修复、空值防护。KdlaCafCache：expireAfterWrite→expireAfter(Expiry) 使 per-key TTL 真实生效。CacheAutoConfigure：@EnableCaching 拆隔离内部静态类+kdla.cache.enabled 默认关+OnBean(RedisTemplate)（矩阵 11.3）。supplement-cache attachment/README.md 配置键文档修正（supplement.sequence.enable→supplement.cache.sequence.enable）。合并 09 审计归属本票的补项。

**Blocked by:** 09, 11（SequenceNoConfigure 闭环依赖 11 票改造后的 RedissonAutoConfigure）

**Status:** done

依据：investigation.md 第 3 节 8/12/23/36/37/62/63/66/68/69/94/95 项、第 11.2/11.3 节矩阵、B12/B13 裁定。R openspec spec：sequence-no-correctness 作验收标准。

- [x] Sequence 全修复落地，sequence-no-correctness spec 三条（非缓存必须持久化/缓存失效限定失败 code/规则格式前置校验）测试通过
- [x] MachineIdHelp SETNX+del 落地（Nit S3 del 竞态只登记不修）
- [x] SequenceNoConfigure 闭环（K4）+CacheAutoConfigure 隔离默认关落地
- [x] R 相关测试回合+审计补项测试
- [x] `mvn -pl kdla-component-supplement-cache -am clean compile` 与 test 绿
- [x] 独立 commit，不 push

## Comments

- 2026-11-14 票 09 审计并入 4 项差额（详见 audit-supplement.md）：
  1.【高】CacheAutoConfigure @PreDestroy 带参违反 JSR-250——本票注册该类后启动即炸，必须同批修
  2.【中】KdlaCafCache.put assert 失效+先设过期后 put 致 per-key TTL 恒无效——不与项 68（expireAfter）同批修则 68 白修
  3.【中】SequenceNoGeneratorV2 customizePool 无判空 NPE+空池无 BizException
  4.【低】KdlaGcache.put assert 失效，null 静默入缓存

- 2026-11-14 完成。commit d1e2bd3。验证：`mvn -pl kdla-component-supplement-cache -am clean test` BUILD SUCCESS，cache 模块累计 10 测试类 15 用例全绿（本票新增 8 类 8 用例；上游同跑全绿）。sequence-no-correctness 三条落点：非缓存必须持久化（V2NonCacheTest）、缓存失效限定失败 code（LockReleaseTest/V2LockReleaseTest）、规则格式前置校验（InvalidLengthTest/OverflowRejectTest/RolloverTest）。
- 分叉判定：7 文件均 T==R 基线零分叉（票 11 已全量核验）→ 整覆盖 R HEAD（KdlaCafCache/KdlaGcache/MachineIdHelp/CacheAutoConfigure/SequenceNoGenerator/SequenceNoGeneratorV2/SequenceNoConfigure）；attachment/README.md R-CHANGED → 整覆盖（配置键 supplement.sequence.enable→supplement.cache.sequence.enable 修正）。
- spring.factories 整覆盖 R HEAD：注册 3 项（RedissonAutoConfigure 已有 + 新增 CacheAutoConfigure/SequenceNoConfigure）。CacheAutoConfigure 默认关（kdla.cache.enabled matchIfMissing=false，矩阵 11.3 一致，@PreDestroy 带参已随 R HEAD 修复，注册不炸）；SequenceNoConfigure 联动闭环（OnClass(RedissonClient)+AutoConfigureAfter(RedissonAutoConfigure)+OnBean(RedissonRedDisLock)+3 键 OR 表达式，矩阵 11.2/K4）。
- 审计 4 项差额均随 R HEAD 覆盖落地：CacheAutoConfigure @PreDestroy、KdlaCafCache put assert+expireAfter(Expiry) per-key TTL、V2 customizePool 判空、KdlaGcache put assert。Nit S3（MachineIdHelp del 竞态）只登记不修（P2 裁定）。
- 测试回合 8 类零适配（无 nonce）。票 15 CHANGELOG 需记：MachineIdHelp SETNX 原子注册+destroy 改 del（B13）、溢出拒绝不清零（B12）。
