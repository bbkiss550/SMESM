package com.smeservicemanager.report;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static com.smeservicemanager.report.ServiceReportRepository.DateBasis.*;

@SpringBootTest
@Transactional
class ServiceReportIntegrationTests {
    @Autowired JdbcClient jdbc;
    @Autowired ServiceReportService service;
    @Autowired ServiceReportRepository repository;
    @Autowired ServiceReportPdfService pdf;
    @Autowired ReportController controller;
    private void job(String created, String status, String active, int value) {
        jobNamed("R-" + UUID.randomUUID(), created, status, active, value);
    }
    private void jobNamed(String jobNo, String created, String status, String active, int value) {
        jdbc.sql("""
            insert into t_job(id_m_customer,id_m_service,j_job_no,j_title,j_address,j_job_status,status,
                j_appointment_start,j_appointment_end,j_completed_date,j_grand_total,create_date,update_date)
            values((select min(id_m_customer) from m_customer),(select min(id_m_service) from m_service),
                :no,'Report test','Test',:state,:active,timestamp '2018-04-02 09:00',timestamp '2018-04-02 10:00',
                case when :state = 'COMPLETED' then timestamp '2018-04-03 18:00' else null end,
                :value,cast(:created as timestamp),cast(:created as timestamp))
            """).param("no", jobNo).param("state",status).param("active",active)
                .param("value",value).param("created",created).update();
    }
    @Test void rowsAreOrderedByJobNumberAscendingOnScreenAndInPrint() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String low = "R-001-" + suffix, middle = "R-002-" + suffix, high = "R-003-" + suffix;
        jobNamed(high, "2018-04-01 08:00", "NEW", "A", 100);
        jobNamed(low, "2018-04-01 10:00", "NEW", "A", 100);
        jobNamed(middle, "2018-04-01 09:00", "NEW", "A", 100);
        var day = LocalDate.of(2018,4,1);
        var expected = java.util.List.of(low, middle, high);
        assertIterableEquals(expected, service.load(day,day,CREATED,null,null,null,0).jobs()
                .stream().map(com.smeservicemanager.job.Job::getJobNo).toList());
        assertIterableEquals(expected, repository.findForPrint(day,day,CREATED,null,null,null)
                .stream().map(com.smeservicemanager.job.Job::getJobNo).toList());
    }
    @Test void inclusiveRangeAndSummaryExcludeInactiveAndCancelledValue() {
        job("2018-04-01 00:00", "COMPLETED", "A", 107);
        job("2018-04-01 23:59:59", "CANCELLED", "A", 999);
        job("2018-04-02 00:00", "NEW", "A", 300);
        job("2018-04-01 12:00", "NEW", "D", 500);
        var day = LocalDate.of(2018,4,1);
        var result = service.load(day,day,CREATED,null,null,null,0);
        assertEquals(2,result.total()); assertEquals(1,result.completed()); assertEquals(1,result.cancelled());
        assertEquals(0,new BigDecimal("107").compareTo(result.value()));
        assertEquals(2,result.jobs().size());
        assertEquals(1, service.load(day,day,CREATED,com.smeservicemanager.shared.domain.DomainTypes.JobStatus.COMPLETED,null,null,0).total());
        assertEquals(0, service.load(day,day,CREATED,null,Long.MAX_VALUE,null,0).total());
        assertEquals(0, service.load(day,day,CREATED,null,null,Long.MAX_VALUE,0).total());
        assertEquals(3, service.load(day.plusDays(1),day.plusDays(1),APPOINTMENT,null,null,null,0).total());
        assertEquals(1, service.load(day.plusDays(2),day.plusDays(2),COMPLETED,null,null,null,0).total());
    }
    @Test void summaryCoversAllPagesAndPageIsClamped() {
        for (int i=0;i<12;i++) job("2018-04-01 12:00", "NEW", "A", 100);
        var day = LocalDate.of(2018,4,1);
        var first = service.load(day,day,CREATED,null,null,null,0);
        var last = service.load(day,day,CREATED,null,null,null,999);
        assertEquals(12,first.total()); assertEquals(10,first.jobs().size());
        assertEquals(2,last.jobs().size()); assertEquals(1,last.page());
        assertEquals(first.value(),last.value());
        assertThrows(IllegalArgumentException.class, () -> service.load(day.plusDays(1),day,CREATED,null,null,null,0));
    }
    @Test void printRendersJasperPdfForFilteredJobsAndEmptyResults() {
        job("2018-04-01 12:00", "COMPLETED", "A", 107);
        var day = LocalDate.of(2018,4,1);
        byte[] withJob = pdf.render(day,day,CREATED,null,null,null);
        byte[] empty = pdf.render(day.plusDays(2),day.plusDays(2),CREATED,null,null,null);
        assertTrue(withJob.length > 1000);
        assertTrue(empty.length > 1000);
        assertEquals("%PDF-", new String(withJob,0,5,java.nio.charset.StandardCharsets.US_ASCII));
        assertEquals("%PDF-", new String(empty,0,5,java.nio.charset.StandardCharsets.US_ASCII));
    }
    @Test void previewReturnsPdfInJsonWithoutTriggeringPdfDownload() {
        var day = LocalDate.of(2018,4,1);
        var response = controller.servicePreview(day, day, CREATED, null, null, null);
        assertEquals("no-store", response.getHeaders().getCacheControl());
        byte[] decoded = java.util.Base64.getDecoder().decode(response.getBody().get("pdfBase64"));
        assertEquals("%PDF-", new String(decoded,0,5,java.nio.charset.StandardCharsets.US_ASCII));
    }
}
