package top.kdla.framework.supplement.trdcloud.utils;

import org.junit.jupiter.api.Test;
import top.kdla.framework.exception.BizException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StringUtilTest {

    @Test
    void md5KnownVector() {
        assertThat(StringUtil.hashString("abc", "MD5")).isEqualTo("900150983cd24fb0d6963f7d28e17f72");
    }

    @Test
    void sha256KnownVector() {
        assertThat(StringUtil.hashString("abc", "SHA-256"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void unknownAlgorithmThrowsInsteadOfReturningPlaintext() {
        // fail-open 旧实现会返回原文 "secret" 冒充哈希
        assertThatThrownBy(() -> StringUtil.hashString("secret", "NO-SUCH-ALG"))
                .isInstanceOf(BizException.class);
    }

    @Test
    void nullMessageThrows() {
        assertThatThrownBy(() -> StringUtil.hashString(null, "MD5"))
                .isInstanceOf(BizException.class);
    }
}
