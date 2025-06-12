package top.kdla.framework.supplement.trdcloud.cloud;

import org.mapstruct.Mapper;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformTaskBo;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TrdPlatformConverter {

    List<TrdPlatformTask> toTrdPlatformTasks(List<TrdPlatformTaskBo> bos);

    TrdPlatformTask toTrdPlatformTask(TrdPlatformTaskBo bo);
}
