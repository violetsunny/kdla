package top.kdla.framework.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Response with batch page record to return,
 * usually use in page query
 * <p/>
 * @author kll
 * @since 2021/7/9 14:15
 */
public class PageResponse<T> extends Response {

    private static final long serialVersionUID = 1L;

    private long totalCount = 0;

    private long pageSize = 1;

    private long pageNum = 1;

    private Collection<T> data;

    private long totalPages = 0;

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getPageSize() {
        if (pageSize < 1) {
            return 1;
        }
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        if (pageSize < 1) {
            this.pageSize = 1;
        } else {
            this.pageSize = pageSize;
        }
    }

    public long getPageNum() {
        if (pageNum < 1) {
            return 1;
        }
        return pageNum;
    }

    public void setPageNum(long pageNum) {
        if (pageNum < 1) {
            this.pageNum = 1;
        } else {
            this.pageNum = pageNum;
        }
    }

    public List<T> getData() {
        return null == data ? Collections.emptyList() : new ArrayList<>(data);
    }

    public void setData(Collection<T> data) {
        this.data = data;
    }

    public void setTotalPages(long totalPages) {
        if (totalPages < 1) {
            this.totalPages = 0;
        } else {
            this.totalPages = totalPages;
        }
    }

    public long getTotalPages() {
        return this.totalCount % this.pageSize == 0 ? this.totalCount
            / this.pageSize : (this.totalCount / this.pageSize) + 1;
    }

    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public static PageResponse buildSuccess() {
        PageResponse response = new PageResponse();
        response.setSuccess();
        return response;
    }

    public static <T> PageResponse<T> buildSuccess(Collection<T> data) {
        PageResponse<T> response = new PageResponse<>();
        response.setSuccess();
        response.setData(data);
        return response;
    }

    public static PageResponse buildFailure(String code, String message) {
        PageResponse response = new PageResponse();
        response.setSuccess(false);
        response.setCode(code);
        response.setMsg(message);
        return response;
    }

    public static <T> PageResponse<T> of(long pageSize, long pageNum) {
        PageResponse<T> response = new PageResponse<>();
        response.setSuccess();
        response.setData(Collections.emptyList());
        response.setTotalCount(0);
        response.setPageSize(pageSize);
        response.setPageNum(pageNum);
        return response;
    }

    public static <T> PageResponse<T> of(Collection<T> data, long totalCount, long pageSize, long pageNum) {
        PageResponse<T> response = buildSuccess(data);
        response.setTotalCount(totalCount);
        response.setPageSize(pageSize);
        response.setPageNum(pageNum);
        return response;
    }

}
