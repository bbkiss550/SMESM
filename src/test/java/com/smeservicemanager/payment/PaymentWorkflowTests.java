package com.smeservicemanager.payment;

import com.smeservicemanager.job.JobRepository;
import com.smeservicemanager.shared.domain.DomainTypes.PaymentType;
import com.smeservicemanager.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
class PaymentWorkflowTests {
    @Autowired JobRepository jobs;
    @Autowired PaymentService service;
    @Autowired PaymentRepository payments;

    @Test
    void recordsDepositAndUpdatesReceivableBalanceWithRollback() {
        JobRepository.ReceivableJob selected = jobs.findReceivableJobs().getFirst();
        PaymentForm form = new PaymentForm();
        form.setAmount(selected.getRemaining().min(new BigDecimal("1.00")));
        form.setPaymentType(PaymentType.DEPOSIT);
        Payment payment = service.record(selected.getId(), form);
        assertNotNull(payment.getId());
        assertNotNull(payment.getReceiptNo());
        BigDecimal expected = selected.getRemaining().subtract(form.getAmount());
        if (expected.signum() > 0) {
            BigDecimal actual = jobs.findReceivableJobs().stream().filter(j -> j.getId().equals(selected.getId()))
                    .findFirst().orElseThrow().getRemaining();
            assertEquals(0, expected.compareTo(actual));
        } else {
            assertTrue(jobs.findReceivableJobs().stream().noneMatch(j -> j.getId().equals(selected.getId())));
        }
    }

    @Test
    void rejectsOverpaymentWithoutCreatingATransaction() {
        JobRepository.ReceivableJob selected = jobs.findReceivableJobs().getFirst();
        long before = payments.count();
        PaymentForm form = new PaymentForm();
        form.setAmount(selected.getRemaining().add(new BigDecimal("0.01")));
        assertThrows(BusinessException.class, () -> service.record(selected.getId(), form));
        assertEquals(before, payments.count());
    }
}
