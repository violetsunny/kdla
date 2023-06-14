package top.kdla.framework.supplement.cache.sequence.model.entity;

import lombok.Data;

import java.util.Date;

/**
 * 数据库持久化层的entity
 */
@Data
public class CodeGeneratorCfg {

    private Integer id;
    private String code;
    private String name;
    private String maxValue;
    private String rule;
    private Integer isCache;
    private Integer cacheNum;
    private Integer isDeleted;
    private String remark;
    private Date updateTime;
    private Date createTime;

}
