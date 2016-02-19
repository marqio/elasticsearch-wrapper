package com.baidu.siem.http.manager;

import com.baidu.siem.config.CommonConfig;
import com.baidu.siem.exception.InvokeException;
import com.baidu.siem.exchange.Response;
import com.baidu.siem.invoker.Invoker;
import com.baidu.siem.invoker.InvokerFactory;
import com.baidu.siem.model.Consist;
import com.baidu.siem.model.SearchModel;
import com.baidu.siem.throttle.EsThrottle;
import com.baidu.siem.throttle.EsThrottleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by yuxuefeng on 15/10/14.
 */
@Service("ESBulkManager")
public class ESBatchManagerImpl extends BaseManager implements ESBatchManager {
    protected static Logger logger = LoggerFactory.getLogger(ESBatchManagerImpl.class);
    @Autowired
    private InvokerFactory invokerFactory;

    public ESBatchManagerImpl() {
    }

    @Override
    public Response search(SearchModel searchModel) {
        String clusterName = CommonConfig.commonConfig.getString(Consist.DEFAULT_CLUSTER_NAME, CommonConfig.defaultClusterName);
        String queryStr = searchModel.getRequest().getQueryString();
        clusterName = getString(queryStr, Consist.CLUSTER_NAME, clusterName);
        try {
            EsThrottle esSearchThrottle = EsThrottleFactory.getEsSearchThrottleByClusteName(clusterName);
            Invoker invoker = invokerFactory.generateEsHttpClientSearchInvoker(esSearchThrottle, searchModel);
            //执行调用
            Response response = invoker.invoke();

            return response;

        } catch (InvokeException e) {
            return new Response(e.getCode(), buildJsonStr(new Response(e.getCode(), e.getMessage())));
        }
    }

    @Override
    public Response write(SearchModel searchModel) {
        String clusterName = CommonConfig.commonConfig.getString(Consist.DEFAULT_CLUSTER_NAME, CommonConfig.defaultClusterName);
        String queryStr = searchModel.getRequest().getQueryString();
        clusterName = getString(queryStr, Consist.CLUSTER_NAME, clusterName);
        try {
            EsThrottle esSearchThrottle = EsThrottleFactory.getEsSearchThrottleByClusteName(clusterName);
            Invoker invoker = invokerFactory.generateEsHttpClientWriteInvoker(esSearchThrottle, searchModel);
            //执行调用
            Response response = invoker.invoke();

            return response;

        } catch (InvokeException e) {
            return new Response(e.getCode(), buildJsonStr(new Response(e.getCode(), e.getMessage())));
        }
    }

}
