package top.kdla.framework.infra.dal.mybatis.common;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base DO for MyBatis-Plus projects.
 *
 * <p>Use {@link BaseDo} when you don't use MyBatis-Plus logic delete.</p>
 */
@Data
public class MpBaseDo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Logic delete flag. 0 = not deleted, 1 = deleted.
     */
    @TableLogic
    private Integer deleted;

    private String remark;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;
}
