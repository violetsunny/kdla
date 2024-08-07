package top.kdla.framework.supplement.excel.imp;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import top.kdla.framework.common.help.KdlaStringHelp;
import top.kdla.framework.common.help.MultiThreadInvokeHelp;
import top.kdla.framework.exception.BizException;
import top.kdla.framework.supplement.excel.exp.KdlaExcelWriteHelp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


@Slf4j
public class ExcelReadListener<T, R> extends AnalysisEventListener<T> {

//    public static final ConcurrentHashMap<String, SoftReference<Future<?>>> ERROR_REPORT_FUTURES = new ConcurrentHashMap<>();

    private static final int DEFAULT_MAX_ROW = 1000_0;

    private final List<R> validatedRecord = new ArrayList<>();

    private final List<String> errorMessageList = new ArrayList<>();

    private final ExcelImportResult importResult = new ExcelImportResult();

    private final ExcelImportError importError = new ExcelImportError<>();

    private int maxRow = DEFAULT_MAX_ROW;

    private Function<List<R>, ExcelImportError> recordFunction;

    private Consumer<ExcelImportResult> resultConsumer;

    private ExcelRowConverter<T> converter;

    private ExcelRowConverter<T> errorConverter;

    private int totalCount = 0;

    private int processCount = 500;

    private boolean needGenerateReport = false;

    private Executor executorService;

    private String reportFilePrefix = "excel-import-report";

    private String reportFileSuffix = ".xlsx";

    private Integer column;

    private MultipartFile multipartFile;

    public ExcelReadListener<T, R> generateReportFile(boolean flag) {
        this.needGenerateReport = flag;
        return this;
    }


    public ExcelReadListener<T, R> generateReportFile(boolean flag, Executor executorService) {
        this.needGenerateReport = flag;
        this.executorService = executorService;
        return this;
    }


    /**
     * 设置导入行数限制
     *
     * @param maxRow 最大行数
     * @return excelImportHelper
     */
    public ExcelReadListener<T, R> importRowLimit(int maxRow) {
        this.maxRow = maxRow;
        return this;
    }

    /**
     * 导入数据处理器
     *
     * @param function 导入数据函数
     * @return excelImportHelper
     */
    public ExcelReadListener<T, R> recordHandler(Function<List<R>, ExcelImportError> function) {
        this.recordFunction = function;
        return this;
    }


    /**
     * 导入结果处理器
     *
     * @param consumer 导入结果消费者
     * @return excelImportHelper
     */
    public ExcelReadListener<T, R> resultHandler(Consumer<ExcelImportResult> consumer) {
        this.resultConsumer = consumer;
        return this;
    }


    /**
     * 对象转换，将excel数据对象转换为entity对象
     *
     * @param converter 转换函数
     * @return entity
     */
    public ExcelReadListener<T, R> converter(ExcelRowConverter<T> converter) {
        this.converter = converter;
        return this;
    }

    public ExcelReadListener<T, R> errorConverter(ExcelRowConverter<T> errorConverter) {
        this.errorConverter = errorConverter;
        return this;
    }

    /**
     * 每次处理条数
     *
     * @param processCount
     * @return
     */
    public ExcelReadListener<T, R> processCount(int processCount) {
        this.processCount = processCount;
        return this;
    }

    /**
     * 生成错误文件
     *
     * @param prefix
     * @param suffix
     * @return
     */
    public ExcelReadListener<T, R> errorReportFileName(String prefix, String suffix) {
        this.reportFilePrefix = prefix;
        this.reportFileSuffix = suffix;
        return this;
    }

    private void doRead(InputStream inputStream, Class<T> clazz) {
        EasyExcel.read(inputStream, clazz, this)
                .doReadAll();
    }


    public void doRead(MultipartFile file, Class<T> clazz) {
        try {
            multipartFile = file;
            InputStream inputStream = new BufferedInputStream(file.getInputStream());
            doRead(inputStream, clazz);
        } catch (IOException e) {
            throw new BizException("读取Excel文件失败，" + e.getMessage());
        }

    }


