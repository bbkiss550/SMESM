package com.smeservicemanager.report;

import com.smeservicemanager.catalog.ServiceCatalogRepository;
import com.smeservicemanager.security.UserRepository;
import com.smeservicemanager.shared.domain.DomainTypes.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class ReportController {
    private final ServiceReportService serviceReport;
    private final ServiceReportPdfService serviceReportPdf;
    private final AdditionalReportService additionalReport;
    private final AdditionalReportPdfService additionalReportPdf;
    private final ServiceCatalogRepository services;
    private final UserRepository users;
    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("reportGroups", ReportCatalog.groups());
        return "report/index";
    }

    @GetMapping("/reports/service")
    public String service(
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end,
            @RequestParam(defaultValue = "CREATED") ServiceReportRepository.DateBasis basis,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(defaultValue = "0") int page, Model model) {
        var today = LocalDate.now(ZoneId.of("Asia/Bangkok"));
        if (start == null) start = today.withDayOfMonth(1);
        if (end == null) end = today;
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        model.addAttribute("basis", basis);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("serviceId", serviceId);
        model.addAttribute("technicianId", technicianId);
        model.addAttribute("statuses", JobStatus.values());
        // Include inactive master records so historical jobs remain filterable.
        model.addAttribute("services", services.findAll(Sort.by("serviceName")));
        model.addAttribute("technicians", users.findReportTechnicians());
        try { model.addAttribute("result", serviceReport.load(start, end, basis, status, serviceId, technicianId, page)); }
        catch (IllegalArgumentException ex) { model.addAttribute("reportError", ex.getMessage()); }
        return "report/service :: detail";
    }

    @GetMapping(value = "/reports/service/pdf", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    public org.springframework.http.ResponseEntity<byte[]> servicePdf(
            @RequestParam LocalDate start, @RequestParam LocalDate end,
            @RequestParam(defaultValue = "CREATED") ServiceReportRepository.DateBasis basis,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) Long technicianId) {
        byte[] pdf = serviceReportPdf.render(start, end, basis, status, serviceId, technicianId);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=service-report.pdf")
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf);
    }

    @GetMapping(value = "/reports/service/preview", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public org.springframework.http.ResponseEntity<Map<String, String>> servicePreview(
            @RequestParam LocalDate start, @RequestParam LocalDate end,
            @RequestParam(defaultValue = "CREATED") ServiceReportRepository.DateBasis basis,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) Long technicianId) {
        byte[] pdf = serviceReportPdf.render(start, end, basis, status, serviceId, technicianId);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Map.of("pdfBase64", Base64.getEncoder().encodeToString(pdf)));
    }

    @GetMapping("/reports/detail/{code}")
    public String additional(@PathVariable String code,
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end,
            @RequestParam(defaultValue = "0") int page, Model model) {
        var today = LocalDate.now(ZoneId.of("Asia/Bangkok"));
        if (start == null) start = today.withDayOfMonth(1);
        if (end == null) end = today;
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        model.addAttribute("definition", AdditionalReportCatalog.require(code));
        try { model.addAttribute("result", additionalReport.load(code, start, end, page, false)); }
        catch (IllegalArgumentException ex) { model.addAttribute("reportError", ex.getMessage()); }
        return "report/additional :: detail";
    }

    @GetMapping(value = "/reports/detail/{code}/preview", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public org.springframework.http.ResponseEntity<Map<String, String>> additionalPreview(
            @PathVariable String code, @RequestParam LocalDate start, @RequestParam LocalDate end) {
        byte[] pdf = additionalReportPdf.render(code, start, end);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Map.of("pdfBase64", Base64.getEncoder().encodeToString(pdf)));
    }

    @GetMapping(value = "/reports/detail/{code}/pdf", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    public org.springframework.http.ResponseEntity<byte[]> additionalPdf(
            @PathVariable String code, @RequestParam LocalDate start, @RequestParam LocalDate end) {
        byte[] pdf = additionalReportPdf.render(code, start, end);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=" + code.toLowerCase(java.util.Locale.ROOT) + "-report.pdf")
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf);
    }
}
