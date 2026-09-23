package com.smeservicemanager.security;

import com.smeservicemanager.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "m_user")
public class User extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_m_user")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_m_role", nullable = false)
    private Role role;

    @Column(name = "u_username", nullable = false, unique = true, length = 80)
    private String username;

    @Column(name = "u_password", nullable = false, length = 100)
    private String password;

    @Column(name = "u_first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "u_last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "u_phone", length = 30)
    private String phone;

    @Column(name = "u_email", length = 180)
    private String email;

    @Column(name = "u_profile_image", length = 500)
    private String profileImage;

    @Column(name = "u_force_password_change", nullable = false)
    private boolean forcePasswordChange;

    public String getFullName() { return firstName + " " + lastName; }
}
