package com.baidu.siem.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jason
 *         <p/>
 *         2011-12-2 上午11:41:03
 */
@SuppressWarnings("serial")
public class Pagination implements Serializable {
    private int currentPage;
    private int pageSize = 20;
    private long pageCount;
    private long totalCount;
    private List<?> result;

    public Pagination() {
    }

    public Pagination(int curretPage, int pageSize) {
        this.currentPage = curretPage;
        this.pageSize = pageSize;
    }

    public Pagination(int currentPage, int pageSize, long totalCount) {
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
    }

    public Pagination(List<?> result) {
        this.result = result;
    }

    public static long getPageCount(long totalCount, int pageSize) {
        if (totalCount < 0 || pageSize <= 0) return 0;
        if (totalCount % pageSize == 0) return totalCount / pageSize;
        else return totalCount / pageSize + 1;
    }

    public static Pagination emptyInstance() {
        Pagination pg = new Pagination();
        pg.setResult(new ArrayList<Object>());
        return pg;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public long getPageCount() {
        return pageCount;
    }

    public void setPageCount(long pageCount) {
        this.pageCount = pageCount;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<?> getResult() {
        return result;
    }

    public void setResult(List<?> result) {
        this.result = result;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

//	@SuppressWarnings("unchecked")
//	public <F, T> Pagination convertResult(ResultConverter<F, T> converter)
//	{
//		List<T> rs = new ArrayList<T>();
//		for (Object obj : result)
//		{
//			rs.add(converter.convert((F) obj));
//		}
//		result = rs;
//		return this;
//	}

    @SuppressWarnings("unchecked")
    public <T> List<T> getResult(Class<T> clazz) {
        return (List<T>) result;
    }

    @Override
    public String toString() {
        return "Pagination [currentPage=" + currentPage + ", pageCount=" + pageCount + ", pageSize=" + pageSize
                + ", result.size()=" + result.size() + ", totalCount=" + totalCount + "]";
    }
}
