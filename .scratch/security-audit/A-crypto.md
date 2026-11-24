# kdla 加密与数据安全专项审计报告（A02 Cryptographic Failures / A08 Data Integrity Failures）

- 仓库：`/Users/kangll13/aiot/java-code/self/gggggggood/kdla`，分支 `2.0.0-SNAPSHOT`（JDK 21 / Spring Boot 3.4.0）
- 审计日期：2026-11-24。只读审计，未改任何源码/pom，未跑 mvn，未做 git 操作。
- 范围：`kdla-component-common`（utils/help）、`kdla-component-supplement-trdplatformcloud`（JwtUtil/StringUtil 及其调用方）、`kdla-component-supplement-cache`（lock/properties/sequence）、`kdla-component-model`。
- 方法：逐文件读实际代码；grep 全仓核实调用方；对 hutool 5.8.36 解包 `~/.m2` 中 sources jar 静态核查 JWT 验签实现；对 JDK 21 `src.zip` 核查 `SecureRandom(byte[])` 算法选择逻辑。
- 框架上下文判级口径：(a) 框架真实漏洞、下游难缓解；(b) 下游预期覆盖的不安全默认；(c) 纯下游责任。几乎所有第三方依赖 provided，exploitability 按下游真实 classpath 与用法评估。

## 严重度计数

| 严重度 | 数量 |
|---|---|
| Critical | 0 |
| High | 0 |
| Medium | 6（C-01 ~ C-06） |
| Low | 7（C-07 ~ C-13） |

---

## 已知开放项核实结论（先读代码，不盲信前序结论）

1. **E-05（trd StringUtil.hashString fail-open）——属实**。`StringUtil.java:68-76` catch 后 `return message`。全仓 grep 核实：仓库内 0 个生产调用方（仅定义处）。详见 C-04。
2. **JwtUtil 密钥编码——kdla 侧可静态判定：raw UTF-8 bytes**。实际实现是 hutool 5.8.36（非 jjwt）：`JWTSignerUtil.hs256(signKey.getBytes(UTF_8))` → `HMacJWTSigner("HmacSHA256", key)` 直接用原始字节做 HMAC key，无 Base64 假设。与 jjwt 0.9 `signWith(alg, String)` 的 Base64 解码语义存在互操作冲突，但方向是 fail-closed（验签必失败），不是绕过。详见 C-06。
3. **"AES/SM4 已随机 IV + v1: 前缀兼容、无降级面"——属实**。`AESUtil.java:40-48`、`Sm4Util.java:124-135` 加密侧均为 `new SecureRandom().nextBytes(iv)` + CBC + `v1:` 前缀。解密侧保留旧格式分支（AES 全零 IV / SM4 ECB），但攻击者无密钥无法把 v1 密文改造成可解的旧格式密文，剥前缀只会得到垃圾明文/padding 错误——不构成降级攻击。残留问题是 CBC 无认证（C-02），不是降级面。
4. **"cache 密码明文日志已随 F-01 修复"——属实**。`KRedissonConfigProperties.java` 四处构建日志（177-180、214-216、244-246、278-280）均只记录 address/codec/threads 等非敏感项，无 `toYAML` 输出；`KRedissonLockFactory`/`KRedissonAutoConfigure` 无密码日志。残留面是 Lombok `@Data` 生成的 toString（C-12）。
5. **"AES SHA1PRNG deterministic=true 判为误报"——维持，且有新证据**。当前 `AESUtil` 直接用 password 的 raw UTF-8 字节做 `SecretKeySpec`（`AESUtil.java:33-39`），全类无 SHA1PRNG、无 KeyGenerator，旧误报针对的代码路径已不存在。另对 JDK 21 源码核查：`SecureRandom(byte[] seed)` → `getDefaultPRNG` → SUN provider 默认算法为 `NativePRNG`（`/dev/(u)random` 可用时）否则 `DRBG`（`SunEntries.java:361-366`），**JDK 21 默认永不选 SHA1PRNG**，种子构造器混入系统熵、非确定性。这改变了 Sm4/Sm2 废弃种子派生 API 的风险定性（C-07）。

---

## Medium

### C-01 ObjectUtil.byteToObject：无过滤 Java 原生反序列化（A08）

