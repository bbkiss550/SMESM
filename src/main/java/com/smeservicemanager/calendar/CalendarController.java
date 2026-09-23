package com.smeservicemanager.calendar;

import com.smeservicemanager.job.JobRepository;
import com.smeservicemanager.security.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import com.smeservicemanager.job.Job;

@Controller
public class CalendarController {
    private final JobRepository jobs;
    private final UserRepository users;

    public CalendarController(JobRepository jobs, UserRepository users) { this.jobs = jobs; this.users = users; }

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) LocalDate month, Model model) {
        LocalDate selected = month == null ? LocalDate.now() : month;
        LocalDateTime start = selected.withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = selected.plusMonths(1).withDayOfMonth(1).atStartOfDay();
        List<Job> monthJobs = jobs.findByAppointmentStartBetweenOrderByAppointmentStartAsc(start.minusDays(7), end.plusDays(7));
        Map<LocalDate,List<Job>> grouped = monthJobs.stream().collect(Collectors.groupingBy(j -> j.getAppointmentStart().toLocalDate()));
        LocalDate gridStart = selected.withDayOfMonth(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        List<CalendarDay> days = IntStream.range(0,42).mapToObj(i -> gridStart.plusDays(i))
                .map(date -> new CalendarDay(date, date.getMonth()==selected.getMonth(), grouped.getOrDefault(date,List.of()))).toList();
        model.addAttribute("month", selected); model.addAttribute("days", days);
        model.addAttribute("monthLabel", selected.format(
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("th-TH"))));
        model.addAttribute("weekdayNames", List.of("อาทิตย์", "จันทร์", "อังคาร", "พุธ", "พฤหัสบดี", "ศุกร์", "เสาร์"));
        model.addAttribute("technicians", users.findActiveTechnicians());
        return "calendar/index";
    }

    public record CalendarDay(LocalDate date, boolean currentMonth, List<Job> jobs) {}
}
