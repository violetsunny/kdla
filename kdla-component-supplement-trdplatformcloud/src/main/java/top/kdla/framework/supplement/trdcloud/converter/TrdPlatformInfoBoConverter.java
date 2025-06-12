package top.kdla.framework.supplement.trdcloud.converter;

import org.mapstruct.Mapper;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformInfoBo;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformInfoEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TrdPlatformInfoBoConverter {

    TrdPlatformInfoEntity fromTrdPlatformInfo(TrdPlatformInfoBo bo);

    List<TrdPlatformInfoBo> toTrdPlatformInfos(List<TrdPlatformInfoEntity> records);

    TrdPlatformInfoBo toTrdPlatformInfo(TrdPlatformInfoEntity entity);

}
