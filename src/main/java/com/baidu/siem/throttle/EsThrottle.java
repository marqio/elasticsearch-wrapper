package com.baidu.siem.throttle;

import com.baidu.siem.config.AbstractConfig;
import com.baidu.siem.model.Consist;
import com.baidu.siem.threadpool.NamedThreadFactory;

import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by yuxuefeng on 15/9/16.
 */
public class EsThrottle extends AbstractConfig {
    private Properties configs = new Properties();
    //高频访问属性
    private String clusterName;
    private int retries;
    private boolean monitor;

    //后期处理属性
    private int searchBufferSizeOld;
    private int writeBufferSizeOld;
    private String[] httpNodes;//host:port,host1:port
    private String[] transportNodes;//host:port,host1:port
    private String[] filterChain;//monitorFilter,xxxFilter
    private BalanceType balanceType;
    private ThreadPoolExecutor searchExecutor;
    private ThreadPoolExecutor searchExecutorOld;
    private ThreadPoolExecutor writeExecutor;
    private ThreadPoolExecutor writeExecutorOld;
    private Random random;
    private AtomicInteger atomicInteger;

    public EsThrottle() {
    }

    public void reset() {
        retries = getPositiveInt(Consist.RETRIES, 3);
        monitor = getBoolean(Consist.MONITOR, false);
        balanceType = EsThrottle.BalanceType.fromValue(getString(Consist.BALANCE_TYPE, "ROUNDROBIN"));
        httpNodes = getStringArray(Consist.HTTP_NODES, "localhost:8200", ",");
        transportNodes = getStringArray(Consist.TRANSPORT_NODES, "localhost:9400", ",");
        filterChain = getStringArray(Consist.FILTER_CHAIN, "monitorFilter", ",");
        random = new Random();
        atomicInteger = new AtomicInteger(0);
        //update writeThreadPool
        updateWriteThreadPool();
        //update searchThreadPool
        updateSearchThreadPool();
        writeBufferSizeOld = getPositiveInt(Consist.WRITE_BUFFER_SIZE, -1);
        searchBufferSizeOld = getPositiveInt(Consist.SEARCH_BUFFER_SIZE, -1);
    }

    private void updateSearchThreadPool() {
        int bufferSize = getPositiveInt(Consist.SEARCH_BUFFER_SIZE, 500);
        int concurrentCount = getPositiveInt(Consist.SEARCH_CONCURRENT_COUNT, 30);
        //初始化searchExecutor
        if (searchExecutor == null) {
            logger.info("SearchExecutor int");
            BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(bufferSize);
            searchExecutor = new ThreadPoolExecutor(concurrentCount, concurrentCount, 1, TimeUnit.HOURS, queue, new NamedThreadFactory("ESSearchTask", true), new ThreadPoolExecutor.AbortPolicy());
            logger.info("SearchExecutor int completed");
            return;
        }

        //未改变
        if (searchExecutor.getMaximumPoolSize() == concurrentCount && bufferSize == searchBufferSizeOld) {
            return;
        }
        //队列未改变
        if (bufferSize == searchBufferSizeOld) {
            logger.info("Start to update the [concurrentCount] of the searchExecutor from:[" + searchExecutor.getMaximumPoolSize() + "] to [" + concurrentCount + "] ");
            searchExecutor.setCorePoolSize(concurrentCount);
            searchExecutor.setMaximumPoolSize(concurrentCount);
            logger.info("Update the concurrentCount of the searchExecutor completed ");

            return;
        }

        searchExecutor.setCorePoolSize(5);
        searchExecutor.setMaximumPoolSize(5);
        searchExecutorOld = searchExecutor;
        //重新创建线程池
        logger.info("Start to create new searchExecutor with bufferSize:[" + bufferSize + "],concurrentCount:[" + concurrentCount + "]...");
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(bufferSize);
        searchExecutor = new ThreadPoolExecutor(concurrentCount, concurrentCount, 1, TimeUnit.HOURS, queue, new NamedThreadFactory("ESWriteTask", true), new ThreadPoolExecutor.AbortPolicy());
        logger.info("Create new searchExecutor completed...");

        //关闭旧线程池
        logger.info("Start to shutdown searchExecutorOld after all submitted tasks completed...");
        searchExecutorOld.shutdown();
        boolean flag = false;
        int index = 0;
        //最多等10分钟
        while (!flag) {
            try {
                flag = searchExecutorOld.awaitTermination(1, TimeUnit.SECONDS);
                logger.info("SearchExecutorOld shutdown take [" + (index + 1) + "] s");
                if (++index > 600) {
                    searchExecutorOld.shutdownNow();
                    searchExecutorOld = null;
                    logger.error("Error occurs when threadPool shutdown after 10 min,Maybe some task not illegal!");
                    break;
                }
            } catch (Exception e) {
                logger.error("Error occurs when threadPool shutdown after 10 min,Maybe some task not illegal!");
                flag = false;
            }
        }
        logger.info("SearchExecutorOld shutdown completed");
    }

