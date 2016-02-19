package com.baidu.siem.admin.manager;

import com.baidu.siem.throttle.EsThrottle;

import java.util.List;

/**
 * Created by yuxuefeng on 15/9/25.
 */
public interface EsAdminManager {
    List<EsThrottle> getAllThrottles();

    EsThrottle getByClusterName(String clusterName);

    EsThrottle updateEsSearchThrottle(EsThrottle esSearchThrottle);
}
