package com.smeservicemanager.dashboard;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardOverviewTests {
    @Test void dailyIncludesSelectedPreviousAndSevenDays() {
        var windows = DashboardPeriod.DAILY.windows(LocalDate.of(2026,10,2));
        assertEquals(9, windows.size()); assertEquals(LocalDate.of(2026,10,1), windows.get(1).start());
        assertEquals(LocalDate.of(2026,9,26), windows.get(2).start());
    }
    @Test void monthlyHandlesLeapYear() {
        var windows = DashboardPeriod.MONTHLY.windows(LocalDate.of(2024,2,25));
        assertEquals(31, windows.size()); assertEquals(LocalDate.of(2024,3,1), windows.get(0).end());
        assertEquals(LocalDate.of(2024,1,1), windows.get(1).start());
    }
    @Test void yearlyHasTwelveMonthsAndPreviousYear() {
        var windows = DashboardPeriod.YEARLY.windows(LocalDate.of(2026,10,2));
        assertEquals(14, windows.size()); assertEquals(LocalDate.of(2025,1,1), windows.get(1).start());
        assertEquals(LocalDate.of(2026,12,1), windows.get(13).start());
    }
    @Test void comparisonsHandleZeroAndNegativeChange() {
        assertNull(DashboardOverviewService.change(5,0)); assertNull(DashboardOverviewService.change(0,0));
        assertEquals(new BigDecimal("133.3"), DashboardOverviewService.change(7,3));
        assertEquals(new BigDecimal("-50.0"), DashboardOverviewService.change(1,2));
    }
    @Test void revenueKeepsSatangAndUsesPreviousPeriod() {
        var repository = mock(DashboardOverviewRepository.class);
        when(repository.aggregate(anyList(),any())).thenReturn(List.of());
        when(repository.revenue(anyList(),any())).thenReturn(Map.of("selected",new BigDecimal("125.75"),"previous",new BigDecimal("100.00")));
        var result = new DashboardOverviewService(repository).overview(DashboardPeriod.DAILY,LocalDate.of(2026,10,2),LocalDateTime.of(2026,10,2,12,0));
        var revenue = result.cards().stream().filter(c -> c.key().equals("revenue")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("125.75"),revenue.value());
        assertEquals(new BigDecimal("25.8"),revenue.change()); assertTrue(revenue.money());
        assertEquals(0,result.total());
    }
    @Test void currentPeriodStopsAtNow() {
        var now = LocalDateTime.of(2026,10,2,12,0);
        assertEquals(now, DashboardPeriod.cutoff(DashboardPeriod.MONTHLY.window("selected",now.toLocalDate()),now));
    }
    @Test void pendingGroupsAllPreWorkStatuses() {
        for (String status : List.of("NEW","SCHEDULED","ASSIGNED")) assertEquals("PENDING",DashboardOverviewService.groupStatus(status));
        assertEquals("WAITING_PART",DashboardOverviewService.groupStatus("WAITING_PART"));
    }
    @Test void emptyPeriodHasZeroCardsAndNoComparisonBase() {
        var repository = mock(DashboardOverviewRepository.class);
        when(repository.aggregate(anyList(),any())).thenReturn(List.of());
        var result = new DashboardOverviewService(repository).overview(DashboardPeriod.DAILY,LocalDate.of(2026,10,2),LocalDateTime.of(2026,10,2,12,0));
        assertEquals(0,result.total()); assertEquals(10,result.cards().size());
        assertTrue(result.cards().stream().allMatch(card -> card.value().longValue()==0 && card.change()==null));
    }
    @Test void totalsStatusAndServiceShareSamePopulation() {
        var repository = mock(DashboardOverviewRepository.class);
        when(repository.aggregate(anyList(),any())).thenReturn(List.of(Map.of("key","selected","snapshot_status","ASSIGNED","s_service_name","ซ่อมแอร์","total",3L,"opened",1L,"carried",2L,"overdue",1L)));
        var result = new DashboardOverviewService(repository).overview(DashboardPeriod.DAILY,LocalDate.of(2026,10,2),LocalDateTime.of(2026,10,2,12,0));
        assertEquals(3,result.total()); assertEquals(3,result.statuses().stream().mapToLong(DashboardOverviewService.Group::value).sum());
        assertEquals(3,result.services().stream().mapToLong(DashboardOverviewService.Group::value).sum());
        assertEquals(3,result.cards().stream().filter(c->c.key().equals("pending")).findFirst().orElseThrow().value().longValue());
    }
}
