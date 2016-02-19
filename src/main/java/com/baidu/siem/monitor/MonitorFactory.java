package com.baidu.siem.monitor;

import com.baidu.siem.invoker.Invoker;

/**
 * Created by yuxuefeng on 15/9/21.
 */
public interface MonitorFactory {
    /**
     * Create monitor.
     *
     * @param invoker
     * @return monitor
     */
    Monitor getMonitor(Invoker invoker);
}
