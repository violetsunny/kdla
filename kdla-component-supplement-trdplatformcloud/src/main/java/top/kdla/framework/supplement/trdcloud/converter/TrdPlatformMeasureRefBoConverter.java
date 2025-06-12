package top.kdla.framework.supplement.trdcloud.converter;

import org.mapstruct.Mapper;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformMeasureRefBo;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformMeasureRefEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TrdPlatformMeasureRefBoConverter {

    TrdPlatformMeasureRefEntity fromTrdPlatformMeasureRef(TrdPlatformMeasureRefBo bo);

    List<TrdPlatformMeasureRefBo> toTrdPlatformMeasureRefs(List<TrdPlatformMeasureRefEntity> records);

    TrdPlatformMeasureRefBo toTrdPlatformMeasureRef(TrdPlatformMeasureRefEntity entity);

}
