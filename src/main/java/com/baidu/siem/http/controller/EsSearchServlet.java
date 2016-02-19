package com.baidu.siem.http.controller;

import com.baidu.siem.exchange.Response;
import com.baidu.siem.http.manager.ESWrapperManager;
import com.baidu.siem.model.SearchModel;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 纯servlets实现，旨在对比spring性能
 * Created by yuxuefeng on 15/9/19.
 */
public class EsSearchServlet extends HttpServlet {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(EsSearchServlet.class);
    @Autowired
    private ESWrapperManager esWrapperManager;
//    private ESWrapperManager esWrapperManager = new ESWrapperManagerImpl();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Response respone = esWrapperManager.search(new SearchModel(request));
        response.getWriter().write(respone.getMsg());
    }
}
