package top.kdla.framework.supplement.trdcloud.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.kdla.framework.dto.MultiResponse;
import top.kdla.framework.dto.PageResponse;
import top.kdla.framework.dto.SingleResponse;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformModelRefBo;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformModelRefPageQueryBo;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformModelRefQueryBo;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformInfoEntity;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformMeasureRefEntity;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformModelRefEntity;
import top.kdla.framework.supplement.trdcloud.server.trd.TrdPlatformInfoService;
import top.kdla.framework.supplement.trdcloud.server.trd.TrdPlatformMeasureRefService;
import top.kdla.framework.supplement.trdcloud.server.trd.TrdPlatformModelRefService;
import top.kdla.framework.supplement.trdcloud.vo.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@Validated
@Tag(name = "云平台模型映射管理")
@RequestMapping("/trd/platform/model/ref")
public class TrdPlatformModelRefController {

    @Resource
    TrdPlatformModelRefService trdPlatformModelRefService;
    @Resource
    TrdPlatformInfoService trdPlatformInfoService;
    @Resource
    TrdPlatformMeasureRefService trdPlatformMeasureRefService;

    @GetMapping("/detail/{id}")
    @Operation(summary = "详情")
    public SingleResponse<TrdPlatformModelRefVo> detail(@PathVariable String id) {
        TrdPlatformModelRefEntity entity = trdPlatformModelRefService.getById(id);
        return SingleResponse.buildSuccess(BeanUtil.copyProperties(entity, TrdPlatformModelRefVo.class));
    }

    @GetMapping("/page")
    @Operation(summary = "三方平台模型映射分页")
    public PageResponse<TrdPlatformModelRefVo> page(TrdPlatformModelRefPageQueryVo trdPlatformModelRefPageQueryVo) {
        TrdPlatformModelRefPageQueryBo trdPlatformModelRefPageQueryBo = BeanUtil.copyProperties(trdPlatformModelRefPageQueryVo, TrdPlatformModelRefPageQueryBo.class);
        PageResponse<TrdPlatformModelRefBo> page = trdPlatformModelRefService.queryPage(trdPlatformModelRefPageQueryBo);
        return PageResponse.of(BeanUtil.copyToList(page.getData(), TrdPlatformModelRefVo.class), page.getTotalCount(), page.getPageSize(), page.getPageNum());
    }

    @GetMapping("/list")
    @Operation(summary = "三方平台模型映射列表")
    public MultiResponse<TrdPlatformModelRefVo> list(TrdPlatformModelRefQueryVo trdPlatformModelRefQueryVo) {
        TrdPlatformModelRefQueryBo trdPlatformModelRefQueryBo = BeanUtil.copyProperties(trdPlatformModelRefQueryVo, TrdPlatformModelRefQueryBo.class);
        MultiResponse<TrdPlatformModelRefBo> list = trdPlatformModelRefService.list(trdPlatformModelRefQueryBo);
        return MultiResponse.buildSuccess(BeanUtil.copyToList(list.getData(), TrdPlatformModelRefVo.class));
    }

