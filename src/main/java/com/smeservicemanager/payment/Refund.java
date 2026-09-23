package com.smeservicemanager.payment;

import com.smeservicemanager.job.Job;
import com.smeservicemanager.shared.domain.AuditableEntity;
import com.smeservicemanager.shared.domain.DomainTypes.PaymentMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "t_refund")
public class Refund extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_t_refund")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_t_job", nullable = false)
    private Job job;

    @Column(name = "rf_refund_no", nullable = false, unique = true, length = 40)
    private String refundNo;

    @Column(name = "rf_receipt_no", nullable = false, unique = true, length = 40)
    private String receiptNo;

    @Column(name = "rf_refund_date", nullable = false)
    private LocalDateTime refundDate;

    @Column(name = "rf_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "rf_refund_method", nullable = false, length = 30)
    private PaymentMethod refundMethod;

    @Column(name = "rf_reference_no", length = 100)
    private String referenceNo;

    @Column(name = "rf_reason", nullable = false, columnDefinition = "text")
    private String reason;
}
