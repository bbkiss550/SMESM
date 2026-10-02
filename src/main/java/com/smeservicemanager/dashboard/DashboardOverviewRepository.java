package com.smeservicemanager.dashboard;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class DashboardOverviewRepository {
    private final JdbcClient jdbc;

    public DashboardOverviewRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public Map<String, java.math.BigDecimal> revenue(List<DashboardPeriod.Window> windows, LocalDateTime now) {
        var values = new java.util.ArrayList<String>();
        for (int i = 0; i < windows.size(); i++) values.add("(:key" + i + ",cast(:start" + i + " as timestamp),cast(:cutoff" + i + " as timestamp))");
        var query = jdbc.sql("with windows(key,start_at,cutoff_at) as (values " + String.join(",", values) + ")" + """
            select w.key,coalesce(sum(p.p_amount),0) amount from windows w
            left join t_payment p on p.status='A' and p.p_record_status='ACTIVE'
              and (p.p_payment_date at time zone 'Asia/Bangkok' at time zone 'UTC')>=w.start_at
              and (p.p_payment_date at time zone 'Asia/Bangkok' at time zone 'UTC')<w.cutoff_at
            group by w.key
            """);
        for (int i = 0; i < windows.size(); i++) {
            var window = windows.get(i);
            query.param("key" + i, window.key()).param("start" + i, window.start().atStartOfDay())
                .param("cutoff" + i, DashboardPeriod.cutoff(window, now));
        }
        var result = new java.util.HashMap<String, java.math.BigDecimal>();
        for (var row : query.query().listOfRows()) result.put((String)row.get("key"),(java.math.BigDecimal)row.get("amount"));
        return result;
    }

    // Aggregate each bounded period in PostgreSQL; never load individual jobs into Java.
    public List<Map<String, Object>> aggregate(List<DashboardPeriod.Window> windows, LocalDateTime now) {
        var values = new java.util.ArrayList<String>();
        for (int i = 0; i < windows.size(); i++) values.add("(:key" + i + ",cast(:start" + i + " as timestamp),cast(:end" + i + " as timestamp),cast(:cutoff" + i + " as timestamp))");
        String sql = "with windows(key,start_at,end_at,cutoff_at) as (values " + String.join(",", values) + ")" + """
            , history as (
                select id_t_job, ja_activity_date, id_t_job_activity, ja_old_value, ja_new_value
                from t_job_activity where status='A'
                  and ja_new_value in ('NEW','SCHEDULED','ASSIGNED','IN_PROGRESS','WAITING_PART','COMPLETED','CANCELLED')
            ), jobs as (
                select j.*, coalesce(j.j_completed_date,
                    (select min(h.ja_activity_date) from history h where h.id_t_job=j.id_t_job and h.ja_new_value='CANCELLED'),
                    case when j.j_job_status='CANCELLED' then j.update_date end) closed_at
                from t_job j where j.status='A'
            ), population as (
                select w.*,j.*,s.s_service_name,
                    coalesce(
                      (select h.ja_new_value from history h where h.id_t_job=j.id_t_job and h.ja_activity_date<w.cutoff_at
                       order by h.ja_activity_date desc,h.id_t_job_activity desc limit 1),
                      (select nullif(h.ja_old_value,'') from history h where h.id_t_job=j.id_t_job and h.ja_activity_date>=w.cutoff_at
                         and h.ja_old_value in ('NEW','SCHEDULED','ASSIGNED','IN_PROGRESS','WAITING_PART','COMPLETED','CANCELLED')
                       order by h.ja_activity_date,h.id_t_job_activity limit 1),
                      case when j.closed_at>=w.cutoff_at or j.update_date>=w.cutoff_at then 'UNKNOWN' else j.j_job_status end) snapshot_status
                from windows w join jobs j on w.start_at<w.cutoff_at and j.create_date<w.cutoff_at
                    and (j.create_date>=w.start_at or j.closed_at is null or j.closed_at>=w.start_at)
                join m_service s on s.id_m_service=j.id_m_service
            )
            select key,snapshot_status,s_service_name,count(*) total,
              count(*) filter(where create_date>=start_at) opened,
              count(*) filter(where create_date<start_at) carried,
              count(*) filter(where j_completed_date>=start_at and j_completed_date<cutoff_at) completed,
              count(*) filter(where j_job_status='CANCELLED' and closed_at>=start_at and closed_at<cutoff_at) cancelled,
              count(*) filter(where snapshot_status not in ('COMPLETED','CANCELLED')
                and (j_appointment_end at time zone 'Asia/Bangkok' at time zone 'UTC')<cutoff_at) overdue,
              count(*) filter(where snapshot_status='UNKNOWN') unknown
            from population group by key,snapshot_status,s_service_name
            """;
        var query = jdbc.sql(sql);
        for (int i = 0; i < windows.size(); i++) {
            var window = windows.get(i);
            query.param("key" + i, window.key()).param("start" + i, window.start().atStartOfDay())
                .param("end" + i, window.end().atStartOfDay()).param("cutoff" + i, DashboardPeriod.cutoff(window, now));
        }
        return query.query().listOfRows();
    }
}