- 位置：`kdla-component-common/src/main/java/top/kdla/framework/common/utils/ObjectUtil.java:30-45`
- 代码：

```java
public static Object byteToObject(byte[] bytes) {
    Object obj = null;
    try {
        ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
        ObjectInputStream oi = new ObjectInputStream(bi);
        obj = oi.readObject();
        ...
    } catch (Exception e) {
        log.error("ObjectUtil byteToObject Exception", e);
    }
    return obj;
}
```

- 问题：公开框架 API 直接 `readObject()`，无 `ObjectInputFilter`（JEP 290）、无类型白名单；异常吞掉返回 null。
- 攻击场景：下游把网络报文、MQ 消息、Redis 缓存值等不可信字节喂给 `byteToObject`，classpath 存在 gadget 库（如 commons-collections4——trd 模块自身就 provided 引入）时触发反序列化 RCE。
- 框架上下文 exploitability：(a) 类——框架交付了不安全原语，下游除非不用或自行包 filter 否则无法缓解。但全仓 grep 核实 `byteToObject` 生产调用方为 0（仅测试；`ObjectToByte` 被 supplement 模块 VertxHttpClient 使用，序列化方向无此风险），当前仓库内无攻击路径，属潜伏 API 缺陷。**若下游用于不可信数据应升级 High。**
- 严重度：Medium
- 修复：加 `ObjectInputFilter` 白名单（或全局 `jdk.serialFilter` 指引）；更彻底是废弃该 API、引导下游用 JacksonUtil。
- 破坏性分类：**破坏性需裁定**——白名单会拒绝下游合法自定义类；废弃 API 影响存量调用。

### C-02 AESUtil/Sm4Util：CBC 无认证加密 + 旧格式兼容分支（A02）

- 位置：`kdla-component-common/src/main/java/top/kdla/framework/common/utils/AESUtil.java:20,77-94`；`Sm4Util.java:37,137-152`
- 代码（AESUtil 解密，异常携带可区分 cause）：

```java
private static final String AES_PATH = "AES/CBC/PKCS5Padding";
...
} catch (Exception e) {
    throw new RuntimeException("decrypt exception", e);
}
...
// 旧格式分支：全零 IV
byte[] zeroIv = new byte[PASSWORD_LENGTH];
cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(zeroIv));
```

```java
// Sm4Util.decryptCompat 旧 ECB 兼容分支
byte[] cipherTextBytes = Base64.getDecoder().decode(cipherText);
byte[] decryptBytes = encryptMode(secretKey, cipherTextBytes, Cipher.DECRYPT_MODE); // SM4/ECB/PKCS5Padding
```

- 问题：密文无 MAC/AEAD，CBC 可延展（malleable）。`RuntimeException` 消息统一但 cause 链可区分 `BadPaddingException` 与其他异常。
- 攻击场景：
  1. 能篡改存储/传输中密文的攻击者做 CBC 比特翻转（改 IV 即可预测性篡改首明文块），框架无完整性校验可发现；
  2. 若下游异常处理把 padding 错误与其他错误区分回显（不同 message/状态码），构成经典 CBC padding oracle，可逐字节恢复明文。
- 框架上下文 exploitability：(a)/(b) 混合——框架交付无认证原语且异常 cause 可区分，下游易无意造出 oracle；是否成真取决于下游错误回显与密文可提交性。旧格式分支本身经核实无降级面（见开放项 3），但存量零 IV/ECB 数据仍带确定性加密弱点。另：AES key 为 16 字节 UTF-8 口令直用、无 KDF/盐，低熵口令可离线爆破（加固项）。
- 严重度：Medium
- 修复：迁移 AES-GCM 或 Encrypt-then-MAC（可复用 Sm3Util.hmac）；过渡期在文档中要求下游统一解密错误回显。
- 破坏性分类：**破坏性需裁定**——密文格式变更影响下游存量密文，需 v2 前缀双读迁移方案。

### C-03 RSAUtil：隐式 PKCS#1 v1.5 填充（A02）

- 位置：`kdla-component-common/src/main/java/top/kdla/framework/common/utils/RSAUtil.java:41,54`
- 代码：

