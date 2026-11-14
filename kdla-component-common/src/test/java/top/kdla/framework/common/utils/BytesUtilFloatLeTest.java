package top.kdla.framework.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * 回归测试：floatToLe(byte[], float, int) 内部曾误调 intToBe（大端），
 * 与单参重载 floatToLe(float)（小端）字节序相反。
 * 修复后两个重载输出必须一致，均为 floatToIntBits 的小端字节序。
 */
class BytesUtilFloatLeTest {

    @Test
    void floatToLe_withTargetArray_matchesLittleEndianBits() {
        float value = 1.5f;
        byte[] target = new byte[4];

        byte[] actual = BytesUtil.floatToLe(target, value, 0);
        byte[] expected = BytesUtil.intToLe(Float.floatToIntBits(value));

        assertArrayEquals(expected, actual,
                "floatToLe(byte[],float,int) 必须输出小端字节序（1.5f -> 00 00 C0 3F），而不是大端");
        // 1.5f 的 IEEE754 位为 0x3FC00000，小端应为 00 00 C0 3F
        assertArrayEquals(new byte[]{0x00, 0x00, (byte) 0xC0, 0x3F}, actual);
    }

    @Test
    void floatToLe_withOffset_writesLittleEndianAtOffset() {
        float value = 1.5f;
        byte[] target = new byte[6];

        BytesUtil.floatToLe(target, value, 2);

        // 偏移 0-1 保持原值 0，偏移 2 起为小端字节 00 00 C0 3F
        byte[] expected = new byte[]{0x00, 0x00, 0x00, 0x00, (byte) 0xC0, 0x3F};
        assertArrayEquals(expected, target, "带偏移量写入也必须是 00 00 C0 3F 的小端序");
    }
}
