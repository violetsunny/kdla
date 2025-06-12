package top.kdla.framework.supplement.trdcloud.server.trd;

import com.baomidou.mybatisplus.extension.service.IService;
import top.kdla.framework.dto.MultiResponse;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformApiBo;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformApiQueryBo;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformApiEntity;

public interface TrdPlatformApiService extends IService<TrdPlatformApiEntity> {

    MultiResponse<TrdPlatformApiBo> list(TrdPlatformApiQueryBo trdPlatformApiQueryBo);

    TrdPlatformApiBo getDetailById(String id);

    Boolean saveApi(TrdPlatformApiBo trdPlatformApiBo);

    Boolean updateApiById(TrdPlatformApiBo trdPlatformApiBo);

    boolean isExistName(String code,String name);

    boolean isExistCode(String code);

    MultiResponse<TrdPlatformApiBo> listApi(TrdPlatformApiQueryBo trdPlatformApiQueryBo);

    Boolean removeApiById(Long id);

}
