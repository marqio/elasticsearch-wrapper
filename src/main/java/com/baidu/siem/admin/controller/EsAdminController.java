package com.baidu.siem.admin.controller;

import com.baidu.siem.admin.manager.EsAdminManager;
import com.baidu.siem.http.controller.MyBaseController;
import com.baidu.siem.model.AdminModel;
import com.baidu.siem.model.Pagination;
import com.baidu.siem.throttle.EsThrottle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Created by yuxuefeng on 15/9/24.
 */
//@Controller
//@RequestMapping("/admin")
public class EsAdminController extends MyBaseController {

    @Autowired
    private EsAdminManager esAdminManager;

    private String prefix = "/admin/" ;

    @RequestMapping(value = "/manage", method = RequestMethod.GET)
    public String manage(Model model) {

        AdminModel adminModel = new AdminModel();
        adminModel.setClusters(esAdminManager.getAllThrottles());
        model.addAttribute("queryModel", adminModel);

        return prefix + "manage";
    }

    @RequestMapping(value = "/manage", method = RequestMethod.POST)
    public String manage(@ModelAttribute("queryModel") AdminModel adminModel, Model model) {

        Pagination pagination = new Pagination();
        pagination.setResult(esAdminManager.getAllThrottles());
        model.addAttribute("pagn", pagination);

        return prefix + "queryResult";
    }

    @RequestMapping(value = "/modify/{clusterName}", method = RequestMethod.GET)
    public String mod(@PathVariable("clusterName") String clusterName, Model model) {
        EsThrottle esSearchThrottle = esAdminManager.getByClusterName(clusterName);
        model.addAttribute("queryModel", esSearchThrottle);
        return prefix + "mod";
    }

    @ResponseBody
    @RequestMapping(value = "/modify", method = RequestMethod.POST)
    public String mod(@ModelAttribute("queryModel") EsThrottle esSearchThrottle, Model model) {
        esSearchThrottle = esAdminManager.updateEsSearchThrottle(esSearchThrottle);
        return "sucess";
    }
}
