package com.smeservicemanager.settings;

import com.smeservicemanager.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "t_setting")
public class Setting extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_t_setting")
    private Long id;

    @Column(name = "s_key", nullable = false, unique = true, length = 120)
    private String key;

    @Column(name = "s_value", columnDefinition = "text")
    private String value;

    @Column(name = "s_group", nullable = false, length = 50)
    private String groupName;

    @Column(name = "s_description", length = 300)
    private String description;
}
