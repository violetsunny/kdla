package top.kdla.framework.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：toBean(String, Type) / toBean(String, TypeReference) 反序列化失败时，
 * 旧实现 throw new RuntimeException(errMsg) 丢失原始异常，排障时无从定位根因；
 * 修复后必须携带 cause。
 */
class JacksonUtilTest {

    @Test
    void toBeanByType_badJson_runtimeExceptionCarriesCause() {
        Type type = new TypeReference<Map<String, Object>>() {
        }.getType();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> JacksonUtil.toBean("{bad json", type));
        assertTrue(ex.getCause() instanceof IOException, "RuntimeException 必须携带原始 IOException 作为 cause");
    }

    @Test
    void toBeanByTypeReference_badJson_runtimeExceptionCarriesCause() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> JacksonUtil.toBean("{bad json", new TypeReference<List<String>>() {
                }));
        assertTrue(ex.getCause() instanceof IOException, "RuntimeException 必须携带原始 IOException 作为 cause");
    }

    @Test
    void toBeanByType_validJson_stillWorks() {
        Type type = new TypeReference<Map<String, Object>>() {
        }.getType();

        Map<String, Object> result = JacksonUtil.toBean("{\"k\":\"v\"}", type);
        assertEquals("v", result.get("k"));
    }
}
