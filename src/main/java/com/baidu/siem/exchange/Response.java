package com.baidu.siem.exchange;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yuxuefeng on 15/9/16.
 */
public class Response {
    private int status;
    private transient String contentType = "application/json;charset=UTF-8";
    private String msg;
    private transient Map<String, String> params = new HashMap<>();

    public Response() {
    }

    public Response(String msg) {
        this.msg = msg;
    }

    public Response(int code, String msg) {
        this.status = code;
        this.msg = msg;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
