package com.baidu.siem.loadbalance;

import com.baidu.siem.throttle.EsThrottle;

/**
 * Created by yuxuefeng on 15/9/24.
 */
public interface LoadBalance {
    /**
     * 根据当前loadbalance策略选择目标node
     *
     * @param esSearchThrottle
     * @return
     */
    public String doSelect(EsThrottle esSearchThrottle);
}
