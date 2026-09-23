package com.smeservicemanager.job;

import com.smeservicemanager.payment.PaymentService;
import com.smeservicemanager.payment.RefundForm;
import com.smeservicemanager.shared.domain.DomainTypes.JobStatus;
import com.smeservicemanager.shared.exception.BusinessException;
import com.smeservicemanager.shared.exception.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class CancellationService {
    private final JobRepository jobs;
    private final JobService jobService;
    private final PaymentService paymentService;

    public CancellationService(JobRepository jobs, JobService jobService, PaymentService paymentService) {
        this.jobs = jobs; this.jobService = jobService; this.paymentService = paymentService;
    }

    @Transactional
    public void cancel(Long jobId, CancellationForm form) {
        Job job = jobs.findByIdForUpdate(jobId).orElseThrow(() -> new ResourceNotFoundException("ไม่พบใบงาน"));
        if (job.getJobStatus() == JobStatus.COMPLETED || job.getJobStatus() == JobStatus.CANCELLED) {
            throw new BusinessException("ไม่สามารถยกเลิกใบงานที่ปิดแล้ว");
        }
        BigDecimal refundable = paymentService.totalPaid(jobId).subtract(paymentService.totalRefunded(jobId));
        BigDecimal refundAmount = switch (form.getRefundOption()) {
            case "FULL" -> refundable;
            case "PARTIAL" -> {
                if (form.getRefundAmount() == null || form.getRefundAmount().signum() <= 0) {
                    throw new BusinessException("กรุณาระบุจำนวนเงินคืนบางส่วนให้มากกว่า 0");
                }
                yield form.getRefundAmount();
            }
            case "NONE" -> BigDecimal.ZERO;
            default -> throw new BusinessException("ตัวเลือกการคืนเงินไม่ถูกต้อง");
        };
        if (refundAmount.signum() > 0) {
            RefundForm refund = new RefundForm();
            refund.setAmount(refundAmount); refund.setRefundMethod(form.getRefundMethod());
            refund.setReferenceNo(form.getRefundReference()); refund.setReason("ยกเลิกใบงาน: " + form.getReason());
            paymentService.refund(jobId, refund);
        }
        if (form.getReturnItemIds() != null) {
            for (Long itemId : form.getReturnItemIds()) jobService.removeItem(jobId, itemId);
        }
        jobService.transition(jobId, JobStatus.CANCELLED, form.getReason());
    }
}
