package com.baidu.siem.client;

import com.baidu.siem.model.Consist;
import com.baidu.siem.throttle.EsThrottle;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by yuxuefeng on 15/10/15.
 */
public class ClientFactory {
    private static Logger logger = LoggerFactory.getLogger(ClientFactory.class);
    //TODO 二期优化成连接池形式，减少不必要的资源浪费
    private static ConcurrentMap<String, Client> esClientsMap = new ConcurrentHashMap<>();

    public static Client generateTransportClient(EsThrottle esSearchThrottle) {
        Client client = esClientsMap.get(esSearchThrottle.getString(Consist.CLUSTER_NAME, "defaul-es"));
        if (client != null) {
            return client;
        }
        return updateClient(esSearchThrottle, false);
    }

    private synchronized static Client updateClient(EsThrottle esSearchThrottle, boolean reload) {
        String clusterName = esSearchThrottle.getString(Consist.CLUSTER_NAME, "defaul-es");
        Client client = esClientsMap.get(clusterName);
        if (!reload && client != null) {
            return client;
        }
        logger.info("Start to init [" + clusterName + "]  TransportClient...\n");

        Settings settings = ImmutableSettings.settingsBuilder()
                .put("cluster.name", clusterName).build();
        TransportClient transportClient = new TransportClient(settings);

        String[] nodes = esSearchThrottle.getTransportNodes();
        for (int i = 0; i < nodes.length; i++) {
            String[] ipport = nodes[i].split(":");
            transportClient.addTransportAddress(new InetSocketTransportAddress(ipport[0], Integer.parseInt(ipport[1])));
        }

        logger.info("[" + clusterName + "] TransportClient Completed!\n");
        return putEsClient(clusterName, transportClient);
    }

    public static Client putEsClient(String clusterName, Client client) {

        esClientsMap.put(clusterName, client);

        return esClientsMap.get(clusterName);
    }

}
