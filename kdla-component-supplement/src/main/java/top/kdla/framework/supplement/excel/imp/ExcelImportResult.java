package top.kdla.framework.supplement.excel.imp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.kdla.framework.supplement.excel.BaseExcel;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExcelImportResult extends BaseExcel {

    private boolean success;

    private Integer totalCount;

    private Integer errorCount;

    private String errorReport;

    private List<String> errorMessage;

}
