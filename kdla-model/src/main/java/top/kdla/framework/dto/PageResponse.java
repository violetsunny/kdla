package top.kdla.framework.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Response with batch page record to return,
 * usually use in page query
 * <p/>
 * @author vincent.li
 * @since 2021/7/9 14:15
 */
public class PageResponse<T> extends Response {

    private static final long serialVersionUID = 1L;

    private int totalCount = 0;

    private int pageSize = 1;

    private int pageNum = 1;

    private Collection<T> data;

    private int totalPages = 0;

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getPageSize() {
        if (pageSize < 1) {
            return 1;
        }
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        if (pageSize < 1) {
            this.pageSize = 1;
        } else {
            this.pageSize = pageSize;
        }
    }

    public int getPageNum() {
        if (pageNum < 1) {
            return 1;
        }
        return pageNum;
    }

    public void setPageNum(int pageNum) {
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

    public void setTotalPages(int totalPages) {
        if (totalPages < 1) {
            this.totalPages = 0;
        } else {
            this.totalPages = totalPages;
        }
    }

    public int getTotalPages() {
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
        response.setMessage(message);
        return response;
    }

    public static <T> PageResponse<T> of(int pageSize, int pageNum) {
        PageResponse<T> response = new PageResponse<>();
        response.setSuccess();
        response.setData(Collections.emptyList());
        response.setTotalCount(0);
        response.setPageSize(pageSize);
        response.setPageNum(pageNum);
        return response;
    }

    public static <T> PageResponse<T> of(Collection<T> data, int totalCount, int pageSize, int pageNum) {
        PageResponse<T> response =  buildSuccess(data);
        response.setTotalCount(totalCount);
        response.setPageSize(pageSize);
        response.setPageNum(pageNum);
        return response;
    }

}
