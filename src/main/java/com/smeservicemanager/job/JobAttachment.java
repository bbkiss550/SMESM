package com.smeservicemanager.job;

import com.smeservicemanager.shared.domain.AuditableEntity;
import com.smeservicemanager.shared.domain.DomainTypes.AttachmentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "t_job_attachment")
public class JobAttachment extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_t_job_attachment")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_t_job", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(name = "jat_attachment_type", nullable = false, length = 20)
    private AttachmentType attachmentType;

    @Column(name = "jat_original_file_name", nullable = false, length = 500)
    private String originalFileName;

    @Column(name = "jat_stored_file_name", nullable = false, unique = true, length = 100)
    private String storedFileName;

    @Column(name = "jat_file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "jat_content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "jat_file_size", nullable = false)
    private long fileSize;

    public boolean isImage() { return contentType != null && contentType.startsWith("image/"); }
}
