package top.kdla.framework.supplement.trdcloud.server.trd.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import top.kdla.framework.dto.MultiResponse;
import top.kdla.framework.dto.PageResponse;
import top.kdla.framework.supplement.trdcloud.TrdPlatformCloudServer;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformTaskBo;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformTaskPageQueryBo;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformTaskQueryBo;
import top.kdla.framework.supplement.trdcloud.cloud.TrdPlatformTaskMessage;
import top.kdla.framework.supplement.trdcloud.converter.TrdPlatformTaskBoConverter;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformTaskEntity;
import top.kdla.framework.supplement.trdcloud.enums.TrdPlatformEnum;
import top.kdla.framework.supplement.trdcloud.mapper.TrdPlatformTaskMapper;
import top.kdla.framework.supplement.trdcloud.repository.TrdPlatformTaskRepository;
import top.kdla.framework.supplement.trdcloud.server.trd.TrdPlatformTaskService;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TrdPlatformTaskServiceImpl extends ServiceImpl<TrdPlatformTaskMapper, TrdPlatformTaskEntity> implements TrdPlatformTaskService {

    @Autowired
    private TrdPlatformTaskRepository trdPlatformTaskRepository;
    @Resource
    private TrdPlatformTaskBoConverter trdPlatformTaskBoConverter;
    @Resource
    private TrdPlatformCloudServer trdPlatformCloudServer;

    @Override
    public PageResponse<TrdPlatformTaskBo> queryPage(TrdPlatformTaskPageQueryBo tdPlatformTaskPageQueryBo) {
        return trdPlatformTaskRepository.queryPage(tdPlatformTaskPageQueryBo);
    }

    @Override
    public boolean isExistName(String platformCode, String name) {
        LambdaQueryWrapper<TrdPlatformTaskEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(TrdPlatformTaskEntity::getPlatformCode, platformCode)
                .eq(TrdPlatformTaskEntity::getTaskName, name)
                .eq(TrdPlatformTaskEntity::getIsDelete, 0);
        List<TrdPlatformTaskEntity> list = trdPlatformTaskRepository.list(queryWrapper);
        return !CollectionUtils.isEmpty(list);
    }

    @Override
    public boolean isExistCode(String platformCode, String code) {
        LambdaQueryWrapper<TrdPlatformTaskEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(TrdPlatformTaskEntity::getPlatformCode, platformCode)
                .eq(TrdPlatformTaskEntity::getTaskCode, code)
                .eq(TrdPlatformTaskEntity::getIsDelete, 0);
        List<TrdPlatformTaskEntity> list = trdPlatformTaskRepository.list(queryWrapper);
        return !CollectionUtils.isEmpty(list);
    }

    @Override
    public MultiResponse<TrdPlatformTaskBo> list(TrdPlatformTaskQueryBo trdPlatformTaskQueryBo) {
        LambdaQueryWrapper<TrdPlatformTaskEntity> queryWrapper = new LambdaQueryWrapper<>();
        if (!StringUtils.isEmpty(trdPlatformTaskQueryBo.getTaskName())) {
            queryWrapper.like(TrdPlatformTaskEntity::getTaskName, trdPlatformTaskQueryBo.getTaskName());
        }
        if (!StringUtils.isEmpty(trdPlatformTaskQueryBo.getPlatformCode())) {
            queryWrapper.eq(TrdPlatformTaskEntity::getPlatformCode, trdPlatformTaskQueryBo.getPlatformCode());
        }
        if (!StringUtils.isEmpty(trdPlatformTaskQueryBo.getTaskCode())) {
            queryWrapper.eq(TrdPlatformTaskEntity::getTaskCode, trdPlatformTaskQueryBo.getTaskCode());
        }
        if (!StringUtils.isEmpty(trdPlatformTaskQueryBo.getProductId())) {
            queryWrapper.eq(TrdPlatformTaskEntity::getProductId, trdPlatformTaskQueryBo.getProductId());
        }
        List<TrdPlatformTaskEntity> list = trdPlatformTaskRepository.list(queryWrapper);
        return MultiResponse.buildSuccess(BeanUtil.copyToList(list, TrdPlatformTaskBo.class));
    }

    @Override
    public Boolean saveTask(TrdPlatformTaskEntity entity) {
        Boolean res = this.save(entity);
        if (res) {
            TrdPlatformTaskMessage message = trdPlatformTaskBoConverter.fromTrdPlatformTaskEntity(entity);
            trdPlatformCloudServer.operateTaskWork(message, TrdPlatformEnum.ADD.getCode());
        }
        return res;
    }

    @Override
    public Boolean updateTask(TrdPlatformTaskEntity updateEntity) {
        updateEntity.setPlatformCode(null);
        updateEntity.setTaskCode(null);
        Boolean res = this.updateById(updateEntity);
        if (res) {
            TrdPlatformTaskEntity entity = this.getById(updateEntity.getId());
            TrdPlatformTaskMessage message = trdPlatformTaskBoConverter.fromTrdPlatformTaskEntity(entity);
            trdPlatformCloudServer.operateTaskWork(message, TrdPlatformEnum.UPDATE.getCode());
        }
        return res;
    }

    @Override
    public Boolean removeTask(Long id) {
        TrdPlatformTaskEntity entity = this.getById(id);
        if (entity == null) {
            return false;
        } else {
            Boolean res = this.removeById(id);
            if (res) {
                TrdPlatformTaskMessage message = trdPlatformTaskBoConverter.fromTrdPlatformTaskEntity(entity);
                trdPlatformCloudServer.operateTaskWork(message, TrdPlatformEnum.REMOVE.getCode());
            }
            return res;
        }
    }
}
