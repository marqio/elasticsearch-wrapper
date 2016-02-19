package com.baidu.siem.admin.manager;

import com.baidu.siem.throttle.EsThrottle;
import com.baidu.siem.throttle.EsThrottleFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by yuxuefeng on 15/9/25.
 */
@Service("esAdminManager")
public class EsAdminManagerImpl implements EsAdminManager {
    @Override
    public List<EsThrottle> getAllThrottles() {
        Map<String, EsThrottle> maps = EsThrottleFactory.getEsSearchThrottlesMap();
        List<EsThrottle> esSearchThrottles = new ArrayList<>(maps.size());
        for (Map.Entry<String, EsThrottle> entry : maps.entrySet()) {
            esSearchThrottles.add(entry.getValue());
        }
        return esSearchThrottles;
    }

    @Override
    public EsThrottle getByClusterName(String clusterName) {

        return EsThrottleFactory.getEsSearchThrottleByClusteName(clusterName);
    }

    @Override
    public EsThrottle updateEsSearchThrottle(EsThrottle esSearchThrottle) {
        return EsThrottleFactory.updateByEsSearchThrottle(esSearchThrottle, true);
    }

}
