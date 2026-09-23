package com.smeservicemanager.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobAttachmentRepository extends JpaRepository<JobAttachment, Long> {
    Optional<JobAttachment> findByStoredFileName(String storedFileName);
}
