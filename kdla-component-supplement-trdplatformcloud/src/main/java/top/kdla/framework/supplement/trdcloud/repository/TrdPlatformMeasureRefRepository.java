package top.kdla.framework.supplement.trdcloud.repository;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformMeasureRefBo;
import top.kdla.framework.supplement.trdcloud.converter.TrdPlatformMeasureRefBoConverter;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformMeasureRefEntity;
import top.kdla.framework.supplement.trdcloud.mapper.TrdPlatformMeasureRefMapper;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TrdPlatformMeasureRefRepository extends ServiceImpl<TrdPlatformMeasureRefMapper, TrdPlatformMeasureRefEntity> implements IService<TrdPlatformMeasureRefEntity> {

    @Resource
    TrdPlatformMeasureRefBoConverter trdPlatformMeasureRefBoConverter;

    public List<TrdPlatformMeasureRefBo> queryById(Long modelRefId) {
        LambdaQueryChainWrapper<TrdPlatformMeasureRefEntity> queryChainWrapper = this.lambdaQuery()
                .eq(modelRefId!=null, TrdPlatformMeasureRefEntity::getModelRefId, modelRefId);
        return trdPlatformMeasureRefBoConverter.toTrdPlatformMeasureRefs(queryChainWrapper.list());
    }

}
