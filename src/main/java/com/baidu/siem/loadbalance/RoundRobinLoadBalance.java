package com.baidu.siem.loadbalance;

import com.baidu.siem.throttle.EsThrottle;
import org.springframework.stereotype.Component;

/**
 * 轮询负载均衡
 * Created by yuxuefeng on 15/9/24.
 */
@Component("roundRobinLoadBalance")
public class RoundRobinLoadBalance implements LoadBalance {
    @Override
    public String doSelect(EsThrottle esSearchThrottle) {

        String[] nodes = esSearchThrottle.getHttpNodes();
        return nodes[esSearchThrottle.getAtomicInteger().getAndIncrement() % nodes.length];

    }
}
