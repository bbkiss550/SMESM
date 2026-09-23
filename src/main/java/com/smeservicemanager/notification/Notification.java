package com.smeservicemanager.notification;

import com.smeservicemanager.security.User;
import com.smeservicemanager.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "t_notification")
public class Notification extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_t_notification")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_m_user")
    private User user;

    @Column(name = "n_type", nullable = false, length = 50)
    private String type;

    @Column(name = "n_title", nullable = false, length = 200)
    private String title;

    @Column(name = "n_message", nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "n_target_url", length = 500)
    private String targetUrl;

    @Column(name = "n_is_read", nullable = false)
    private boolean read;
}