```java
Cipher cipher = Cipher.getInstance(ALGORITHM); // ALGORITHM = "RSA"
cipher.init(Cipher.ENCRYPT_MODE, publicKey);
...
Cipher cipher = Cipher.getInstance(ALGORITHM);
cipher.init(Cipher.DECRYPT_MODE, privateKey);
```

- 问题：`Cipher.getInstance("RSA")` 在 SunJCE 下默认 `RSA/ECB/PKCS1Padding`，即 PKCS#1 v1.5 加密填充，非 OAEP；transformation 还随 provider 漂移。
- 攻击场景：下游用其加密口令等敏感数据且解密错误可区分时，Bleichenbacher padding oracle 可恢复明文；v1.5 填充本身已被现代标准弃用。
- 框架上下文 exploitability：(a)/(b)——不安全默认由框架 API 固化，下游难以在不改工具类的情况下换 OAEP。全仓 grep：RSAUtil 仓库内 0 调用方，纯下游暴露面。密钥长度 2048 合格。
- 严重度：Medium
- 修复：显式 `RSA/ECB/OAEPWithSHA-256AndMGF1Padding`。
- 破坏性分类：**破坏性需裁定**——OAEP 与 v1.5 密文互不兼容，影响下游存量密文。

### C-04 StringUtil.hashString：fail-open 哈希（E-05 核实属实）

- 位置：`kdla-component-supplement-trdplatformcloud/src/main/java/top/kdla/framework/supplement/trdcloud/utils/StringUtil.java:68-76`
- 代码：

```java
public static String hashString(String message, String algorithm) {
    try {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] hashedBytes = digest.digest(message.getBytes(StandardCharsets.UTF_8));
        return convertByteArrayToHexString(hashedBytes);
    } catch (Exception e) {
        return message;
    }
}
```

- 问题：任何异常（算法名拼错、不可用算法、message 为 null 的 NPE）静默返回**原文**当作"哈希值"。algorithm 参数还允许调用方传 MD5/SHA-1。
- 攻击场景：下游若用它做完整性校验/签名摘要且 algorithm 名可被输入影响（配置注入、请求参数），攻击者传非法算法名使"哈希"=明文，破坏依赖该值的校验/去重/签名逻辑；即使无攻击者，拼错算法名也会静默产出错误值而非报错。
- 框架上下文 exploitability：(a) 类缺陷 API，但全仓 grep 核实仓库内 0 调用方——当前无实际攻击路径，属交付给下游的潜伏 fail-open 原语。fail-open 是安全原语的设计错误，不是风格问题。
- 严重度：Medium
- 修复：失败抛异常（fail-closed），删除 catch-return-message。
- 破坏性分类：**非破坏性可直接修**——仅错误路径行为变化，合法调用不受影响；依赖 fail-open 的调用本身就是 bug。

### C-05 SequenceNoGeneratorV2：RAN 规则用 java.util.Random 生成业务编号随机段（A02）

- 位置：`kdla-component-supplement-cache/src/main/java/top/kdla/framework/supplement/cache/sequence/SequenceNoGeneratorV2.java:317-323`
- 代码：

```java
StringBuilder sb = new StringBuilder();
Random random = new Random();
int i = 1;
while (randomTypeRule.getSize() >= i) {
    sb.append(poolList.get(random.nextInt(poolList.size())));
    i++;
}
```

- 问题：`java.util.Random` 是 48-bit LCG，观察少量输出即可恢复种子并预测后续全部输出。编号还经本地队列批量预生成（`noCacheMap`，151-161 行），按生成顺序发放，进一步降低预测成本。
- 攻击场景：下游把含 RAN 段的编号当作不可猜测凭证（取件码、兑换码、凭编号查询/操作单据）时，攻击者收集几个已发放编号即可推算后续编号，冒领/越权查询。
- 框架上下文 exploitability：(b)/(c) 边界——编号是否安全敏感取决于下游用法，但"随机规则"用可预测 PRNG 是框架自身缺陷，修复零成本且不改输出格式。V1（SequenceNoGenerator）无 RAN 规则，仅 `UUID.randomUUID()`（173 行），不受影响。
- 严重度：Medium
- 修复：`new SecureRandom()` 替换 `new Random()`。
- 破坏性分类：**非破坏性可直接修**——输出字符池与格式不变，仅随机源更换。

