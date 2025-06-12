package top.kdla.framework.supplement.trdcloud.bo;

import lombok.Data;
import top.kdla.framework.dto.PageQuery;

@Data
public class TrdPlatformTaskPageQueryBo extends PageQuery {

    /**
     * 平台code
     */
    private String platformCode;

    /**
     * 任务名称
     */
    private String taskName;

}
