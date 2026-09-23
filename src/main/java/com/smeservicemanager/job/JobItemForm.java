package com.smeservicemanager.job;

import com.smeservicemanager.shared.domain.DomainTypes.JobItemType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class JobItemForm {
    @NotNull private JobItemType itemType = JobItemType.PRODUCT;
    private Long productId;
    private String description;
    @NotNull @Positive private BigDecimal quantity = BigDecimal.ONE;
    private String unit = "ชิ้น";
    @NotNull @PositiveOrZero private BigDecimal unitPrice = BigDecimal.ZERO;
}
