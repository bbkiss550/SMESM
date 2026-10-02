package com.smeservicemanager.dashboard;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
public class DashboardController {
    private final DashboardMetrics metrics;
    private final DashboardOverviewService overview;

    public DashboardController(DashboardMetrics metrics, DashboardOverviewService overview) { this.metrics = metrics; this.overview = overview; }

    @GetMapping("/dashboard/overview")
    @org.springframework.web.bind.annotation.ResponseBody
    public DashboardOverviewService.Overview overview(@RequestParam(defaultValue = "DAILY") DashboardPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return overview.overview(period, date == null ? LocalDate.now(ZoneId.of("Asia/Bangkok")) : date);
    }

    @GetMapping("/dashboard/details")
    public String details(@RequestParam String kind, @RequestParam(defaultValue = "current") String scope,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(defaultValue = "0") int page, Model model) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Bangkok"));
        model.addAttribute("details", metrics.details(kind, scope, date == null ? today : date, month == null ? YearMonth.from(today) : month, page));
        model.addAttribute("revenueDetails", kind.equals("REVENUE"));
        model.addAttribute("detailDateLabel", metrics.dateLabel(kind));
        return "dashboard/details :: content";
    }

    @GetMapping("/dashboard/cards")
    public String cards(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month, Model model) {
        prepareCards(model, date, month);
        return "dashboard/cards :: content";
    }

    private void prepareCards(Model model, LocalDate date, YearMonth month) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Bangkok"));
        LocalDate selected = date == null ? today : date;
        YearMonth selectedMonth = month == null ? YearMonth.from(today) : month;
        model.addAttribute("selectedDate", selected); model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("monthNames", java.util.stream.IntStream.rangeClosed(1,12).mapToObj(i -> YearMonth.of(2026,i).atDay(1).format(DateTimeFormatter.ofPattern("MMMM",Locale.forLanguageTag("th-TH")))).toList());
        model.addAttribute("dayCards", metrics.cards("day", selected, selectedMonth));
        model.addAttribute("monthCards", metrics.cards("month", selected, selectedMonth));
        model.addAttribute("currentCards", metrics.cards("current", selected, selectedMonth));
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("overview", overview.overview(DashboardPeriod.DAILY, LocalDate.now(ZoneId.of("Asia/Bangkok"))));
        return "dashboard/index";
    }
}
