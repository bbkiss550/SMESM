package com.smeservicemanager.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PaymentEntryForm extends PaymentForm {
    @NotNull @Positive
    private Long jobId;
}
