package com.smeservicemanager.job;

import com.smeservicemanager.security.User;
import com.smeservicemanager.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "t_job_activity")
public class JobActivity extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_t_job_activity")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_t_job", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_m_user")
    private User user;

    @Column(name = "ja_activity_type", nullable = false, length = 50)
    private String activityType;

    @Column(name = "ja_description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "ja_old_value", columnDefinition = "text")
    private String oldValue;

    @Column(name = "ja_new_value", columnDefinition = "text")
    private String newValue;

    @Column(name = "ja_activity_date", nullable = false)
    private LocalDateTime activityDate = LocalDateTime.now();
}
