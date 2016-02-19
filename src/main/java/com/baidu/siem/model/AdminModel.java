package com.baidu.siem.model;

import com.baidu.siem.throttle.EsThrottle;

import java.util.List;

/**
 * Created by yuxuefeng on 15/9/24.
 */
public class AdminModel extends PaginModel {
    private String clusterName;
    private List<EsThrottle> clusters;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public List<EsThrottle> getClusters() {
        return clusters;
    }

    public void setClusters(List<EsThrottle> clusters) {
        this.clusters = clusters;
    }
}
