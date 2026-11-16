package top.kdla.framework.infra.dal.mybatis.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import top.kdla.framework.infra.dal.mybatis.common.DoConstants;

import java.util.Map;

/**
 * 查询参数
 *
 * @author haoxin
 * @date 2021-02-02
 **/
public class IPageQuery<T> {

    /**
     * 每页条数上限，防止超大limit拖垮数据库
     */
    private static final long MAX_LIMIT = 500;

    public IPage<T> mapPage(Map<String, Object> params) {
        return this.mapPage(params, null, false);
    }

    public IPage<T> mapPage(Map<String, Object> params, String defaultOrderField, boolean isAsc) {
        //分页参数（容错解析：非String/非数字/负数一律回退默认值，limit加上限）
        long curPage = parsePagingValue(params.get(DoConstants.PAGE), 1);
        long limit = parsePagingValue(params.get(DoConstants.LIMIT), 10);
        if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }

        //分页对象
        Page<T> page = new Page<>(curPage, limit);

        //分页参数
        params.put(DoConstants.PAGE, page);

        //排序字段
        //防止SQL注入（因为sidx、order是通过拼接SQL实现排序的，会有SQL注入风险）
        String orderField = SqlFilter.sqlInject((String)params.get(DoConstants.ORDER_FIELD));
        String order = (String)params.get(DoConstants.ORDER);

        //前端字段排序
        if (StringUtils.isNotEmpty(orderField) && StringUtils.isNotEmpty(order)) {
            if (DoConstants.ASC.equalsIgnoreCase(order)) {
                return page.addOrder(OrderItem.asc(orderField));
            } else {
                return page.addOrder(OrderItem.desc(orderField));
            }
        }

        //没有排序字段，则不排序
        if (StringUtils.isBlank(defaultOrderField)) {
            return page;
        }

        //默认排序
        if (isAsc) {
            page.addOrder(OrderItem.asc(defaultOrderField));
        } else {
            page.addOrder(OrderItem.desc(defaultOrderField));
        }

        return page;
    }

    /**
     * 分页参数容错解析：值可能是String/Integer等任意类型，统一String.valueOf后try-parse，
     * 解析失败或为负数时回退默认值（此前(String)强转+parseLong无防护，类型不符抛ClassCastException、非数字抛NumberFormatException）
     *
     * @param value        原始参数值，可为null
     * @param defaultValue 回退默认值
     * @return 解析后的分页参数
     */
    private static long parsePagingValue(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            long parsed = Long.parseLong(String.valueOf(value).trim());
            return parsed < 0 ? defaultValue : parsed;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
