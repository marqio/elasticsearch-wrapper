package com.baidu.siem.invoker;

import com.baidu.siem.exception.InvokeException;
import com.baidu.siem.exchange.Response;
import com.baidu.siem.throttle.EsThrottle;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Created by yuxuefeng on 15/9/17.
 */
public interface Invoker {

    /**
     * 获得当前调用的httpRequest
     *
     * @return
     */
    public HttpServletRequest getRequest();

    /**
     * 获得当前调用的目标节点,eg:hostname:port
     *
     * @return
     */
    public String getTargetNode();

    /**
     * 获得当前调用的url
     *
     * @return
     */
    public String getUrl();

    /**
     * 获得当前调用的requestData
     *
     * @return
     */
    public String getRequestData();

    /**
     * 获得当前调用的阀门对象
     *
     * @return
     */
    public EsThrottle getEsSearchThrottle();


    /**
     * 获得invoke response
     *
     * @return
     */
    public Response getResponse();


    /**
     * 获得参数map
     *
     * @return
     */
    public Map<String, String> getParameters();


    /**
     * 根据key返回invoker value
     *
     * @param key
     * @return
     */
    public String getParameter(String key);


    /**
     * 根据key返回invoker value，如果为null或者length==0 则返回defaultValue
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public String getParameter(String key, String defaultValue);


    /**
     * 根据key返回Response value，如果为null或者length==0 则返回defaultValue
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public String getResponseParameter(String key, String defaultValue);

    /**
     * 执行调用
     *
     * @return invoke Response
     */
    Response invoke() throws InvokeException;
}
