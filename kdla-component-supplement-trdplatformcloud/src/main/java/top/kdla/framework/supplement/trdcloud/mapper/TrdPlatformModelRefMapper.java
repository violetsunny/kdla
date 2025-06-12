package top.kdla.framework.supplement.trdcloud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformModelRefEntity;

import java.util.Map;

/**
 * @description TrdPlatformModelRefMapper
 * @author qk
 * @date 2024-04-08
 */
public interface TrdPlatformModelRefMapper extends BaseMapper<TrdPlatformModelRefEntity> {

    IPage<TrdPlatformModelRefEntity> queryPage(IPage<TrdPlatformModelRefEntity> page, @Param("params") Map<String, Object> params);

}