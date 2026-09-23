package com.smeservicemanager.payment;

import com.smeservicemanager.job.Job;
import com.smeservicemanager.job.JobActivity;
import com.smeservicemanager.job.JobActivityRepository;
import com.smeservicemanager.job.JobRepository;
import com.smeservicemanager.shared.domain.DomainTypes.JobStatus;
import com.smeservicemanager.shared.domain.DomainTypes.PaymentStatus;
import com.smeservicemanager.shared.exception.BusinessException;
import com.smeservicemanager.shared.exception.ResourceNotFoundException;
import com.smeservicemanager.shared.service.DocumentNumberService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class PaymentService {
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final JobRepository jobs;
    private final JobActivityRepository activities;
    private final DocumentNumberService numbers;

    public PaymentService(PaymentRepository payments, RefundRepository refunds, JobRepository jobs,
                          JobActivityRepository activities, DocumentNumberService numbers) {
        this.payments = payments; this.refunds = refunds; this.jobs = jobs; this.activities = activities; this.numbers = numbers;
    }

    @Transactional
    public Payment record(Long jobId, PaymentForm form) {
        if (form.getAmount() == null || form.getAmount().signum() <= 0) {
            throw new BusinessException("จำนวนเงินที่ชำระต้องมากกว่า 0");
        }
        Job job = jobs.findByIdForUpdate(jobId).orElseThrow(() -> new ResourceNotFoundException("ไม่พบใบงาน"));
        if (job.getJobStatus() == JobStatus.CANCELLED) throw new BusinessException("ไม่สามารถรับชำระเงินสำหรับงานที่ยกเลิกแล้ว");
        BigDecimal paid = payments.totalPaid(jobId);
        BigDecimal refunded = refunds.totalRefunded(jobId);
        BigDecimal remaining = job.getGrandTotal().subtract(paid.subtract(refunded));
        if (form.getAmount().compareTo(remaining) > 0) throw new BusinessException("ยอดชำระเกินยอดคงเหลือ " + remaining + " บาท");

        Payment payment = new Payment();
        payment.setJob(job); payment.setPaymentNo(numbers.next("PAYMENT", "PAY"));
        payment.setReceiptNo(numbers.next("RECEIPT", "RCPT")); payment.setPaymentDate(form.getPaymentDate());
        payment.setAmount(form.getAmount()); payment.setPaymentType(form.getPaymentType()); payment.setPaymentMethod(form.getPaymentMethod());
        payment.setReferenceNo(form.getReferenceNo()); payment.setNote(form.getNote());
        payments.save(payment);
        updatePaymentStatus(job, paid.add(form.getAmount()), refunded);
        addActivity(job, "PAYMENT_ADDED", "รับชำระเงิน " + form.getAmount() + " บาท", null, payment.getPaymentNo());
        return payment;
    }

    @Transactional
    public Refund refund(Long jobId, RefundForm form) {
        if (form.getAmount() == null || form.getAmount().signum() <= 0) {
            throw new BusinessException("จำนวนเงินคืนต้องมากกว่า 0");
        }
        Job job = jobs.findByIdForUpdate(jobId).orElseThrow(() -> new ResourceNotFoundException("ไม่พบใบงาน"));
        BigDecimal paid = payments.totalPaid(jobId);
        BigDecimal refunded = refunds.totalRefunded(jobId);
        BigDecimal refundable = paid.subtract(refunded);
        if (form.getAmount().compareTo(refundable) > 0) throw new BusinessException("ยอดคืนเงินเกินยอดที่รับเงินจริง");

        Refund refund = new Refund(); refund.setJob(job); refund.setRefundNo(numbers.next("REFUND", "RFND"));
        refund.setReceiptNo(numbers.next("REFUND_RECEIPT", "RFND-RCPT")); refund.setRefundDate(LocalDateTime.now());
        refund.setAmount(form.getAmount()); refund.setRefundMethod(form.getRefundMethod()); refund.setReferenceNo(form.getReferenceNo());
        refund.setReason(form.getReason()); refunds.save(refund);
        updatePaymentStatus(job, paid, refunded.add(form.getAmount()));
        addActivity(job, "PAYMENT_REFUNDED", "คืนเงิน " + form.getAmount() + " บาท", null, refund.getRefundNo());
        return refund;
    }

    @Transactional(readOnly = true)
    public BigDecimal totalPaid(Long jobId) { return payments.totalPaid(jobId); }

    @Transactional(readOnly = true)
    public BigDecimal totalRefunded(Long jobId) { return refunds.totalRefunded(jobId); }

    private void updatePaymentStatus(Job job, BigDecimal paid, BigDecimal refunded) {
        BigDecimal net = paid.subtract(refunded);
        if (refunded.signum() > 0 && net.signum() == 0) job.setPaymentStatus(PaymentStatus.REFUNDED);
        else if (refunded.signum() > 0) job.setPaymentStatus(PaymentStatus.PARTIAL_REFUND);
        else if (net.signum() == 0) job.setPaymentStatus(PaymentStatus.UNPAID);
        else if (net.compareTo(job.getGrandTotal()) >= 0) job.setPaymentStatus(PaymentStatus.PAID);
        else job.setPaymentStatus(PaymentStatus.PARTIAL);
        jobs.save(job);
    }

    private void addActivity(Job job, String type, String description, String oldValue, String newValue) {
        JobActivity activity = new JobActivity(); activity.setJob(job); activity.setActivityType(type);
        activity.setDescription(description); activity.setOldValue(oldValue); activity.setNewValue(newValue);
        activity.setActivityDate(LocalDateTime.now()); activities.save(activity);
    }
}
