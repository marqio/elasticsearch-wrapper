package com.baidu.siem.http.manager;

import com.baidu.siem.exchange.Response;
import com.baidu.siem.model.SearchModel;

/**
 * Created by yuxuefeng on 15/10/14.
 */
public interface ESBatchManager {
    /**
     * batch查询操作
     *
     * @param searchModel
     * @return
     */
    Response search(SearchModel searchModel);

    /**
     * batch增、删、改操作
     *
     * @param searchModel
     * @return
     */
    Response write(SearchModel searchModel);
}
