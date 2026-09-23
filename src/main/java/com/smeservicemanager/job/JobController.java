package com.smeservicemanager.job;

import com.smeservicemanager.catalog.ProductRepository;
import com.smeservicemanager.catalog.ServiceCatalogRepository;
import com.smeservicemanager.customer.CustomerRepository;
import com.smeservicemanager.payment.*;
import com.smeservicemanager.security.UserRepository;
import com.smeservicemanager.shared.domain.DomainTypes.*;
import com.smeservicemanager.shared.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/jobs")
public class JobController {
    private final JobRepository jobs;
    private final JobService jobService;
    private final CustomerRepository customers;
    private final ServiceCatalogRepository services;
    private final ProductRepository products;
    private final UserRepository users;
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final PaymentService paymentService;
    private final CancellationService cancellationService;

    public JobController(JobRepository jobs, JobService jobService, CustomerRepository customers,
                         ServiceCatalogRepository services, ProductRepository products, UserRepository users,
                         PaymentRepository payments, RefundRepository refunds, PaymentService paymentService,
                         CancellationService cancellationService) {
        this.jobs = jobs; this.jobService = jobService; this.customers = customers; this.services = services;
        this.products = products; this.users = users; this.payments = payments; this.refunds = refunds; this.paymentService = paymentService;
        this.cancellationService = cancellationService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q, @RequestParam(required = false) JobStatus status,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("jobs", jobs.search(q, status, PageRequest.of(page, 10)));
        model.addAttribute("q", q); model.addAttribute("selectedStatus", status); model.addAttribute("statuses", JobStatus.values());
        prepareForm(model, new JobForm());
        model.addAttribute("modalMode", true);
        return "job/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long customerId,
                             @RequestParam(required = false) Long originalJobId, Model model) {
        JobForm form = new JobForm(); form.setCustomerId(customerId); form.setOriginalJobId(originalJobId);
        prepareForm(model, form); model.addAttribute("modalMode", false); return "job/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("jobForm") JobForm form, BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) { prepareForm(model, form); return "job/form"; }
        Job job = jobService.create(form); redirect.addFlashAttribute("successMessage", "สร้างใบงานเรียบร้อยแล้ว");
        return "redirect:/jobs/" + job.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Job job = jobService.requireDetailed(id); model.addAttribute("job", job);
        model.addAttribute("products", products.findByStatusOrderByProductNameAsc("A"));
        model.addAttribute("technicians", users.findActiveTechnicians());
        model.addAttribute("itemForm", new JobItemForm()); model.addAttribute("paymentForm", new PaymentForm()); model.addAttribute("refundForm", new RefundForm());
        model.addAttribute("cancellationForm", new CancellationForm());
        model.addAttribute("payments", payments.findByJobIdAndRecordStatusOrderByPaymentDateDesc(id, "ACTIVE"));
        model.addAttribute("refunds", refunds.findByJobIdOrderByRefundDateDesc(id));
        model.addAttribute("paid", paymentService.totalPaid(id)); model.addAttribute("refunded", paymentService.totalRefunded(id));
        return "job/detail";
    }

    @PostMapping("/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam Long technicianId, RedirectAttributes redirect) {
        jobService.assign(id, technicianId); success(redirect, "มอบหมายหัวหน้าช่างเรียบร้อยแล้ว"); return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id, @RequestParam JobStatus status, @RequestParam(required = false) String reason,
                         RedirectAttributes redirect) { jobService.transition(id, status, reason); success(redirect, "อัปเดตสถานะเรียบร้อยแล้ว"); return "redirect:/jobs/" + id; }

    @PostMapping("/{id}/items")
    public String addItem(@PathVariable Long id, @Valid JobItemForm form, BindingResult binding, RedirectAttributes redirect) {
        rejectInvalid(binding, "กรุณาตรวจสอบข้อมูลรายการ");
        jobService.addItem(id, form); success(redirect, "เพิ่มรายการเรียบร้อยแล้ว"); return "redirect:/jobs/" + id;
    }

    @PostMapping("/{jobId}/items/{itemId}/remove")
    public String removeItem(@PathVariable Long jobId, @PathVariable Long itemId, RedirectAttributes redirect) {
        jobService.removeItem(jobId, itemId); success(redirect, "นำรายการออกและปรับสต๊อกแล้ว"); return "redirect:/jobs/" + jobId;
    }

    @PostMapping("/{id}/attachments")
    public String attachment(@PathVariable Long id, @RequestParam AttachmentType type, @RequestParam MultipartFile file,
                             RedirectAttributes redirect) { jobService.addAttachment(id, type, file); success(redirect, "อัปโหลดไฟล์เรียบร้อยแล้ว"); return "redirect:/jobs/" + id; }

    @PostMapping("/{id}/payments")
    public String payment(@PathVariable Long id, @Valid PaymentForm form, BindingResult binding, RedirectAttributes redirect) {
        rejectInvalid(binding, "กรุณาตรวจสอบข้อมูลการชำระเงิน");
        paymentService.record(id, form); success(redirect, "บันทึกการชำระเงินเรียบร้อยแล้ว"); return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id}/refunds")
    public String refund(@PathVariable Long id, @Valid RefundForm form, BindingResult binding, RedirectAttributes redirect) {
        rejectInvalid(binding, "กรุณาตรวจสอบข้อมูลการคืนเงิน");
        paymentService.refund(id, form); success(redirect, "บันทึกการคืนเงินเรียบร้อยแล้ว"); return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, @Valid CancellationForm form, BindingResult binding, RedirectAttributes redirect) {
        rejectInvalid(binding, "กรุณาระบุเหตุผลที่ยกเลิกงาน");
        cancellationService.cancel(id, form); success(redirect, "ยกเลิกใบงานและดำเนินการทางการเงินเรียบร้อยแล้ว"); return "redirect:/jobs/" + id;
    }

    private void prepareForm(Model model, JobForm form) {
        model.addAttribute("jobForm", form); model.addAttribute("customers", customers.findTop50ByStatusOrderByNameAsc("A"));
        model.addAttribute("services", services.findByStatusOrderByServiceNameAsc("A")); model.addAttribute("technicians", users.findActiveTechnicians());
        model.addAttribute("priorities", JobPriority.values()); model.addAttribute("vatModes", VatMode.values());
    }

    private void success(RedirectAttributes redirect, String message) { redirect.addFlashAttribute("successMessage", message); }

    private void rejectInvalid(BindingResult binding, String message) {
        if (binding.hasErrors()) throw new BusinessException(message);
    }
}
