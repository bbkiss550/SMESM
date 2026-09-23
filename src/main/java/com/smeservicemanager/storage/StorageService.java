package com.smeservicemanager.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    StoredFile store(MultipartFile file);
    Resource load(String storedFileName);
    record StoredFile(String originalName, String storedName, String relativePath, String contentType, long size) {}
}
