package com.smeservicemanager.job;

import com.smeservicemanager.payment.PaymentForm;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeparatedDateTimeFormTests {

    @Test
    void combinesJobDateAndTimeFieldsForTheServiceLayer() {
        JobForm form = new JobForm();
        form.setAppointmentStartDate(LocalDate.of(2026, 9, 23));
        form.setAppointmentStartTime(LocalTime.of(9, 30));
        form.setAppointmentEndDate(LocalDate.of(2026, 9, 23));
        form.setAppointmentEndTime(LocalTime.of(11, 0));

        assertEquals(LocalDateTime.of(2026, 9, 23, 9, 30), form.getAppointmentStart());
        assertEquals(LocalDateTime.of(2026, 9, 23, 11, 0), form.getAppointmentEnd());
    }

    @Test
    void combinesPaymentDateAndTimeFieldsForTheServiceLayer() {
        PaymentForm form = new PaymentForm();
        form.setPaymentDay(LocalDate.of(2026, 9, 23));
        form.setPaymentTime(LocalTime.of(14, 45));

        assertEquals(LocalDateTime.of(2026, 9, 23, 14, 45), form.getPaymentDate());
    }
}
