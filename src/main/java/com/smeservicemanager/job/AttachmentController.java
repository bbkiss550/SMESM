package com.smeservicemanager.job;

import com.smeservicemanager.shared.exception.ResourceNotFoundException;
import com.smeservicemanager.storage.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;
import com.smeservicemanager.shared.domain.DomainTypes.AttachmentType;

import java.nio.charset.StandardCharsets;

@Controller
public class AttachmentController {
    private final JobAttachmentRepository attachments;
    private final StorageService storage;

    public AttachmentController(JobAttachmentRepository attachments, StorageService storage) {
        this.attachments = attachments; this.storage = storage;
    }

    @GetMapping("/jobs/attachments/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        JobAttachment attachment = attachments.findById(id).orElseThrow(() -> new ResourceNotFoundException("ไม่พบไฟล์"));
        Resource resource = storage.load(attachment.getStoredFileName());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(attachment.getOriginalFileName(), StandardCharsets.UTF_8).build().toString())
                .body(resource);
    }

    @GetMapping("/track/{token}/attachments/{id}")
    public ResponseEntity<Resource> publicAfterImage(@PathVariable UUID token, @PathVariable Long id) {
        JobAttachment attachment = attachments.findById(id)
                .filter(a -> a.getJob().getPublicToken().equals(token)
                        && a.getAttachmentType() == AttachmentType.AFTER && a.isImage())
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบรูปภาพ"));
        Resource resource = storage.load(attachment.getStoredFileName());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(attachment.getContentType())).body(resource);
    }
}
