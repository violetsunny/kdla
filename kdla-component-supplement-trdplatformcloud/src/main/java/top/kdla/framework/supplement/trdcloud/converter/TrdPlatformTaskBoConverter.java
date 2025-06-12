package top.kdla.framework.supplement.trdcloud.converter;

import org.mapstruct.Mapper;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformTaskBo;
import top.kdla.framework.supplement.trdcloud.cloud.TrdPlatformTaskMessage;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformTaskEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TrdPlatformTaskBoConverter {

    TrdPlatformTaskEntity fromTrdPlatformTask(TrdPlatformTaskBo bo);

    List<TrdPlatformTaskBo> toTrdPlatformTasks(List<TrdPlatformTaskEntity> records);

    TrdPlatformTaskBo toTrdPlatformTask(TrdPlatformTaskEntity entity);

    TrdPlatformTaskMessage fromTrdPlatformTaskEntity(TrdPlatformTaskEntity entity);

}
