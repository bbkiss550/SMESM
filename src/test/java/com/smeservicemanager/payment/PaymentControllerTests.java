package com.smeservicemanager.payment;

import com.smeservicemanager.job.JobRepository;
import com.smeservicemanager.settings.SettingRepository;
import com.smeservicemanager.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentControllerTests {
    private PaymentService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(PaymentService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PaymentController(mock(PaymentRepository.class),
                mock(RefundRepository.class), mock(SettingRepository.class), mock(JobRepository.class), service)).build();
    }

    @Test
    void recordsPaymentAndReturnsReceiptWithoutRedirecting() throws Exception {
        Payment payment = new Payment();
        payment.setId(47L);
        when(service.record(eq(1L), any())).thenReturn(payment);
        mvc.perform(post("/payments").param("jobId", "1").param("amount", "100.00")
                        .param("paymentType", "DEPOSIT").param("paymentMethod", "CASH")
                        .param("paymentDay", "2026-10-01").param("paymentTime", "08:30"))
                .andExpect(status().isOk()).andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.receiptUrl").value("/payments/47/receipt"));
    }

    @Test
    void rejectsMissingJobAndInvalidMoneyWithoutRecording() throws Exception {
        for (String amount : new String[]{"-1", "0", "0.001"}) {
            mvc.perform(post("/payments").param("jobId", "1").param("amount", amount))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/payments").param("amount", "100"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void returnsBusinessErrorInsideTheCurrentModal() throws Exception {
        when(service.record(eq(1L), any())).thenThrow(new BusinessException("ยอดชำระเกินยอดคงเหลือ"));
        mvc.perform(post("/payments").param("jobId", "1").param("amount", "100"))
                .andExpect(status().isBadRequest()).andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.message").value("ยอดชำระเกินยอดคงเหลือ"));
    }
}
