package top.kdla.framework.supplement.excel.imp;

import lombok.Data;
import top.kdla.framework.supplement.excel.BaseExcel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

@Data
public class ExcelImportError<T> extends BaseExcel {

    private boolean hasError = false;

    private Collection<T> errors;

    public void addError(T error){
        if(errors == null){
            errors = new ArrayList<>();
        }
        errors.add(error);
    }

    public boolean hasError(){
        if(errors == null){
            return false;
        }
        return !errors.isEmpty();
    }


    public void merge(ExcelImportError<T> error){
        if(error == null){
            return;
        }
        error.getErrors()
                .forEach(this::addError);
    }

}
