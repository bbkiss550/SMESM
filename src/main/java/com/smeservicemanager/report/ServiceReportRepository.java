package com.smeservicemanager.report;

import com.smeservicemanager.job.Job;
import com.smeservicemanager.shared.domain.DomainTypes.JobStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ServiceReportRepository {
    private final EntityManager entityManager;
    public enum DateBasis { CREATED, APPOINTMENT, COMPLETED }
    public record Result(List<Job> jobs, long total, long completed, long cancelled,
                         BigDecimal value, int page, int pages) {}

    public List<Job> findForPrint(LocalDate start, LocalDate end, DateBasis basis,
                                  JobStatus status, Long serviceId, Long technicianId) {
        String where = whereClause(basis, status, serviceId, technicianId);
        var rows = entityManager.createQuery("select j" + where.replace("from Job j where",
                "from Job j join fetch j.customer join fetch j.service left join fetch j.technician where")
                + " order by j.jobNo asc, j.id asc", Job.class);
        bind(rows, start, end, status, serviceId, technicianId);
        return rows.getResultList();
    }

    @Transactional(readOnly = true)
    public Result load(LocalDate start, LocalDate end, DateBasis basis, JobStatus status,
                       Long serviceId, Long technicianId, int requestedPage) {
        String where = whereClause(basis, status, serviceId, technicianId);
        var summary = entityManager.createQuery("select count(j), "
                + "sum(case when j.jobStatus = 'COMPLETED' then 1 else 0 end), "
                + "sum(case when j.jobStatus = 'CANCELLED' then 1 else 0 end), "
                + "sum(case when j.jobStatus <> 'CANCELLED' then j.grandTotal else 0 end)" + where, Object[].class);
        bind(summary, start, end, status, serviceId, technicianId);
        Object[] totals = summary.getSingleResult();
        long total = ((Number) totals[0]).longValue();
        int pages = (int) Math.max(1, (total + 9) / 10);
        int page = Math.min(Math.max(0, requestedPage), pages - 1);
        var rows = entityManager.createQuery("select j" + where.replace("from Job j where",
                "from Job j join fetch j.customer join fetch j.service left join fetch j.technician where")
                + " order by j.jobNo asc, j.id asc", Job.class);
        bind(rows, start, end, status, serviceId, technicianId);
        List<Job> jobs = rows.setFirstResult(page * 10).setMaxResults(10).getResultList();
        // Initialize only the relations used by the template before leaving the transaction.
        jobs.forEach(j -> { j.getCustomer().getDisplayName(); j.getService().getServiceName();
            if (j.getTechnician() != null) j.getTechnician().getFullName(); });
        return new Result(jobs, total, totals[1] == null ? 0 : ((Number) totals[1]).longValue(),
                totals[2] == null ? 0 : ((Number) totals[2]).longValue(),
                totals[3] == null ? BigDecimal.ZERO : (BigDecimal) totals[3], page, pages);
    }
    private String dateExpression(DateBasis basis) {
        return switch (basis) {
            case CREATED -> "j.createDate";
            case APPOINTMENT -> "j.appointmentStart";
            case COMPLETED -> "j.completedDate";
        };
    }
    private String whereClause(DateBasis basis, JobStatus status, Long serviceId, Long technicianId) {
        String date = dateExpression(basis);
        return " from Job j where j.status = 'A' and " + date + " >= :start and " + date + " < :end"
                + (status == null ? "" : " and j.jobStatus = :status")
                + (serviceId == null ? "" : " and j.service.id = :serviceId")
                + (technicianId == null ? "" : " and j.technician.id = :technicianId");
    }
    private void bind(jakarta.persistence.Query query, LocalDate start, LocalDate end,
                      JobStatus status, Long serviceId, Long technicianId) {
        query.setParameter("start", start.atStartOfDay()).setParameter("end", end.plusDays(1).atStartOfDay());
        if (status != null) query.setParameter("status", status);
        if (serviceId != null) query.setParameter("serviceId", serviceId);
        if (technicianId != null) query.setParameter("technicianId", technicianId);
    }
}
