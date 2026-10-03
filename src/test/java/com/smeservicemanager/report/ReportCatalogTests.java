package com.smeservicemanager.report;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class ReportCatalogTests {
    @Test void hubContainsExactlyNineReportsInFourGroups() {
        var groups = ReportCatalog.groups();
        var reports = groups.stream().flatMap(group -> group.reports().stream()).toList();
        assertEquals(4, groups.size());
        assertEquals(9, reports.size());
        assertEquals(Set.of("SERVICE", "OVERDUE", "APPOINTMENT", "PAYMENT", "EXPENSE", "TECHNICIAN", "CUSTOMER", "REWORK", "STOCK"), reports.stream().map(ReportCatalog.Report::code).collect(Collectors.toSet()));
        assertEquals(9, reports.stream().map(ReportCatalog.Report::number).distinct().count());
        assertEquals(4, groups.getFirst().reports().size());
    }
    @Test void controllerProvidesOnlyHubMetadata() {
        var model = new ExtendedModelMap();
        assertEquals("report/index", new ReportController(null, null, null, null, null, null).reports(model));
        assertEquals(Set.of("reportGroups"), model.keySet());
    }
}
