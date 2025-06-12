package top.kdla.framework.supplement.trdcloud.repository;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformApiBo;
import top.kdla.framework.supplement.trdcloud.converter.TrdPlatformApiBoConverter;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformApiEntity;
import top.kdla.framework.supplement.trdcloud.mapper.TrdPlatformApiMapper;

import javax.annotation.Resource;

@Service
public class TrdPlatformApiRepository extends ServiceImpl<TrdPlatformApiMapper, TrdPlatformApiEntity> implements IService<TrdPlatformApiEntity> {

    @Resource
    TrdPlatformApiBoConverter trdPlatformApiBoConverter;

    public TrdPlatformApiEntity searchById(Long id) {
        LambdaQueryChainWrapper<TrdPlatformApiEntity> queryChainWrapper = this.lambdaQuery()
                .eq(id!=null, TrdPlatformApiEntity::getId, id);
        return queryChainWrapper.one();
    }

    public TrdPlatformApiBo getById(Long id) {
        TrdPlatformApiEntity entity = searchById(id);
        return trdPlatformApiBoConverter.toTrdPlatformApi(entity);
    }
}
