package com.baidu.siem.http.manager;

import com.baidu.siem.exception.InvokeException;
import com.baidu.siem.exchange.Response;
import com.google.gson.Gson;
import org.apache.log4j.Logger;

/**
 * Created by yuxuefeng on 15/11/30.
 */
public abstract class BaseManager {
    protected static Logger logger = Logger.getLogger(BaseManager.class);

    protected String getString(String queryStr, String key, String defaultValue) {
        if (queryStr != null) {
            int index = -1;
            if ((index = queryStr.indexOf(key)) != -1) {
                try {
                    String to = queryStr.substring(index + key.length() + 1).substring(0, queryStr.substring(index + +key.length() + 1).indexOf("&") == -1 ? queryStr.substring(index + +key.length() + 1).length() : queryStr.substring(index + +key.length() + 1).indexOf("&"));
                    return to;
                } catch (Exception e) {
                    logger.error(" [" + key + "] param type error,please recheck!");
                    throw new InvokeException(" [" + key + "] param type error,please recheck!", 511);
                }
            }
        }
        return defaultValue;
    }

    protected String buildJsonStr(Response response) {
        Gson gson = new Gson();
        String json = gson.toJson(response);
        return json;
    }
}
