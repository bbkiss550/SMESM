package com.smeservicemanager.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByJobIdOrderByRefundDateDesc(Long jobId);

    @Query("select coalesce(sum(r.amount), 0) from Refund r where r.job.id = :jobId and r.status = 'A'")
    BigDecimal totalRefunded(@Param("jobId") Long jobId);

    @EntityGraph(attributePaths = {"job", "job.customer"})
    @Query("select r from Refund r where r.id = :id")
    Optional<Refund> findReceiptById(@Param("id") Long id);
}
