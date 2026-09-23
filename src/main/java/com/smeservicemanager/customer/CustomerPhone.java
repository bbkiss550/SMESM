package com.smeservicemanager.customer;

import com.smeservicemanager.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "m_customer_phone")
public class CustomerPhone extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_m_customer_phone")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_m_customer", nullable = false)
    private Customer customer;

    @Column(name = "cp_phone", nullable = false, length = 30)
    private String phone;

    @Column(name = "cp_label", nullable = false, length = 50)
    private String label = "มือถือ";

    @Column(name = "cp_is_primary", nullable = false)
    private boolean primaryPhone;
}
