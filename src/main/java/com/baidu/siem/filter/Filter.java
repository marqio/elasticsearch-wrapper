package com.baidu.siem.filter;

import com.baidu.siem.exchange.Response;
import com.baidu.siem.exception.InvokeException;
import com.baidu.siem.invoker.Invoker;

/**
 * Created by yuxuefeng on 15/9/21.
 */
public interface Filter {
    /**
     * do invoke filter.
     * <p/>
     * <code>
     * // before filter
     * Result result = invoker.invoke(invocation);
     * // after filter
     * return result;
     * </code>
     *
     * @param invoker    service
     * @return invoke result.
     */
    Response invoke(Invoker invoker) throws InvokeException;
}
