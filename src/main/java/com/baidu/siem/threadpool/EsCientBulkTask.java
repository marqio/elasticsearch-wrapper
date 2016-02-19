package com.baidu.siem.threadpool;

import com.baidu.siem.client.ClientFactory;
import com.baidu.siem.exception.InvokeException;
import com.baidu.siem.model.Consist;
import com.baidu.siem.throttle.EsThrottle;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * ES client task
 * 默认 3次重试机会
 * Created by yuxuefeng on 15/10/15.
 */
public class EsCientBulkTask implements Callable<String> {
    private static Logger logger = LoggerFactory.getLogger(EsCientBulkTask.class);
    String defaultIndex = null;
    String defaultType = Consist.DEFAULT_TYPE;
    private HttpServletRequest request;
    private EsThrottle esThrottle;

    public EsCientBulkTask(EsThrottle esThrottle, HttpServletRequest request) {
        this.esThrottle = esThrottle;
        this.request = request;
    }

    @Override
    public String call() throws Exception {
//        Thread.sleep(10000);
        String res = "";
        TransportClient client = (TransportClient) ClientFactory.generateTransportClient(esThrottle);

        try {
            BulkRequestBuilder bulkRequest = generateBulkRequest(client);

            BulkResponse bulkResponse = bulkRequest.execute().actionGet();

            if (!bulkResponse.hasFailures()) {
//                bulkResponse.getItems()[0].
                //TODO 此处省去结果的构建，优化网络传输
                return "Es Bulk operations success ! BatchSize:[" + bulkRequest.numberOfActions() + "]\n";
            }
            long failureCount = 0;
            for (BulkItemResponse bulkItemResponse : bulkResponse.getItems()) {
                if (bulkItemResponse.isFailed()) {
                    failureCount++;
                }
            }

            return "Es Bulk failure,Count:[" + failureCount + "],Exception msg:" + bulkResponse.buildFailureMessage() + "\n";

        } catch (ActionRequestValidationException e) {
            logger.error("Fail to send Java_Client [bulk] request, because no requestActions added,\nException message:" + e.getMessage() + "\n");
            res = e.getMessage() + "\n";
        } catch (Exception e) {
            logger.error("Fail to send Java_Client [bulk] request\nException message:" + e.getMessage() + "\n");
            res = e.getMessage() + "\n";
        }

        throw new InvokeException("Send ES [Bulk] Request Error,Original Exception message:\n" + res);

    }

//    @Override
//    public String call() throws Exception {
//        Thread.sleep(5000);
//        return "sucess";
//    }

    private BulkRequestBuilder generateBulkRequest(TransportClient client) throws IOException, JSONException {

        String requestURI = request.getRequestURI();
        int count = 0;
        int offset = 0;
        String des = "/";
        while ((offset = requestURI.indexOf(des, offset)) != -1) {
            offset = offset + des.length();
            count++;
        }

        if (count == 1) {  //eg:/_bulk

            return readLineMsg(client);

        } else if (count == 2) {//eg: /index/_bulk
            defaultIndex = requestURI.substring(1, requestURI.lastIndexOf(des));

            return readLineMsg(client);

        } else if (count == 3) {//eg: /index/type/_bulk
            String indexType = requestURI.substring(1, requestURI.lastIndexOf(des));
            String[] indexTypes = indexType.split(des);
            defaultIndex = indexTypes[0];
            defaultType = indexTypes[1];

            return readLineMsg(client);

        } else {
            throw new InvokeException("Unkown error!");
        }
    }

    private BulkRequestBuilder readLineMsg(TransportClient client) throws IOException, JSONException {
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(request.getInputStream()));

        String action = bufferedReader.readLine();
        String msg = null;
        //兼容linux curl命令行第一行空白的情况
        if ("".equals(action.trim())) {
            while ((action = bufferedReader.readLine()) != null && (msg = bufferedReader.readLine()) != null) {
                logger.info("1-current bulk request, action:" + action + ",msg:" + msg);
                addAction(client, bulkRequest, action, msg);
            }

            return bulkRequest;
        }
        //
        if ((msg = bufferedReader.readLine()) != null) {
            do {
                logger.info("2-current bulk request, action:" + action + ",msg:" + msg);
                addAction(client, bulkRequest, action, msg);
            }
            while ((action = bufferedReader.readLine()) != null && (msg = bufferedReader.readLine()) != null);
        }
        return bulkRequest;
    }

    private void addAction(TransportClient client, BulkRequestBuilder bulkRequest, String action, String msg) throws JSONException {
        JSONObject jsonObject = new JSONObject(action);
        String key = jsonObject.keys().next().toString();
        JSONObject value = jsonObject.getJSONObject(key);
        Map<String, String> info = new HashMap();

        Iterator iterator = value.keys();
        while (iterator.hasNext()) {
            String k = (String) iterator.next();
            String v = illegalValue(value.getString(k));
            info.put(k, v);
        }

        //判断bulk请求串是否合法
        if (defaultIndex == null && info.get(Consist._INDEX) == null) {
            throw new JSONException("Json String error,[index] missing");
        }

        //index
        if (Consist.ES_INDEX.equals(key)) {

            bulkRequest.add(client.prepareIndex(info.get(Consist._INDEX) != null ? info.get(Consist._INDEX) : defaultIndex,
                    info.get(Consist._TYPE) != null ? info.get(Consist._TYPE) : defaultType, info.get(Consist._ID)
            ).setSource(msg.getBytes()));

            return;
        }
    }

    private String illegalValue(String string) {
        //非法值转换返回null
        if (string == null || "null".equalsIgnoreCase(string) || "".equals(string.trim())) {
            return null;
        }

        return string;
    }
}
