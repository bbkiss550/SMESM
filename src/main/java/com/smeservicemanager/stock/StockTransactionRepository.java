package com.smeservicemanager.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
    @Override
    @EntityGraph(attributePaths = {"product", "job"})
    Page<StockTransaction> findAll(Pageable pageable);
}
