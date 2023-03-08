package top.kdla.framework.dto;

/**
 * Page Query Param
 *
 * @author kll
 * @since 2021/7/9 14:15
 */
public abstract class PageQuery extends Query {

    public PageQuery() {
    }

    private static final long serialVersionUID = 1L;

    public static final String ASC = "ASC";

    public static final String DESC = "DESC";

    private static final long DEFAULT_PAGE_SIZE = 10L;

    /**
     * 页大小，默认10
     */
    private long pageSize = DEFAULT_PAGE_SIZE;

    /**
     * 页码，默认1
     */
    private long pageNum = 1L;

    private String orderBy;

    private String orderDirection = DESC;

    public long getPageNum() {
        if (pageNum < 1) {
            return 1;
        }
        return pageNum;
    }

    public PageQuery setPageNum(long pageNum) {
        this.pageNum = pageNum;
        return this;
    }

    public long getPageSize() {
        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        return pageSize;
    }

    public PageQuery setPageSize(long pageSize) {
        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        this.pageSize = pageSize;
        return this;
    }

    public long offSet() {
        return (getPageNum() - 1) * getPageSize();
    }

    public String getOrderBy() {
        return orderBy;
    }

    public PageQuery setOrderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public String getOrderDirection() {
        return orderDirection;
    }

    public PageQuery setOrderDirection(String orderDirection) {
        if (ASC.equalsIgnoreCase(orderDirection) || DESC.equalsIgnoreCase(orderDirection)) {
            this.orderDirection = orderDirection;
        }
        return this;
    }
}
