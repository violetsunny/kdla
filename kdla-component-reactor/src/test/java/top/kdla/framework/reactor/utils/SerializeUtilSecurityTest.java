package top.kdla.framework.reactor.utils;

import gadget.GadgetPayload;
import gadget.JsonPojo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.kdla.framework.reactor.trace.data.SpanDataInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** CWE-502 回归：OBJECT 分支 JEP 290 过滤 + JSON 分支 loadClass 白名单。无修复则本类失败。 */
class SerializeUtilSecurityTest {

    private static ObjectInputFilter defaultFilter;
    private static List<String> defaultPrefixes;

    @BeforeAll
    static void captureDefaults() {
        defaultFilter = SerializeUtil.getObjectInputFilter();
        defaultPrefixes = SerializeUtil.getJsonClassPrefixes();
    }

    @BeforeEach
    void resetFlag() {
        GadgetPayload.deserialized = false;
    }

    @AfterEach
    void restoreDefaults() {
        SerializeUtil.setObjectInputFilter(defaultFilter);
        SerializeUtil.setJsonClassPrefixes(defaultPrefixes);
        GadgetPayload.deserialized = false;
    }

    private static byte[] write(Object value) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            SerializeUtil.writeObject(value, oos);
        }
        return bos.toByteArray();
    }

    @Test
    void objectBranch_rejectsClassOutsideAllowlist() throws Exception {
        byte[] bytes = write(new GadgetPayload());
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            assertThatThrownBy(() -> SerializeUtil.readObject(ois))
                    .isInstanceOf(InvalidClassException.class);
        }
        assertThat(GadgetPayload.deserialized).isFalse();
    }

    @Test
    void objectBranch_allowsJdkBaseType() throws Exception {
        Date now = new Date();
        byte[] bytes = write(now);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            assertThat(SerializeUtil.readObject(ois)).isEqualTo(now);
        }
    }

    @Test
    void objectBranch_rejectsOversizedArray() throws Exception {
        byte[] bytes = write(new int[20000]);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            assertThatThrownBy(() -> SerializeUtil.readObject(ois))
                    .isInstanceOf(InvalidClassException.class);
        }
    }

    @Test
    void externalizableReadExternal_appliesFilter() throws Exception {
        SpanDataInfo span = new SpanDataInfo();
        span.setApp("app");
        span.setName("name");
        span.setTraceId("trace");
        span.setAttributes(Map.of("k", new GadgetPayload()));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(span);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            assertThatThrownBy(ois::readObject).isInstanceOf(InvalidClassException.class);
        }
        assertThat(GadgetPayload.deserialized).isFalse();
    }

    @Test
    void jsonBranch_rejectsClassOutsideAllowlist() throws Exception {
        byte[] bytes = write(new JsonPojo("x"));
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            assertThatThrownBy(() -> SerializeUtil.readObject(ois))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("gadget.JsonPojo");
        }
    }

    @Test
    void jsonBranch_allowsConfiguredPrefix() throws Exception {
        SerializeUtil.setJsonClassPrefixes(List.of("gadget."));
        byte[] bytes = write(new JsonPojo("x"));
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object read = SerializeUtil.readObject(ois);
            assertThat(read).isInstanceOf(JsonPojo.class);
            assertThat(((JsonPojo) read).getName()).isEqualTo("x");
        }
    }

    @Test
    void nullFilter_disablesObjectFilter() throws Exception {
        SerializeUtil.setObjectInputFilter(null);
        byte[] bytes = write(new GadgetPayload());
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            assertThat(SerializeUtil.readObject(ois)).isInstanceOf(GadgetPayload.class);
        }
        assertThat(GadgetPayload.deserialized).isTrue();
    }

    @Test
    void objectBranch_filteredEvenWhenOuterStreamPermissive() throws Exception {
        byte[] bytes = write(new GadgetPayload());
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            ois.setObjectInputFilter(ObjectInputFilter.Config.createFilter("*"));
            assertThatThrownBy(() -> SerializeUtil.readObject(ois))
                    .isInstanceOf(InvalidClassException.class);
        }
        assertThat(GadgetPayload.deserialized).isFalse();
    }

    @Test
    void jsonBranch_allowlistNotBypassedByWarmClassCache() throws Exception {
        SerializeUtil.setJsonClassPrefixes(List.of("gadget."));
        byte[] bytes = write(new JsonPojo("x"));
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            assertThat(SerializeUtil.readObject(ois)).isInstanceOf(JsonPojo.class);
        }
        SerializeUtil.setJsonClassPrefixes(List.of("top.kdla."));
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            assertThatThrownBy(() -> SerializeUtil.readObject(ois))
                    .isInstanceOf(SecurityException.class);
        }
    }
}
