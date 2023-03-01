package top.kdla.framework.infra.dal.mybatis.common;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础PO
 *
 * @author haoxin
 * @date 2021-01-26
 **/
@Data
public class BaseDo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 删除标识
     */
    private Boolean isDeleted;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
