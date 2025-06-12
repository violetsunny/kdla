package top.kdla.framework.supplement.trdcloud.converter;

import org.mapstruct.Mapper;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformModelRefBo;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformModelRefEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TrdPlatformModelRefBoConverter {

    TrdPlatformModelRefEntity fromTrdPlatformModelRef(TrdPlatformModelRefBo bo);

    List<TrdPlatformModelRefBo> toTrdPlatformModelRefs(List<TrdPlatformModelRefEntity> records);

    TrdPlatformModelRefBo toTrdPlatformModelRef(TrdPlatformModelRefEntity entity);

}
