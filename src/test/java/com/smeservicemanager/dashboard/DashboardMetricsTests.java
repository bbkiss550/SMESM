package com.smeservicemanager.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.YearMonth;
import static org.junit.jupiter.api.Assertions.*;

class DashboardMetricsTests {
    private final LocalDate date = LocalDate.of(2026,12,31);
    @Test void dailyRangeUsesSelectedDayAndExclusiveNextDay() {
        var range = DashboardMetrics.range("day",date,YearMonth.of(2026,10));
        assertEquals(date,range.start()); assertEquals(LocalDate.of(2027,1,1),range.end());
    }
    @Test void monthlyRangeIsIndependentOfSelectedDayAndHandlesLeapYear() {
        var range = DashboardMetrics.range("month",date,YearMonth.of(2024,2));
        assertEquals(LocalDate.of(2024,2,1),range.start()); assertEquals(LocalDate.of(2024,3,1),range.end());
    }
    @Test void currentStatusHasNoDateRestriction() {
        assertNull(DashboardMetrics.range("current",date,YearMonth.of(2026,10)));
    }
    @Test void unsupportedMetricsAndScopesAreRejected() {
        assertThrows(ResponseStatusException.class, () -> DashboardMetrics.range("invalid",date,YearMonth.of(2026,10)));
        assertThrows(ResponseStatusException.class, () -> DashboardMetrics.Metric.parse("invalid"));
    }
}
