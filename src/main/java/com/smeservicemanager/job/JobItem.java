package com.smeservicemanager.job;

import com.smeservicemanager.catalog.Product;
import com.smeservicemanager.shared.domain.AuditableEntity;
import com.smeservicemanager.shared.domain.DomainTypes.JobItemType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "t_job_item")
public class JobItem extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_t_job_item")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_t_job", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_m_product")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "ji_item_type", nullable = false, length = 20)
    private JobItemType itemType;

    @Column(name = "ji_description", nullable = false, length = 300)
    private String description;

    @Column(name = "ji_qty", nullable = false, precision = 14, scale = 2)
    private BigDecimal quantity;

    @Column(name = "ji_unit", nullable = false, length = 50)
    private String unit;

    @Column(name = "ji_unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "ji_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    public void calculateTotal() {
        total = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }
}
