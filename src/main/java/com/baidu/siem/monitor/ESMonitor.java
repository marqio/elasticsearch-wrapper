package com.baidu.siem.monitor;

import com.baidu.siem.config.CommonConfig;
import com.baidu.siem.invoker.Invoker;
import com.baidu.siem.model.Consist;
import com.baidu.siem.threadpool.NamedThreadFactory;
import com.baidu.siem.throttle.EsThrottle;
import org.apache.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by yuxuefeng on 15/9/21.
 */
public class ESMonitor implements Monitor {
    private static final Logger logger = Logger.getLogger(ESMonitor.class);
    //    private static final Logger monitorLogger = Logger.getLogger("monitorLogger");
    private static final Logger slowLogger = Logger.getLogger("slowLogger");
    /**
     * 节点并发数
     */
    private static final String NODE_CONCURRENT_MIN = "nodeConcurrent_min";
    private static final String NODE_CONCURRENT_AVE = "nodeConcurrent_ave";
    private static final String NODE_CONCURRENT_MAX = "nodeConcurrent_max";
    /**
     * 集群并发数
     */
    private static final String CLUSTER_CONCURRENT_MIN = "clusterConcurrent_min";
    private static final String CLUSTER_CONCURRENT_AVE = "clusterConcurrent_ave";
    private static final String CLUSTER_CONCURRENT_MAX = "clusterConcurrent_max";
    /**
     * web wrapper server并发数
     */
    private static final String SERVER_CONCURRENT_MIN = "serverConcurrent_min";
    private static final String SERVER_CONCURRENT_AVE = "serverConcurrent_ave";
    private static final String SERVER_CONCURRENT_MAX = "serverConcurrent_max";
    //Server监控项
    private static final String[] SERVER_TYPES = {SUCESS, FAILURE, SERVER_CONCURRENT_MIN, SERVER_CONCURRENT_AVE, SERVER_CONCURRENT_MAX};
    /**
     * 调用时间
     */
    private static final String ELAPSED_MIN = "elapsed_min";
    private static final String ELAPSED_AVE = "elapsed_ave";
    private static final String ELAPSED_MAX = "elapsed_max";
    //节点监控项
    private static final String[] NODE_TYPES = {SUCESS, FAILURE, ELAPSED_MIN, ELAPSED_AVE, ELAPSED_MAX, NODE_CONCURRENT_MIN, NODE_CONCURRENT_AVE, NODE_CONCURRENT_MAX};
    /**
     * 读线程池 queue_size
     */
    private static final String SEARCH_THREAD_POOL_QUEUE_SIZE_MIN = "search_thread_pool_queue_size_min";
    private static final String SEARCH_THREAD_POOL_QUEUE_SIZE_AVE = "search_thread_pool_queue_size_ave";
    private static final String SEARCH_THREAD_POOL_QUEUE_SIZE_MAX = "search_thread_pool_queue_size_max";
    /**
     *  读线程池 线程数
     */
    private static final String SEARCH_THREAD_POOL_SIZE_MIN = "search_thread_pool_size_min";
    private static final String SEARCH_THREAD_POOL_SIZE_AVE = "search_thread_pool_size_ave";
    private static final String SEARCH_THREAD_POOL_SIZE_MAX = "search_thread_pool_size_max";
    /**
     * 写线程池 queue_size
     */
    private static final String WRITE_THREAD_POOL_QUEUE_SIZE_MIN = "write_thread_pool_queue_size_min";
    private static final String WRITE_THREAD_POOL_QUEUE_SIZE_AVE = "write_thread_pool_queue_size_ave";
    private static final String WRITE_THREAD_POOL_QUEUE_SIZE_MAX = "write_thread_pool_queue_size_max";
    /**
     *  写线程池 线程数
     */
    private static final String WRITE_THREAD_POOL_SIZE_MIN = "write_thread_pool_size_min";
    private static final String WRITE_THREAD_POOL_SIZE_AVE = "write_thread_pool_size_ave";
    private static final String WRITE_THREAD_POOL_SIZE_MAX = "write_thread_pool_size_max";
    //集群监控项
    private static final String[] CLUSTER_TYPES = {SUCESS, FAILURE, ELAPSED_MIN, ELAPSED_AVE, ELAPSED_MAX, CLUSTER_CONCURRENT_MIN, CLUSTER_CONCURRENT_AVE, CLUSTER_CONCURRENT_MAX,
            WRITE_THREAD_POOL_QUEUE_SIZE_MIN, WRITE_THREAD_POOL_QUEUE_SIZE_AVE, WRITE_THREAD_POOL_QUEUE_SIZE_MAX, WRITE_THREAD_POOL_SIZE_MIN, WRITE_THREAD_POOL_SIZE_AVE, WRITE_THREAD_POOL_SIZE_MAX,
            SEARCH_THREAD_POOL_QUEUE_SIZE_MIN, SEARCH_THREAD_POOL_QUEUE_SIZE_AVE, SEARCH_THREAD_POOL_QUEUE_SIZE_MAX, SEARCH_THREAD_POOL_SIZE_MIN, SEARCH_THREAD_POOL_SIZE_AVE, SEARCH_THREAD_POOL_SIZE_MAX
    };
    private final EsThrottle esSearchThrottle;
    private final ConcurrentMap<String, ConcurrentMap<String, AtomicReference<long[]>>> clusterMap = new ConcurrentHashMap<String, ConcurrentMap<String, AtomicReference<long[]>>>();
    //所有统计项长度
    private int allTypesLength = 26;
    //上次成功数
    private long lastSuccess;
    //上次失败数
    private long lastFailure;
    // 统计信息收集定时器
    private ScheduledFuture<?> sendFuture;
    // 定时写任务执行器
    private ScheduledExecutorService scheduledWriteService;
    //merge任务线程池
    private ExecutorService executorMergeService;
    private BlockingQueue<Invoker> invokersQueue;//
    //监控数据收集频率
    private long monitorInterval;
    private int monitorQueueSize;
    private int monitorMergeThreadNum;//异步从queue中获取数据进行处理的的线程数
    private FileWriter fileWriter;

