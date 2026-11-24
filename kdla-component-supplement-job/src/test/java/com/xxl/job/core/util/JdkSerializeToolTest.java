package com.xxl.job.core.util;

import com.xxl.job.core.biz.model.HandleCallbackParam;
import gadget.GadgetPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputFilter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** CWE-502 回归：callbacklog 文件反序列化须按白名单过滤。无修复则拒绝类断言失败。 */
class JdkSerializeToolTest {

    @BeforeEach
    void resetFlag() {
        GadgetPayload.deserialized = false;
    }

    @Test
    @SuppressWarnings("unchecked")
    void deserialize_roundTripsCallbackParamList() {
        List<HandleCallbackParam> params = new ArrayList<>();
        params.add(new HandleCallbackParam(1L, 2L, 200, "ok"));
        params.add(new HandleCallbackParam(3L, 4L, 500, "fail"));

        byte[] bytes = JdkSerializeTool.serialize(params);
        List<HandleCallbackParam> read =
                (List<HandleCallbackParam>) JdkSerializeTool.deserialize(bytes, List.class);

        assertThat(read).hasSize(2);
        assertThat(read.get(0).getLogId()).isEqualTo(1L);
        assertThat(read.get(1).getHandleMsg()).isEqualTo("fail");
    }

    @Test
    void deserialize_rejectsClassNotMatchingDeclaredType() {
        byte[] bytes = JdkSerializeTool.serialize(new GadgetPayload());

        assertThat(JdkSerializeTool.deserialize(bytes, List.class)).isNull();
        assertThat(GadgetPayload.deserialized).isFalse();
    }

    @Test
    void deserialize_rejectsGadgetNestedInList() {
        List<Object> params = new ArrayList<>();
        params.add(new HandleCallbackParam(1L, 2L, 200, "ok"));
        params.add(new GadgetPayload());

        byte[] bytes = JdkSerializeTool.serialize(params);

        assertThat(JdkSerializeTool.deserialize(bytes, List.class)).isNull();
        assertThat(GadgetPayload.deserialized).isFalse();
    }

    @Test
    void deserialize_explicitFilterOverridesDefault() {
        byte[] bytes = JdkSerializeTool.serialize(new GadgetPayload());
        ObjectInputFilter allowAll = ObjectInputFilter.Config.createFilter("*");

        Object read = JdkSerializeTool.deserialize(bytes, GadgetPayload.class, allowAll);

        assertThat(read).isInstanceOf(GadgetPayload.class);
        assertThat(GadgetPayload.deserialized).isTrue();
    }

    @Test
    void deserialize_nullFilterKeepsUnfilteredBehaviour() {
        byte[] bytes = JdkSerializeTool.serialize(new GadgetPayload());

        assertThat(JdkSerializeTool.deserialize(bytes, GadgetPayload.class, null))
                .isInstanceOf(GadgetPayload.class);
    }
}
