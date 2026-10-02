package com.smeservicemanager.report;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReportController {
    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("reportGroups", ReportCatalog.groups());
        return "report/index";
    }
}
