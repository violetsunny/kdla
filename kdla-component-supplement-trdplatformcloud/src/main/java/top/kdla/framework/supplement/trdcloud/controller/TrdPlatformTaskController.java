package top.kdla.framework.supplement.trdcloud.controller;

import cn.hutool.core.bean.BeanUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.kdla.framework.dto.MultiResponse;
import top.kdla.framework.dto.PageResponse;
import top.kdla.framework.dto.SingleResponse;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformTaskBo;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformTaskPageQueryBo;
import top.kdla.framework.supplement.trdcloud.bo.TrdPlatformTaskQueryBo;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformInfoEntity;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformTaskEntity;
import top.kdla.framework.supplement.trdcloud.server.trd.TrdPlatformInfoService;
import top.kdla.framework.supplement.trdcloud.server.trd.TrdPlatformTaskService;
import top.kdla.framework.supplement.trdcloud.vo.TrdPlatformTaskAddVo;
import top.kdla.framework.supplement.trdcloud.vo.TrdPlatformTaskPageQueryVo;
import top.kdla.framework.supplement.trdcloud.vo.TrdPlatformTaskQueryVo;
import top.kdla.framework.supplement.trdcloud.vo.TrdPlatformTaskVo;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Validated
@Tag(name = "云平台任务管理")
@RequestMapping("/trd/platform/task")
public class TrdPlatformTaskController {

    @Resource
    TrdPlatformTaskService trdPlatformTaskService;

    @Resource
    TrdPlatformInfoService trdPlatformInfoService;

    @GetMapping("/detail/{id}")
    @Operation(summary = "任务详情")
    public SingleResponse<TrdPlatformTaskVo> detail(@PathVariable String id) {
        TrdPlatformTaskEntity entity = trdPlatformTaskService.getById(id);
        if (entity == null) {
            return SingleResponse.buildFailure("10000", "对象不存在");
        }
        TrdPlatformInfoEntity trdPlatformInfoEntity = trdPlatformInfoService.getByPCode(entity.getPlatformCode());
        TrdPlatformTaskVo trdPlatformTaskVo = BeanUtil.copyProperties(entity, TrdPlatformTaskVo.class);
        trdPlatformTaskVo.setPId(trdPlatformInfoEntity.getId());
        trdPlatformTaskVo.setPName(trdPlatformInfoEntity.getPlatformName());
        trdPlatformTaskVo.setPType(trdPlatformInfoEntity.getPlatformType());
        return SingleResponse.buildSuccess(trdPlatformTaskVo);
    }

    @GetMapping("/page")
    @Operation(summary = "任务分页")
    public PageResponse<TrdPlatformTaskVo> page(TrdPlatformTaskPageQueryVo trdPlatformTaskPageQueryVo) {
        TrdPlatformTaskPageQueryBo trdPlatformTaskPageQueryBo = BeanUtil.copyProperties(trdPlatformTaskPageQueryVo, TrdPlatformTaskPageQueryBo.class);
        trdPlatformTaskPageQueryBo.setPlatformCode(trdPlatformTaskPageQueryVo.getPlatformCode());
        PageResponse<TrdPlatformTaskBo> page = trdPlatformTaskService.queryPage(trdPlatformTaskPageQueryBo);
        return PageResponse.of(BeanUtil.copyToList(page.getData(), TrdPlatformTaskVo.class), page.getTotalCount(), page.getPageSize(), page.getPageNum());
    }

    @GetMapping("/list")
    @Operation(summary = "任务列表")
    public MultiResponse<TrdPlatformTaskVo> list(TrdPlatformTaskQueryVo trdPlatformTaskQueryVo) {
        TrdPlatformTaskQueryBo trdPlatformTaskQueryBo = BeanUtil.copyProperties(trdPlatformTaskQueryVo, TrdPlatformTaskQueryBo.class);
        trdPlatformTaskQueryBo.setPlatformCode(trdPlatformTaskQueryVo.getPlatformCode());
        MultiResponse<TrdPlatformTaskBo> list = trdPlatformTaskService.list(trdPlatformTaskQueryBo);
        List<TrdPlatformTaskVo> trdPlatformTaskVos = BeanUtil.copyToList(list.getData(), TrdPlatformTaskVo.class);
        if (!CollectionUtils.isEmpty(trdPlatformTaskVos)) {
            List<String> codes = trdPlatformTaskVos.stream().map(TrdPlatformTaskVo::getPCode).collect(Collectors.toList());
            Map<String, TrdPlatformInfoEntity> byPCodes = trdPlatformInfoService.getByPCodes(codes);
            trdPlatformTaskVos.forEach(trdPlatformTaskVo -> {
                TrdPlatformInfoEntity trdPlatformInfoEntity = byPCodes.getOrDefault(trdPlatformTaskVo.getPCode(), null);
                if (trdPlatformInfoEntity != null) {
                    trdPlatformTaskVo.setPId(trdPlatformInfoEntity.getId());
                    trdPlatformTaskVo.setPName(trdPlatformInfoEntity.getPlatformName());
                    trdPlatformTaskVo.setPType(trdPlatformInfoEntity.getPlatformType());
                }
            });
        }
        return MultiResponse.buildSuccess(trdPlatformTaskVos);
    }

    @PostMapping("/save")
    @Operation(summary = "新增任务")
    public SingleResponse<Boolean> save(@Valid @RequestBody TrdPlatformTaskAddVo trdPlatformTaskAddVo, @RequestHeader(value = "blade-auth", required = false) String bladeAuth) {
        if (trdPlatformTaskService.isExistName(trdPlatformTaskAddVo.getPlatformCode(), trdPlatformTaskAddVo.getTaskName())) {
            return SingleResponse.buildFailure("10001", "名称已存在，请换一个试试");
        }
        if (trdPlatformTaskService.isExistCode(trdPlatformTaskAddVo.getPlatformCode(), trdPlatformTaskAddVo.getTaskCode())) {
            return SingleResponse.buildFailure("10002", "Code已存在，请换一个试试");
        }
        return SingleResponse.buildSuccess(trdPlatformTaskService.saveTask(trdPlatformTaskAddVo.createEntity(trdPlatformTaskAddVo, bladeAuth)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "根据ID修改任务")
    public SingleResponse<Boolean> update(@PathVariable Long id, @Valid @RequestBody TrdPlatformTaskAddVo trdPlatformTaskAddVo, @RequestHeader(value = "blade-auth", required = false) String bladeAuth) {
        return SingleResponse.buildSuccess(trdPlatformTaskService.updateTask(trdPlatformTaskAddVo.updateEntity(id, trdPlatformTaskAddVo, bladeAuth)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务")
    public SingleResponse<Boolean> remove(@PathVariable Long id) {
        return SingleResponse.buildSuccess(trdPlatformTaskService.removeTask(id));
    }

}
