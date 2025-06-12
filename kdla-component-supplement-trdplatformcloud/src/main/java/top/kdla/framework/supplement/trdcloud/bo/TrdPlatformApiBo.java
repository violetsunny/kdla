package top.kdla.framework.supplement.trdcloud.bo;

import lombok.Data;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformApiEntity;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformApiParamEntity;

import java.util.List;

@Data
public class TrdPlatformApiBo extends TrdPlatformApiEntity {

    List<TrdPlatformApiParamEntity> apiParams;

}
