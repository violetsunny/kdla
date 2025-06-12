package top.kdla.framework.supplement.trdcloud.server.trd;

import com.baomidou.mybatisplus.extension.service.IService;
import top.kdla.framework.dto.MultiResponse;
import top.kdla.framework.dto.PageResponse;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformModelRefBo;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformModelRefPageQueryBo;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformModelRefQueryBo;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformModelRefEntity;

public interface TrdPlatformModelRefService extends IService<TrdPlatformModelRefEntity> {

    PageResponse<TrdPlatformModelRefBo> queryPage(TrdPlatformModelRefPageQueryBo trdPlatformModelRefPageQueryBo);

    String entityParamCheck(TrdPlatformModelRefEntity trdPlatformModelRefEntity);

    MultiResponse<TrdPlatformModelRefBo> list(TrdPlatformModelRefQueryBo trdPlatformModelRefQueryBo);

}
