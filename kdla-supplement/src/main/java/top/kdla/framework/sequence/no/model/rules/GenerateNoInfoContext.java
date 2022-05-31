package top.kdla.framework.sequence.no.model.rules;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.kdla.framework.sequence.no.model.entity.CodeGeneratorCfgV2;

import java.util.Date;

/**
 * 进行一次编号生成的时候的环境信息
 * @author kll
 * @date 2022/2/28
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenerateNoInfoContext {
    private String code;
    private Long epoch;
    private CodeGeneratorCfgV2 cfg;
    private Date date;
    private String dateFormatResult;
}
