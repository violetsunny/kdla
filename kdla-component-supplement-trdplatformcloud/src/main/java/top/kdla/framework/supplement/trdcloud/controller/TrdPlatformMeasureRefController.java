package top.kdla.framework.supplement.trdcloud.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.kdla.framework.dto.MultiResponse;
import top.kdla.framework.dto.SingleResponse;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformMeasureRefEntity;
import top.kdla.framework.supplement.trdcloud.server.trd.TrdPlatformMeasureRefService;
import top.kdla.framework.supplement.trdcloud.vo.TrdPlatformMeasureRefAddVo;
import top.kdla.framework.supplement.trdcloud.vo.TrdPlatformMeasureRefVo;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@Validated
@Tag(name = "云平台测点射管理")
@RequestMapping("/trd/platform/measure/ref")
public class TrdPlatformMeasureRefController {

    @Resource
    TrdPlatformMeasureRefService trdPlatformMeasureRefService;

    @GetMapping("/detail/{id}")
    @Operation(summary = "详情")
    public SingleResponse<TrdPlatformMeasureRefVo> detail(@PathVariable String id) {
        TrdPlatformMeasureRefEntity entity = trdPlatformMeasureRefService.getById(id);
        return SingleResponse.buildSuccess(BeanUtil.copyProperties(entity, TrdPlatformMeasureRefVo.class));
    }

    @GetMapping("/list")
    @Operation(summary = "三方平台测点映射列表")
    public MultiResponse<TrdPlatformMeasureRefVo> list(Long modelRefId) {
        LambdaQueryWrapper<TrdPlatformMeasureRefEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(TrdPlatformMeasureRefEntity::getModelRefId, modelRefId);
        List<TrdPlatformMeasureRefEntity> list = trdPlatformMeasureRefService.list(queryWrapper);
        return MultiResponse.buildSuccess(BeanUtil.copyToList(list, TrdPlatformMeasureRefVo.class));
    }

    @PostMapping("/save")
    @Operation(summary = "新增三方平台测点映射")
    public SingleResponse<?> save(@Valid @RequestBody TrdPlatformMeasureRefAddVo trdPlatformMeasureRefAddVo, @RequestHeader(value = "blade-auth", required = false) String bladeAuth) {
        String checkResult = trdPlatformMeasureRefService.entityParamCheck(BeanUtil.copyProperties(trdPlatformMeasureRefAddVo, TrdPlatformMeasureRefEntity.class));
        if (StringUtils.isNotBlank(checkResult)) {
            return SingleResponse.buildFailure("10001", checkResult);
        }
        return SingleResponse.buildSuccess(trdPlatformMeasureRefService.save(trdPlatformMeasureRefAddVo.createEntity(trdPlatformMeasureRefAddVo, bladeAuth)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "根据ID修改三方平台测点映射")
    public SingleResponse<Boolean> update(@PathVariable Long id, @Valid @RequestBody TrdPlatformMeasureRefAddVo trdPlatformMeasureRefAddVo, @RequestHeader(value = "blade-auth", required = false) String bladeAuth) {
        TrdPlatformMeasureRefEntity trdPlatformMeasureRefEntity = BeanUtil.copyProperties(trdPlatformMeasureRefAddVo, TrdPlatformMeasureRefEntity.class);
        trdPlatformMeasureRefEntity.setId(id);
        String checkResult = trdPlatformMeasureRefService.entityParamCheck(trdPlatformMeasureRefEntity);
        if (StringUtils.isNotBlank(checkResult)) {
            return SingleResponse.buildFailure("10001", checkResult);
        }
        return SingleResponse.buildSuccess(trdPlatformMeasureRefService.updateById(trdPlatformMeasureRefAddVo.updateEntity(id, trdPlatformMeasureRefAddVo, bladeAuth)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除三方平台测点映射")
    public SingleResponse<Boolean> remove(@PathVariable Long id) {
        return SingleResponse.buildSuccess(trdPlatformMeasureRefService.removeById(id));
    }

    @GetMapping("/bind/{ennMeasureId}")
    @Operation(summary = "查看物模型测点是否被绑定")
    public SingleResponse<Boolean> isBindModel(@PathVariable String ennMeasureId) {
        LambdaQueryWrapper<TrdPlatformMeasureRefEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrdPlatformMeasureRefEntity::getEnnMeasureId, ennMeasureId);
        return SingleResponse.buildSuccess(!CollectionUtils.isEmpty(trdPlatformMeasureRefService.list(queryWrapper)));
    }

}
