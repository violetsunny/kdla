package top.kdla.framework.supplement.sequence.no.model.entity;

import lombok.Data;

import java.util.Date;

/**
 * 数据库持久化层的entity
 */
@Data
public class CodeGeneratorCfgV2 {

    private Integer id;
    private String code;
    private String name;
    private Long epoch;
    private String maxValue;
    private String rule;
    private Integer isCache;
    private Integer cacheNum;
    private Integer isDeleted;
    private String remark;
    private Date updateTime;
    private Date createTime;

}
