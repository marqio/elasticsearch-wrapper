package com.baidu.siem.loadbalance;

import com.baidu.siem.throttle.EsThrottle;
import org.springframework.stereotype.Component;

/**
 * 随机负载均衡
 * Created by yuxuefeng on 15/9/24.
 */
@Component("randomLoadBalance")
public class RandomLoadBalance implements LoadBalance {
    @Override
    public String doSelect(EsThrottle esSearchThrottle) {

        String[] nodes = esSearchThrottle.getHttpNodes();
        return nodes[esSearchThrottle.getRandom().nextInt(nodes.length)];

    }
}
