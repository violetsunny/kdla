package top.kdla.framework.supplement.excel.imp;

public interface ExcelRowConverter<T> {
    <R> R convert(int rowIndex, T data);

    <E> E errorConvert(int rowIndex, T data, String errorMessage);
}