### C-06 JwtUtil：密钥编码 raw UTF-8 与 jjwt 0.9 Base64 语义冲突（fail-closed 互操作缺陷，非绕过）

- 位置：`kdla-component-supplement-trdplatformcloud/src/main/java/top/kdla/framework/supplement/trdcloud/utils/JwtUtil.java:36-37`
- 代码：

```java
jwt = JWTUtil.parseToken(token)
        .setSigner(JWTSignerUtil.hs256(signKey.getBytes(StandardCharsets.UTF_8)));
```

- 静态判定（hutool 5.8.36 sources jar 实证，非猜测）：
  - `JWTSignerUtil.hs256(byte[] key)` → `HMacJWTSigner("HmacSHA256", key)`，HMAC key = 配置字符串的 **raw UTF-8 字节**，无 Base64 环节。kdla 侧编码逻辑**可静态判定**。
  - jjwt 0.9.x `signWith(SignatureAlgorithm.HS256, String)` 把 String 按 **Base64 解码**后做 key。若下游 BladeX 用该重载且两侧配置同一 key 字符串，则密钥字节必然不同 → 所有真实 token 验签失败。
  - BladeX 侧用哪个 jjwt 版本/重载**未实测**（not measured 维持）；若 BladeX 用 raw bytes 签名（如 jjwt 0.11+ `Keys.hmacShaKeyFor`）则两侧一致可用。
- 安全方向核实（均静态实证）：
  - **fail-closed 属实**：`requireSignKey()`（57-67 行）未配置/空白抛 BizException；`verify()`/`validate(0)` 失败抛异常；调用方（TrdPlatformAPIController:71-75、各 AddVo）无 try-catch 吞异常。
  - **alg=none 不可绕过**：hutool `JWT.verify(signer)` 仅在 signer==null 时回落 `NoneJWTSigner`；kdla 恒设 HS256 signer，`HMacJWTSigner.verify` 重算 HMAC 比对，空签名必失败。token 头 alg 不被检查但验签算法恒为 HS256，无算法混淆面。
  - **签名比对恒时**：hutool `Mac.verify` 用 `MessageDigest.isEqual`（Mac.java:223-225）。
  - 子项（Low）：`validate(0)` 对**缺失 exp** 的 token 跳过过期检查（JWTValidator.validateNotBefore：null 即 return）——合法签名但无 exp 的 token 永久有效；密钥无最小长度/熵校验，下游配短弱 key 可被离线爆破。
- 攻击场景：非攻击者绕过，而是集成事故——下游按 BladeX 的 sign-key 原样配置后，携带 blade-auth 的 trd save/update 接口全部"JWT 验签失败"不可用；运维为"修好"可能换弱 key 或关验签，间接削弱安全。另注意：blade-auth 头 `required=false`，缺失时 createUser/updateUser 落 null（审计字段缺口；接口鉴权本身是下游网关责任，A01 不在本专项范围）。
- 框架上下文 exploitability：(b)——密钥配置是下游责任，但编码语义歧义由框架文档/实现固化，下游难自行发现。
- 严重度：Medium（可用性/集成风险；无验签绕过）
- 修复：文档明确"raw UTF-8 字节"语义并给 BladeX 各版本的对接示例；可选支持 `base64:` 前缀显式声明编码；强制最小密钥长度（≥32 字节）；拒绝无 exp 的 token。
- 破坏性分类：**破坏性需裁定**——编码语义/前缀约定与最小长度校验会改变下游存量配置的行为。

---

## Low

### C-07 Sm4Util.generateKey / Sm2Util.generateKeyPair：低熵种子派生废弃 API 残留（JDK 21 新证据重新定性）

- 位置：`kdla-component-common/src/main/java/top/kdla/framework/common/utils/Sm4Util.java:84-91`；`Sm2Util.java:85-92`
- 代码：

```java
@Deprecated
public static String generateKey(String keySrc) throws Exception {
    KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
    SecureRandom random = new SecureRandom(keySrc.getBytes(CHARSET_UTF8));
    keyGen.init(DEFAULT_KEY_SIZE, random);
    ...
}
```

