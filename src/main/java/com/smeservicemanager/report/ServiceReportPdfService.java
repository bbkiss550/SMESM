package com.smeservicemanager.report;

import com.smeservicemanager.catalog.ServiceCatalogRepository;
import com.smeservicemanager.job.Job;
import com.smeservicemanager.security.UserRepository;
import com.smeservicemanager.settings.SettingRepository;
import com.smeservicemanager.shared.domain.DomainTypes.JobStatus;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ServiceReportPdfService {
    private final ServiceReportRepository repository;
    private final ServiceCatalogRepository services;
    private final UserRepository users;
    private final SettingRepository settings;
    private volatile JasperReport template;
    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");

    @Transactional(readOnly = true)
    public byte[] render(LocalDate start, LocalDate end, ServiceReportRepository.DateBasis basis,
                         JobStatus status, Long serviceId, Long technicianId) {
        if (start == null || end == null || end.isBefore(start) || end.equals(LocalDate.MAX))
            throw new IllegalArgumentException("ช่วงวันที่รายงานไม่ถูกต้อง");
        List<Job> jobs = repository.findForPrint(start, end, basis, status, serviceId, technicianId);
        long completed = jobs.stream().filter(j -> j.getJobStatus() == JobStatus.COMPLETED).count();
        long cancelled = jobs.stream().filter(j -> j.getJobStatus() == JobStatus.CANCELLED).count();
        BigDecimal value = jobs.stream().filter(j -> j.getJobStatus() != JobStatus.CANCELLED)
                .map(Job::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> params = new HashMap<>();
        params.put("companyName", setting("company.name", "SME Service Manager"));
        params.put("companyAddress", setting("company.address", ""));
        params.put("period", thaiDate(start) + " – " + thaiDate(end));
        params.put("periodDays", "รวม " + (ChronoUnit.DAYS.between(start, end) + 1) + " วัน");
        params.put("basisLabel", switch (basis) {
            case CREATED -> "วันที่สร้างใบงาน";
            case APPOINTMENT -> "วันที่นัดหมาย";
            case COMPLETED -> "วันที่ปิดงาน";
        });
        String service = serviceId == null ? "ทุกบริการ" : services.findById(serviceId)
                .map(s -> s.getServiceName()).orElse("บริการ #" + serviceId);
        String technician = technicianId == null ? "ทุกคน" : users.findById(technicianId)
                .map(u -> u.getFullName()).orElse("หัวหน้าช่าง #" + technicianId);
        params.put("statusFilter", status == null ? "ทุกสถานะ" : statusLabel(status));
        params.put("serviceFilter", service);
        params.put("technicianFilter", technician);
        params.put("totalCount", Long.toString(jobs.size()));
        params.put("completedCount", Long.toString(completed));
        params.put("cancelledCount", Long.toString(cancelled));
        params.put("valueAmount", "฿" + money(value));
        params.put("generatedAt", "พิมพ์เมื่อ " + thaiDateTime(LocalDateTime.now(BANGKOK)));
        List<Map<String, ?>> rows = new ArrayList<>(jobs.size());
        for (Job job : jobs) {
            LocalDateTime date = switch (basis) {
                case CREATED -> job.getCreateDate();
                case APPOINTMENT -> job.getAppointmentStart();
                case COMPLETED -> job.getCompletedDate();
            };
            rows.add(Map.of("jobNo", job.getJobNo(), "customer", job.getCustomer().getDisplayName(),
                    "service", job.getService().getServiceName(),
                    "technician", job.getTechnician() == null ? "ยังไม่มอบหมาย" : job.getTechnician().getFullName(),
                    "date", date == null ? "–" : thaiDateTime(date), "status", shortStatusLabel(job.getJobStatus()),
                    "amount", money(job.getGrandTotal())));
        }
        try {
            var print = JasperFillManager.fillReport(template(), params, new JRMapCollectionDataSource(rows));
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException | IOException ex) {
            throw new IllegalStateException("สร้าง PDF รายงานงานบริการไม่สำเร็จ", ex);
        }
    }
    private JasperReport template() throws JRException, IOException {
        if (template == null) synchronized (this) {
            if (template == null) try (var source = new ClassPathResource("report/service-report.jrxml").getInputStream()) {
                template = JasperCompileManager.compileReport(source);
            }
        }
        return template;
    }
    private static String thaiDate(LocalDate date) {
        return "%02d/%02d/%04d".formatted(date.getDayOfMonth(), date.getMonthValue(), date.getYear() + 543);
    }
    private String setting(String key, String fallback) {
        return settings.findByKey(key).map(s -> s.getValue())
                .filter(value -> !value.isBlank()).orElse(fallback);
    }
    private static String thaiDateTime(LocalDateTime date) {
        return thaiDate(date.toLocalDate()) + " " + date.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
    private static String money(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(2); format.setMaximumFractionDigits(2);
        return format.format(amount);
    }
    private static String statusLabel(JobStatus status) {
        return switch (status) {
            case NEW -> "ใหม่";
            case SCHEDULED -> "นัดหมายแล้ว";
            case ASSIGNED -> "มอบหมายแล้ว";
            case IN_PROGRESS -> "กำลังดำเนินการ";
            case WAITING_PART -> "รออะไหล่";
            case COMPLETED -> "เสร็จสิ้น";
            case CANCELLED -> "ยกเลิก";
        };
    }
    private static String shortStatusLabel(JobStatus status) {
        return switch (status) {
            case NEW -> "ใหม่";
            case SCHEDULED -> "นัดหมาย";
            case ASSIGNED -> "มอบหมาย";
            case IN_PROGRESS -> "กำลังทำ";
            case WAITING_PART -> "รออะไหล่";
            case COMPLETED -> "เสร็จสิ้น";
            case CANCELLED -> "ยกเลิก";
        };
    }
}
