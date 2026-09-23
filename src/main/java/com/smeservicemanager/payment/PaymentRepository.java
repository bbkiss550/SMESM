package com.smeservicemanager.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @EntityGraph(attributePaths = {"job", "job.customer"})
    @Query("select p from Payment p where p.recordStatus = 'ACTIVE' and (:q = '' or lower(p.paymentNo) like lower(concat('%', :q, '%')) or lower(p.job.jobNo) like lower(concat('%', :q, '%')) or lower(p.job.customer.name) like lower(concat('%', :q, '%'))) order by p.paymentDate desc")
    Page<Payment> search(@Param("q") String query, Pageable pageable);

    List<Payment> findByJobIdAndRecordStatusOrderByPaymentDateDesc(Long jobId, String recordStatus);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.job.id = :jobId and p.recordStatus = 'ACTIVE' and p.status = 'A'")
    BigDecimal totalPaid(@Param("jobId") Long jobId);

    @EntityGraph(attributePaths = {"job", "job.customer"})
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findReceiptById(@Param("id") Long id);
}
