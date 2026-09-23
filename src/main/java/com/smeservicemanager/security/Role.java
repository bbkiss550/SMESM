package com.smeservicemanager.security;

import com.smeservicemanager.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "m_role")
public class Role extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_m_role")
    private Long id;

    @Column(name = "r_code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "r_name", nullable = false, length = 100)
    private String name;
}
