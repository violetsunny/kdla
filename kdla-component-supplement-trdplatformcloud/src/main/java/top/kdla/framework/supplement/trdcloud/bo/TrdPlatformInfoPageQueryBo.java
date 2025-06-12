package top.kdla.framework.supplement.trdcloud.bo;

import lombok.Data;
import top.kdla.framework.dto.PageQuery;

@Data
public class TrdPlatformInfoPageQueryBo extends PageQuery {

    /**
     * 平台类别
     */
    private Integer platformType;

    /**
     * 平台code
     */
    private String platformCode;

    /**
     * 平台名字
     */
    private String platformName;

    private String platformSource;
}
