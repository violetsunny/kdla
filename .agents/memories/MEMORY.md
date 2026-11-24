# Memory

## 踩坑记录

- kdla T/R两仓共享GAV top.kdla.framework:*:1.0.1-SNAPSHOT，~/.m2被最后install方污染；mvn -pl单模块验证必须带-am用reactor产物，否则NoSuchMethodError。
- kdla 2.x：Spring 6.1 删除 LVT 参数名发现器，未开 -parameters 时 SpEL #参数名 静默失效；MdcAspect 已加字节码 LVT 回退，log/RequestLogAspect 同隐患未修。
- Netty EmbeddedChannel.advanceTimeBy 只平移虚拟时钟不跑任务，须紧接 runScheduledPendingTasks() 才触发超时回调。
- MqttPublishMessage.refCnt() 内部 ensureAccessible，归零后调用抛 IllegalReferenceCountException；断言前须先取出 payload ByteBuf。
- Spring 6.1 删 LVT 参数名发现器：同类型多 Bean 注入靠参数名消歧会启动炸 NoUniqueBeanDefinitionException，须显式 @Qualifier。
- Groovy SecureASTCustomizer 沙箱包前缀须写"包.**"：写"包."被 normalizeStarImports 转成"包.*"，致 endsWith(".") 白名单过滤永久失效。
- EC/SM2 hex 私钥须 new BigInteger(1,bytes)：new BigInteger(byte[]) 按符号解析，最高位为1得负数，抛 Scalar not in [1,n-1]。
- JDK21 ObjectInputStream.setObjectInputFilter 读取中抛 IllegalStateException；Externalizable.readExternal 内无法设过滤，须改长度前缀字节块+自建流。
- Vertx NetClientOptions.DEFAULT_HOSTNAME_VERIFICATION_ALGORITHM 为 null，net/MQTT 默认不校验 hostname，须显式置 "HTTPS"（HttpClientOptions 才默认开）。
- Groovy SecureASTCustomizer 不拦 AttributeExpression(.@)/MethodPointerExpression(.&)/MethodReferenceExpression(::)，沙箱须显式拒绝这三类。
- Vertx WebClient 默认 maxRedirects=16、HttpClient5 默认跟随重定向，302 指向内网绕过首跳 SSRF 校验，须显式关闭。
- 无状态HMAC会话下spring-security默认HttpSessionCsrfTokenRepository失效；CSRF token可用HMAC(会话token)派生+$.ajaxSetup单点注入，零依赖。
- kdla 的 .scratch/ 与 .agents/ 实际已被 git 纳管（58 文件），AGENTS.md 称未纳管是错的；审计报告明文生产 DB 口令已在 HEAD。

## 技术约定

- 本机 JDK 装在 /Library/Java/JavaVirtualMachines：jdk-1.8(8u491)、adoptopenjdk-8(8u292)、jdk-17、jdk-21；默认 java 是 21。kdla 1.x 分支可用 JAVA_HOME 指向 jdk-1.8 编译。

## 项目背景

- kdla 2.x 合并的是 1.0.0（cbf097f），丢失 1.0.1 的 19 个 fix 提交（merge-base 320529d）；多数 2.x bug 是丢失修复，可从 1.0.1 按文件恢复。审计报告 .scratch/jdk21-audit/report.md。

## 工作方式反馈

- kdla 安全审计：用户裁定 B-08 默认管理员核实、DB 口令轮换、git 历史清理、登录限速改 Redis 四项均不做（风险接受），勿再提议。
