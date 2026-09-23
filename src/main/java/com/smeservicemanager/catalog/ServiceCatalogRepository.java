package com.smeservicemanager.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, Long> {
    Page<ServiceCatalog> findByServiceNameContainingIgnoreCaseOrServiceCodeContainingIgnoreCase(String name, String code, Pageable pageable);
    List<ServiceCatalog> findByStatusOrderByServiceNameAsc(String status);
    boolean existsByServiceCode(String code);
}
