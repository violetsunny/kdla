package top.kdla.framework.domain.shared;

import java.io.Serializable;
import java.util.List;

/**
 * 类描述
 *
 * @author haoxin
 * @date 2021-02-04
 **/
public class Page<T> implements ValueObject<Page<T>>, Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 总记录数
     */
    private long totalCount;
    /**
     * 每页记录数
     */
    private long pageSize;
    /**
     * 总页数
     */
    private long totalPage;
    /**
     * 当前页数
     */
    private long currPage;
    /**
     * 列表数据
     */
    private List<T> list;

    /**
     * 分页
     *
     * @param list       列表数据
     * @param totalCount 总记录数
     * @param pageSize   每页记录数
     * @param currPage   当前页数
     */
    public Page(List<T> list, long totalCount, long pageSize, long currPage) {
        this.list = list;
        this.totalCount = totalCount;
        this.pageSize = pageSize;
        this.currPage = currPage;
        this.totalPage = (int)Math.ceil((double)totalCount / pageSize);
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public long getCurrPage() {
        return currPage;
    }

    public void setCurrPage(int currPage) {
        this.currPage = currPage;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    @Override public boolean sameValueAs(Page other) {
        return false;
    }

    @Override public String toString() {
        return "Page {" + "totalCount=" + totalCount + ", pageSize=" + pageSize + ", totalPage=" + totalPage
            + ", currPage=" + currPage + ", list=" + list + '}';
    }
}
