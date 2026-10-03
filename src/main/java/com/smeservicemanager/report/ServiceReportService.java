package com.smeservicemanager.report;

import com.smeservicemanager.shared.domain.DomainTypes.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ServiceReportService {
    private final ServiceReportRepository repository;
    @Transactional(readOnly = true)
    public ServiceReportRepository.Result load(LocalDate start, LocalDate end,
            ServiceReportRepository.DateBasis basis, JobStatus status, Long serviceId,
            Long technicianId, int page) {
        if (end.isBefore(start) || end.equals(LocalDate.MAX))
            throw new IllegalArgumentException("วันที่สิ้นสุดต้องไม่น้อยกว่าวันที่เริ่มต้น");
        return repository.load(start, end, basis, status, serviceId, technicianId, page);
    }
}
