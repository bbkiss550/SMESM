package com.smeservicemanager.customer;

import com.smeservicemanager.job.JobRepository;
import com.smeservicemanager.shared.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customers")
public class CustomerController {
    private final CustomerRepository customers;
    private final CustomerService service;
    private final JobRepository jobs;

    public CustomerController(CustomerRepository customers, CustomerService service, JobRepository jobs) {
        this.customers = customers; this.service = service; this.jobs = jobs;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue="") String q, @RequestParam(defaultValue="0") int page, Model model) {
        model.addAttribute("customers", customers.search(q, PageRequest.of(page, 10))); model.addAttribute("q", q);
        model.addAttribute("customerForm", new CustomerForm()); return "customer/list";
    }

    @PostMapping
    public String create(@Valid CustomerForm form, BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) { redirect.addFlashAttribute("errorMessage", "กรุณาตรวจสอบข้อมูลลูกค้า"); return "redirect:/customers"; }
        Customer customer = service.create(form); redirect.addFlashAttribute("successMessage", "เพิ่มลูกค้าเรียบร้อยแล้ว");
        return "redirect:/customers/" + customer.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Customer customer = customers.findOneWithPhonesById(id).orElseThrow(() -> new ResourceNotFoundException("ไม่พบลูกค้า"));
        model.addAttribute("customer", customer); model.addAttribute("jobs", jobs.findTop20ByCustomerIdOrderByAppointmentStartDesc(id));
        return "customer/detail";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes redirect) {
        service.toggle(id); redirect.addFlashAttribute("successMessage", "อัปเดตสถานะลูกค้าแล้ว"); return "redirect:/customers/" + id;
    }
}
