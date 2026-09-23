package com.smeservicemanager.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class ServiceForm {
    @NotBlank private String serviceName;
    private String description;
    @NotNull @PositiveOrZero private BigDecimal defaultPrice = BigDecimal.ZERO;
    @NotNull @Positive private Integer defaultDurationMinutes = 120;
}
