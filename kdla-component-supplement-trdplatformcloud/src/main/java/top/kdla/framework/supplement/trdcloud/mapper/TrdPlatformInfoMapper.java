package top.kdla.framework.supplement.trdcloud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformInfoEntity;

import java.util.Map;

/**
 * @author ruanhong
 * @description trd_platform_info
 * @date 2024-03-13
 */
public interface TrdPlatformInfoMapper extends BaseMapper<TrdPlatformInfoEntity> {

    IPage<TrdPlatformInfoEntity> queryPage(IPage<TrdPlatformInfoEntity> page, @Param("params") Map<String, Object> params);

}