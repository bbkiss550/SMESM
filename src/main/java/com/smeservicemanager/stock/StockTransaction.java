package com.smeservicemanager.stock;

import com.smeservicemanager.catalog.Product;
import com.smeservicemanager.job.Job;
import com.smeservicemanager.shared.domain.AuditableEntity;
import com.smeservicemanager.shared.domain.DomainTypes.StockTransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "t_stock_transaction")
public class StockTransaction extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_t_stock_transaction")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_m_product", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_t_job")
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(name = "st_transaction_type", nullable = false, length = 20)
    private StockTransactionType transactionType;

    @Column(name = "st_qty", nullable = false, precision = 14, scale = 2)
    private BigDecimal quantity;

    @Column(name = "st_qty_before", nullable = false, precision = 14, scale = 2)
    private BigDecimal quantityBefore;

    @Column(name = "st_qty_after", nullable = false, precision = 14, scale = 2)
    private BigDecimal quantityAfter;

    @Column(name = "st_reference", length = 100)
    private String reference;

    @Column(name = "st_note", columnDefinition = "text")
    private String note;
}
