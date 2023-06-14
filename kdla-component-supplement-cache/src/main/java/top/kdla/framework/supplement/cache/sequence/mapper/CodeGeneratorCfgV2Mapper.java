package top.kdla.framework.supplement.cache.sequence.mapper;

import org.apache.ibatis.annotations.Mapper;
import top.kdla.framework.supplement.cache.sequence.model.entity.CodeGeneratorCfgV2;

@Mapper
public interface CodeGeneratorCfgV2Mapper {

    CodeGeneratorCfgV2 selectByCode(String code);

    void updateById(CodeGeneratorCfgV2 cfg);
}
