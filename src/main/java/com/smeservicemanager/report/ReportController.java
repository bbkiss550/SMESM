package com.smeservicemanager.report;

import com.smeservicemanager.dashboard.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReportController {
    private final DashboardService dashboard;
    public ReportController(DashboardService dashboard){this.dashboard=dashboard;}
    @GetMapping("/reports") public String reports(Model model){
        model.addAttribute("summary",dashboard.summary());model.addAttribute("revenueByDay",dashboard.revenueByDay());
        model.addAttribute("monthlyRevenue",dashboard.monthlyRevenue());model.addAttribute("jobStatuses",dashboard.jobStatusBreakdown());
        model.addAttribute("serviceBreakdown",dashboard.popularServices());model.addAttribute("technicians",dashboard.technicianPerformance());return "report/index";}
}
