package com.smeservicemanager.job;

import com.smeservicemanager.catalog.ProductRepository;
import com.smeservicemanager.shared.domain.DomainTypes.*;
import com.smeservicemanager.shared.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/my-jobs")
public class TechnicianJobController {
    private final JobRepository jobs;
    private final JobService service;
    private final ProductRepository products;

    public TechnicianJobController(JobRepository jobs, JobService service, ProductRepository products) {
        this.jobs = jobs; this.service = service; this.products = products;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        model.addAttribute("jobs", jobs.findByTechnicianUsernameAndJobStatusInOrderByAppointmentStartAsc(authentication.getName(),
                List.of(JobStatus.ASSIGNED, JobStatus.IN_PROGRESS, JobStatus.WAITING_PART, JobStatus.COMPLETED)));
        return "technician/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("job", service.requireDetailed(id)); model.addAttribute("products", products.findByStatusOrderByProductNameAsc("A"));
        model.addAttribute("itemForm", new JobItemForm()); return "technician/detail";
    }

    @PostMapping("/{id}/accept") public String accept(@PathVariable Long id, RedirectAttributes r) { service.accept(id); r.addFlashAttribute("successMessage", "รับงานแล้ว"); return "redirect:/my-jobs/" + id; }
    @PostMapping("/{id}/status") public String status(@PathVariable Long id, @RequestParam JobStatus status, @RequestParam(required=false) String reason, RedirectAttributes r) { service.transition(id, status, reason); r.addFlashAttribute("successMessage", "อัปเดตสถานะแล้ว"); return "redirect:/my-jobs/" + id; }
    @PostMapping("/{id}/items") public String item(@PathVariable Long id, @Valid JobItemForm form, BindingResult binding, RedirectAttributes r) {
        if (binding.hasErrors()) throw new BusinessException("กรุณาตรวจสอบข้อมูลรายการ");
        service.addItem(id, form); r.addFlashAttribute("successMessage", "เพิ่มอะไหล่แล้ว"); return "redirect:/my-jobs/" + id;
    }
    @PostMapping("/{id}/attachments") public String attachment(@PathVariable Long id, @RequestParam AttachmentType type, @RequestParam MultipartFile file, RedirectAttributes r) { service.addAttachment(id, type, file); r.addFlashAttribute("successMessage", "เพิ่มรูปแล้ว"); return "redirect:/my-jobs/" + id; }
}
