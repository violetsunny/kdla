package top.kdla.framework.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 回归测试：reverse(data, offset, length) 原实现 rev = len - i 未加 offset，
 * 且 len 钳制与循环次数错位——offset>0 时交换索引必错并可能越界。
 * 修复后按标准区间反转 [offset, offset+length)，越界显式抛异常。
 */
class BytesUtilReverseTest {

    @Test
    void reverse_withOffset_reversesOnlyTargetRange() {
        byte[] data = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        // 旧实现交换 (2,4)(3,3)，结果必错；正确应交换 (2,5)(3,4)
        BytesUtil.reverse(data, 2, 4);
        assertArrayEquals(new byte[]{0, 1, 5, 4, 3, 2, 6, 7, 8, 9}, data);
    }

    @Test
    void reverse_wholeArray_behaviorUnchanged() {
        byte[] data = {1, 2, 3, 4};
        BytesUtil.reverse(data);
        assertArrayEquals(new byte[]{4, 3, 2, 1}, data);
    }

    @Test
    void reverse_outOfBounds_throwsInsteadOfSilentCorruption() {
        assertThrows(IndexOutOfBoundsException.class, () -> BytesUtil.reverse(new byte[4], 2, 4));
    }
}
