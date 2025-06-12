package top.kdla.framework.supplement.trdcloud.server.trd;

import com.baomidou.mybatisplus.extension.service.IService;
import top.kdla.framework.dto.MultiResponse;
import top.kdla.framework.dto.PageResponse;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformInfoBo;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformInfoPageQueryBo;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformInfoQueryBo;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformInfoEntity;

import java.util.List;
import java.util.Map;

public interface TrdPlatformInfoService extends IService<TrdPlatformInfoEntity> {

    PageResponse<TrdPlatformInfoBo> queryPage(TrdPlatformInfoPageQueryBo trdPlatformInfoPageQueryBo);

    boolean isExistName(String name);

    TrdPlatformInfoEntity getByPCode(String pCode);

    Map<String, TrdPlatformInfoEntity> getByPCodes(List<String> pCodes);

    boolean isExistCode(String code);

    MultiResponse<TrdPlatformInfoBo> list(TrdPlatformInfoQueryBo trdPlatformInfoQueryBo);


    boolean updateConfig(Long id, String configJson);
}
