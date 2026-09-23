package com.smeservicemanager.catalog;

import com.smeservicemanager.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "m_service")
public class ServiceCatalog extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_m_service")
    private Long id;

    @Column(name = "s_service_code", nullable = false, unique = true, length = 30)
    private String serviceCode;

    @Column(name = "s_service_name", nullable = false, length = 200)
    private String serviceName;

    @Column(name = "s_description", columnDefinition = "text")
    private String description;

    @Column(name = "s_default_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal defaultPrice = BigDecimal.ZERO;

    @Column(name = "s_default_duration_minutes", nullable = false)
    private Integer defaultDurationMinutes = 120;
}
