package com.smeservicemanager.job;

import com.smeservicemanager.shared.domain.DomainTypes.JobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, Long> {
    @EntityGraph(attributePaths = {"customer", "service", "technician", "technician.role"})
    @Query("select j from Job j where (:q = '' or lower(j.jobNo) like lower(concat('%', :q, '%')) or lower(j.title) like lower(concat('%', :q, '%')) or lower(j.customer.name) like lower(concat('%', :q, '%')) or lower(coalesce(j.customer.companyName, '')) like lower(concat('%', :q, '%')) or j.contactPhone like concat('%', :q, '%')) and (:status is null or j.jobStatus = :status) order by j.appointmentStart desc")
    Page<Job> search(@Param("q") String query, @Param("status") JobStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "service", "technician", "technician.role", "originalJob"})
    @Query("select distinct j from Job j where j.id = :id")
    Optional<Job> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customer", "service", "technician", "attachments"})
    Optional<Job> findByPublicToken(UUID token);

    @EntityGraph(attributePaths = {"customer", "service", "technician"})
    List<Job> findTop8ByOrderByCreateDateDesc();

    @EntityGraph(attributePaths = {"customer", "service", "technician"})
    List<Job> findByAppointmentStartBetweenOrderByAppointmentStartAsc(LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"service", "technician"})
    List<Job> findTop20ByCustomerIdOrderByAppointmentStartDesc(Long customerId);

    @EntityGraph(attributePaths = {"customer", "service", "technician"})
    List<Job> findByTechnicianUsernameAndJobStatusInOrderByAppointmentStartAsc(String username, List<JobStatus> statuses);

    @Query("select count(j) > 0 from Job j where j.technician.id = :technicianId and j.id <> coalesce(:excludeJobId, -1) and j.jobStatus in ('SCHEDULED','ASSIGNED','IN_PROGRESS','WAITING_PART') and j.appointmentStart < :endAt and j.appointmentEnd > :startAt")
    boolean hasScheduleConflict(@Param("technicianId") Long technicianId, @Param("excludeJobId") Long excludeJobId, @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from Job j where j.id = :id")
    Optional<Job> findByIdForUpdate(@Param("id") Long id);

    boolean existsByJobNo(String jobNo);
}
