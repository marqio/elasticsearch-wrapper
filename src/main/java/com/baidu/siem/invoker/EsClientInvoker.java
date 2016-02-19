package com.baidu.siem.invoker;

import com.baidu.siem.exchange.Response;
import com.baidu.siem.threadpool.EsCientBulkTask;
import com.baidu.siem.throttle.EsThrottle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by yuxuefeng on 15/10/15.
 */
public class EsClientInvoker extends AbstractInvoker {
    protected static Logger logger = LoggerFactory.getLogger(EsJavaHttpWriteInvoker.class);
    private final EsThrottle esSearchThrottle;
    //    private final String targetNode;
    private final long timeout;
    private final String sync;

    private final HttpServletRequest request;
    private final Response response;
    private final Map<String, String> parameters;


    public EsClientInvoker(EsThrottle esSearchThrottle, HttpServletRequest request) {
        this(esSearchThrottle, 3000, "true", request);
    }

    public EsClientInvoker(EsThrottle esSearchThrottle, long timeout, String sync, HttpServletRequest request) {
        this(esSearchThrottle, timeout, sync, new Response(), new ConcurrentHashMap<String, String>(), request);
    }

    public EsClientInvoker(EsThrottle esSearchThrottle, long timeout, String sync, Response response, Map<String, String> parameters, HttpServletRequest request) {
        this.esSearchThrottle = esSearchThrottle;
        this.timeout = timeout;
        this.sync = sync;
        this.response = response;
        this.parameters = parameters;
//        this.targetNode = targetNode;
        this.request = request;
    }

    @Override
    protected RunnableFuture<String> doInvoke() {
        return new FutureTask<String>(new EsCientBulkTask(esSearchThrottle, request));
    }

    @Override
    public EsThrottle getEsSearchThrottle() {
        return esSearchThrottle;
    }

    @Override
    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    public Response getResponse() {
        return response;
    }

    @Override
    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String getSync() {
        return sync;
    }

    @Override
    protected ThreadPoolExecutor getExecutor() {
        //获得查询executor
        return esSearchThrottle.getWriteExecutor();
    }

    @Override
    public long getTimeout() {
        return timeout;
    }
}
