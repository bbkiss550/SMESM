package com.smeservicemanager.dashboard;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardMetrics {
    private static final String STARTED = " left join (select id_t_job, min(ja_activity_date) as started_at from t_job_activity where status='A' and ja_activity_type='JOB_STARTED' group by id_t_job) a on a.id_t_job=j.id_t_job ";
    private final JdbcClient jdbc;
    public DashboardMetrics(JdbcClient jdbc) { this.jdbc = jdbc; }

    public enum Metric {
        ALL("งานทั้งหมด", "ข้อมูลทั้งหมด", "clipboard2-check", "j.create_date", "true"),
        OPENED("เคสเปิดใหม่", "วันที่เปิดเคส", "file-earmark-plus", "j.create_date", "true"),
        APPOINTMENT("นัดหมาย", "วันนัดหมายล่าสุด", "calendar-event", "(j.j_appointment_start at time zone 'Asia/Bangkok' at time zone 'UTC')", "true"),
        STARTED_WORK("เริ่มงานจริง", "เวลาเริ่มงานครั้งแรก", "play", "a.started_at", "true"),
        COMPLETED("เสร็จงาน", "วันที่ปิดงานจริง", "check-circle", "j.j_completed_date", "j.j_job_status='COMPLETED'"),
        REVENUE("ยอดรับชำระ", "วันที่ชำระเงินจริง", "cash-stack", "(p.p_payment_date at time zone 'Asia/Bangkok' at time zone 'UTC')", "p.status='A' and p.p_record_status='ACTIVE'"),
        UNASSIGNED("รอมอบหมาย", "สถานะปัจจุบัน", "clock", "j.j_appointment_start", "j.j_job_status in ('NEW','SCHEDULED')"),
        ASSIGNED("มอบหมายแล้ว", "สถานะปัจจุบัน", "person-check", "j.j_appointment_start", "j.j_job_status='ASSIGNED'"),
        PROGRESS("กำลังดำเนินการ", "สถานะปัจจุบัน", "gear", "j.j_appointment_start", "j.j_job_status='IN_PROGRESS'"),
        WAITING("รออะไหล่", "สถานะปัจจุบัน", "wrench", "j.j_appointment_start", "j.j_job_status='WAITING_PART'");
        final String label, caption, icon, column, condition;
        Metric(String label, String caption, String icon, String column, String condition) {
            this.label=label; this.caption=caption; this.icon=icon; this.column=column; this.condition=condition;
        }
        public static Metric parse(String kind) {
            try { return valueOf(kind); } catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ไม่พบประเภทข้อมูล"); }
        }
    }
    public record Card(String kind, String label, String caption, String icon, Object value, boolean money) {}
    public record Range(LocalDate start, LocalDate end) {}
    public static Range range(String scope, LocalDate date, YearMonth month) {
        return switch (scope) {
            case "day" -> new Range(date, date.plusDays(1));
            case "month" -> new Range(month.atDay(1), month.plusMonths(1).atDay(1));
            case "current" -> null;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ไม่พบช่วงเวลา");
        };
    }
    private String condition(Metric metric, Range range) {
        return metric.condition + (range == null ? "" : " and " + metric.column + ">=:startAt and " + metric.column + "<:endAt");
    }
    private JdbcClient.StatementSpec bind(String sql, Range range) {
        var query = jdbc.sql(sql);
        return range == null ? query : query.param("startAt", range.start().atStartOfDay()).param("endAt", range.end().atStartOfDay());
    }
    public List<Card> cards(String scope, LocalDate date, YearMonth month) {
        Range range = range(scope, date, month);
        List<Metric> metrics = switch (scope) {
            case "day" -> List.of(Metric.OPENED, Metric.APPOINTMENT, Metric.STARTED_WORK, Metric.COMPLETED);
            case "month" -> List.of(Metric.OPENED, Metric.STARTED_WORK, Metric.COMPLETED);
            default -> List.of(Metric.UNASSIGNED, Metric.ASSIGNED, Metric.PROGRESS, Metric.WAITING);
        };
        String columns = metrics.stream().map(m -> "count(*) filter (where " + condition(m, range) + ") as " + m.name()).collect(java.util.stream.Collectors.joining(","));
        Map<String,Object> counts = bind("select " + columns + " from t_job j " + STARTED + " where j.status='A'", range).query().singleRow();
        var result = new java.util.ArrayList<Card>();
        for (Metric metric : metrics) result.add(new Card(metric.name(), metric.label, metric.caption, metric.icon, counts.get(metric.name().toLowerCase(java.util.Locale.ROOT)), false));
        if (range != null) {
            Object paid = bind("select coalesce(sum(p.p_amount),0) from t_payment p where " + condition(Metric.REVENUE, range), range).query(java.math.BigDecimal.class).single();
            result.add(new Card(Metric.REVENUE.name(), Metric.REVENUE.label, Metric.REVENUE.caption, Metric.REVENUE.icon, paid, true));
        }
        return result;
    }
    public DashboardService.DetailPage details(String kind, String scope, LocalDate date, YearMonth month, int page) {
        Metric metric = Metric.parse(kind);
        Range range = range(scope, date, month);
        boolean revenue = metric == Metric.REVENUE;
        if (revenue && range == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "กรุณาเลือกช่วงเวลา");
        String from = revenue ? " from t_payment p join t_job j on j.id_t_job=p.id_t_job " : " from t_job j " + STARTED;
        from += " join m_customer c on c.id_m_customer=j.id_m_customer where " + (revenue ? "" : "j.status='A' and ") + condition(metric, range);
        long total = bind("select count(*)" + from, range).query(Long.class).single();
        int current = Math.min(Math.max(0,page), (int)Math.max(0,(total-1)/10));
        String fields = "select j.j_job_no as job_no, coalesce(nullif(c.c_company_name,''),c.c_name) as customer, j.j_title as title, j.j_job_status as job_status, ";
        fields += revenue ? "p.p_payment_no as document_no, p.p_amount as amount, " : "j.j_job_no as document_no, j.j_grand_total as amount, ";
        String displayColumn = switch (metric) {
            case UNASSIGNED, ASSIGNED, PROGRESS, WAITING -> Metric.APPOINTMENT.column;
            default -> metric.column;
        };
        fields += "to_char(" + displayColumn + ",'DD/MM/YYYY HH24:MI') as date_label";
        String order = metric.column + " desc nulls last, " + (revenue ? "p.id_t_payment" : "j.id_t_job") + " desc";
        var rows = bind(fields + from + " order by " + order + " limit 10 offset :offset", range).param("offset",current*10).query().listOfRows();
        return new DashboardService.DetailPage(rows,total,current,(int)((total+9)/10));
    }
    public String dateLabel(String kind) {
        return switch (Metric.parse(kind)) {
            case ALL, OPENED -> "วันที่เปิดเคส";
            case UNASSIGNED, ASSIGNED, PROGRESS, WAITING, APPOINTMENT -> "นัดหมาย";
            default -> Metric.parse(kind).caption;
        };
    }
}
