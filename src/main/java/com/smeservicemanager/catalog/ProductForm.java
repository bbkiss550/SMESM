package com.smeservicemanager.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class ProductForm {
    @NotBlank private String productName;
    private String category;
    @NotBlank private String unit = "ชิ้น";
    @NotNull @PositiveOrZero private BigDecimal costPrice = BigDecimal.ZERO;
    @NotNull @PositiveOrZero private BigDecimal salePrice = BigDecimal.ZERO;
    @NotNull @PositiveOrZero private BigDecimal openingStock = BigDecimal.ZERO;
    @NotNull @PositiveOrZero private BigDecimal minStock = BigDecimal.ZERO;
}
