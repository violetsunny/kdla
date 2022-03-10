package top.kdla.framework.dto;

/**
 * Page Query Param
 *
 * @author vincent.li
 * @since 2021/7/9 14:15
 */
public abstract class PageQuery extends Query {

    public PageQuery() {
    }

    private static final long serialVersionUID = 1L;

    public static final String ASC = "ASC";

    public static final String DESC = "DESC";

    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 页大小，默认10
     */
    private int pageSize = DEFAULT_PAGE_SIZE;

    /**
     * 页码，默认1
     */
    private int pageNum = 1;

    private String orderBy;

    private String orderDirection = DESC;

    private String groupBy;

    private boolean needTotalCount = true;

    public int getPageNum() {
        if (pageNum < 1) {
            return 1;
        }
        return pageNum;
    }

    public PageQuery setPageNum(int pageNum) {
        this.pageNum = pageNum;
        return this;
    }

    public int getPageSize() {
        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        return pageSize;
    }

    public PageQuery setPageSize(int pageSize) {
        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        this.pageSize = pageSize;
        return this;
    }

    public int getOffset() {
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

    public String getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public boolean isNeedTotalCount() {
        return needTotalCount;
    }

    public void setNeedTotalCount(boolean needTotalCount) {
        this.needTotalCount = needTotalCount;
    }

}
