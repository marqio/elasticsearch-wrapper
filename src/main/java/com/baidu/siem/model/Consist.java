package com.baidu.siem.model;

/**
 * Created by yuxuefeng on 15/10/9.
 */
public class Consist {
    public static final String DEFAULT_COMMON_FILE_NAME = "wrapper-common.properties";
    public static final String DEFAULT_CLUSTER_NAME = "defaultClusterName";
    public static final String DEFAULT_TIMEOUT = "defaultTimeout";
    public static final String DEFAULT_LOG_DIR = "defaultLogDir";
    public static final String DEFAULT_SLOW_TIMEOUT = "defaultSlowTimeout";

    //HTTP RequestMethod
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";

    //ES ACTION TYPE
    public static final String ES_INDEX = "index";
    public static final String ES_DELETE = "delete";
    public static final String ES_UPDATE = "update";

    //ES bulk meta data
    public static final String _INDEX = "_index";
    public static final String _TYPE = "_type";
    public static final String DEFAULT_TYPE = "logs";
    public static final String _ID = "_id";

    //esthrottle config name
    public static final String CLUSTER_NAME = "clusterName";
    public static final String SEARCH_CONCURRENT_COUNT = "searchConcurrentCount";
    public static final String WRITE_CONCURRENT_COUNT = "writeConcurrentCount";
    public static final String SEARCH_BUFFER_SIZE = "searchBufferSize";
    public static final String WRITE_BUFFER_SIZE = "writeBufferSize";
    public static final String HTTP_NODES = "httpNodes";
    public static final String TRANSPORT_NODES = "transportNodes";
    public static final String BALANCE_TYPE = "balanceType";
    public static final String RETRIES = "retries";
    public static final String FILTER_CHAIN = "filterChain";
    public static final String MONITOR = "monitor";
    public static final String MONITOR_INTERVAL = "monitorInterval";
    public static final String MONITOR_QUEUE_SIZE = "monitorQueueSize";
    public static final String MONITOR_MERGE_THREAD_NUM = "monitorMergeThreadNum";
    public static final String SYNC = "syncWrapper";
}
