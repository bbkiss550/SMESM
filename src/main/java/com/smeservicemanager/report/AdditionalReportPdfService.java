package com.smeservicemanager.report;

import com.smeservicemanager.settings.SettingRepository;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdditionalReportPdfService {
    private final AdditionalReportService reports;
    private final SettingRepository settings;
    private volatile JasperReport template;

    public byte[] render(String code, LocalDate start, LocalDate end) {
        var result = reports.load(code, start, end, 0, true);
        Map<String, Object> params = new HashMap<>();
        params.put("title", "รายงาน" + result.definition().title());
        params.put("code", result.definition().code());
        params.put("companyName", setting("company.name", "SME Service Manager"));
        params.put("companyAddress", setting("company.address", ""));
        params.put("period", thaiDate(start) + " – " + thaiDate(end));
        params.put("dateLabel", result.definition().dateLabel());
        params.put("note", result.definition().note());
        params.put("generatedAt", "พิมพ์เมื่อ " + thaiDate(LocalDate.now(ZoneId.of("Asia/Bangkok"))) + " "
                + LocalDateTime.now(ZoneId.of("Asia/Bangkok")).format(DateTimeFormatter.ofPattern("HH:mm")));
        for (int i = 0; i < 6; i++) params.put("h" + (i + 1), result.definition().columns().get(i));
        for (int i = 0; i < 4; i++) {
            params.put("mLabel" + (i + 1), result.metrics().get(i).label());
            params.put("mValue" + (i + 1), result.metrics().get(i).value());
        }
        List<Map<String, ?>> rows = new ArrayList<>();
        for (var row : result.rows()) {
            Map<String, Object> cells = new HashMap<>();
            for (int i = 0; i < 6; i++) cells.put("c" + (i + 1), row.cells().get(i));
            rows.add(cells);
        }
        try {
            return JasperExportManager.exportReportToPdf(
                    JasperFillManager.fillReport(template(), params, new JRMapCollectionDataSource(rows)));
        } catch (JRException | IOException ex) {
            throw new IllegalStateException("สร้าง PDF รายงานไม่สำเร็จ", ex);
        }
    }

    private JasperReport template() throws JRException, IOException {
        if (template == null) synchronized (this) {
            if (template == null) try (var source = new ClassPathResource("report/additional-report.jrxml").getInputStream()) {
                template = JasperCompileManager.compileReport(source);
            }
        }
        return template;
    }

    private String setting(String key, String fallback) {
        return settings.findByKey(key).map(s -> s.getValue()).filter(v -> !v.isBlank()).orElse(fallback);
    }

    private static String thaiDate(LocalDate date) {
        return "%02d/%02d/%04d".formatted(date.getDayOfMonth(), date.getMonthValue(), date.getYear() + 543);
    }
}
