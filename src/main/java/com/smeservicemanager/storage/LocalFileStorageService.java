package com.smeservicemanager.storage;

import com.smeservicemanager.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorageService implements StorageService {
    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf");
    private static final Set<String> MIME_TYPES = Set.of("image/jpeg", "image/png", "application/pdf");
    private final Path uploadRoot;
    private final long maxFileSize;

    public LocalFileStorageService(@Value("${app.storage.upload-dir}") String uploadDir,
                                   @Value("${app.storage.max-file-size}") long maxFileSize) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
        try { Files.createDirectories(uploadRoot); }
        catch (IOException ex) { throw new IllegalStateException("ไม่สามารถสร้างโฟลเดอร์จัดเก็บไฟล์", ex); }
    }

    @Override
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("กรุณาเลือกไฟล์");
        if (file.getSize() > maxFileSize) throw new BusinessException("ไฟล์ต้องมีขนาดไม่เกิน 10 MB");
        String original = Path.of(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()).getFileName().toString();
        String extension = extensionOf(original);
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!EXTENSIONS.contains(extension) || !MIME_TYPES.contains(contentType)) {
            throw new BusinessException("รองรับเฉพาะ JPG, JPEG, PNG และ PDF");
        }
        String stored = UUID.randomUUID() + "." + extension;
        Path target = uploadRoot.resolve(stored).normalize();
        if (!target.startsWith(uploadRoot)) throw new BusinessException("ชื่อไฟล์ไม่ปลอดภัย");
        try { Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING); }
        catch (IOException ex) { throw new BusinessException("บันทึกไฟล์ไม่สำเร็จ"); }
        return new StoredFile(original, stored, target.toString(), contentType, file.getSize());
    }

    @Override
    public Resource load(String storedFileName) {
        if (storedFileName.contains("..") || storedFileName.contains("/") || storedFileName.contains("\\")) {
            throw new BusinessException("ชื่อไฟล์ไม่ปลอดภัย");
        }
        try {
            Path file = uploadRoot.resolve(storedFileName).normalize();
            if (!file.startsWith(uploadRoot)) throw new BusinessException("ไม่พบไฟล์");
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) throw new BusinessException("ไม่พบไฟล์");
            return resource;
        } catch (IOException ex) { throw new BusinessException("ไม่สามารถอ่านไฟล์ได้"); }
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
