# 02: common 工具类修复（13 项）+ 测试设施

**What to build:** common 模块 13 个工具类的已知缺陷全部修复并有测试锁定：BigDecimalHelp（toString(20) 笔误→十进制、compareTo 判零）、KdlaStringHelp（驼峰 contains 方向）、BytesUtil（floatToLe 字节序、reverse(data,offset,length) 错位+越界显式抛 IndexOutOfBoundsException，无参重载不变）、LocalDateTimeHelp（YearMonth 跨年误判）、DateHelp（按 pattern 分派解析）、PinYinHelp（空串防护）、JacksonUtil（异常带 cause）、ObjectUtil（flush 防截断）、FileChunkTransmitHelp（append→覆盖写、IOException 上抛、流级联关闭、try-with-resources）、ThreadPoolHelp（每池独立有界队列、单线程池 LinkedBlockingQueue、Builder 全参默认值防拆箱 NPE）、AsyncThreadHelp（重建前 shutdown 旧池、volatile+synchronized 防并发重建、prettyPrint 判空、awaitTermination 后恢复中断位）、LocalIpHelp（getLocalIp4Address static volatile 缓存）、SelfSnowflakeGeneratorHelp（check-then-act→AtomicInteger.updateAndGet，修 traceId 并发重复）。

**Blocked by:** 01

**Status:** done

依据：investigation.md 第 3 节 9/13/28-31/41-43/73/77 项（d6efa60/d7d7bfe/7d86361/b9695b2）。R openspec spec：traceid-atomicity 作验收标准。

- [x] 13 项修复落地（基线核验 13/13 与 R 初始导入一致后整文件移植，零分叉）
- [x] common pom 零改动（与 R 逐字一致，test 依赖 T 已有）
- [x] R 的 common 测试类回合 16 个（MdcAspectTest/StopWatchWrapperAspectTest 留票 03，5 个加密测试留票 17），traceid-atomicity 相关 SelfSnowflakeGeneratorHelpTest 通过
- [x] `mvn -pl kdla-component-common -am clean compile` 与 test 绿（Tests run: 41, Failures: 0, Errors: 0）
- [x] 独立 commit 7c0cf81（29 文件），未 push

## Comments

- 2026-11-14 完成。发现拆票遗漏：R common 改动含 AESUtil/Sm2Util/Sm4Util 无票归属，已补立票 17。JDK21 跑 target 1.8 测试全绿，mockito 未报错。