    @Override
    public void invoke(T data, AnalysisContext context) {
        totalCount++;
        verifyMaxRow(totalCount);
        Integer currentIndex = context.readRowHolder().getRowIndex();
        if (validateRowData(data, currentIndex)) {
            validatedRecord.add(converter.convert(currentIndex, data));
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        column = context.readSheetHolder().getTempCellData().getColumnIndex();
        if (recordFunction == null) {
            log.error("导入失败，recordHandler未设置");
            return;
        }
        importResult.setTotalCount(totalCount);
        List<List<R>> validatedRecords = Lists.partition(validatedRecord, processCount);
        List<ExcelImportError> errors = Lists.newArrayList();
        if (executorService != null) {
            List<Supplier<ExcelImportError>> suppliers = Lists.newArrayList();
            validatedRecords.forEach(record -> {
                Supplier<ExcelImportError> supplier = () -> recordFunction.apply(record);
                suppliers.add(supplier);
            });
            try {
                errors = MultiThreadInvokeHelp.invokeGetS(suppliers, executorService);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            for (List<R> record : validatedRecords) {
                errors.add(recordFunction.apply(record));
            }
        }

        if (errors.stream().anyMatch(ExcelImportError::hasError) || importError.hasError()) {
            log.warn("导入数据有误");
            errors.forEach(importError::merge);//合并数据
            importResult.setErrorCount(importError.getErrors().size());
            importResult.setSuccess(false);
            importResult.setErrorMessage(errorMessageList);
            if (needGenerateReport) {
                File tempFile = generateErrorReportTempFile();
                writeErrorToReport(tempFile, importError.getErrors());
                importResult.setErrorReport(tempFile.getName());
            }
        } else {
            importResult.setSuccess(true);
        }

        if (resultConsumer != null) {
            resultConsumer.accept(importResult);
        }
    }


    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        Integer rowIndex = context.readRowHolder().getRowIndex();
        String message = exception.getMessage();
        if (errorConverter != null) {
            importError.addError(errorConverter.errorConvert(rowIndex, null, message));
        }
        String errorMsg = "导入异常，" + rowIndex + "行，" + message;
        errorMessageList.add(errorMsg);
        log.error(errorMsg);
    }


    private File generateErrorReportTempFile() {
        try {
            return File.createTempFile(reportFilePrefix, reportFileSuffix);
        } catch (IOException e) {
            throw new RuntimeException("生成导入报告异常，" + e.getMessage());
        }
    }


//    private void writeErrorToReport(File tempFile) {
//        //在原文件中取出报错行，增加一列报错信息，然后输出新的文件
//        try (InputStream inputStream = multipartFile.getInputStream();
//             Workbook workbook = new XSSFWorkbook(inputStream)) {
//
//            Sheet sheet = workbook.getSheetAt(0);
//            int col = column + 1;
//            Row row0 = sheet.getRow(0);
//            row0.createCell(col).setCellValue("错误信息");
//            //写入错误信息
//            importError.getErrors()
//                    .forEach((i, msg) -> {
//                        Row row = sheet.getRow(i) == null ? sheet.createRow(i) : sheet.getRow(i);
//                        Cell cell = row.getCell(col) == null ? row.createCell(col) : row.getCell(col);
//                        cell.setCellValue(msg);
//                    });
//            //TODO 是否需要删除没有报错行 sheet.getLastRowNum();
//            workbook.write(Files.newOutputStream(tempFile.toPath()));
//        } catch (Exception e) {
//            throw new RuntimeException("写入导入报告数据异常，" + e.getMessage());
//        }
//    }

    private void writeErrorToReport(File tempFile, Collection list) {
        KdlaExcelWriteHelp.writeFile(tempFile, list);
    }

    /**
     * 校验数据
     */
    private boolean validateRowData(T data, int rowIndex) {
        String validateMessage = ExcelImpValid.valid(data);
        if (KdlaStringHelp.isNotBlank(validateMessage)) {
            if (errorConverter != null) {
                importError.addError(errorConverter.errorConvert(rowIndex, data, validateMessage));
            }
            String errorMsg = "导入错误，" + rowIndex + "行，" + validateMessage;
            errorMessageList.add(errorMsg);
            log.warn(errorMsg);
            return false;
        }
        return true;
    }


    /**
     * 确认导入行数是否超限
     *
     * @param currentRowIndex 当前行数
     */
    private void verifyMaxRow(int currentRowIndex) {
        if (currentRowIndex > maxRow) {
            throw new RuntimeException("导入数据量超过限制 " + maxRow);
        }
    }
}
