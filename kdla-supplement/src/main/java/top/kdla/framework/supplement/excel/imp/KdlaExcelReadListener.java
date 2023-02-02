/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.kdla.framework.supplement.excel.imp;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.kdla.framework.supplement.excel.BaseExcel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * excel解析
 *
 * @author kanglele
 * @version $Id: KdlaExcelPare, v 0.1 2022/5/11 18:19 kanglele Exp $
 */
@Slf4j
@Getter
public abstract class KdlaExcelReadListener<T extends BaseExcel, E extends BaseExcel> extends AnalysisEventListener<T> {

    /**
     * 整体Excel的解析、格式校验是否成功
     */
    private boolean success = true;
    /**
     * 解析后的数据
     */
    private final List<T> dataList = new ArrayList<>();
    /**
     * 校验后的结果数据
     */
    private final List<E> resList = new ArrayList<>();
    /**
     * 业务
     */
    private Predicate<List<T>> predicate;
    /**
     * 返回
     */
    private Predicate<List<E>> predicateRes;

    public KdlaExcelReadListener(Predicate<List<T>> predicate) {
        super();
        this.predicate = predicate;
    }

    public KdlaExcelReadListener(Predicate<List<T>> predicate, Predicate<List<E>> predicateRes) {
        super();
        this.predicate = predicate;
        this.predicateRes = predicateRes;
    }

    @Override
    public void invoke(T t, AnalysisContext analysisContext) {
        if (t != null) {
            dataList.add(t);
            E e = checkData(t);
            if (e != null) {
                resList.add(e);
            }
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        if (dataList.size() > 0) {
            success = predicate.test(dataList);
            dataList.clear();
        }
        if (resList.size() > 0 && predicateRes != null) {
            predicateRes.test(resList);
            resList.clear();
        }
    }

    public abstract E checkData(T t);

}
