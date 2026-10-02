package com.smeservicemanager.job;

import com.smeservicemanager.security.User;
import com.smeservicemanager.security.UserRepository;
import com.smeservicemanager.notification.NotificationRepository;
import com.smeservicemanager.shared.domain.DomainTypes.JobStatus;
import com.smeservicemanager.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobRescheduleTests {
    private final JobRepository jobs = mock(JobRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final JobActivityRepository activities = mock(JobActivityRepository.class);
    private final JobService service = new JobService(jobs, null, activities, null, null, null, null, users, null, mock(NotificationRepository.class), null, null);
    private final LocalDateTime start = LocalDateTime.of(2026, 10, 3, 10, 0);

    private Job setup(JobStatus status) {
        Job job = new Job(); job.setId(1L); job.setJobNo("JOB-TEST"); job.setJobStatus(status);
        job.setAppointmentStart(start.minusDays(1)); job.setAppointmentEnd(start.minusDays(1).plusHours(1));
        when(jobs.findByIdForUpdate(1L)).thenReturn(Optional.of(job));
        User technician = mock(User.class, RETURNS_DEEP_STUBS);
        when(technician.isActive()).thenReturn(true); when(technician.getRole().getCode()).thenReturn("TECHNICIAN");
        when(technician.getFullName()).thenReturn("ช่างทดสอบ");
        when(users.findById(2L)).thenReturn(Optional.of(technician));
        return job;
    }

    @Test void refusesClosedOrStartedJobs() {
        for (JobStatus status : new JobStatus[]{JobStatus.COMPLETED, JobStatus.CANCELLED, JobStatus.IN_PROGRESS, JobStatus.WAITING_PART}) {
            setup(status);
            assertThrows(BusinessException.class, () -> service.reschedule(1L, start, start.plusHours(1), 2L, "ลูกค้าขอเลื่อน"));
        }
        verify(jobs, never()).save(any());
    }
    @Test void conflictDoesNotChangeExistingAppointment() {
        Job job = setup(JobStatus.ASSIGNED);
        when(jobs.hasScheduleConflict(2L, 1L, start, start.plusHours(1))).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.reschedule(1L, start, start.plusHours(1), 2L, "ลูกค้าขอเลื่อน"));
        assertEquals(start.minusDays(1), job.getAppointmentStart());
        verify(jobs, never()).save(any());
    }
    @Test void requiresTechnicianReasonAndValidRange() {
        setup(JobStatus.SCHEDULED);
        assertThrows(BusinessException.class, () -> service.reschedule(1L, start, start.plusHours(1), null, "เลื่อน"));
        assertThrows(BusinessException.class, () -> service.reschedule(1L, start, start.plusHours(1), 2L, " "));
        assertThrows(BusinessException.class, () -> service.reschedule(1L, start, start, 2L, "เลื่อน"));
    }
    @Test void updatesAppointmentAndTechnicianAndRecordsHistory() {
        Job job = setup(JobStatus.SCHEDULED);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", "unused"));
        try {
            service.reschedule(1L, start, start.plusHours(1), 2L, "ลูกค้าขอเลื่อน");
            assertEquals(start, job.getAppointmentStart()); assertEquals(start.plusHours(1), job.getAppointmentEnd());
            assertEquals(JobStatus.ASSIGNED, job.getJobStatus()); assertNotNull(job.getTechnician());
            verify(activities).save(argThat(a -> a.getActivityType().equals("JOB_RESCHEDULED") && a.getOldValue().contains("02/10/2026") && a.getNewValue().contains("03/10/2026")));
        } finally { SecurityContextHolder.clearContext(); }
    }
}
