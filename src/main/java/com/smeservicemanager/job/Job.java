package com.smeservicemanager.job;

import com.smeservicemanager.catalog.ServiceCatalog;
import com.smeservicemanager.customer.Customer;
import com.smeservicemanager.security.User;
import com.smeservicemanager.shared.domain.AuditableEntity;
import com.smeservicemanager.shared.domain.DomainTypes.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "t_job")
public class Job extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_t_job")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_m_customer", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_m_service", nullable = false)
    private ServiceCatalog service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_m_user")
    private User technician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_t_original_job")
    private Job originalJob;

    @Column(name = "j_job_no", nullable = false, unique = true, length = 40)
    private String jobNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "j_job_type", nullable = false, length = 20)
    private JobType jobType = JobType.NORMAL;

    @Column(name = "j_title", nullable = false, length = 250)
    private String title;

    @Column(name = "j_description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "j_priority", nullable = false, length = 20)
    private JobPriority priority = JobPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "j_job_status", nullable = false, length = 30)
    private JobStatus jobStatus = JobStatus.NEW;

    @Column(name = "j_appointment_start", nullable = false)
    private LocalDateTime appointmentStart;

    @Column(name = "j_appointment_end", nullable = false)
    private LocalDateTime appointmentEnd;

    @Column(name = "j_contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "j_address", nullable = false, columnDefinition = "text")
    private String address;

    @Column(name = "j_internal_note", columnDefinition = "text")
    private String internalNote;

    @Column(name = "j_rework_reason", columnDefinition = "text")
    private String reworkReason;

    @Column(name = "j_under_warranty", nullable = false)
    private boolean underWarranty;

    @Column(name = "j_chargeable", nullable = false)
    private boolean chargeable = true;

    @Column(name = "j_subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "j_discount", nullable = false, precision = 14, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "j_vat_mode", nullable = false, length = 20)
    private VatMode vatMode = VatMode.INCLUSIVE;

    @Column(name = "j_vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate = new BigDecimal("7.00");

    @Column(name = "j_tax_base", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxBase = BigDecimal.ZERO;

    @Column(name = "j_vat_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "j_grand_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "j_payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "j_public_token", nullable = false, unique = true)
    private UUID publicToken = UUID.randomUUID();

    @Column(name = "j_completed_date")
    private LocalDateTime completedDate;

    @Column(name = "j_cancel_reason", columnDefinition = "text")
    private String cancelReason;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<JobItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL)
    @OrderBy("activityDate DESC")
    private List<JobActivity> activities = new ArrayList<>();

    @OneToMany(mappedBy = "job")
    @OrderBy("createDate DESC")
    private List<JobAttachment> attachments = new ArrayList<>();

    public void recalculateTotals() {
        subtotal = items.stream().filter(JobItem::isActive).map(JobItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        grandTotal = subtotal.subtract(discount == null ? BigDecimal.ZERO : discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        if (vatMode == VatMode.INCLUSIVE && vatRate.signum() > 0) {
            taxBase = grandTotal.multiply(new BigDecimal("100"))
                    .divide(new BigDecimal("100").add(vatRate), 2, RoundingMode.HALF_UP);
            vatAmount = grandTotal.subtract(taxBase);
        } else {
            taxBase = grandTotal;
            vatAmount = BigDecimal.ZERO;
        }
    }
}
