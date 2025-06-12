package top.kdla.framework.supplement.trdcloud.vo;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformTaskEntity;
import top.kdla.framework.supplement.trdcloud.utils.JwtUtil;
import top.kdla.framework.supplement.trdcloud.utils.RegexConstant;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;

@Data
@Schema(description = "三方云平台信息")
public class TrdPlatformTaskAddVo implements Serializable {

    @Schema(description = "任务code")
    @NotNull(message = "任务code不能为空")
    private String taskCode;

    @Schema(description = "平台code")
    @NotNull(message = "平台code不能为空")
    private String platformCode;

    @Schema(description = "任务名称")
    @NotNull(message = "任务名称不能为空")
    @Pattern(regexp = RegexConstant.NAME_PATTER, message = RegexConstant.NAME_ILLEGAL_MESSAGE)
    private String taskName;

    @Schema(description = "任务频率")
    private String frequency;

    @Schema(description = "ApiID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long apiId;

    @Schema(description = "产品Id")
    @NotNull(message = "产品Id不能为空")
    private String productId;

    @Schema(description = "备注")
    @Pattern(regexp = RegexConstant.CONTENT_PATTER, message = RegexConstant.CONTENT_ILLEGAL_MESSAGE)
    private String remark;

    public TrdPlatformTaskEntity createEntity(TrdPlatformTaskAddVo trdPlatformTaskAddVo, String bladeAuth) {
        TrdPlatformTaskEntity entity = BeanUtil.copyProperties(trdPlatformTaskAddVo, TrdPlatformTaskEntity.class);
        entity.setPlatformCode(trdPlatformTaskAddVo.getPlatformCode());
        Date current = new Date();
        entity.setCreateTime(current);
        entity.setUpdateTime(current);
        if (StringUtils.isNotEmpty(bladeAuth)) {
            String account = JwtUtil.getInfoFromToken(bladeAuth, "account");
            entity.setCreateUser(account);
            entity.setUpdateUser(account);
        }
        entity.setStatus(1);
        entity.setIsDelete(0);
        return entity;
    }

    public TrdPlatformTaskEntity updateEntity(Long id, TrdPlatformTaskAddVo trdPlatformTaskAddVo, String bladeAuth) {
        TrdPlatformTaskEntity entity = BeanUtil.copyProperties(trdPlatformTaskAddVo, TrdPlatformTaskEntity.class);
        entity.setId(id);
        entity.setPlatformCode(trdPlatformTaskAddVo.getPlatformCode());
        entity.setUpdateTime(new Date());
        if (StringUtils.isNotEmpty(bladeAuth)) {
            String account = JwtUtil.getInfoFromToken(bladeAuth, "account");
            entity.setUpdateUser(account);
        }
        return entity;
    }

}
