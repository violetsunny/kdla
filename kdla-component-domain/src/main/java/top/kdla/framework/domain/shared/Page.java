package top.kdla.framework.domain.shared;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

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
        // pageSize<=0时totalPage置0（此前除0得Infinity再转int溢出为Integer.MAX_VALUE）；(long)替代(int)避免大值截断
        this.totalPage = pageSize <= 0 ? 0 : (long) Math.ceil((double) totalCount / pageSize);
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

    @Override
    public boolean sameValueAs(Page other) {
        // 值对象按属性值比较（此前恒返回false，违反ValueObject契约）
        if (other == null) {
            return false;
        }
        return totalCount == other.totalCount
                && pageSize == other.pageSize
                && totalPage == other.totalPage
                && currPage == other.currPage
                && Objects.equals(list, other.list);
    }

    @Override
    public String toString() {
        return "Page {" + "totalCount=" + totalCount + ", pageSize=" + pageSize + ", totalPage=" + totalPage
            + ", currPage=" + currPage + ", list=" + list + '}';
    }
}
