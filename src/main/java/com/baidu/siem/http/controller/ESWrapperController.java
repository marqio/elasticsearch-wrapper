package com.baidu.siem.http.controller;

import com.baidu.siem.exchange.Response;
import com.baidu.siem.http.manager.ESBatchManager;
import com.baidu.siem.http.manager.ESWrapperManager;
import com.baidu.siem.model.SearchModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;

/**
 * ES wrapper
 * Created by Jason峰 on 2014/12/26.
 */
@Controller
public class ESWrapperController extends MyBaseController {

    @Autowired
    private ESWrapperManager esWrapperManager;
    @Autowired
    private ESBatchManager esBatchManager;


    //    @ResponseBody
//    @RequestMapping(value = "/**", produces = "application/json;charset=utf-8")
    @RequestMapping(value = "/esRoot")
    public void es(Model model, HttpServletRequest request, HttpServletResponse res) throws ParseException, IOException {
        //TODO 暂时解决tomcat 根目录兼容性问题
        Response response = esWrapperManager.root(new SearchModel(request));

        res.setStatus(response.getStatus());
        res.setContentType(response.getContentType());

        res.getWriter().print(response.getMsg());
        res.getWriter().flush();

    }

    @RequestMapping(value = "/**/_source")
    public void source(Model model, HttpServletRequest request, HttpServletResponse res) throws ParseException, IOException {
//        logger.info("source request:" + request.getRequestURI());
        Response response = esWrapperManager.search(new SearchModel(request));

        res.setStatus(response.getStatus());
        res.setContentType(response.getContentType());

        res.getWriter().print(response.getMsg());
        res.getWriter().flush();

    }

    @RequestMapping(value = "/**/_mget")
    public void mget(Model model, HttpServletRequest request, HttpServletResponse res) throws ParseException, IOException {
//        logger.info("mget request:" + request.getRequestURI());
        Response response = esWrapperManager.search(new SearchModel(request));

        res.setStatus(response.getStatus());
        res.setContentType(response.getContentType());

        res.getWriter().print(response.getMsg());
        res.getWriter().flush();

    }

    @RequestMapping(value = "/**/_search")
    public void search(Model model, HttpServletRequest request, HttpServletResponse res) throws ParseException, IOException {
//        logger.info("search request:" + request.getRequestURI());
        Response response = esWrapperManager.search(new SearchModel(request));

        res.setStatus(response.getStatus());
        res.setContentType(response.getContentType());

        res.getWriter().print(response.getMsg());
        res.getWriter().flush();

    }

    @RequestMapping(value = "/**/_msearch")
    public void msearch(Model model, HttpServletRequest request, HttpServletResponse res) throws ParseException, IOException {
//        logger.info("msearch request:" + request.getRequestURI());
        Response response = esBatchManager.search(new SearchModel(request));

        res.setStatus(response.getStatus());
        res.setContentType(response.getContentType());

        res.getWriter().print(response.getMsg());
        res.getWriter().flush();

    }

    @RequestMapping(value = "/**/_bulk")
    public void bulk(Model model, HttpServletRequest request, HttpServletResponse res) throws ParseException, IOException {

        Response response = esBatchManager.write(new SearchModel(request));

        res.setStatus(response.getStatus());
        res.setContentType(response.getContentType());

        res.getWriter().print(response.getMsg());
        res.getWriter().flush();

    }

    //查询操作
    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.HEAD})
    public void get(Model model, HttpServletRequest request, HttpServletResponse res) throws ParseException, IOException {
//        logger.info("Http get request:" + request.getRequestURI());
        Response response = esWrapperManager.search(new SearchModel(request));

        res.setStatus(response.getStatus());
        res.setContentType(response.getContentType());

        res.getWriter().print(response.getMsg());
        res.getWriter().flush();

    }

    //写入操作
    @RequestMapping(value = "/**")
    public void searchAll(Model model, HttpServletRequest request, HttpServletResponse res) throws ParseException, IOException {

        Response response = esWrapperManager.write(new SearchModel(request));

        res.setStatus(response.getStatus());
        res.setContentType(response.getContentType());

        res.getWriter().print(response.getMsg());
        res.getWriter().flush();

    }

}
