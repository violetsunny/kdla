package top.kdla.framework.common.utils;

import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：ObjectToByte 旧实现在 bo.toByteArray() 前未 oo.flush()，
 * ObjectOutputStream 块缓冲区尾部字节可能丢失导致序列化数据截断；且流未用 try-with-resources。
 */
class ObjectUtilTest {

    static class Payload implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String name;
        private final int age;

        Payload(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    @Test
    void objectToByte_roundTripIntact() {
        // 构造超过 1K 块缓冲区的字符串字段，覆盖块缓冲边界场景
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("kdla-flush-regression-");
        }
        Payload payload = new Payload(sb.toString(), 7);

        byte[] bytes = ObjectUtil.ObjectToByte(payload);
        assertNotNull(bytes, "序列化结果不能为 null");

        Object back = ObjectUtil.byteToObject(bytes);
        assertTrue(back instanceof Payload, "截断的字节流会导致反序列化失败");
        assertEquals(payload.name, ((Payload) back).name);
        assertEquals(payload.age, ((Payload) back).age);
    }

    @Test
    void objectToByte_notSerializable_returnsNullNotThrow() {
        // 不可序列化对象：保持原有降级语义（log + 返回 null），不抛异常
        assertEquals(null, ObjectUtil.ObjectToByte(new Object()));
    }
}
