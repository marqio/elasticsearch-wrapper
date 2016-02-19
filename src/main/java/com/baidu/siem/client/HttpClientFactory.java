package com.baidu.siem.client;

import org.apache.http.HttpVersion;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.log4j.Logger;

/**
 * 线程安全的 httpClient 工厂类
 * 适合多线程的HttpClient,用httpClient4.2.1实现
 * Created by yuxuefeng on 15/11/4.
 */
public class HttpClientFactory {
    private static Logger logger = Logger.getLogger(HttpClientFactory.class);
    private static DefaultHttpClient httpClient;

    public static DefaultHttpClient getHttpClient() {
        if (httpClient != null) {
            return httpClient;
        }
        return createHttpClient();
    }

    /**
     * @return DefaultHttpClient
     */
    public synchronized static DefaultHttpClient createHttpClient() {

        if (httpClient != null) {
            return httpClient;
        }

        // 设置组件参数, HTTP协议的版本,1.1/1.0/0.9
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setUserAgent(params, "Apache HttpClient/1.1");
        HttpProtocolParams.setUseExpectContinue(params, true);

        //设置连接超时时间
        int REQUEST_TIMEOUT = 3 * 1000;  //设置建立连接超时3秒钟
        int SO_TIMEOUT = 60 * 1000;       //设置等待数据超时时间60秒钟,两次数据包间隔时间
        long CONN_MANAGER_TIMEOUT = 1000;   //定义了当从ClientConnectionManager中检索ManagedClientConnection实例时使用的毫秒级的超时时间
        params.setLongParameter(ClientPNames.CONN_MANAGER_TIMEOUT, CONN_MANAGER_TIMEOUT);
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, REQUEST_TIMEOUT);
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, SO_TIMEOUT);
//        HttpConnectionParams.setConnectionTimeout(params, REQUEST_TIMEOUT);
//        HttpConnectionParams.setSoTimeout(params, SO_TIMEOUT);

        //设置访问协议
        SchemeRegistry schreg = new SchemeRegistry();
        schreg.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        schreg.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));

        //多连接的线程安全的管理器
        PoolingClientConnectionManager pccm = new PoolingClientConnectionManager(schreg);
        pccm.setDefaultMaxPerRoute(100); //每个主机host的最大并行连接数
        pccm.setMaxTotal(1000);          //httpClient客户端总并行连接最大数

        DefaultHttpClient httpClient = new DefaultHttpClient(pccm, params);
        //请求重试处理Handler
//        HttpRequestRetryHandler httpRequestRetryHandler = new DefaultHttpRequestRetryHandler() {
//            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
//                super.retryRequest(exception, executionCount, context);
//                if (executionCount >= 3) {
//                    // 如果超过最大重试次数，那么就不要继续了
//                    return false;
//                }
//                if (exception instanceof NoHttpResponseException) {
//                    // 如果服务器丢掉了连接，那么就重试
//                    return true;
//                }
//                if (exception instanceof SSLHandshakeException) {
//                    // 不要重试SSL握手异常
//                    return false;
//                }
//                return false;
//            }
//        };
//      httpClient.setHttpRequestRetryHandler(httpRequestRetryHandler);
        httpClient.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
        //预防客户端异常，导致服务连接冗余，一般可不配
//        httpClient.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy() {
//            @Override
//            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
//                long keepAlive = super.getKeepAliveDuration(response, context);
//                if (keepAlive == -1) {
//                    // 如果keep-alive值没有由服务器明确设置，那么保持连接持续10分钟。
//                    keepAlive = 10 * 60 * 1000;
//                }
//                return keepAlive;
//            }
//        });

        HttpClientFactory.httpClient = httpClient;

        return httpClient;
    }
}
