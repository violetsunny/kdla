package top.kdla.framework.supplement.trdcloud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformMeasureRefEntity;

import java.util.Map;

/**
 * @description TrdPlatformModelRefMapper
 * @author qk
 * @date 2024-04-08
 */
public interface TrdPlatformMeasureRefMapper extends BaseMapper<TrdPlatformMeasureRefEntity> {

    IPage<TrdPlatformMeasureRefEntity> queryPage(IPage<TrdPlatformMeasureRefEntity> page, @Param("params") Map<String, Object> params);

}