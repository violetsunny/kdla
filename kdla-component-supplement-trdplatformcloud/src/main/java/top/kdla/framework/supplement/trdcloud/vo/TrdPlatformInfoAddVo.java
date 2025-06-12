package top.kdla.framework.supplement.trdcloud.vo;

import cn.hutool.core.bean.BeanUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformInfoEntity;
import top.kdla.framework.supplement.trdcloud.enums.ModelSourceEnum;
import top.kdla.framework.supplement.trdcloud.utils.JwtUtil;
import top.kdla.framework.supplement.trdcloud.utils.RegexConstant;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;

@Data
@Schema(description = "三方云平台信息")
public class TrdPlatformInfoAddVo implements Serializable {

    /**
     * 平台类别
     */
    @Schema(description = "平台类别")
    @NotNull
    private Integer platformType;

    /**
     * 平台code
     */
    @Schema(description = "平台code")
    @NotNull
    private String platformCode;

    /**
     * 平台名字
     */
    @Schema(description = "平台名字")
    @NotNull
    @Pattern(regexp = RegexConstant.NAME_PATTER, message = RegexConstant.NAME_ILLEGAL_MESSAGE)
    private String platformName;

    @Schema(description = "来源")
//    @NotNull
//    @EnumValid(enumClass = ModelSourceEnum.class,message = "来源[platformSource]不在枚举内",checkMethod = "checkCode")
    private String platformSource;

    /**
     * 配置参数json(appkey、域名等)
     */
    @Schema(description = "json化配置，根据platformType：" +
            "1 -- {\"baseUrl\":\"\",\"appKey\":\"\",\"appSecret\":\"\"} " +
            "2 -- {\"baseUrl\":\"\"}" +
            "3 -- {\"host\":\"\",\"port\":0,\"salveAddress\":0}")
    private String configJson;

    /**
     * 协议Id
     */
    @Schema(description = "协议Id")
    private String protocolId;

    /**
     * 备注
     */
    @Schema(description = "备注")
    @Pattern(regexp = RegexConstant.CONTENT_PATTER, message = RegexConstant.CONTENT_ILLEGAL_MESSAGE)
    private String remark;

    public TrdPlatformInfoEntity createEntity(TrdPlatformInfoAddVo trdPlatformInfoAddVo, String bladeAuth) {
        TrdPlatformInfoEntity entity = BeanUtil.copyProperties(trdPlatformInfoAddVo, TrdPlatformInfoEntity.class);
        Date current = new Date();
        entity.setPlatformType(trdPlatformInfoAddVo.getPlatformType());
        entity.setPlatformCode(trdPlatformInfoAddVo.getPlatformCode());
        entity.setPlatformName(trdPlatformInfoAddVo.getPlatformName());
        entity.setPlatformSource(StringUtils.isNotBlank(trdPlatformInfoAddVo.getPlatformSource())?trdPlatformInfoAddVo.getPlatformSource():ModelSourceEnum.CUSTOM.getCode());
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

    public TrdPlatformInfoEntity updateEntity(Long id, TrdPlatformInfoAddVo trdPlatformInfoAddVo, String bladeAuth) {
        TrdPlatformInfoEntity entity = BeanUtil.copyProperties(trdPlatformInfoAddVo, TrdPlatformInfoEntity.class);
        entity.setId(id);
        entity.setPlatformType(trdPlatformInfoAddVo.getPlatformType());
        entity.setPlatformCode(trdPlatformInfoAddVo.getPlatformCode());
        entity.setPlatformName(trdPlatformInfoAddVo.getPlatformName());
        entity.setPlatformSource(StringUtils.isNotBlank(trdPlatformInfoAddVo.getPlatformSource())?trdPlatformInfoAddVo.getPlatformSource(): ModelSourceEnum.CUSTOM.getCode());
        Date current = new Date();
        entity.setUpdateTime(current);
        if (StringUtils.isNotEmpty(bladeAuth)) {
            String account = JwtUtil.getInfoFromToken(bladeAuth, "account");
            entity.setUpdateUser(account);
        }
        return entity;
    }

}
