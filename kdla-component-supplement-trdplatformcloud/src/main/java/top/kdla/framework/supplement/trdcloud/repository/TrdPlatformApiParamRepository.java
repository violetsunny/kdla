package top.kdla.framework.supplement.trdcloud.repository;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformApiParamBo;
import top.kdla.framework.supplement.trdcloud.converter.TrdPlatformApiParamBoConverter;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformApiParamEntity;
import top.kdla.framework.supplement.trdcloud.mapper.TrdPlatformApiParamMapper;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TrdPlatformApiParamRepository extends ServiceImpl<TrdPlatformApiParamMapper, TrdPlatformApiParamEntity> implements IService<TrdPlatformApiParamEntity> {

    @Resource
    private TrdPlatformApiParamBoConverter trdPlatformApiParamBoConverter;

    public List<TrdPlatformApiParamEntity> searchById(Long apiId) {
        LambdaQueryChainWrapper<TrdPlatformApiParamEntity> queryChainWrapper = this.lambdaQuery()
                .eq(apiId!=null, TrdPlatformApiParamEntity::getApiId, apiId);
        return queryChainWrapper.list();
    }

    public List<TrdPlatformApiParamBo> getById(Long apiId) {
        List<TrdPlatformApiParamEntity> entity = searchById(apiId);
        return trdPlatformApiParamBoConverter.toTrdPlatformApiParams(entity);
    }

}
