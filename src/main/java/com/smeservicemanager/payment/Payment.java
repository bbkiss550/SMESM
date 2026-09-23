package com.smeservicemanager.payment;

import com.smeservicemanager.job.Job;
import com.smeservicemanager.shared.domain.AuditableEntity;
import com.smeservicemanager.shared.domain.DomainTypes.PaymentMethod;
import com.smeservicemanager.shared.domain.DomainTypes.PaymentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "t_payment")
public class Payment extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_t_payment")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_t_job", nullable = false)
    private Job job;

    @Column(name = "p_payment_no", nullable = false, unique = true, length = 40)
    private String paymentNo;

    @Column(name = "p_receipt_no", nullable = false, unique = true, length = 40)
    private String receiptNo;

    @Column(name = "p_payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "p_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "p_payment_type", nullable = false, length = 20)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "p_payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "p_reference_no", length = 100)
    private String referenceNo;

    @Column(name = "p_note", columnDefinition = "text")
    private String note;

    @Column(name = "p_record_status", nullable = false, length = 20)
    private String recordStatus = "ACTIVE";
}
