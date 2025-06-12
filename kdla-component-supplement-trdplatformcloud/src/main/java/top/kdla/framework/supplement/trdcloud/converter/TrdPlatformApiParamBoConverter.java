package top.kdla.framework.supplement.trdcloud.converter;

import org.mapstruct.Mapper;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformApiParamBo;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformApiParamEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TrdPlatformApiParamBoConverter {

    List<TrdPlatformApiParamBo> toTrdPlatformApiParams(List<TrdPlatformApiParamEntity> entity);

    TrdPlatformApiParamBo toTrdPlatformApiParam(TrdPlatformApiParamEntity entity);

}