package top.kdla.framework.supplement.trdcloud.repository;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import top.kdla.framework.dto.PageResponse;
import top.kdla.framework.infra.dal.mybatis.util.PlusPageQuery;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformModelRefBo;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformModelRefPageQueryBo;
import top.kdla.framework.supplement.trdcloud.converter.TrdPlatformModelRefBoConverter;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformModelRefEntity;
import top.kdla.framework.supplement.trdcloud.mapper.TrdPlatformModelRefMapper;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class TrdPlatformModelRefRepository extends ServiceImpl<TrdPlatformModelRefMapper, TrdPlatformModelRefEntity> implements IService<TrdPlatformModelRefEntity> {

    @Resource
    TrdPlatformModelRefBoConverter trdPlatformModelRefBoConverter;

    public PageResponse<TrdPlatformModelRefBo> queryPage(TrdPlatformModelRefPageQueryBo pageQuery) {
        Map<String, Object> params = BeanUtil.beanToMap(pageQuery);
        IPage<TrdPlatformModelRefEntity> page = baseMapper.queryPage(new PlusPageQuery<TrdPlatformModelRefEntity>(pageQuery).getPage(params), params);
        List<TrdPlatformModelRefBo> list = trdPlatformModelRefBoConverter.toTrdPlatformModelRefs(page.getRecords());
        return PageResponse.of(list, page.getTotal(), page.getSize(), page.getCurrent());
    }

    public TrdPlatformModelRefBo queryByCode(String code,String productId) {
        LambdaQueryChainWrapper<TrdPlatformModelRefEntity> queryChainWrapper = this.lambdaQuery()
                .eq(StringUtils.hasText(code), TrdPlatformModelRefEntity::getPlatformCode, code)
                .eq(StringUtils.hasText(productId), TrdPlatformModelRefEntity::getEnnProductId, productId);
        return trdPlatformModelRefBoConverter.toTrdPlatformModelRef(queryChainWrapper.one());
    }

}
