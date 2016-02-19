package com.baidu.siem.invoker;

import com.baidu.siem.config.CommonConfig;
import com.baidu.siem.exception.InvokeException;
import com.baidu.siem.exchange.Response;
import com.baidu.siem.filter.Filter;
import com.baidu.siem.loadbalance.LoadBalance;
import com.baidu.siem.model.Consist;
import com.baidu.siem.model.SearchModel;
import com.baidu.siem.throttle.EsThrottle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.RunnableFuture;

/**
 * Created by yuxuefeng on 15/9/17.
 */
@Service("invokerFactory")
public class InvokerFactory {
    private static Logger logger = LoggerFactory.getLogger(InvokerFactory.class);

    @Value("#{filtersMap}")
    private Map<String, Filter> filtersMap;

    @Value("#{LoadBalancesMap}")
    private Map<String, LoadBalance> loadBalancesMap;


    public Invoker generateEsHttpRootInvoker(EsThrottle esSearchThrottle, SearchModel searchModel) {
        HttpServletRequest request = searchModel.getRequest();
        String queryStr = request.getQueryString();

        //获得timeout参数
        long timeout = generateTimeout(queryStr);

        //获取请求参数
        String requestData = getRequestData(request);

        //负载均衡策略
        String targetNode = doLoadBalance(esSearchThrottle);

        String url = "http://" + targetNode.trim() + "/" + (queryStr == null ? "" : "?" + queryStr);

        return buildFilterChain(new EsJavaHttpWriteInvoker(esSearchThrottle, url, requestData, timeout, "true", targetNode, request));

    }

    //获得java http writeInvoker
    public Invoker generateEsHttpWriteInvoker(EsThrottle esSearchThrottle, SearchModel param) {

        HttpServletRequest request = param.getRequest();
        String queryStr = request.getQueryString();

        //获得timeout参数
        long timeout = generateTimeout(queryStr);
        //获得同步参数
        String sync = getString(queryStr, Consist.SYNC, "true");
        //获取请求参数
        String requestData = getRequestData(request);

        //负载均衡策略
        String targetNode = doLoadBalance(esSearchThrottle);

        String url = "http://" + targetNode.trim() + request.getRequestURI() + (queryStr == null ? "" : "?" + queryStr);

        return buildFilterChain(new EsJavaHttpWriteInvoker(esSearchThrottle, url, requestData, timeout, sync, targetNode, request));
    }

    //获得java http searchInvoker
    public Invoker generateEsHttpSearchInvoker(EsThrottle esSearchThrottle, SearchModel param) {

        HttpServletRequest request = param.getRequest();
        String queryStr = request.getQueryString();

        //获得timeout参数
        long timeout = generateTimeout(queryStr);
        //获得同步参数
        String sync = getString(queryStr, Consist.SYNC, "true");
        //获取请求参数
        String requestData = getRequestData(request);

        //负载均衡策略
        String targetNode = doLoadBalance(esSearchThrottle);

        String url = "http://" + targetNode.trim() + request.getRequestURI() + (queryStr == null ? "" : "?" + queryStr);

        return buildFilterChain(new EsJavaHttpSearchInvoker(esSearchThrottle, url, requestData, timeout, sync, targetNode, request));
    }

    //获得client invoker
    public Invoker generateClientInvoker(EsThrottle esSearchThrottle, SearchModel searchModel) {
        HttpServletRequest request = searchModel.getRequest();
        String queryStr = request.getQueryString();

//        Client client = ClientFactory.generateTransportClient(esSearchThrottle);

        //获得timeout参数
        long timeout = generateTimeout(queryStr);
        //获得同步参数
        String sync = getString(queryStr, Consist.SYNC, "true");
//        //获取请求参数
//        String requestData = getRequestData(request);

        //负载均衡策略
//        String targetNode = doLoadBalance(esSearchThrottle);


        return buildFilterChain(new EsClientInvoker(esSearchThrottle, timeout, sync, request));
    }

    //获得httpClient writeInvoker
    public Invoker generateEsHttpClientWriteInvoker(EsThrottle esSearchThrottle, SearchModel param) {

        HttpServletRequest request = param.getRequest();
        String queryStr = request.getQueryString();

        //获得timeout参数
        long timeout = generateTimeout(queryStr);
        //获得同步参数
        String sync = getString(queryStr, Consist.SYNC, "true");
        //获取请求参数
        String requestData = getRequestData(request);

        //负载均衡策略
        String targetNode = doLoadBalance(esSearchThrottle);

        String url = "http://" + targetNode.trim() + request.getRequestURI() + (queryStr == null ? "" : "?" + queryStr);

        return buildFilterChain(new EsHttpClientWriteInvoker(esSearchThrottle, url, requestData, timeout, sync, targetNode, request));

    }

