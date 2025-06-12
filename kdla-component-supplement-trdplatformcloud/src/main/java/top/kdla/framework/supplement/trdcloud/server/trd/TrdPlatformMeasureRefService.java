package top.kdla.framework.supplement.trdcloud.server.trd;

import com.baomidou.mybatisplus.extension.service.IService;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformMeasureRefEntity;

public interface TrdPlatformMeasureRefService extends IService<TrdPlatformMeasureRefEntity> {

    String entityParamCheck(TrdPlatformMeasureRefEntity trdPlatformMeasureRefEntity);

}
