package top.kdla.framework.common.help;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 安全回归：CertificateHelp.transfer 旧实现对长度不足保留位数的证件号原样返回，导致短证件号明文泄露。
 * 修复后短输入全掩码，null 返回 null。
 */
class CertificateHelpTest {

    @Test
    void idCard_normalMask() {
        // 18 位身份证保留前 3 后 4，中间 11 位掩码
        assertEquals("110***********1234", CertificateHelp.coverCertificate("1", "110101199911111234"));
    }

    @Test
    void shortInput_fullyMasked_notPlaintext() {
        // Prove-It：6 位短证件号（ID 保留 3+4=7 位）旧实现原样返回，修复后必须全掩码
        assertEquals("******", CertificateHelp.coverCertificate("1", "123456"));
    }

    @Test
    void nullInput_returnsNull() {
        assertNull(CertificateHelp.coverCertificate("1", null));
    }

    @Test
    void passport_normalMask() {
        // 护照保留前 1 后 1
        assertEquals("E*******8", CertificateHelp.coverCertificate("3", "E12345678"));
    }
}