    @PostMapping("/save")
    @Operation(summary = "新增三方平台模型映射")
    public SingleResponse<?> save(@Valid @RequestBody TrdPlatformModelRefAddVo trdPlatformModelRefAddVo, @RequestHeader(value = "blade-auth", required = false) String bladeAuth) {
        String checkResult = trdPlatformModelRefService.entityParamCheck(BeanUtil.copyProperties(trdPlatformModelRefAddVo, TrdPlatformModelRefEntity.class));
        if (StringUtils.isNotBlank(checkResult)) {
            return SingleResponse.buildFailure("10001", checkResult);
        }
        TrdPlatformInfoEntity trdPlatformInfo = trdPlatformInfoService.getByPCode(trdPlatformModelRefAddVo.getPlatformCode());
        if (trdPlatformInfo == null) {
            return SingleResponse.buildFailure("10001", trdPlatformModelRefAddVo.getPlatformCode() + " 没有平台信息，请确认信息是否正确");
        }
        trdPlatformModelRefAddVo.setEnnModelSource(StringUtils.isNotBlank(trdPlatformModelRefAddVo.getEnnModelSource()) ? trdPlatformModelRefAddVo.getEnnModelSource() : trdPlatformInfo.getPlatformSource());
        return SingleResponse.buildSuccess(trdPlatformModelRefService.save(trdPlatformModelRefAddVo.createEntity(trdPlatformModelRefAddVo, bladeAuth)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "根据ID修改三方平台模型映射")
    public SingleResponse<Boolean> update(@PathVariable Long id, @Valid @RequestBody TrdPlatformModelRefAddVo trdPlatformModelRefAddVo, @RequestHeader(value = "blade-auth", required = false) String bladeAuth) {
        TrdPlatformModelRefEntity trdPlatformModelRefEntity = BeanUtil.copyProperties(trdPlatformModelRefAddVo, TrdPlatformModelRefEntity.class);
        trdPlatformModelRefEntity.setId(id);
        String checkResult = trdPlatformModelRefService.entityParamCheck(trdPlatformModelRefEntity);
        if (StringUtils.isNotBlank(checkResult)) {
            return SingleResponse.buildFailure("10001", checkResult);
        }
        TrdPlatformInfoEntity trdPlatformInfo = trdPlatformInfoService.getByPCode(trdPlatformModelRefAddVo.getPlatformCode());
        if (trdPlatformInfo == null) {
            return SingleResponse.buildFailure("10001", trdPlatformModelRefAddVo.getPlatformCode() + " 没有平台信息，请确认信息是否正确");
        }
        trdPlatformModelRefAddVo.setEnnModelSource(StringUtils.isNotBlank(trdPlatformModelRefAddVo.getEnnModelSource()) ? trdPlatformModelRefAddVo.getEnnModelSource() : trdPlatformInfo.getPlatformSource());
        return SingleResponse.buildSuccess(trdPlatformModelRefService.updateById(trdPlatformModelRefAddVo.updateEntity(id, trdPlatformModelRefAddVo, bladeAuth)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除三方平台模型映射")
    public SingleResponse<Boolean> remove(@PathVariable Long id) {
        LambdaQueryWrapper<TrdPlatformMeasureRefEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(TrdPlatformMeasureRefEntity::getModelRefId, id);
        List<TrdPlatformMeasureRefEntity> list = trdPlatformMeasureRefService.list(queryWrapper);
        if (CollectionUtil.isNotEmpty(list)) {
            return SingleResponse.buildFailure("10001", "当前模型映射下存在测点映射，请先删除测点映射!");
        }
        return SingleResponse.buildSuccess(trdPlatformModelRefService.removeById(id));
    }

    @GetMapping("/listByPlatformCode/{platformCode}")
    @Operation(summary = "根据平台code查询模型映射")
    public MultiResponse<TrdPlatformModelRefVo> listByPlatformCode(@PathVariable String platformCode) {
        LambdaQueryWrapper<TrdPlatformModelRefEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrdPlatformModelRefEntity::getPlatformCode, platformCode).eq(TrdPlatformModelRefEntity::getIsDelete, 0);
        List<TrdPlatformModelRefEntity> modelRefList = trdPlatformModelRefService.list(queryWrapper);
        if (CollectionUtils.isEmpty(modelRefList)) {
            return MultiResponse.buildSuccess();
        }
        List<TrdPlatformModelRefVo> modelRefVoList = BeanUtil.copyToList(modelRefList, TrdPlatformModelRefVo.class);
        modelRefVoList.forEach(vo -> {
            LambdaQueryWrapper<TrdPlatformMeasureRefEntity> measureRefQueryWrapper = new LambdaQueryWrapper<>();
            measureRefQueryWrapper.eq(TrdPlatformMeasureRefEntity::getModelRefId, vo.getId());
            List<TrdPlatformMeasureRefEntity> measureRefList = trdPlatformMeasureRefService.list(measureRefQueryWrapper);
            vo.setTrdPlatformMeasureRefList(BeanUtil.copyToList(measureRefList, TrdPlatformMeasureRefVo.class));
        });
        return MultiResponse.buildSuccess(modelRefVoList);
    }

    @GetMapping("/bind/{ennModelId}")
    @Operation(summary = "查看物模型是否被绑定")
    public SingleResponse<Boolean> isBindModel(@PathVariable String ennModelId) {
        LambdaQueryWrapper<TrdPlatformModelRefEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrdPlatformModelRefEntity::getEnnModelId, ennModelId).eq(TrdPlatformModelRefEntity::getIsDelete, 0);
        return SingleResponse.buildSuccess(!CollectionUtils.isEmpty(trdPlatformModelRefService.list(queryWrapper)));
    }

}
