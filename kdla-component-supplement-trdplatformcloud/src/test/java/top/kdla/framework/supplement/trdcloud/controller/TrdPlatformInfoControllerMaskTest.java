package top.kdla.framework.supplement.trdcloud.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.kdla.framework.dto.MultiResponse;
import top.kdla.framework.dto.PageResponse;
import top.kdla.framework.dto.SingleResponse;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformInfoBo;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformInfoQueryBo;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformInfoEntity;
import top.kdla.framework.supplement.trdcloud.server.trd.TrdPlatformInfoService;
import top.kdla.framework.supplement.trdcloud.vo.TrdPlatformInfoPageQueryVo;
import top.kdla.framework.supplement.trdcloud.vo.TrdPlatformInfoQueryVo;
import top.kdla.framework.supplement.trdcloud.vo.TrdPlatformInfoVo;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrdPlatformInfoControllerMaskTest {

    private static final String CONFIG = "{\"baseUrl\":\"https://x.example\",\"appKey\":\"ak-123\",\"appSecret\":\"s3cr3t\"}";

    private TrdPlatformInfoController controller;
    private TrdPlatformInfoService service;

    @BeforeEach
    void setUp() throws Exception {
        controller = new TrdPlatformInfoController();
        service = mock(TrdPlatformInfoService.class);
        Field field = TrdPlatformInfoController.class.getDeclaredField("trdPlatformInfoService");
        field.setAccessible(true);
        field.set(controller, service);
    }

    private TrdPlatformInfoBo bo() {
        TrdPlatformInfoBo bo = new TrdPlatformInfoBo();
        bo.setConfigJson(CONFIG);
        bo.setCreateTime(new Date());
        bo.setUpdateTime(new Date());
        return bo;
    }

    private void assertMasked(String configJson) {
        assertThat(configJson).doesNotContain("ak-123").doesNotContain("s3cr3t");
        assertThat(configJson).contains("https://x.example");
    }

    @Test
    void detailMasksCredentials() {
        // B-04 升级链 2：detail 不得明文返回 appKey/appSecret
        when(service.getById("1")).thenReturn(bo());
        SingleResponse<TrdPlatformInfoVo> resp = controller.detail("1");
        assertMasked(resp.getData().getConfigJson());
    }

    @Test
    void pageMasksCredentials() {
        when(service.queryPage(any())).thenReturn(PageResponse.of(Collections.singletonList(bo()), 1, 10, 1));
        PageResponse<TrdPlatformInfoVo> resp = controller.page(new TrdPlatformInfoPageQueryVo());
        assertMasked(resp.getData().get(0).getConfigJson());
    }

    @Test
    void listMasksCredentials() {
        when(service.list(any(TrdPlatformInfoQueryBo.class))).thenReturn(MultiResponse.buildSuccess(Collections.singletonList(bo())));
        MultiResponse<TrdPlatformInfoVo> resp = controller.list(new TrdPlatformInfoQueryVo());
        assertMasked(resp.getData().iterator().next().getConfigJson());
    }
}
