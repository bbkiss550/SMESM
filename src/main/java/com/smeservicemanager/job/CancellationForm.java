package com.smeservicemanager.job;

import com.smeservicemanager.shared.domain.DomainTypes.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class CancellationForm {
    @NotBlank private String reason;
    private String refundOption = "NONE";
    private BigDecimal refundAmount = BigDecimal.ZERO;
    private PaymentMethod refundMethod = PaymentMethod.TRANSFER;
    private String refundReference;
    private List<Long> returnItemIds = new ArrayList<>();
}
