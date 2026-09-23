package com.smeservicemanager.payment;

import com.smeservicemanager.shared.domain.DomainTypes.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class RefundForm {
    @NotNull @Positive private BigDecimal amount;
    @NotNull private PaymentMethod refundMethod = PaymentMethod.TRANSFER;
    private String referenceNo;
    @NotBlank private String reason;
}