    //获得httpClient searchInvoker
    public Invoker generateEsHttpClientSearchInvoker(EsThrottle esSearchThrottle, SearchModel param) {

        HttpServletRequest request = param.getRequest();
        String queryStr = request.getQueryString();

        //获得timeout参数
        long timeout = generateTimeout(queryStr);
        //获得同步参数
        String sync = getString(queryStr, Consist.SYNC, "true");
        //获取请求参数
        String requestData = getRequestData(request);

        //负载均衡策略
        String targetNode = doLoadBalance(esSearchThrottle);

        String url = "http://" + targetNode.trim() + request.getRequestURI() + (queryStr == null ? "" : "?" + queryStr);

        return buildFilterChain(new EsHttpClientSearchInvoker(esSearchThrottle, url, requestData, timeout, sync, targetNode, request));

    }

    private String getRequestData(HttpServletRequest request) {
        try {
            StringBuffer requestData = new StringBuffer();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(request.getInputStream(), request.getCharacterEncoding() == null ? Charset.defaultCharset().toString() : request.getCharacterEncoding()));
            String s;
            while ((s = bufferedReader.readLine()) != null) {
                requestData.append(s + "\n");
            }
            return requestData.toString();
        } catch (IOException e) {
            throw new InvokeException("Socket getInputStream error,please check and retry~");
        }
    }

    private long generateTimeout(String queryStr) {
        if (queryStr != null) {
            int index;
            if ((index = queryStr.indexOf("timeout")) != -1) {
                try {
                    String to = queryStr.substring(index + 8).substring(0, !queryStr.substring(index + 8).contains("&") ? queryStr.substring(index + 8).length() : queryStr.substring(index + 8).indexOf("&"));

                    return to.indexOf("ms") == -1 ? Long.parseLong(to) : Long.parseLong(to.substring(0, to.length() - 2));
                } catch (Exception e) {
                    logger.error("[timeout] param error,please recheck!" +
                            "default and only support:[ms] currently");
                    throw new InvokeException("[timeout] param error,please recheck!default and only support:[ms] currently", 511);
                }
            }
        }
        return CommonConfig.commonConfig.getPositiveLong(Consist.DEFAULT_TIMEOUT, CommonConfig.defaultTimeout);
    }

    private long getLong(String queryStr, String key, long defaultValue) {
        if (queryStr != null) {
            int index;
            if ((index = queryStr.indexOf(key)) != -1) {
                String to = queryStr.substring(index + key.length() + 1).substring(0, !queryStr.substring(index + +key.length() + 1).contains("&") ? queryStr.substring(index + +key.length() + 1).length() : queryStr.substring(index + +key.length() + 1).indexOf("&"));
                try {
                    return Long.parseLong(to);
                } catch (Exception e) {
                    logger.error(" [" + key + "] param type error,please recheck!");
                    throw new InvokeException(" [" + key + "] param type error,please recheck!", 511);
                }
            }
        }
        return defaultValue;
    }

    private String getString(String queryStr, String key, String defaultValue) {
        if (queryStr != null) {
            int index;
            if ((index = queryStr.indexOf(key)) != -1) {
                try {
                    return queryStr.substring(index + key.length() + 1).substring(0, !queryStr.substring(index + +key.length() + 1).contains("&") ? queryStr.substring(index + +key.length() + 1).length() : queryStr.substring(index + +key.length() + 1).indexOf("&"));
                } catch (Exception e) {
                    logger.error(" [" + key + "] param type error,please recheck!");
                    throw new InvokeException(" [" + key + "] param type error,please recheck!", 511);
                }
            }
        }
        return defaultValue;
    }

    private Invoker buildFilterChain(Invoker invoker) {
        Invoker last = invoker;
        String[] filters = invoker.getEsSearchThrottle().getFilterChain();
        if (filters.length > 0) {
            for (int i = filters.length - 1; i >= 0; i--) {
                final Filter filter = filtersMap.get(filters[i]);
                if (filter != null) {
                    final Invoker next = last;
                    last = new AbstractInvoker() {
                        @Override
                        public Response invoke() throws InvokeException {
                            return filter.invoke(next);
                        }

                        @Override
                        protected RunnableFuture<String> doInvoke() {
                            return null;
                        }

                    };
                    continue;
                }
                logger.error("Filter not exists:[" + filters[i] + "],please check!");
            }
        }

        return last;
    }

    private String doLoadBalance(EsThrottle esSearchThrottle) {

        LoadBalance loadBalance = loadBalancesMap.get(esSearchThrottle.getBalanceType().name());
        if (loadBalance != null) {
            return loadBalance.doSelect(esSearchThrottle);
        }

        if (esSearchThrottle.getBalanceType() == EsThrottle.BalanceType.NONE) {

            return esSearchThrottle.getHttpNodes()[0];
        }

        throw new InvokeException("LoadBalanceType:[" + esSearchThrottle.getBalanceType().name() + "] not exists,please check the map in [spring-servlet.xml]~~~\n");

    }

}
