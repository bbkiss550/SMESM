package com.smeservicemanager.payment;

import com.smeservicemanager.settings.SettingRepository;
import com.smeservicemanager.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class PaymentController {
    private final PaymentRepository payments; private final RefundRepository refunds; private final SettingRepository settings;
    public PaymentController(PaymentRepository payments,RefundRepository refunds,SettingRepository settings){this.payments=payments;this.refunds=refunds;this.settings=settings;}

    @GetMapping("/payments") public String list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,Model model){
        model.addAttribute("payments",payments.search(q,PageRequest.of(page,10)));model.addAttribute("q",q);return "payment/list";}

    @GetMapping("/payments/{id}/receipt") public String receipt(@PathVariable Long id,Model model){
        Payment payment=payments.findReceiptById(id).orElseThrow(() -> new ResourceNotFoundException("ไม่พบใบเสร็จ"));
        model.addAttribute("document",payment);model.addAttribute("refundDocument",false);model.addAttribute("company",company());return "payment/receipt";}

    @GetMapping("/refunds/{id}/receipt") public String refundReceipt(@PathVariable Long id,Model model){
        Refund refund=refunds.findReceiptById(id).orElseThrow(() -> new ResourceNotFoundException("ไม่พบใบคืนเงิน"));
        model.addAttribute("document",refund);model.addAttribute("refundDocument",true);model.addAttribute("company",company());return "payment/receipt";}

    private Map<String,String> company(){return settings.findByGroupNameOrderByKey("COMPANY").stream().collect(Collectors.toMap(s->s.getKey().replace("company.",""),s->s.getValue()==null?"":s.getValue()));}
}
