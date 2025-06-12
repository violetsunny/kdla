package top.kdla.framework.supplement.trdcloud.server.trd;

import com.baomidou.mybatisplus.extension.service.IService;
import top.kdla.framework.dto.MultiResponse;
import top.kdla.framework.dto.PageResponse;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformTaskBo;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformTaskPageQueryBo;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformTaskQueryBo;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformTaskEntity;

public interface TrdPlatformTaskService extends IService<TrdPlatformTaskEntity> {

    PageResponse<TrdPlatformTaskBo> queryPage(TrdPlatformTaskPageQueryBo trdPlatformTaskPageQueryBo);

    boolean isExistName(String platformCode,String name);

    boolean isExistCode(String platformCode,String code);

    MultiResponse<TrdPlatformTaskBo> list(TrdPlatformTaskQueryBo trdPlatformTaskQueryBo);

    Boolean saveTask(TrdPlatformTaskEntity entity);

    Boolean updateTask(TrdPlatformTaskEntity updateEntity);

    Boolean removeTask(Long id);

}
