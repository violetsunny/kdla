package top.kdla.framework.supplement.trdcloud.converter;

import org.mapstruct.Mapper;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformApiBo;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformApiEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TrdPlatformApiBoConverter {

    TrdPlatformApiEntity fromTrdPlatformApi(TrdPlatformApiBo bo);

    List<TrdPlatformApiBo> toTrdPlatformApis(List<TrdPlatformApiEntity> records);

    TrdPlatformApiBo toTrdPlatformApi(TrdPlatformApiEntity entity);

}