    public ESMonitor(final EsThrottle esSearchThrottle) {
        this.esSearchThrottle = esSearchThrottle;
        //初始化参数
        initRequiredParams(esSearchThrottle);
    }

    private void initRequiredParams(EsThrottle esSearchThrottle) {
        this.monitorMergeThreadNum = esSearchThrottle.getPositiveInt(Consist.MONITOR_MERGE_THREAD_NUM, 5);
        this.monitorQueueSize = esSearchThrottle.getPositiveInt(Consist.MONITOR_QUEUE_SIZE, 10000);
        this.monitorInterval = esSearchThrottle.getPositiveInt(Consist.MONITOR_INTERVAL, 10000);
        this.invokersQueue = new ArrayBlockingQueue<Invoker>(monitorQueueSize);
        scheduledWriteService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("ESMonitorWrite", true));
        executorMergeService = Executors.newFixedThreadPool(monitorMergeThreadNum, new NamedThreadFactory("ESMonitorMerge", true));
        // 启动统计信息收集定时器
        sendFuture = scheduledWriteService.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                // 收集统计信息
                try {
                    write();
                } catch (Throwable t) { // 防御性容错
                    logger.error("Unexpected error occurs when logging monitorlog, cause: " + t.getMessage(), t);
                } finally {
                    if (fileWriter != null) {
                        try {
                            fileWriter.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }, monitorInterval, monitorInterval, TimeUnit.MILLISECONDS);

        //提交merge任务
        for (int i = 0; i < monitorMergeThreadNum; i++)
            executorMergeService.execute(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            merge();
                        } catch (Exception e) {
                            logger.error("ESMonitor merge error:" + e.getMessage());
                        }
                    }
                }
            });
    }

    private void merge() throws InterruptedException {
        Invoker invoker = invokersQueue.take();

        // 读写统计变量
        String clusterName = invoker.getEsSearchThrottle().getClusterName();
        String targetName = invoker.getTargetNode();
        int success = Integer.parseInt(invoker.getParameter(Monitor.SUCESS, "0"));
        int failure = Integer.parseInt(invoker.getParameter(Monitor.FAILURE, "0"));
        int elapsed = Integer.parseInt(invoker.getParameter(Monitor.ELAPSED, "0"));
        int nodeConcurrent = Integer.parseInt(invoker.getParameter(Monitor.NODE_CONCURRENT, "0"));
        int clusterConcurrent = Integer.parseInt(invoker.getParameter(Monitor.CLUSTER_CONCURRENT, "0"));
        int serverConcurrent = Integer.parseInt(invoker.getParameter(Monitor.SERVER_CONCURRENT, "0"));
        int writeQueueSize = Integer.parseInt(invoker.getResponseParameter(Monitor.WRITE_THREAD_POOL_QUEUE_SIZE, "0"));
        int writeThreadCount = Integer.parseInt(invoker.getResponseParameter(Monitor.WRITE_THREAD_POOL_SIZE, "0"));
        int searchQueueSize = Integer.parseInt(invoker.getResponseParameter(Monitor.SEARCH_THREAD_POOL_QUEUE_SIZE, "0"));
        int searchThreadCount = Integer.parseInt(invoker.getResponseParameter(Monitor.SEARCH_THREAD_POOL_SIZE, "0"));
//        logger.info(writeQueueSize + "," + writeThreadCount + "," + searchQueueSize + "," + searchThreadCount);
        ConcurrentMap<String, AtomicReference<long[]>> nodesMap = clusterMap.get(clusterName);
        if (nodesMap == null) {
            clusterMap.putIfAbsent(clusterName, new ConcurrentHashMap<String, AtomicReference<long[]>>());
            nodesMap = clusterMap.get(clusterName);
        }
        AtomicReference<long[]> reference = nodesMap.get(targetName);
        if (reference == null) {
            nodesMap.putIfAbsent(targetName, new AtomicReference<long[]>());
            reference = nodesMap.get(targetName);
        }
        // CompareAndSet并发加入统计数据
        long[] current;
        long[] update = new long[allTypesLength];
        do {
            current = reference.get();
            if (current == null) {
                update[0] = success;//成功
                update[1] = failure;//失败
                update[2] = elapsed;//最小调用时间
                update[3] = elapsed;//调用时间总和
                update[4] = elapsed;//最大调用时间
                update[5] = nodeConcurrent;//node最小并发数
                update[6] = nodeConcurrent;//node并发数总和
                update[7] = nodeConcurrent;//node最大并发数
                update[8] = clusterConcurrent;//cluster最小并发数
                update[9] = clusterConcurrent;//cluster并发数总和
                update[10] = clusterConcurrent;//cluster最大并发数
                update[11] = serverConcurrent;//server最小并发数
                update[12] = serverConcurrent;//server并发数总和
                update[13] = serverConcurrent;//server最大并发数
                update[14] = writeQueueSize;//写缓存任务数最小数
                update[15] = writeQueueSize;//写缓存任务数总和
                update[16] = writeQueueSize;//写缓存任务数最大数
                update[17] = writeThreadCount;//写线程数最小数
                update[18] = writeThreadCount;//写线程数总和
                update[19] = writeThreadCount;//写线程数最大数
                update[20] = searchQueueSize;//读缓存任务数最小数
                update[21] = searchQueueSize;//读缓存任务数总和
                update[22] = searchQueueSize;//读缓存任务数最大数
                update[23] = searchThreadCount;//读线程数最小数
                update[24] = searchThreadCount;//读线程数总和
                update[25] = searchThreadCount;//读线程数最大数
            } else {
                update[0] = current[0] + success;
                update[1] = current[1] + failure;
                update[2] = current[2] > elapsed ? elapsed : current[2];
                update[3] = current[3] + elapsed;
                update[4] = current[4] < elapsed ? elapsed : current[4];
                update[5] = current[5] > nodeConcurrent ? nodeConcurrent : current[5];//node最小并发数
                update[6] = current[6] += nodeConcurrent;//node并发数总和
                update[7] = current[7] < nodeConcurrent ? nodeConcurrent : current[7];//node最大并发数
                update[8] = current[8] > clusterConcurrent ? clusterConcurrent : current[8];//cluster最小并发数
                update[9] = current[9] += clusterConcurrent;//cluster并发数总和
                update[10] = current[10] < clusterConcurrent ? clusterConcurrent : current[10];//cluster最大并发数
                update[11] = current[11] > serverConcurrent ? serverConcurrent : current[11];//server最小并发数
                update[12] = current[12] += serverConcurrent;//server并发数总和
                update[13] = current[13] < serverConcurrent ? serverConcurrent : current[13];//server最大并发数
                update[14] = current[14] > writeQueueSize ? writeQueueSize : current[14];//缓存任务数最小数
                update[15] = current[15] += writeQueueSize;//缓存任务数总和
                update[16] = current[16] < writeQueueSize ? writeQueueSize : current[16];//缓存任务数最大数
                update[17] = current[17] > writeThreadCount ? writeThreadCount : current[17];//线程数最小数
                update[18] = current[18] += writeThreadCount;//线程数总和
                update[19] = current[19] < writeThreadCount ? writeThreadCount : current[19];//线程数最大数
                update[20] = current[20] > searchQueueSize ? searchQueueSize : current[20];//缓存任务数最小数
                update[21] = current[21] += searchQueueSize;//缓存任务数总和
                update[22] = current[22] < searchQueueSize ? searchQueueSize : current[22];//缓存任务数最大数
                update[23] = current[23] > searchThreadCount ? searchThreadCount : current[23];//线程数最小数
                update[24] = current[24] += searchThreadCount;//线程数总和
                update[25] = current[25] < searchThreadCount ? searchThreadCount : current[25];//线程数最大数
            }
        } while (!reference.compareAndSet(current, update));

        //记录慢日志
        doSlowLog(invoker);
    }

    private void doSlowLog(Invoker invoker) {
        int elapsed = Integer.parseInt(invoker.getParameter(Monitor.ELAPSED, "0"));

        if (elapsed < CommonConfig.commonConfig.getPositiveLong(Consist.DEFAULT_SLOW_TIMEOUT, CommonConfig.defaultSlowTimeout)) {
            return;
        }
        boolean flag = false;
        int index = 0;
        String url = invoker.getUrl();
        String requestData = invoker.getRequestData();
        String response = invoker.getResponse().getMsg();
        String method = "error";
        String hotname = "error";
        int port = -1;
        String sourceIp = "error";
        do {
            flag = false;
            try {
                method = invoker.getRequest().getMethod();
                hotname = invoker.getRequest().getRemoteHost();
                port = invoker.getRequest().getRemotePort();
                sourceIp = invoker.getRequest().getRemoteAddr();
            } catch (Exception e) {
                flag = true;
            }
        } while (flag && ++index < 3);

        slowLogger.info("\nThe Elapsed:[" + elapsed + "] ms" + "\n"
                + "The SourceIp:" + sourceIp + ",The HostName:" + hotname + ",The SourcePort:" + port + "\n"
                + "The RequestMethod:[" + method + "]\n"
                + "The RequestURL:[" + url + "]\n"
                + "The RequestData:[" + requestData + "]\n"
                + "The Response:" + response + "\n"
                + "--------------------------------------------------");

    }

    public void collect(Invoker invoker) {
        if (!invokersQueue.offer(invoker)) {
            logger.warn("MonitorInvokersQueue is full, size:[" + monitorQueueSize + "]");
        }
    }

    public synchronized void write() throws IOException {
        fileWriter = new FileWriter(CommonConfig.commonConfig.getString(Consist.DEFAULT_LOG_DIR, CommonConfig.defaultLogDir) + "/monitor.log", false);
        long[] numbersServer = new long[SERVER_TYPES.length];
        numbersServer[2] = Long.MAX_VALUE;

        for (Map.Entry<String, ConcurrentMap<String, AtomicReference<long[]>>> entry : clusterMap.entrySet()) {
            handleCluster(entry, numbersServer);
        }
        int serverCluster = 0;
        for (String k : SERVER_TYPES) {
            //success failure
            if (serverCluster == 0 || serverCluster == 1) {
                fileWriter.write("es-wrapper_" + k + "_count= " + numbersServer[serverCluster] + "\r\n");
                fileWriter.write("es-wrapper_" + k + "_tps= " + String.format("%10.2f", numbersServer[serverCluster++] / (monitorInterval * 1.0 / 1000 == 0 ? 1.0 : (monitorInterval * 1.0 / 1000))).trim() + "\r\n");
                continue;
            }
            //ave
            if (serverCluster == 3) {
                fileWriter.write("es-wrapper_" + k + "= " + String.format("%10.2f", numbersServer[serverCluster++] / (numbersServer[0] + numbersServer[1] == 0 ? 1 : numbersServer[0] + numbersServer[1]) * 1.0).trim() + "\r\n");
                continue;
            }

            fileWriter.write("es-wrapper_" + k + "= " + (numbersServer[serverCluster++] == Long.MAX_VALUE ? 0 : numbersServer[serverCluster - 1]) + "\r\n");

        }
        fileWriter.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\r\n");
        fileWriter.flush();
    }

    private void handleCluster(Map.Entry<String, ConcurrentMap<String, AtomicReference<long[]>>> entry, long[] numbersServer) throws IOException {

        // 获取已统计数据
        String clusterName = entry.getKey();
        ConcurrentMap<String, AtomicReference<long[]>> nodesMap = entry.getValue();

        long[] numbersCluster = new long[CLUSTER_TYPES.length];
        numbersCluster[2] = Long.MAX_VALUE;
        numbersCluster[5] = Long.MAX_VALUE;
        numbersCluster[8] = Long.MAX_VALUE;
        numbersCluster[11] = Long.MAX_VALUE;
        numbersCluster[14] = Long.MAX_VALUE;
        numbersCluster[17] = Long.MAX_VALUE;

        for (Map.Entry<String, AtomicReference<long[]>> nodeEntry : nodesMap.entrySet()) {
            String nodeName = nodeEntry.getKey();
            if (esSearchThrottle.getString(Consist.HTTP_NODES, "localhost:8200").indexOf(nodeName) > -1)
                handleNode(clusterName, nodeEntry, numbersCluster, numbersServer);
            else {
                //去除已经移除的node节点
                nodesMap.remove(nodeName);
            }
        }

        int indexCluster = 0;
        for (String k : CLUSTER_TYPES) {
            //success failure
            if (indexCluster == 0 || indexCluster == 1) {
                fileWriter.write(clusterName + "_" + k + "_count= " + numbersCluster[indexCluster] + "\r\n");
                fileWriter.write(clusterName + "_" + k + "_tps= " + String.format("%10.2f", numbersCluster[indexCluster++] / (monitorInterval * 1.0 / 1000 == 0 ? 1.0 : (monitorInterval * 1.0 / 1000))).trim() + "\r\n");
                continue;
            }
            //ave
            if (indexCluster == 3 || indexCluster == 6 || indexCluster == 9 || indexCluster == 12 || indexCluster == 15 || indexCluster == 18) {
                fileWriter.write(clusterName + "_" + k + "= " + String.format("%10.2f", numbersCluster[indexCluster++] / (numbersCluster[0] + numbersCluster[1] == 0 ? 1 : numbersCluster[0] + numbersCluster[1]) * 1.0).trim() + "\r\n");
                continue;
            }

            fileWriter.write(clusterName + "_" + k + "= " + (numbersCluster[indexCluster++] == Long.MAX_VALUE ? 0 : numbersCluster[indexCluster - 1]) + "\r\n");
        }
        fileWriter.write("==============================================================\r\n");
    }

    private void handleNode(String clusterName, Map.Entry<String, AtomicReference<long[]>> nodeEntry, long[] numbersCluster, long[] numbersServer) throws IOException {
        String nodeName = nodeEntry.getKey();
        AtomicReference<long[]> reference = nodeEntry.getValue();
        long[] numbers = reference.get();

        long success = numbers[0];//本时间段成功数
        long failure = numbers[1];//本时间段失败数
//        lastSuccess = numbers[0];//重新记录
//        lastFailure = numbers[1];//重新记录
        long elapsedMin = numbers[2];
        long elapsedSum = numbers[3];
        long elapsedMax = numbers[4];
        long nodeConcurrentMin = numbers[5];
        long nodeConcurrentSum = numbers[6];
        long nodeConcurrentMax = numbers[7];
        long clusterConcurrentMin = numbers[8];
        long clusterConcurrentSum = numbers[9];
        long clusterConcurrentMax = numbers[10];
        long serverConcurrentMin = numbers[11];
        long serverConcurrentSum = numbers[12];
        long serverConcurrentMax = numbers[13];
        long writeQueueSizeMin = numbers[14];
        long writeQueueSizeSum = numbers[15];
        long writeQueueSizeMax = numbers[16];
        long writeThreadCountMin = numbers[17];
        long writeThreadCountSum = numbers[18];
        long writeThreadCountMax = numbers[19];
        long searchQueueSizeMin = numbers[20];
        long searchQueueSizeSum = numbers[21];
        long searchQueueSizeMax = numbers[22];
        long searchThreadCountMin = numbers[23];
        long searchThreadCountSum = numbers[24];
        long searchThreadCountMax = numbers[25];

        //计算集群信息
        numbersCluster[0] += success;
        numbersCluster[1] += failure;
        numbersCluster[2] = numbersCluster[2] < elapsedMin ? numbersCluster[2] : elapsedMin;
        numbersCluster[3] += elapsedSum;
        numbersCluster[4] = numbersCluster[4] > elapsedMax ? numbersCluster[4] : elapsedMax;
        numbersCluster[5] = numbersCluster[5] > clusterConcurrentMin ? clusterConcurrentMin : numbersCluster[5];
        numbersCluster[6] += clusterConcurrentSum;
        numbersCluster[7] = numbersCluster[7] < clusterConcurrentMax ? clusterConcurrentMax : numbersCluster[7];
        numbersCluster[8] = numbersCluster[8] > writeQueueSizeMin ? writeQueueSizeMin : numbersCluster[8];
        numbersCluster[9] += writeQueueSizeSum;
        numbersCluster[10] = numbersCluster[10] < writeQueueSizeMax ? writeQueueSizeMax : numbersCluster[10];
        numbersCluster[11] = numbersCluster[11] > writeThreadCountMin ? writeThreadCountMin : numbersCluster[11];
        numbersCluster[12] += writeThreadCountSum;
        numbersCluster[13] = numbersCluster[13] < writeThreadCountMax ? writeThreadCountMax : numbersCluster[13];
        numbersCluster[14] = numbersCluster[14] > searchQueueSizeMin ? searchQueueSizeMin : numbersCluster[14];
        numbersCluster[15] += searchQueueSizeSum;
        numbersCluster[16] = numbersCluster[16] < searchQueueSizeMax ? searchQueueSizeMax : numbersCluster[16];
        numbersCluster[17] = numbersCluster[17] > searchThreadCountMin ? searchThreadCountMin : numbersCluster[17];
        numbersCluster[18] += searchThreadCountSum;
        numbersCluster[19] = numbersCluster[19] < searchThreadCountMax ? searchThreadCountMax : numbersCluster[19];

        //计算server信息
        numbersServer[0] += success;
        numbersServer[1] += failure;
        numbersServer[2] = numbersServer[2] > serverConcurrentMin ? serverConcurrentMin : numbersServer[2];
        numbersServer[3] += clusterConcurrentSum;
        numbersServer[4] = numbersServer[4] < serverConcurrentMax ? serverConcurrentMax : numbersServer[4];


        int index = 0;
        for (String k : NODE_TYPES) {
            //success  failure
            if (index == 0 || index == 1) {
                fileWriter.write(clusterName + "_" + nodeName + "_" + k + "_count= " + numbers[index] + "\r\n");
                fileWriter.write(clusterName + "_" + nodeName + "_" + k + "_tps= " + String.format("%10.2f", numbers[index++] / (monitorInterval * 1.0 / 1000 == 0 ? 1.0 : (monitorInterval * 1.0 / 1000))).trim() + "\r\n");
                continue;
            }
            if (index == 3 || index == 6) {
                fileWriter.write(clusterName + "_" + nodeName + "_" + k + "= " + String.format("%10.2f", numbers[index++] / (success + failure == 0 ? 1.0 : success + failure) * 1.0).trim() + "\r\n");
                continue;
            }
            fileWriter.write(clusterName + "_" + nodeName + "_" + k + "= " + (numbers[index++] == Long.MAX_VALUE ? 0 : numbers[index - 1]) + "\r\n");
        }
        fileWriter.write("-----------------------------------------------------------------\r\n");

        // 减掉已统计数据
        long[] current;
        long[] update = new long[allTypesLength];
        do {
            current = reference.get();
            if (current == null) {
                update[0] = 0;
                update[1] = 0;
                update[2] = 0;
                update[3] = 0;
                update[4] = 0;
                update[5] = 0;
                update[6] = 0;
                update[7] = 0;
                update[8] = 0;
                update[9] = 0;
                update[10] = 0;
                update[11] = 0;
                update[12] = 0;
                update[13] = 0;
                update[14] = 0;
                update[15] = 0;
                update[16] = 0;
                update[17] = 0;
                update[18] = 0;
                update[19] = 0;
                update[20] = 0;
                update[21] = 0;
                update[22] = 0;
                update[23] = 0;
                update[24] = 0;
                update[25] = 0;
            } else {
//                update[0] = current[0];
//                update[1] = current[1];
                update[0] = current[0] - success;
                update[1] = current[1] - failure;
                update[2] = current[2] < elapsedMin ? current[2] : Long.MAX_VALUE;
                update[3] = current[3] - elapsedSum;
                update[4] = current[4] > elapsedMax ? current[4] : 0;
                update[5] = current[5] < nodeConcurrentMin ? current[5] : Long.MAX_VALUE;//node最小并发数
                update[6] = current[6] - nodeConcurrentSum;//node并发数总和
                update[7] = current[7] > nodeConcurrentMax ? current[7] : 0;//node最大并发数
                update[8] = current[8] < clusterConcurrentMin ? current[8] : Long.MAX_VALUE;//cluster最小并发数
                update[9] = current[9] - clusterConcurrentSum;//cluster并发数总和
                update[10] = current[10] > clusterConcurrentMax ? current[10] : 0;//cluster最大并发数
                update[11] = current[11] < serverConcurrentMin ? current[11] : Long.MAX_VALUE;//server最小并发数
                update[12] = current[12] - serverConcurrentSum;//server并发数总和
                update[13] = current[13] > serverConcurrentMax ? current[13] : 0;//server最大并发数
                update[14] = current[14] < writeQueueSizeMin ? current[14] : Long.MAX_VALUE;//缓存任务数最小数
                update[15] = current[15] - writeQueueSizeSum;//缓存任务数总和
                update[16] = current[16] > writeQueueSizeMax ? current[16] : 0;//缓存任务数最大数
                update[17] = current[17] < writeThreadCountMin ? current[17] : Long.MAX_VALUE;//线程数最小数
                update[18] = current[18] - writeThreadCountSum;//线程数总和
                update[19] = current[19] > writeThreadCountMax ? current[19] : 0;//线程数最大数
                update[20] = current[20] < searchQueueSizeMin ? current[20] : Long.MAX_VALUE;//缓存任务数最小数
                update[21] = current[21] - searchQueueSizeSum;//缓存任务数总和
                update[22] = current[22] > searchQueueSizeMax ? current[22] : 0;//缓存任务数最大数
                update[23] = current[23] < searchThreadCountMin ? current[23] : Long.MAX_VALUE;//线程数最小数
                update[24] = current[24] - searchThreadCountSum;//线程数总和
                update[25] = current[25] > searchThreadCountMax ? current[25] : 0;//线程数最大数
            }
        } while (!reference.compareAndSet(current, update));
    }

    //cancel
    public void cancel() {
        try {
            sendFuture.cancel(true);// 关闭定时任务
            scheduledWriteService.shutdownNow();//关闭定时线程
            executorMergeService.shutdownNow();
        } catch (Exception t) {
            logger.error("Unexpected error occur when cancel monitor timers, cause: " + t.getMessage(), t);
        }
    }

    @Override
    public List<Invoker> lookup(Invoker query) {
        return null;
    }

    @Override
    public boolean checkModified() {

        return esSearchThrottle.getPositiveLong(Consist.MONITOR_INTERVAL, 10000) != monitorInterval || esSearchThrottle.getPositiveInt(Consist.MONITOR_QUEUE_SIZE, 10000) != monitorQueueSize || esSearchThrottle.getPositiveInt(Consist.MONITOR_MERGE_THREAD_NUM, 5) != monitorMergeThreadNum;

    }

    @Override
    public void update() {
        cancel();
        initRequiredParams(esSearchThrottle);
    }

    public EsThrottle getEsSearchThrottle() {
        return esSearchThrottle;
    }

    public long getMonitorInterval() {
        return monitorInterval;
    }

    public ScheduledExecutorService getScheduledWriteService() {
        return scheduledWriteService;
    }

    public ExecutorService getExecutorMergeService() {
        return executorMergeService;
    }

    public ConcurrentMap<String, ConcurrentMap<String, AtomicReference<long[]>>> getClusterMap() {
        return clusterMap;
    }

    public BlockingQueue<Invoker> getInvokersQueue() {
        return invokersQueue;
    }

    public int getMonitorMergeThreadNum() {
        return monitorMergeThreadNum;
    }

    public void setMonitorMergeThreadNum(int monitorMergeThreadNum) {
        this.monitorMergeThreadNum = monitorMergeThreadNum;
    }
}