- 新证据（JDK 21 `src.zip` 实证）：`SecureRandom(byte[] seed)` → `getDefaultPRNG(true, seed)`；SUN provider 默认算法 `DEF_SECURE_RANDOM_ALGO = NativePRNG.isAvailable() && seedSource∈{/dev/urandom,/dev/random} ? "NativePRNG" : "DRBG"`（SunEntries.java:361-366）。两者 `engineSetSeed` 均为**混入**系统熵，非确定性。
- 结论修正：
  1. 在 2.x（JDK 21）标准平台上，"同 keySrc 必派生同密钥、keySrc 可猜测即密文全破"**不成立**——种子不是唯一熵源。@deprecated javadoc 的确定性描述在 JDK 21 上是错的。
  2. 反过来，javadoc 承诺的"值一致密钥文本就一致"可复现性在 JDK 21 上**也不成立**——下游若靠 keySrc 重生成密钥解存量数据会拿到不同密钥（功能性隐患）。
  3. 在 JDK 8（1.x 线仍在发布）且默认选 SHA1PRNG 的平台（如 Windows）上，种子构造器确定性成立，可猜测 keySrc → 密钥全恢复的真实风险仍在。
- 框架上下文 exploitability：(b)/(c)——两方法均已 @Deprecated 且提供 `generateSecureKey()`/`generateSecureKeyPair()` 真随机替代；仓库内生产调用方 0（仅回归测试用固定种子）。
- 严重度：Low（2.x）；1.x/JDK 8 平台按 Medium 对待。
- 修复：更正 javadoc（说明确定性依赖平台 PRNG，JDK 21 不可复现）；下个大版本移除。
- 破坏性分类：javadoc 更正**非破坏性**；移除 API **破坏性需裁定**。

### C-08 Sm3Util.verify：Arrays.equals 非恒时比较（A02 时序侧信道）

- 位置：`kdla-component-common/src/main/java/top/kdla/framework/common/utils/Sm3Util.java:87`
- 代码：

```java
if (Arrays.equals(newHash, sm3Hash)) {
    flag = true;
}
```

- 问题：HMAC-SM3 校验用短路比较，首字节差异即返回，比较耗时随匹配前缀长度变化。
- 攻击场景：攻击者以高精度计时逐字节猜测 MAC。网络抖动下远程利用极难，本地/低噪声场景理论可行。
- 框架上下文 exploitability：(a) 但利用门槛高；仓库内 0 调用方。异常路径返回 false，fail-closed 合格。
- 严重度：Low
- 修复：`MessageDigest.isEqual(newHash, sm3Hash)`。
- 破坏性分类：**非破坏性可直接修**。

### C-09 CertificateHelp：短证件号脱敏 fail-open 返回原文（A02 敏感数据暴露）

- 位置：`kdla-component-common/src/main/java/top/kdla/framework/common/help/CertificateHelp.java:53-65`
- 代码：

```java
private static String transfer(String certificateCode, int startCharLength, int endCharLength) {
    int retainCharLength = startCharLength + endCharLength;
    if (certificateCode != null && certificateCode.length() >= retainCharLength) {
        ... // 正常脱敏
    } else {
        return certificateCode;
    }
}
```

- 问题：长度不足保留位数时**原样返回**。身份证类型保留 3+4=7 位，≤6 位的证件号全明文输出；未知证件类型回落 OTHER（1+1），1 位输入同样原样返回。
- 攻击场景：下游把脱敏结果写日志/回显，畸形或短证件号（数据录入错误、非标准证件）绕过脱敏，PII 明文泄露。
- 框架上下文 exploitability：(a)——脱敏是框架承诺的安全行为，短输入 fail-open 违背承诺；触发依赖下游数据质量。
- 严重度：Low
- 修复：长度不足时全掩码（或最多保留 1 位），不返回原文。
- 破坏性分类：**非破坏性可直接修**（输出只会更掩码；依赖短输入原样输出的下游本身是 bug，需知会）。

### C-10 GzipUtil.unZip：无解压上限（zip bomb DoS）

- 位置：`kdla-component-common/src/main/java/top/kdla/framework/common/utils/GzipUtil.java:53-71`
- 代码：

