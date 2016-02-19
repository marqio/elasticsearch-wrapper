package com.baidu.siem.monitor;

import com.baidu.siem.invoker.Invoker;
import com.baidu.siem.threadpool.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by yuxuefeng on 15/9/21.
 */
public abstract class AbstractMonitorFactory implements MonitorFactory {
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final Map<String, Monitor> MONITORS = new ConcurrentHashMap<String, Monitor>();
    private static final Logger logger = LoggerFactory.getLogger(AbstractMonitorFactory.class);
    // 定时检查monitor更新
    private static final ScheduledExecutorService scheduledWriteService = Executors.newScheduledThreadPool(1, new NamedThreadFactory(AbstractMonitorFactory.class + "configuration check timer", true));
    private static final ScheduledFuture<?> sendFuture = scheduledWriteService.scheduleWithFixedDelay(new Runnable() {
        public void run() {
            // 收集统计信息
            try {
                checkMonitors();
            } catch (Throwable t) { // 防御性容错
                logger.error("Unexpected error occur at monitor configuration check , cause: " + t.getMessage(), t);
            }
        }
    }, 5, 5, TimeUnit.SECONDS);

    public static Collection<Monitor> getMonitors() {
        return Collections.unmodifiableCollection(MONITORS.values());
    }

    private static void checkMonitors() {
        for (Monitor monitor : MONITORS.values()) {
            if (monitor.checkModified()) {
                monitor.update();
            }
        }
    }

    @Override
    public Monitor getMonitor(Invoker invoker) {
        String clusterName = invoker.getEsSearchThrottle().getClusterName();
        LOCK.lock();
        try {
            Monitor monitor = MONITORS.get(clusterName);
            if (monitor != null) {
                return monitor;
            }
            monitor = createMonitor(invoker);
            if (monitor == null) {
                throw new IllegalStateException("Can not create monitor " + clusterName);
            }
            MONITORS.put(clusterName, monitor);
            return monitor;
        } finally {
            // 释放锁
            LOCK.unlock();
        }
    }

    protected abstract Monitor createMonitor(Invoker invoker);


}
