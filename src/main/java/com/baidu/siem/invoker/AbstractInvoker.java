package com.baidu.siem.invoker;

import com.baidu.siem.exception.InvokeException;
import com.baidu.siem.exchange.Response;
import com.baidu.siem.model.Consist;
import com.baidu.siem.monitor.Monitor;
import com.baidu.siem.throttle.EsThrottle;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by yuxuefeng on 15/11/18.
 */
public abstract class AbstractInvoker implements Invoker {
    protected static Logger logger = Logger.getLogger(AbstractInvoker.class);

    /**
     * 获得当前调用的目标节点,eg:hostname:port
     *
     * @return
     */
    public String getTargetNode() {
        return null;
    }

    /**
     * 获得当前调用的url
     *
     * @return
     */
    public String getUrl() {
        return null;
    }

    /**
     * 获得当前调用的requestData
     *
     * @return
     */
    public String getRequestData() {
        return null;
    }

    @Override
    public HttpServletRequest getRequest() {
        return null;
    }

    @Override
    public EsThrottle getEsSearchThrottle() {
        return null;
    }

    @Override
    public Response getResponse() {
        return null;
    }

    @Override
    public Map<String, String> getParameters() {
        return null;
    }

    @Override
    public String getParameter(String key) {
        return getParameters().get(key);
    }

    @Override
    public String getParameter(String key, String defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public String getResponseParameter(String key, String defaultValue) {
        String value = getResponse().getParams().get(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value;
    }

    protected Response initResponse(RunnableFuture future) throws InterruptedException, ExecutionException, TimeoutException {
        //同步
        if ("true".equalsIgnoreCase(getSync())) {
            String obj = future.get(getTimeout(), TimeUnit.MILLISECONDS).toString();
            int index1 = obj.indexOf(",");
            int index2 = obj.indexOf("|");
            int code = Integer.parseInt(obj.substring(0, index1));//http返回码
            String contentType = obj.substring(index1 + 1, index2);//http 返回contentType
            String msg = obj.substring(index2 + 1);//http返回消息体
            this.getResponse().setStatus(code);
            this.getResponse().setContentType(contentType);
            this.getResponse().setMsg(msg);
        } else {
            //异步
            this.getResponse().setStatus(200);
            this.getResponse().setContentType("application/json;charset=UTF-8");
            this.getResponse().setMsg("{\n\"result\":\"Task submmitted successfully,please check the es result in the future!\"\n}");
        }

        this.getResponse().getParams().put(Monitor.WRITE_THREAD_POOL_SIZE, getEsSearchThrottle().getWriteExecutor().getActiveCount() + "");
        this.getResponse().getParams().put(Monitor.WRITE_THREAD_POOL_QUEUE_SIZE, getEsSearchThrottle().getWriteExecutor().getQueue().size() + "");
        this.getResponse().getParams().put(Monitor.SEARCH_THREAD_POOL_SIZE, getEsSearchThrottle().getSearchExecutor().getActiveCount() + "");
        this.getResponse().getParams().put(Monitor.SEARCH_THREAD_POOL_QUEUE_SIZE, getEsSearchThrottle().getSearchExecutor().getQueue().size() + "");

        return this.getResponse();

    }

    @Override
    public Response invoke() throws InvokeException {
        ThreadPoolExecutor threadPoolExecutor = getExecutor();
        //包装future对象
        RunnableFuture<String> future = doInvoke();

        try {
            //提交任务
            threadPoolExecutor.execute(future);

            initResponse(future);

            return getResponse();
        } catch (RejectedExecutionException e) {
            logger.error("Fail to Submit Task,General reasons as below,\nThread pool queue full:[" + getEsSearchThrottle().getString(Consist.WRITE_BUFFER_SIZE, "500") + "],please check!");
            throw new InvokeException("thread pool queue full:[" + getEsSearchThrottle().getString(Consist.WRITE_BUFFER_SIZE, "500") + "],please check!", 429);
        } catch (TimeoutException e) {
            clearTaskFromQueue(threadPoolExecutor, future);
            logger.error("Sorry,request timeout:[" + getTimeout() + "ms],please have a retry!");
            throw new InvokeException("sorry,request timeout:[" + getTimeout() + "ms],please have a retry!", 502);
        } catch (InterruptedException e) {
            clearTaskFromQueue(threadPoolExecutor, future);
            logger.error("ES Http Task Interrupted:" + e.getMessage());
            throw new InvokeException("ES Http Task Interrupted:" + e.getMessage());
        } catch (ExecutionException e) {
            throw new InvokeException(e.getMessage());
        }

    }

    protected ThreadPoolExecutor getExecutor() {
        return null;
    }

    protected abstract RunnableFuture<String> doInvoke();

    public long getTimeout() {
        return -1;
    }

    //清除无效任务
    private void clearTaskFromQueue(ThreadPoolExecutor executor, RunnableFuture<String> future) {
        executor.remove(future);
        future.cancel(false);

    }

    public String getSync() {
        return null;
    }
}
