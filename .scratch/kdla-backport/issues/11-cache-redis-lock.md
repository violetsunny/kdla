# 11: supplement-cache Redis/分布式锁回合

**What to build:** SpringRedisHelp：23 处 e.printStackTrace()→log.error+@Slf4j、unLock Lua 脚本 key/value 改经 RedisTemplate 序列化器（修解锁永远静默失败）、lock() setIfAbsent 返回值 Boolean.TRUE.equals 防拆箱 NPE、getExpire 标 @Deprecated（T 从未删过，仅加注解）。RedissonConfigProperties/RedissonLockFactory：地址解析 fail-fast（空/非法节点启动即抛 IllegalArgumentException，B6）、REPLICATE 模式 passwords/nodes 数量校验、去除 config.toYAML() 密码明文 INFO 日志改记非敏感项。RedissonAutoConfigure：OnClass 改真实 RedissonClient、kdla.cache.redisson.enabled matchIfMissing=true 保持默认开（B5/S1）、锁前缀 appId 缺失回落固定值 "kdla"+log.warn（修多实例互斥失效）、注册 DistributeLockedAspect、COBM。KdlaBizDisLockService.biz()：无限 tryLock→有界 tryLock(waitTimeMs,leaseTimeMs)、unlock isHeldByCurrentThread 守卫、InterruptedException 恢复中断位、入参判空。RedissonRedDisLock.lock(lockKey)：null TimeUnit 防御+中断恢复。DistributeLockedAspect：删 blanket catch(Exception)（业务异常透传）、unlock 包 try/catch 防覆盖业务异常、SpEL StandardEvaluationContext→SimpleEvaluationContext.forReadOnlyDataBinding()、移除 @Component。新增 KdlaLockProperties（kdla.lock.waitTimeMs 默认 3000/kdla.lock.leaseTimeMs 默认 30000）。合并 09 审计归属本票的补项。

**Blocked by:** 09

**Status:** done

依据：investigation.md 第 3 节 5/6/10/14/15/57/65/67/82/90/92 项、第 11.1/11.2 节矩阵、B5/B6 裁定。R openspec spec：distributed-lock-safety 作验收标准。

- [x] 上述修复全部落地，distributed-lock-safety spec 三条（业务异常透传/SpEL 受限/unlock 守卫）测试通过
- [x] RedissonAutoConfigure 装配落地（默认开+真实 OnClass+锁前缀回落）
- [x] supplement-cache pom 补 test 依赖；R 相关测试回合+审计补项测试
- [x] `mvn -pl kdla-component-supplement-cache -am clean compile` 与 test 绿
- [x] 独立 commit（可按 RedisHelp/Redisson/锁分多个），不 push

## Comments

- 2026-11-14 票 09 审计并入 2 项差额（详见 audit-supplement.md）：
  1.【高】DistributeLockedAspect.getLock 只构造不加锁，@DistributeLocked 形同虚设业务永远放行；R 修 tryLock(0,-1)+isHeldByCurrentThread+LockFailException——行为变更（原先恒放行→加锁失败抛异常），必须写入 CHANGELOG（票 15）
  2.【低】RedissonClientConfig.java 整文件注释残留删除（R 已删，项 91 有载但此前无票归属）

- 2026-11-14 完成。commit d675e97。验证：`mvn -pl kdla-component-supplement-cache -am clean test` BUILD SUCCESS，cache 2 测试类 7 用例全绿（上游同跑全绿）。distributed-lock-safety 三条落点核验：业务异常透传（BizExceptionTest 2 用例）、SpEL SimpleEvaluationContext.forReadOnlyDataBinding（DistributeLockedAspect:77）、unlock isHeldByCurrentThread 守卫（Aspect:116 + KdlaBizDisLockService:110）。
- 分叉判定：supplement-cache 全部文件 T==R 基线零分叉，R 工作区干净（B 组无涉及）→ 8 文件整覆盖 R HEAD（SpringRedisHelp/RedissonConfigProperties/RedissonLockFactory/RedissonAutoConfigure/KdlaBizDisLockService/RedissonRedDisLock/DistributeLockedAspect + 新增 KdlaLockProperties）。
- RedissonClientConfig.java（T-only 整文件注释残留）已 git rm（项 91，R 已删）。
- 装配：RedissonAutoConfigure 按 B5/S1 改 matchIfMissing=true（R 为 false，T 现状已注册生效保持默认开），javadoc 同步；OnClass 真实 RedissonClient、锁前缀 appId 回落 "kdla"+log.warn、DistributeLockedAspect/KdlaLockProperties 经 @Bean/@EnableConfigurationProperties 注册（R HEAD 自带）。spring.factories 本票不动（T 现状已含 RedissonAutoConfigure；Cache/SequenceNo 注册归票 12，避免提前注册未修复的 CacheAutoConfigure @PreDestroy 启动炸）。
- pom 整覆盖 R HEAD（唯一差异=test 块：starter-test+mockito-junit-jupiter provided）。测试回合 2 类零适配（无 nonce）。
- 票 15 CHANGELOG 必记：DistributeLockedAspect getLock 恒放行→tryLock 失败抛 LockFailException（行为变更）；Redisson 默认开偏离 R。
