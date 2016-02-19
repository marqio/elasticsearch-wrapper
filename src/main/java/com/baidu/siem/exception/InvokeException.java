package com.baidu.siem.exception;

/**
 * Created by yuxuefeng on 15/9/21.
 */
public class InvokeException extends RuntimeException {
    private int code;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public InvokeException(String msg) {
        super(msg);
    }

    public InvokeException(String message, int code) {
        super(message);
        this.code = code;
    }
}
