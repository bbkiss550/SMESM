package com.smeservicemanager.catalog;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("select p from Product p where (:q = '' or lower(p.productCode) like lower(concat('%', :q, '%')) or lower(p.productName) like lower(concat('%', :q, '%')) or lower(coalesce(p.category, '')) like lower(concat('%', :q, '%'))) order by p.id")
    Page<Product> search(@Param("q") String query, Pageable pageable);
    List<Product> findByStatusOrderByProductNameAsc(String status);
    boolean existsByProductCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
