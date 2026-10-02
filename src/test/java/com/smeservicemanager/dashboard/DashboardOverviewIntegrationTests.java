package com.smeservicemanager.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DashboardOverviewIntegrationTests {
    @Autowired JdbcClient jdbc;
    @Autowired DashboardOverviewService service;

    private long job(String opened, String status, String closed) {
        long id = jdbc.sql("""
            insert into t_job(id_m_customer,id_m_service,j_job_no,j_title,j_address,j_job_status,j_appointment_start,j_appointment_end,j_completed_date,create_date,update_date)
            values((select min(id_m_customer) from m_customer),(select min(id_m_service) from m_service),:no,'Dashboard test','Test',:status,
              timestamp '2020-01-02 17:00',timestamp '2020-01-02 18:00',cast(:closed as timestamp),cast(:opened as timestamp),cast(:updated as timestamp)) returning id_t_job
            """).param("no","T-"+UUID.randomUUID()).param("status",status).param("closed",status.equals("COMPLETED")?closed:null).param("opened",opened)
            .param("updated",closed==null?opened:closed).query(Long.class).single();
        activity(id,opened,"", "ASSIGNED");
        if (status.equals("COMPLETED")) activity(id,closed,"IN_PROGRESS","COMPLETED");
        if (status.equals("CANCELLED")) activity(id,closed,"ASSIGNED","CANCELLED");
        return id;
    }
    private void activity(long id,String date,String before,String after) {
        jdbc.sql("insert into t_job_activity(id_t_job,ja_activity_type,ja_description,ja_old_value,ja_new_value,ja_activity_date) values(:id,'STATUS_CHANGED','Test',:before,:after,cast(:date as timestamp))")
            .param("id",id).param("date",date).param("before",before).param("after",after).update();
    }
    private DashboardOverviewService.Overview overview(DashboardPeriod period, String date) {
        return service.overview(period,LocalDate.parse(date),LocalDateTime.of(2020,1,4,12,0));
    }
    private long value(DashboardOverviewService.Overview dto,String key) { return dto.cards().stream().filter(c->c.key().equals(key)).findFirst().orElseThrow().value().longValue(); }

    @Test void cohortIncludesOpenedAndCarriedButNotEarlierClosedOrFutureJobs() {
        job("2020-01-01 09:00","ASSIGNED",null);
        job("2020-01-02 09:00","ASSIGNED",null);
        job("2020-01-01 09:00","COMPLETED","2020-01-01 10:00");
        job("2020-01-03 09:00","ASSIGNED",null);
        var result=overview(DashboardPeriod.DAILY,"2020-01-02");
        assertEquals(2,result.total()); assertEquals(1,value(result,"opened")); assertEquals(1,value(result,"carried"));
        assertEquals(2,result.statuses().stream().mapToLong(DashboardOverviewService.Group::value).sum());
    }
    @Test void completionAndCancellationUseEventDates() {
        job("2020-01-01 09:00","COMPLETED","2020-01-02 15:00");
        job("2020-01-01 09:00","CANCELLED","2020-01-02 16:00");
        var result=overview(DashboardPeriod.DAILY,"2020-01-02");
        assertEquals(2,result.total()); assertEquals(1,value(result,"completed")); assertEquals(1,value(result,"cancelled"));
        assertEquals(0,value(result,"overdue"));
    }
    @Test void overdueExcludesClosedJobsAndUsesLocalAppointmentTime() {
        job("2020-01-01 09:00","ASSIGNED",null);
        job("2020-01-01 09:00","COMPLETED","2020-01-02 15:00");
        assertEquals(1,value(overview(DashboardPeriod.DAILY,"2020-01-02"),"overdue"));
    }
    @Test void historicalStatusesUseActivityNotCurrentStatus() {
        long id=job("2020-01-01 09:00","COMPLETED","2020-01-03 15:00");
        activity(id,"2020-01-02 10:00","ASSIGNED","WAITING_PART");
        var result=overview(DashboardPeriod.DAILY,"2020-01-02");
        assertEquals(1,value(result,"waiting")); assertEquals(0,value(result,"completed"));
    }
    @Test void monthlyAndYearlyUseWholePeriodWithoutDuplicatingJobs() {
        job("2019-12-30 09:00","ASSIGNED",null);
        job("2020-01-02 09:00","ASSIGNED",null);
        for (var period: new DashboardPeriod[]{DashboardPeriod.MONTHLY,DashboardPeriod.YEARLY}) {
            var result=overview(period,"2020-01-02");
            assertEquals(2,result.total()); assertEquals(1,value(result,"opened")); assertEquals(1,value(result,"carried"));
            assertEquals(2,result.services().stream().mapToLong(DashboardOverviewService.Group::value).sum());
        }
    }
    @Test void futurePeriodIsEmptyEvenWithCurrentBacklog() {
        job("2020-01-01 09:00","ASSIGNED",null);
        assertEquals(0,overview(DashboardPeriod.DAILY,"2020-02-01").total());
    }
    private void payment(long jobId, String date, String amount, String status, String recordStatus) {
        String no="T-"+UUID.randomUUID();
        jdbc.sql("insert into t_payment(id_t_job,p_payment_no,p_receipt_no,p_payment_date,p_amount,p_payment_type,p_payment_method,status,p_record_status) values(:job,:no,:no,cast(:date as timestamp),cast(:amount as numeric),'DEPOSIT','TRANSFER',:status,:record)")
            .param("job",jobId).param("no",no).param("date",date).param("amount",amount).param("status",status).param("record",recordStatus).update();
    }
    @Test void revenueUsesPaymentPeriodAndExcludesVoidInactiveAndFuturePayments() {
        long id=job("2019-12-01 09:00","COMPLETED","2019-12-02 10:00");
        payment(id,"2020-01-02 07:30","125.75","A","ACTIVE");
        payment(id,"2020-01-01 19:00","100.00","A","ACTIVE");
        payment(id,"2020-01-02 19:00","500.00","A","VOID");
        payment(id,"2020-01-02 19:00","600.00","D","ACTIVE");
        payment(id,"2020-01-05 19:00","700.00","A","ACTIVE");
        var daily=overview(DashboardPeriod.DAILY,"2020-01-02");
        var card=daily.cards().stream().filter(c->c.key().equals("revenue")).findFirst().orElseThrow();
        assertEquals(new java.math.BigDecimal("125.75"),card.value());
        assertEquals(new java.math.BigDecimal("100.00"),card.previous());
        assertEquals(0,daily.total());
        for (var period: new DashboardPeriod[]{DashboardPeriod.MONTHLY,DashboardPeriod.YEARLY}) {
            var total=overview(period,"2020-01-02").cards().stream().filter(c->c.key().equals("revenue")).findFirst().orElseThrow();
            assertEquals(new java.math.BigDecimal("225.75"),total.value());
        }
    }
}
