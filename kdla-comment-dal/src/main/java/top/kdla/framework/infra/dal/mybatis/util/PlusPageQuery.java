package top.kdla.framework.infra.dal.mybatis.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import top.kdla.framework.dto.PageQuery;
import top.kdla.framework.infra.dal.mybatis.common.CommonConstant;

import java.util.Map;
import java.util.Objects;

/**
 * Mybatis-Plus查询参数
 *
 * @author lichaoqiang
 * @date 2021-08-02
 **/
@NoArgsConstructor
@Data
public class PlusPageQuery<T> {

    /**
     * 页码
     */
    private long pageNum;
    /**
     * 页大小
     */
    private long pageSize;
    /**
     * 排序字段
     */
    private String orderBy;
    /**
     * 排序方向ASC,DESC
     */
    private String orderDirection;

    public PlusPageQuery(long pageNum, long pageSize, String orderBy, String orderDirection) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.orderBy = orderBy;
        this.orderDirection = orderDirection;
    }

    public PlusPageQuery(PageQuery pageQuery) {
        this.pageNum = pageQuery.getPageNum();
        this.pageSize = pageQuery.getPageSize();
        this.orderBy = pageQuery.getOrderBy();
        this.orderDirection = pageQuery.getOrderDirection();
    }

    public IPage<T> getPage() {
        return this.getPage(null, false);
    }

    public IPage<T> getPage(Map<String, Object> params) {
        return this.getPage(params, null, false);
    }


    public IPage<T> getPage(Map<String, Object> params, String defaultOrderByColumn, boolean isOrderByAsc) {
        //分页参数
        long curPage = 1;
        long limit = 10;

        if (Objects.nonNull(this.getPageNum())) {
            curPage = this.getPageNum();
        }
        if (Objects.nonNull(this.getPageSize())) {
            limit = this.getPageSize();
        }

        //分页对象
        Page<T> page = new Page<>(curPage, limit);

        //分页参数
        params.put(CommonConstant.PAGE, page);

        //排序字段
        //防止SQL注入（因为sidx、order是通过拼接SQL实现排序的，会有SQL注入风险）
        String orderByColumn = SqlFilter.sqlInject(this.getOrderBy());
        String order = this.getOrderDirection();

        //前端字段排序
        if (StringUtils.isNotEmpty(orderByColumn) && StringUtils.isNotEmpty(order)) {
            if (CommonConstant.ASC.equalsIgnoreCase(order)) {
                return page.addOrder(OrderItem.asc(orderByColumn));
            } else {
                return page.addOrder(OrderItem.desc(orderByColumn));
            }
        }

        //没有排序字段，则不排序
        if (StringUtils.isBlank(defaultOrderByColumn)) {
            return page;
        }

        //默认排序
        if (isOrderByAsc) {
            page.addOrder(OrderItem.asc(defaultOrderByColumn));
        } else {
            page.addOrder(OrderItem.desc(defaultOrderByColumn));
        }

        return page;
    }

    public IPage<T> getPage(String defaultOrderByColumn, boolean isOrderByAsc) {
        //分页参数
        long curPage = 1;
        long limit = 10;

        if (Objects.nonNull(this.getPageNum())) {
            curPage = this.getPageNum();
        }
        if (Objects.nonNull(this.getPageSize())) {
            limit = this.getPageSize();
        }

        //分页对象
        Page<T> page = new Page<>(curPage, limit);

        //排序字段
        //防止SQL注入（因为sidx、order是通过拼接SQL实现排序的，会有SQL注入风险）
        String orderByColumn = SqlFilter.sqlInject(this.getOrderBy());
        String order = this.getOrderDirection();

        //前端字段排序
        if (StringUtils.isNotEmpty(orderByColumn) && StringUtils.isNotEmpty(order)) {
            if (CommonConstant.ASC.equalsIgnoreCase(order)) {
                return page.addOrder(OrderItem.asc(orderByColumn));
            } else {
                return page.addOrder(OrderItem.desc(orderByColumn));
            }
        }

        //没有排序字段，则不排序
        if (StringUtils.isBlank(defaultOrderByColumn)) {
            return page;
        }

        //默认排序
        if (isOrderByAsc) {
            page.addOrder(OrderItem.asc(defaultOrderByColumn));
        } else {
            page.addOrder(OrderItem.desc(defaultOrderByColumn));
        }

        return page;
    }
}
