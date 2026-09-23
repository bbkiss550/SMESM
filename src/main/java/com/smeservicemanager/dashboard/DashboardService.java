package com.smeservicemanager.dashboard;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final JdbcClient jdbc;

    public DashboardService(JdbcClient jdbc) { this.jdbc = jdbc; }

    public DashboardSummary summary() {
        Map<String, Object> row = jdbc.sql("""
                select count(*) as total_jobs,
                       count(*) filter (where j_appointment_start::date = current_date) as today_jobs,
                       count(*) filter (where j_job_status = 'IN_PROGRESS') as in_progress,
                       count(*) filter (where j_job_status in ('SCHEDULED','ASSIGNED')) as scheduled,
                       count(*) filter (where j_job_status = 'COMPLETED' and date_trunc('month', j_completed_date) = date_trunc('month', current_date)) as completed_month
                  from t_job where status = 'A'
                """).query().singleRow();
        BigDecimal revenue = jdbc.sql("""
                select coalesce(sum(p_amount),0) from t_payment
                 where status='A' and p_record_status='ACTIVE'
                   and date_trunc('month', p_payment_date)=date_trunc('month', current_date)
                """).query(BigDecimal.class).single();
        return new DashboardSummary(number(row.get("total_jobs")), number(row.get("today_jobs")),
                number(row.get("in_progress")), number(row.get("scheduled")), number(row.get("completed_month")), revenue);
    }

    public List<Map<String, Object>> monthlyJobs() {
        return jdbc.sql("""
                with months as (select generate_series(date_trunc('month', current_date) - interval '5 months', date_trunc('month', current_date), interval '1 month') m)
                select to_char(m.m, 'YYYY-MM') as month, count(j.id_t_job) as value
                  from months m left join t_job j on date_trunc('month', j.create_date) = m.m and j.status='A'
                 group by m.m order by m.m
                """).query().listOfRows();
    }

    public List<Map<String, Object>> monthlyRevenue() {
        return jdbc.sql("""
                with months as (select generate_series(date_trunc('month', current_date) - interval '5 months', date_trunc('month', current_date), interval '1 month') m)
                select to_char(m.m, 'YYYY-MM') as month, coalesce(sum(p.p_amount),0) as value
                  from months m left join t_payment p on date_trunc('month', p.p_payment_date) = m.m and p.status='A' and p.p_record_status='ACTIVE'
                 group by m.m order by m.m
                """).query().listOfRows();
    }

    public List<Map<String, Object>> popularServices() {
        return jdbc.sql("""
                select s.s_service_name as label, count(*) as value
                  from t_job j join m_service s on s.id_m_service=j.id_m_service
                 where j.status='A' group by s.s_service_name order by value desc limit 5
                """).query().listOfRows();
    }

    public List<Map<String, Object>> revenueByDay() {
        return jdbc.sql("""
                with days as (select generate_series(current_date - interval '29 days', current_date, interval '1 day') d)
                select to_char(d.d, 'DD/MM') as label, coalesce(sum(p.p_amount),0) as value
                  from days d left join t_payment p on p.p_payment_date::date=d.d::date and p.p_record_status='ACTIVE' and p.status='A'
                 group by d.d order by d.d
                """).query().listOfRows();
    }

    public List<Map<String, Object>> jobStatusBreakdown() {
        return jdbc.sql("""
                select case j_job_status
                         when 'NEW' then 'ใหม่'
                         when 'SCHEDULED' then 'นัดหมายแล้ว'
                         when 'ASSIGNED' then 'มอบหมายแล้ว'
                         when 'IN_PROGRESS' then 'กำลังดำเนินการ'
                         when 'WAITING_PART' then 'รออะไหล่'
                         when 'COMPLETED' then 'เสร็จสิ้น'
                         when 'CANCELLED' then 'ยกเลิก'
                       end as label,
                       count(*) as value
                  from t_job
                 where status='A'
                 group by j_job_status
                 order by value desc
                """)
                .query().listOfRows();
    }

    public List<Map<String, Object>> technicianPerformance() {
        return jdbc.sql("""
                select concat(u.u_first_name,' ',u.u_last_name) as technician,
                       count(j.id_t_job) as assigned,
                       count(j.id_t_job) filter (where j.j_job_status='COMPLETED') as completed,
                       round(100.0 * count(j.id_t_job) filter (where j.j_job_status='COMPLETED') / nullif(count(j.id_t_job),0),1) as completion_rate,
                       coalesce(sum(j.j_grand_total) filter (where j.j_job_status='COMPLETED'),0) as revenue
                  from m_user u join m_role r on r.id_m_role=u.id_m_role
                  left join t_job j on j.id_m_user=u.id_m_user and j.status='A'
                 where r.r_code='TECHNICIAN' and u.status='A'
                 group by u.id_m_user order by completed desc
                """).query().listOfRows();
    }

    private long number(Object value) { return value == null ? 0 : ((Number) value).longValue(); }

    public record DashboardSummary(long totalJobs, long todayJobs, long inProgress, long scheduled,
                                   long completedMonth, BigDecimal revenueMonth) {}
}
