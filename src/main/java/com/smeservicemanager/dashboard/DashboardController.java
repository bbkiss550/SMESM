package com.smeservicemanager.dashboard;

import com.smeservicemanager.job.JobRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
public class DashboardController {
    private final DashboardService dashboard;
    private final JobRepository jobs;

    public DashboardController(DashboardService dashboard, JobRepository jobs) { this.dashboard = dashboard; this.jobs = jobs; }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("summary", dashboard.summary());
        model.addAttribute("monthlyJobs", dashboard.monthlyJobs());
        model.addAttribute("monthlyRevenue", dashboard.monthlyRevenue());
        model.addAttribute("popularServices", dashboard.popularServices());
        model.addAttribute("recentJobs", jobs.findTop8ByOrderByCreateDateDesc());
        model.addAttribute("todayLabel", LocalDate.now().format(
                DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("th-TH"))));
        return "dashboard/index";
    }
}
