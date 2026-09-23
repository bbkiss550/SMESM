package com.smeservicemanager.catalog;

import com.smeservicemanager.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "m_product")
public class Product extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_m_product")
    private Long id;

    @Column(name = "p_product_code", nullable = false, unique = true, length = 30)
    private String productCode;

    @Column(name = "p_product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "p_category", length = 100)
    private String category;

    @Column(name = "p_unit", nullable = false, length = 50)
    private String unit;

    @Column(name = "p_cost_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "p_sale_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal salePrice = BigDecimal.ZERO;

    @Column(name = "p_stock_qty", nullable = false, precision = 14, scale = 2)
    private BigDecimal stockQty = BigDecimal.ZERO;

    @Column(name = "p_min_stock", nullable = false, precision = 14, scale = 2)
    private BigDecimal minStock = BigDecimal.ZERO;

    public String getStockLevel() {
        if (stockQty.signum() == 0) return "OUT_OF_STOCK";
        if (stockQty.compareTo(minStock) <= 0) return "LOW_STOCK";
        return "NORMAL";
    }
}