```java
byte[] t = Base64.getDecoder().decode(str);
...
while ((num = gzip.read(buf, 0, buf.length)) != -1) {
    bos.write(buf, 0, num);
}
```

- 问题：解压输出无大小上限，高压缩比恶意 gzip（zip bomb）可耗尽堆内存。
- 攻击场景：下游对不可信输入（用户提交的 base64 压缩报文）调用 unZip → OOM 拒绝服务。
- 框架上下文 exploitability：(c) 偏 (b)——输入来源由下游决定；仓库内生产调用方 0（仅测试）。
- 严重度：Low
- 修复：新增带上限的 `unZip(String, long maxBytes)` 重载并在文档标注风险，旧方法保留。
- 破坏性分类：新增重载**非破坏性**；若改旧方法默认上限则**破坏性需裁定**。

### C-11 Md5Util/SHAUtil：弱摘要工具暴露面 + Md5Util 错误日志携带输入

- 位置：`kdla-component-common/src/main/java/top/kdla/framework/common/utils/Md5Util.java:27,37`；`SHAUtil.java:24-26,50-52`
- 代码：

```java
MessageDigest messageDigest = MessageDigest.getInstance("MD5");
...
} catch (NoSuchAlgorithmException e) {
    log.error("md5()-->input{},NoSuchAlgorithmException:{}", input, e.getMessage());
    return null;
}
```

- 问题：MD5/SHA-1 作为通用摘要工具公开交付；Md5Util 异常分支把**输入原文**写日志（若输入是口令则明文进日志），且失败返回 null。
- 核实：全仓 grep，Md5Util/SHAUtil 仓库内无安全用途调用方（Md5Util 仅测试引用）。MD5 在 JDK 中恒可用，该 catch 分支实际是死路径——风险是潜伏的，不是现行泄露。
- 框架上下文 exploitability：(c)——是否用于安全用途由下游决定；框架侧可做的是标注禁用场景与清理日志。
- 严重度：Low
- 修复：日志去掉 input；javadoc/@Deprecated 标注"禁止用于口令/签名等安全用途"。
- 破坏性分类：**非破坏性可直接修**。

### C-12 KRedissonConfigProperties：@Data toString 含 password 残留泄露面

- 位置：`kdla-component-supplement-cache/src/main/java/top/kdla/framework/supplement/cache/lock/properties/KRedissonConfigProperties.java:32,52`
- 代码：

```java
@Data
@Slf4j
public class KRedissonConfigProperties {
    ...
    private String password = null;
```

- 问题：F-01 已修复构建日志（核实属实），但 Lombok `@Data` 生成的 `toString()` 仍包含 password 字段。任何下游 `log.info("{}", props)`、异常拼接或调试器输出都会带出明文密码。Spring Boot 3 actuator configprops 默认 sanitize 含 "password" 的 key，该路径基本安全；残留面是直接 toString。
- 框架上下文 exploitability：(b)——仓库内未发现对该对象的日志输出；泄露需下游不当用法触发，但框架可零成本消除。
- 严重度：Low
- 修复：password 字段加 `@ToString.Exclude`。
- 破坏性分类：**非破坏性可直接修**。

### C-13 Sm2Util：密钥编码双轨歧义 + SM2Engine 默认 C1C2C3 模式

- 位置：`kdla-component-common/src/main/java/top/kdla/framework/common/utils/Sm2Util.java:123,187,215`
- 代码：

```java
SM2Engine localSM2Engine = new SM2Engine(); // BC 1.78 默认 Mode.C1C2C3
...
public static String sign(String plainText, String privateKey) throws Exception {
    return sign(plainText, new BigInteger(Hex.decode(privateKey))); // hex 裸 D
}
...
public static boolean verify(String plainText, String signText, String publicKey) throws Exception {
    return verify(plainText, signText, Hex.decode(publicKey)); // hex 裸点
}
```

