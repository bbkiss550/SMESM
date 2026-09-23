package com.smeservicemanager.payment;

import com.smeservicemanager.shared.domain.DomainTypes.PaymentMethod;
import com.smeservicemanager.shared.domain.DomainTypes.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter @Setter
public class PaymentForm {
    @NotNull @Positive
    private BigDecimal amount;
    @NotNull
    private PaymentType paymentType = PaymentType.DEPOSIT;
    @NotNull
    private PaymentMethod paymentMethod = PaymentMethod.TRANSFER;
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate paymentDay = LocalDate.now();
    @NotNull
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime paymentTime = LocalTime.now().withSecond(0).withNano(0);
    private String referenceNo;
    private String note;

    public LocalDateTime getPaymentDate() {
        return paymentDay == null || paymentTime == null
                ? null
                : LocalDateTime.of(paymentDay, paymentTime);
    }
}
