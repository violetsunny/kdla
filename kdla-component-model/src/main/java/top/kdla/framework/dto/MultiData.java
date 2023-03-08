package top.kdla.framework.dto;

import java.util.Collection;

/**
 * @author kll
 * @Description 批量数据DTO
 * @since 2021/7/16
 */
public class MultiData<T> extends BaseModel {

    /**
     * 总数量
     */
    private int totalCount;
    /**
     * 数据集合
     */
    private Collection<T> items;

    public MultiData() {
    }

    public MultiData(Collection<T> items) {
        this.items = items;
    }

    public MultiData(int totalCount, Collection<T> items) {
        this.totalCount = totalCount;
        this.items = items;
    }

    public int getTotalCount() {
        return this.totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public Collection<T> getItems() {
        return this.items;
    }

    public void setItems(Collection<T> items) {
        this.items = items;
    }
}
