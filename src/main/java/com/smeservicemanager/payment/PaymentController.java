package com.smeservicemanager.payment;

import com.smeservicemanager.settings.SettingRepository;
import com.smeservicemanager.shared.exception.ResourceNotFoundException;
import com.smeservicemanager.shared.exception.BusinessException;
import com.smeservicemanager.job.JobRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class PaymentController {
    private final PaymentRepository payments; private final RefundRepository refunds; private final SettingRepository settings;
    private final JobRepository jobs;
    private final PaymentService paymentService;
    public PaymentController(PaymentRepository payments, RefundRepository refunds, SettingRepository settings,
                             JobRepository jobs, PaymentService paymentService) {
        this.payments=payments; this.refunds=refunds; this.settings=settings;
        this.jobs=jobs; this.paymentService=paymentService;
    }

    @GetMapping("/payments") public String list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,Model model){
        model.addAttribute("payments",payments.search(q,PageRequest.of(page,10)));model.addAttribute("q",q);
        model.addAttribute("paymentEntryForm", new PaymentEntryForm()); return "payment/list";}

    @GetMapping("/payments/payable-jobs")
    @ResponseBody
    public List<JobRepository.ReceivableJob> payableJobs() {
        return jobs.findReceivableJobs();
    }

    @PostMapping("/payments")
    @ResponseBody
    public ResponseEntity<Map<String, String>> record(@Valid PaymentEntryForm form, BindingResult binding) {
        if (binding.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", "กรุณาระบุใบงาน จำนวนเงิน และวันเวลาชำระให้ถูกต้องครบถ้วน"));
        }
        try {
            Payment payment = paymentService.record(form.getJobId(), form);
            return ResponseEntity.ok(Map.of("message", "บันทึกการชำระเงินเรียบร้อยแล้ว",
                    "receiptUrl", "/payments/" + payment.getId() + "/receipt"));
        } catch (BusinessException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(404).body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/payments/{id}/receipt") public String receipt(@PathVariable Long id,Model model){
        Payment payment=payments.findReceiptById(id).orElseThrow(() -> new ResourceNotFoundException("ไม่พบใบเสร็จ"));
        model.addAttribute("document",payment);model.addAttribute("refundDocument",false);model.addAttribute("company",company());return "payment/receipt";}

    @GetMapping("/refunds/{id}/receipt") public String refundReceipt(@PathVariable Long id,Model model){
        Refund refund=refunds.findReceiptById(id).orElseThrow(() -> new ResourceNotFoundException("ไม่พบใบคืนเงิน"));
        model.addAttribute("document",refund);model.addAttribute("refundDocument",true);model.addAttribute("company",company());return "payment/receipt";}

    private Map<String,String> company(){return settings.findByGroupNameOrderByKey("COMPANY").stream().collect(Collectors.toMap(s->s.getKey().replace("company.",""),s->s.getValue()==null?"":s.getValue()));}
}
