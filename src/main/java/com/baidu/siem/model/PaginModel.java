/**
 *
 */
package com.baidu.siem.model;

import java.io.Serializable;

/**
 * @author yuxuefeng
 */
@SuppressWarnings("serial")
public abstract class PaginModel implements Serializable {
    private int pageNo = 1;
    private int pageSize = 10;

    public PaginModel() {

    }

    public PaginModel(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
