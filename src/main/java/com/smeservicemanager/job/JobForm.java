package com.smeservicemanager.job;

import com.smeservicemanager.shared.domain.DomainTypes.JobPriority;
import com.smeservicemanager.shared.domain.DomainTypes.JobType;
import com.smeservicemanager.shared.domain.DomainTypes.VatMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter @Setter
public class JobForm {
    @NotNull(message = "กรุณาเลือกลูกค้า")
    private Long customerId;
    @NotNull(message = "กรุณาเลือกบริการ")
    private Long serviceId;
    private Long technicianId;
    private Long originalJobId;
    @NotBlank(message = "กรุณาระบุหัวข้องาน")
    private String title;
    private String description;
    @NotNull(message = "กรุณาเลือกระดับความสำคัญ")
    private JobPriority priority = JobPriority.NORMAL;
    private JobType jobType = JobType.NORMAL;
    @NotNull(message = "กรุณาระบุวันที่เริ่ม")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate appointmentStartDate;
    @NotNull(message = "กรุณาระบุเวลาเริ่ม")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime appointmentStartTime;
    @NotNull(message = "กรุณาระบุวันที่สิ้นสุด")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate appointmentEndDate;
    @NotNull(message = "กรุณาระบุเวลาสิ้นสุด")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime appointmentEndTime;
    private String contactPhone;
    @NotBlank(message = "กรุณาระบุสถานที่ให้บริการ")
    private String address;
    private String internalNote;
    private String reworkReason;
    private boolean underWarranty;
    private boolean chargeable = true;
    private VatMode vatMode = VatMode.INCLUSIVE;
    private BigDecimal discount = BigDecimal.ZERO;

    public LocalDateTime getAppointmentStart() {
        return combine(appointmentStartDate, appointmentStartTime);
    }

    public LocalDateTime getAppointmentEnd() {
        return combine(appointmentEndDate, appointmentEndTime);
    }

    private LocalDateTime combine(LocalDate date, LocalTime time) {
        return date == null || time == null ? null : LocalDateTime.of(date, time);
    }
}
