# 17: common 加密工具回合（Sm4 CBC+Base64 / AES 随机 IV / Sm2 真随机）

**What to build:** Sm4Util：ECB→CBC+随机 IV，加密输出 v1: 前缀，decrypt 无前缀旧 ECB 密文走兼容分支（B8）；密钥编码统一 Base64、无兼容分支（B9）；新增 generateSecureKey() 真随机 API，旧 generateKey 标 @Deprecated。AESUtil：零 IV→SecureRandom 随机 IV+v1: 前缀，旧密文兼容解密（B10）；异常消息删 password 明文回显。Sm2Util：新增 generateSecureKeyPair() 真随机 API，旧 generateKeyPair(keySrc) 标 @Deprecated；UTF-8 显式编码。

**Blocked by:** 02（同模块顺序执行，避免交叉提交）

**Status:** done

依据：investigation.md 第 3 节 19/24/32/44/74/75/86 项（bb8022f/9cdbe79/d7d7bfe/7d86361/b9695b2）、B8/B9/B10 裁定。拆票时遗漏，2026-11-14 由 R common 改动文件清单核对补立。

- [x] Sm4Util CBC+IV+v1: 前缀+旧密文兼容分支+Base64 密钥统一+generateSecureKey
- [x] AESUtil 随机 IV+v1: 前缀+旧密文兼容+异常不回显 password
- [x] Sm2Util generateSecureKeyPair+旧 API @Deprecated+UTF-8
- [x] R 的 5 个加密测试类回合（Sm4UtilCbcMigrationTest/Sm4UtilKeyCodecTest/Sm2UtilSecureKeyPairTest/Sm2UtilTest/AESUtilTest）
- [x] `mvn -pl kdla-component-common test` 绿（含票 02 已回合测试不回归）
- [x] 独立 commit，不 push；BREAKING（新密文旧代码不可解、存量 Hex 密钥不可读）由票 15 记入 CHANGELOG

## Comments

- 2026-11-14 立票：票 02 范围核对时发现 R common 改动的 AESUtil/Sm2Util/Sm4Util 无票归属，补立本票。
- 2026-11-14 完成，commit 07ffd02（8 文件，+306/-24）。T 三个 util 与 R 基线 6d617f2 逐字一致（零分叉），整文件覆盖移植 R HEAD（CRLF→LF）；5 测试类原样回合。验证：`mvn -pl kdla-component-common test` 56 用例全绿（46 旧+10 新）。BREAKING 项待票 15 记 CHANGELOG。
