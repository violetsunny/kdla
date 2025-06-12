package top.kdla.framework.supplement.trdcloud.server.trd.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import top.kdla.framework.dto.MultiResponse;
import top.kdla.framework.dto.PageResponse;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformInfoBo;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformInfoPageQueryBo;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformInfoQueryBo;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformInfoEntity;
import top.kdla.framework.supplement.trdcloud.mapper.TrdPlatformInfoMapper;
import top.kdla.framework.supplement.trdcloud.repository.TrdPlatformInfoRepository;
import top.kdla.framework.supplement.trdcloud.server.trd.TrdPlatformInfoService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TrdPlatformInfoServiceImpl extends ServiceImpl<TrdPlatformInfoMapper, TrdPlatformInfoEntity> implements TrdPlatformInfoService {

    @Autowired
    private TrdPlatformInfoRepository trdPlatformInfoRepository;

    @Override
    public PageResponse<TrdPlatformInfoBo> queryPage(TrdPlatformInfoPageQueryBo tdPlatformInfoPageQueryBo) {
        return trdPlatformInfoRepository.queryPage(tdPlatformInfoPageQueryBo);
    }

    @Override
    public boolean isExistName(String name) {
        LambdaQueryWrapper<TrdPlatformInfoEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrdPlatformInfoEntity::getPlatformName, name)
                .eq(TrdPlatformInfoEntity::getIsDelete,0);
        List<TrdPlatformInfoEntity> list = trdPlatformInfoRepository.list(queryWrapper);
        return !CollectionUtils.isEmpty(list);
    }

    @Override
    public TrdPlatformInfoEntity getByPCode(String pCode) {
        LambdaQueryWrapper<TrdPlatformInfoEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrdPlatformInfoEntity::getPlatformCode, pCode)
                .eq(TrdPlatformInfoEntity::getIsDelete,0);
        return trdPlatformInfoRepository.getOne(queryWrapper, false);
    }

    @Override
    public Map<String, TrdPlatformInfoEntity> getByPCodes(List<String> pCodes) {
        LambdaQueryWrapper<TrdPlatformInfoEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(TrdPlatformInfoEntity::getPlatformCode, pCodes)
                .eq(TrdPlatformInfoEntity::getIsDelete,0);
        List<TrdPlatformInfoEntity> list = trdPlatformInfoRepository.list(queryWrapper);
        return list.stream().collect(Collectors.toMap(TrdPlatformInfoEntity::getPlatformCode, Function.identity()));
    }

    @Override
    public boolean isExistCode(String code) {
        LambdaQueryWrapper<TrdPlatformInfoEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrdPlatformInfoEntity::getPlatformCode, code)
                .eq(TrdPlatformInfoEntity::getIsDelete,0);
        List<TrdPlatformInfoEntity> list = trdPlatformInfoRepository.list(queryWrapper);
        return !CollectionUtils.isEmpty(list);
    }

    @Override
    public MultiResponse<TrdPlatformInfoBo> list(TrdPlatformInfoQueryBo trdPlatformInfoQueryBo) {
        LambdaQueryWrapper<TrdPlatformInfoEntity> queryWrapper = new LambdaQueryWrapper<>();
        if (!StringUtils.isEmpty(trdPlatformInfoQueryBo.getPlatformName())) {
            queryWrapper.like(TrdPlatformInfoEntity::getPlatformName, trdPlatformInfoQueryBo.getPlatformName());
        }
        if (!StringUtils.isEmpty(trdPlatformInfoQueryBo.getPlatformCode())) {
            queryWrapper.eq(TrdPlatformInfoEntity::getPlatformCode, trdPlatformInfoQueryBo.getPlatformCode());
        }
        if (trdPlatformInfoQueryBo.getPlatformType() != null) {
            queryWrapper.eq(TrdPlatformInfoEntity::getPlatformType, trdPlatformInfoQueryBo.getPlatformType());
        }
        if (StringUtils.isNotBlank(trdPlatformInfoQueryBo.getPlatformSource())) {
            queryWrapper.eq(TrdPlatformInfoEntity::getPlatformSource, trdPlatformInfoQueryBo.getPlatformSource());
        }
        queryWrapper.eq(TrdPlatformInfoEntity::getIsDelete,0);
        List<TrdPlatformInfoEntity> list = trdPlatformInfoRepository.list(queryWrapper);
        return MultiResponse.buildSuccess(BeanUtil.copyToList(list, TrdPlatformInfoBo.class));
    }

    @Override
    public boolean updateConfig(Long id, String configJson) {
        return trdPlatformInfoRepository.update(Wrappers.<TrdPlatformInfoEntity>lambdaUpdate()
                .eq(TrdPlatformInfoEntity::getId, id)
                .set(TrdPlatformInfoEntity::getConfigJson, configJson)
        );
    }

}
