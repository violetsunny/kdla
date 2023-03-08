package top.kdla.framework.supplement.sequence.no.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.kdla.framework.supplement.sequence.no.model.entity.CodeGeneratorCfg;

@Mapper
public interface CodeGeneratorCfgMapper {

    CodeGeneratorCfg selectOne(CodeGeneratorCfg var1);

    int updateByIdAndRemark(CodeGeneratorCfg var1);

    void update(@Param("code") String code,@Param("remark") String remark);

}
