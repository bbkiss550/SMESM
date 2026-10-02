package com.smeservicemanager.dashboard;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.chrono.ThaiBuddhistDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DashboardOverviewService {
    private final DashboardOverviewRepository repository;
    private static final Locale THAI = Locale.forLanguageTag("th-TH");
    private static final List<String> STATUS_KEYS = List.of("PENDING", "IN_PROGRESS", "WAITING_PART", "COMPLETED", "CANCELLED", "UNKNOWN");
    private static final List<String> STATUS_LABELS = List.of("รอดำเนินการ", "กำลังดำเนินการ", "รออะไหล่", "เสร็จสิ้น", "ยกเลิก", "ไม่มีประวัติสถานะย้อนหลัง");
    private static final List<String> COLORS = List.of("#ff5b60", "#ffbd22", "#2a94ff", "#24bb89", "#94a3b8", "#a579ed");

    public DashboardOverviewService(DashboardOverviewRepository repository) { this.repository = repository; }

    public record Group(String label, long value, BigDecimal percentage, String color) {}
    public record Kpi(String key, String label, Number value, Number previous, BigDecimal change, String color, String icon, boolean inverse, boolean money, List<? extends Number> sparkline) {}
    public record Series(String label, String color, List<Long> values) {}
    public record Overview(String period, LocalDate date, String label, String todayLabel, String comparisonLabel,
                           List<Kpi> cards, long total, List<Group> statuses, List<Group> services,
                           List<String> trendLabels, List<Series> trend, boolean historicalLimitations) {}

    static class Totals {
        final Map<String, Long> metrics = new HashMap<>(), statuses = new LinkedHashMap<>(), services = new LinkedHashMap<>();
        long get(String key) { return metrics.getOrDefault(key, 0L); }
        void add(String key, long value) { metrics.merge(key, value, Long::sum); }
    }

    public Overview overview(DashboardPeriod period, LocalDate date) {
        return overview(period, date, LocalDateTime.now(ZoneId.of("Asia/Bangkok")));
    }

    Overview overview(DashboardPeriod period, LocalDate date, LocalDateTime now) {
        var windows = period.windows(date);
        Map<String, Totals> totals = new HashMap<>();
        for (var row : repository.aggregate(windows, now)) {
            Totals value = totals.computeIfAbsent((String) row.get("key"), k -> new Totals());
            for (String metric : List.of("total", "opened", "carried", "completed", "cancelled", "overdue", "unknown")) value.add(metric, number(row.get(metric)));
            String status = groupStatus((String) row.get("snapshot_status"));
            value.statuses.merge(status, number(row.get("total")), Long::sum);
            value.services.merge((String) row.get("s_service_name"), number(row.get("total")), Long::sum);
        }
        Totals selected = totals.getOrDefault("selected", new Totals()), previous = totals.getOrDefault("previous", new Totals());
        var trendWindows = windows.subList(2, windows.size());
        String suffix = switch (period) { case DAILY -> "ในวัน"; case MONTHLY -> "ในเดือน"; case YEARLY -> "ในปี"; };
        var cards = new ArrayList<Kpi>();
        String[][] definitions = {
            {"total", "งานทั้งหมด", "#258cff", "file-earmark-text", "false"},
            {"opened", "งานใหม่" + suffix + "ที่เลือก", "#24bb89", "file-earmark-plus", "false"},
            {"carried", "งานค้างยกมา", "#f5a623", "clock-history", "true"},
            {"completed", "เสร็จสิ้น" + suffix + "ที่เลือก", "#985cf6", "calendar-check", "false"},
            {"overdue", "งานเกินกำหนด", "#ff5b60", "exclamation-circle-fill", "true"},
            {"waiting", "รออะไหล่", "#258cff", "pause-circle", "true"},
            {"cancelled", "ยกเลิก" + suffix + "ที่เลือก", "#94a3b8", "x-circle-fill", "true"},
            {"progress", "กำลังดำเนินการ", "#f5a623", "gear-fill", "false"},
            {"pending", "รอดำเนินการ", "#ff5b60", "hourglass-split", "true"}
        };
        for (var def : definitions) {
            long current = metric(selected, def[0]), before = metric(previous, def[0]);
            cards.add(new Kpi(def[0], def[1], current, before, change(current, before), def[2], def[3], Boolean.parseBoolean(def[4]), false,
                trendWindows.stream().map(w -> metric(totals.getOrDefault(w.key(), new Totals()), def[0])).toList()));
        }
        Map<String, BigDecimal> revenue = repository.revenue(windows, now);
        BigDecimal received = revenue.getOrDefault("selected", BigDecimal.ZERO), receivedBefore = revenue.getOrDefault("previous", BigDecimal.ZERO);
        cards.add(new Kpi("revenue", "รายได้รวม" + suffix + "ที่เลือก", received, receivedBefore,
            change(received, receivedBefore), "#24bb89", "cash-stack", false, true,
            trendWindows.stream().map(w -> revenue.getOrDefault(w.key(), BigDecimal.ZERO)).toList()));
        List<Group> statusGroups = new ArrayList<>();
        List<Series> series = new ArrayList<>();
        for (int i = 0; i < STATUS_KEYS.size(); i++) {
            String key = STATUS_KEYS.get(i), label = STATUS_LABELS.get(i), color = COLORS.get(i);
            long count = selected.statuses.getOrDefault(key, 0L);
            if (count > 0 || !key.equals("UNKNOWN")) statusGroups.add(new Group(label, count, percentage(count, selected.get("total")), color));
            List<Long> values = trendWindows.stream().map(w -> totals.getOrDefault(w.key(), new Totals()).statuses.getOrDefault(key, 0L)).toList();
            if (!key.equals("UNKNOWN") || values.stream().anyMatch(v -> v > 0)) series.add(new Series(label, color, values));
        }
        List<Group> services = new ArrayList<>();
        var ordered = selected.services.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey())).toList();
        long other = 0;
        for (int i = 0; i < ordered.size(); i++) {
            var entry = ordered.get(i);
            if (i >= 5) other += entry.getValue();
            else services.add(new Group(entry.getKey(), entry.getValue(), percentage(entry.getValue(), selected.get("total")), COLORS.get(i)));
        }
        if (other > 0) services.add(new Group("บริการอื่น ๆ", other, percentage(other, selected.get("total")), COLORS.get(5)));
        String format = switch (period) { case DAILY -> "d MMMM yyyy"; case MONTHLY -> "MMMM yyyy"; case YEARLY -> "yyyy"; };
        String label = ThaiBuddhistDate.from(date).format(DateTimeFormatter.ofPattern(format, THAI));
        return new Overview(period.name(), date, label, ThaiBuddhistDate.from(now.toLocalDate()).format(DateTimeFormatter.ofPattern("EEEEที่ d MMMM yyyy", THAI)),
            switch (period) { case DAILY -> "เทียบวันก่อนหน้า"; case MONTHLY -> "เทียบเดือนก่อนหน้า"; case YEARLY -> "เทียบปีก่อนหน้า"; },
            cards, selected.get("total"), statusGroups, services,
            trendWindows.stream().map(w -> w.start().format(DateTimeFormatter.ofPattern(period == DashboardPeriod.YEARLY ? "MMM" : "d MMM", THAI))).toList(),
            series, totals.values().stream().anyMatch(t -> t.get("unknown") > 0) || date.isBefore(now.toLocalDate()));
    }

    static String groupStatus(String status) {
        return switch (status) { case "NEW", "SCHEDULED", "ASSIGNED" -> "PENDING"; default -> status; };
    }
    static long metric(Totals totals, String key) {
        return switch (key) {
            case "waiting" -> totals.statuses.getOrDefault("WAITING_PART", 0L);
            case "progress" -> totals.statuses.getOrDefault("IN_PROGRESS", 0L);
            case "pending" -> totals.statuses.getOrDefault("PENDING", 0L);
            default -> totals.get(key);
        };
    }
    static BigDecimal change(long current, long previous) {
        return change(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }
    static BigDecimal change(BigDecimal current, BigDecimal previous) {
        return previous.signum() == 0 ? null : current.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 1, RoundingMode.HALF_UP);
    }
    static BigDecimal percentage(long count, long total) {
        return total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
    private static long number(Object value) { return value == null ? 0 : ((Number) value).longValue(); }
}
