package com.smeservicemanager.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdditionalReportService {
    private final AdditionalReportRepository repository;

    public record Row(List<String> cells, BigDecimal n1, BigDecimal n2, int flag) {}
    public record Metric(String label, String value) {}
    public record Result(AdditionalReportCatalog.Definition definition, List<Row> rows,
                         List<Metric> metrics, int total, int page, int pages) {}

    @Transactional(readOnly = true)
    public Result load(String code, LocalDate start, LocalDate end, int page, boolean all) {
        var definition = AdditionalReportCatalog.require(code);
        if (start == null || end == null || end.isBefore(start) || end.equals(LocalDate.MAX))
            throw new IllegalArgumentException("ช่วงวันที่รายงานไม่ถูกต้อง");
        List<Row> rows = repository.find(definition.code(), start, end).stream()
                .map(values -> row(definition.code(), values)).toList();
        BigDecimal sum1 = rows.stream().map(Row::n1).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sum2 = rows.stream().map(Row::n2).reduce(BigDecimal.ZERO, BigDecimal::add);
        long flagged = rows.stream().filter(r -> r.flag() > 0).count();
        List<String> values = switch (definition.code()) {
            case "OVERDUE" -> List.of(number(rows.size()), number(flagged), number(sum1), money(sum2));
            case "APPOINTMENT" -> List.of(number(rows.size()), number(flagged), number(sum1), number(sum2));
            case "PAYMENT" -> List.of(number(rows.size()), money(sum1), money(sum2), money(sum1.subtract(sum2)));
            case "EXPENSE" -> List.of(number(rows.size()), number(rows.stream().map(r -> r.cells().get(0)).distinct().count()), number(sum1), money(sum2));
            case "TECHNICIAN" -> List.of(number(rows.size()), number(rows.stream().mapToInt(Row::flag).sum()), number(sum1), number(sum2));
            case "CUSTOMER" -> List.of(number(rows.size()), number(sum1), number(flagged), money(sum2));
            case "REWORK" -> List.of(number(rows.size()), number(flagged), number(sum1), money(sum2));
            case "STOCK" -> List.of(number(rows.size()), number(sum1), number(sum2), number(rows.stream().filter(r -> r.flag()>0).map(r -> r.cells().get(0)).distinct().count()));
            default -> throw new IllegalArgumentException("ไม่พบรายงานที่เลือก");
        };
        List<Metric> metrics = new ArrayList<>();
        for (int i = 0; i < 4; i++) metrics.add(new Metric(definition.metrics().get(i), values.get(i)));
        int pages = Math.max(1, (rows.size() + 9) / 10);
        int safePage = Math.max(0, Math.min(page, pages - 1));
        List<Row> selected = all ? rows : rows.subList(safePage * 10, Math.min(rows.size(), (safePage + 1) * 10));
        return new Result(definition, selected, metrics, rows.size(), safePage, pages);
    }

    private Row row(String code, Map<String, Object> values) {
        List<String> cells = new ArrayList<>(6);
        for (int i = 1; i <= 6; i++) cells.add(labelCell(code, i, String.valueOf(values.getOrDefault("c" + i, ""))));
        return new Row(cells, decimal(values.get("n1")), decimal(values.get("n2")),
                ((Number) values.get("flag")).intValue());
    }

    private static String labelCell(String code, int column, String value) {
        if (column == 6 && (code.equals("OVERDUE") || code.equals("REWORK"))) return switch (value) {
            case "NEW" -> "ใหม่";
            case "SCHEDULED" -> "นัดหมาย";
            case "ASSIGNED" -> "มอบหมาย";
            case "IN_PROGRESS" -> "กำลังทำ";
            case "WAITING_PART" -> "รออะไหล่";
            case "COMPLETED" -> "เสร็จสิ้น";
            case "CANCELLED" -> "ยกเลิก";
            default -> value;
        };
        if (column == 5 && code.equals("PAYMENT")) {
            return value.replace("CASH", "เงินสด").replace("TRANSFER", "โอนเงิน")
                    .replace("PROMPTPAY", "พร้อมเพย์").replace("CREDIT_CARD", "บัตรเครดิต")
                    .replace("OTHER", "อื่น ๆ");
        }
        return value;
    }

    private static BigDecimal decimal(Object value) {
        return value instanceof BigDecimal amount ? amount : new BigDecimal(value.toString());
    }

    private static String number(long value) { return NumberFormat.getIntegerInstance(Locale.US).format(value); }
    private static String number(BigDecimal value) { return NumberFormat.getNumberInstance(Locale.US).format(value); }
    private static String money(BigDecimal value) {
        var format = NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return "฿" + format.format(value);
    }
}