- 问题：encrypt/decrypt 用 Base64 X509/PKCS8，sign/verify 却用 hex 裸私钥 D/hex 裸公钥点——同名"密钥文本"参数两套编码，下游混用必炸（Hex/Base64 解码异常，fail-closed，无静默绕过）。`new SM2Engine()` 默认 C1C2C3 为旧标准序，现行 GB/T 32918 为 C1C3C2，与其他 SM2 实现互通会失败。
- 攻击场景：无直接攻击；属误用陷阱与互操作缺陷。签名算法本身 SM3withSM2 合格，verify 失败返回 false（fail-closed），加密 k 由 `ParametersWithRandom(new SecureRandom())` 提供（无 nonce 复用）。
- 框架上下文 exploitability：(b)——API 清晰度问题，下游踩坑表现为运行时异常而非失密。
- 严重度：Low
- 修复：javadoc 明示两套编码；提供显式 `SM2Engine.Mode.C1C3C2` 选项或统一编码。
- 破坏性分类：文档**非破坏性**；统一编码/改默认模式**破坏性需裁定**（影响存量密文与密钥格式）。

---

## 核实为安全/已修复的项（防止重复上报）

- AESUtil/Sm4Util 新密文：SecureRandom 随机 IV + CBC + `v1:` 前缀，无降级面（开放项 3）。
- cache 密码日志：F-01 修复属实，四处构建日志均脱敏（开放项 4）。
- AES SHA1PRNG：当前代码无该路径，误报定性维持（开放项 5）。
- JwtUtil：fail-closed 属实；alg=none/算法混淆静态排除；MAC 比对恒时（C-06 内实证）。
- BCryptUtil：jbcrypt workload 12，null 输入 fail-closed，无问题。
- JacksonUtil：未启用 default typing，无多态反序列化 gadget 面；FAIL_ON_UNKNOWN_PROPERTIES=false 仅宽松映射。
- KRedissonRedDisLock：tryLock 失败/中断均抛 LockFailException，fail-closed。
- MachineIdHelp：`Math.random()` 仅用于雪花 worker ID 注册失败回退，非安全随机场景，且已有 ERROR 告警与 SETNX 原子注册——不构成加密发现。
- SelfSnowflakeGeneratorHelp：traceId 可预测但非安全令牌，回退用 `UUID.randomUUID()`——不构成发现。
- model 模块：仅 Serializable DTO，无加密/序列化逻辑。
- BytesUtil：纯字节序转换，无加密语义。
- trd 第三方平台 token 明文存 Redis（TrdPlatformCloudServerImpl:368）：属部署访问控制责任 (c)，Redis 失陷即全局失陷，不按框架漏洞上报。

## 分组清单

### 非破坏性可直接修（安全内部修复，不破坏下游兼容）

| 编号 | 修复动作 |
|---|---|
| C-04 | hashString 失败抛异常，删除 return message |
| C-05 | RAN 规则 `new Random()` → `new SecureRandom()` |
| C-08 | Sm3Util.verify 改 `MessageDigest.isEqual` |
| C-09 | CertificateHelp 短输入全掩码，不返回原文 |
| C-10 | 新增带上限的 unZip 重载（旧方法不动） |
| C-11 | Md5Util 日志去掉 input；MD5/SHA-1 标注禁安全用途 |
| C-12 | password 字段 `@ToString.Exclude` |
| C-07（部分） | 更正种子派生 javadoc 的确定性描述 |
| C-13（部分） | javadoc 明示 sign/verify 与 encrypt/decrypt 的密钥编码差异 |

### 破坏性需裁定（改加密 API/密钥格式/兼容行为，影响下游存量密文、token 或配置）

| 编号 | 修复动作 | 影响 |
|---|---|---|
| C-01 | ObjectUtil 加 ObjectInputFilter 白名单或废弃 API | 下游自定义类反序列化被拒 |
| C-02 | AES/SM4 迁移 GCM 或 Encrypt-then-MAC（v2 前缀双读） | 存量密文格式 |
| C-03 | RSA 显式 OAEP | 存量 RSA 密文不可解 |
| C-06 | JWT 密钥编码语义/base64 前缀、最小密钥长度、强制 exp | 下游存量 sign-key 配置与无 exp token |
| C-07（部分） | 移除 generateKey/generateKeyPair 废弃 API | 存量调用方 |
| C-13（部分） | 统一 SM2 密钥编码 / 默认 C1C3C2 | 存量密文与密钥文本 |
