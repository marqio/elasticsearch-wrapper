package com.baidu.siem.monitor;

import com.baidu.siem.invoker.Invoker;
import org.springframework.stereotype.Component;

/**
 *
 * Created by yuxuefeng on 15/9/21.
 */
@Component("searchMonitorFactory")
public class ESMonitorFactory extends AbstractMonitorFactory {
    @Override
    protected Monitor createMonitor(Invoker invoker) {

        return new ESMonitor(invoker.getEsSearchThrottle());
    }
}