    private void updateWriteThreadPool() {
        int bufferSize = getPositiveInt(Consist.WRITE_BUFFER_SIZE, 500);
        int concurrentCount = getPositiveInt(Consist.WRITE_CONCURRENT_COUNT, 30);
        //初始化writeExecutor
        if (writeExecutor == null) {
            logger.info("WriteExecutor int");
            BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(bufferSize);
            writeExecutor = new ThreadPoolExecutor(concurrentCount, concurrentCount, 1, TimeUnit.HOURS, queue, new NamedThreadFactory("ESTask", true), new ThreadPoolExecutor.AbortPolicy());
            logger.info("WriteExecutor int completed");
            return;
        }

        //未改变
        if (writeExecutor.getMaximumPoolSize() == concurrentCount && bufferSize == writeBufferSizeOld) {
            return;
        }
        //队列未改变
        if (bufferSize == writeBufferSizeOld) {
            logger.info("Start to update the [concurrentCount] of the writeExecutor from:[" + writeExecutor.getMaximumPoolSize() + "] to [" + concurrentCount + "] ");
            writeExecutor.setCorePoolSize(concurrentCount);
            writeExecutor.setMaximumPoolSize(concurrentCount);
            logger.info("Update the concurrentCount of the writeExecutor completed ");

            return;
        }

        writeExecutor.setCorePoolSize(5);
        writeExecutor.setMaximumPoolSize(5);
        writeExecutorOld = writeExecutor;
        //重新创建线程池
        logger.info("Start to create new writeExecutor with bufferSize:[" + bufferSize + "],concurrentCount:[" + concurrentCount + "]...");
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(bufferSize);
        writeExecutor = new ThreadPoolExecutor(concurrentCount, concurrentCount, 1, TimeUnit.HOURS, queue, new NamedThreadFactory("ESTask", true), new ThreadPoolExecutor.AbortPolicy());
        logger.info("Create new writeExecutor completed...");

        //关闭旧线程池
        logger.info("Start to shutdown writeExecutorOld after all submitted tasks completed...");
        writeExecutorOld.shutdown();
        boolean flag = false;
        int index = 0;
        //最多等10分钟
        while (!flag) {
            try {
                flag = writeExecutorOld.awaitTermination(1, TimeUnit.SECONDS);
                logger.info("WriteExecutorOld shutdown take [" + (index + 1) + "] s");
                if (++index > 600) {
                    writeExecutorOld.shutdownNow();
                    writeExecutorOld = null;
                    logger.error("Error occurs when threadPool shutdown after 10 min,Maybe some task not illegal!");
                    break;
                }
            } catch (Exception e) {
                logger.error("Error occurs when threadPool shutdown after 10 min,Maybe some task not illegal!");
                flag = false;
            }
        }
        logger.info("WriteExecutorOld shutdown completed");

    }


    public Random getRandom() {
        return random;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public AtomicInteger getAtomicInteger() {
        return atomicInteger;
    }

    public void setAtomicInteger(AtomicInteger atomicInteger) {
        this.atomicInteger = atomicInteger;
    }

    public BalanceType getBalanceType() {
        return balanceType;
    }

    public void setBalanceType(BalanceType balanceType) {
        this.balanceType = balanceType;
    }


    public String[] getFilterChain() {
        return filterChain;
    }

    public void setFilterChain(String[] filterChain) {
        this.filterChain = filterChain;
    }

    public String[] getHttpNodes() {
        return httpNodes;
    }

    public void setHttpNodes(String[] httpNodes) {
        this.httpNodes = httpNodes;
    }

    public String[] getTransportNodes() {
        return transportNodes;
    }

    public void setTransportNodes(String[] transportNodes) {
        this.transportNodes = transportNodes;
    }

    public Properties getConfigs() {
        return configs;
    }

    public void setConfigs(Properties configs) {
        this.configs = configs;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public boolean isMonitor() {
        return monitor;
    }

    public void setMonitor(boolean monitor) {
        this.monitor = monitor;
    }

    @Override
    public Properties getProperties() {
        return configs;
    }

    public ThreadPoolExecutor getSearchExecutor() {
        return searchExecutor;
    }

    public void setSearchExecutor(ThreadPoolExecutor searchExecutor) {
        this.searchExecutor = searchExecutor;
    }

    public ThreadPoolExecutor getWriteExecutor() {
        return writeExecutor;
    }

    public void setWriteExecutor(ThreadPoolExecutor writeExecutor) {
        this.writeExecutor = writeExecutor;
    }


    public enum BalanceType {
        NONE("无负载均衡"), RANDOM("随机"), ROUNDROBIN("轮询");
        private String value;

        private BalanceType(String value) {
            this.value = value;
        }

        public static BalanceType fromValue(String value) {
            for (BalanceType balanceType : BalanceType.values()) {
                if (balanceType.name().equals(value)) {
                    return balanceType;
                }
            }
            throw new RuntimeException("BalanceType not exists! please check and retry!");
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
