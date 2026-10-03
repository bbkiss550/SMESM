package com.smeservicemanager.report;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AdditionalReportRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public List<Map<String, Object>> find(String code, LocalDate start, LocalDate end) {
        var params = new MapSqlParameterSource()
                .addValue("start", start.atStartOfDay())
                .addValue("end", end.plusDays(1).atStartOfDay())
                .addValue("today", LocalDate.now(java.time.ZoneId.of("Asia/Bangkok")));
        return jdbc.queryForList(sql(code), params);
    }

    private static String sql(String code) {
        return switch (code) {
            case "OVERDUE" -> """
                SELECT j.j_job_no c1, c.c_name c2,
                  coalesce(u.u_first_name || ' ' || u.u_last_name, 'ยังไม่มอบหมาย') c3,
                  to_char(j.j_appointment_end,'DD/MM/') || (extract(year from j.j_appointment_end)::int+543) || to_char(j.j_appointment_end,' HH24:MI') c4,
                  greatest(0, :today - j.j_appointment_end::date)::text c5, j.j_job_status c6,
                  greatest(0, :today - j.j_appointment_end::date) n1, j.j_grand_total n2,
                  CASE WHEN j.j_appointment_end::date < :today THEN 1 ELSE 0 END flag
                FROM t_job j JOIN m_customer c ON c.id_m_customer=j.id_m_customer
                LEFT JOIN m_user u ON u.id_m_user=j.id_m_user
                WHERE j.status='A' AND j.j_job_status NOT IN ('COMPLETED','CANCELLED')
                  AND j.j_appointment_end >= :start AND j.j_appointment_end < :end
                ORDER BY j.j_job_no ASC
                """;
            case "APPOINTMENT" -> """
                SELECT j.j_job_no c1, c.c_name c2,
                  to_char(j.j_appointment_start,'DD/MM/') || (extract(year from j.j_appointment_start)::int+543) || to_char(j.j_appointment_start,' HH24:MI') c3,
                  to_char(j.j_appointment_end,'DD/MM/') || (extract(year from j.j_appointment_end)::int+543) || to_char(j.j_appointment_end,' HH24:MI') c4,
                  coalesce(u.u_first_name || ' ' || u.u_last_name, 'ยังไม่มอบหมาย') c5,
                  CASE WHEN coalesce(a.moves,0)=0 THEN 'ไม่เคยเลื่อน' ELSE a.moves::text || ' ครั้ง · ' || coalesce(a.reason,'') END c6,
                  coalesce(a.moves,0) n1, coalesce(a.tech_changes,0) n2,
                  CASE WHEN coalesce(a.moves,0)>0 THEN 1 ELSE 0 END flag
                FROM t_job j JOIN m_customer c ON c.id_m_customer=j.id_m_customer
                LEFT JOIN m_user u ON u.id_m_user=j.id_m_user
                LEFT JOIN LATERAL (
                  SELECT count(*) FILTER (WHERE ja_activity_type='JOB_RESCHEDULED') moves,
                    count(*) FILTER (WHERE ja_activity_type='TECHNICIAN_ASSIGNED' AND ja_old_value IS NOT NULL AND ja_old_value<>ja_new_value)
                      + count(*) FILTER (WHERE ja_activity_type='JOB_RESCHEDULED' AND split_part(ja_old_value,' · ',2)<>split_part(ja_new_value,' · ',2)) tech_changes,
                    (array_agg(ja_description ORDER BY ja_activity_date DESC) FILTER (WHERE ja_activity_type='JOB_RESCHEDULED'))[1] reason
                  FROM t_job_activity WHERE id_t_job=j.id_t_job AND status='A'
                ) a ON true
                WHERE j.status='A' AND j.j_job_status<>'CANCELLED'
                  AND j.j_appointment_start >= :start AND j.j_appointment_start < :end
                ORDER BY j.j_job_no ASC
                """;
            case "PAYMENT" -> """
                SELECT p.p_payment_no c1, j.j_job_no c2, c.c_name c3,
                  to_char(p.p_payment_date,'DD/MM/') || (extract(year from p.p_payment_date)::int+543) || to_char(p.p_payment_date,' HH24:MI') c4,
                  'รับชำระ / ' || p.p_payment_method c5, to_char(p.p_amount,'FM999,999,999,990.00') c6,
                  p.p_amount n1, 0::numeric n2, 0 flag
                FROM t_payment p JOIN t_job j ON j.id_t_job=p.id_t_job JOIN m_customer c ON c.id_m_customer=j.id_m_customer
                WHERE p.status='A' AND p.p_record_status='ACTIVE' AND p.p_payment_date >= :start AND p.p_payment_date < :end
                UNION ALL
                SELECT r.rf_refund_no, j.j_job_no, c.c_name,
                  to_char(r.rf_refund_date,'DD/MM/') || (extract(year from r.rf_refund_date)::int+543) || to_char(r.rf_refund_date,' HH24:MI'),
                  'คืนเงิน / ' || r.rf_refund_method, '-' || to_char(r.rf_amount,'FM999,999,999,990.00'),
                  0::numeric, r.rf_amount, 1
                FROM t_refund r JOIN t_job j ON j.id_t_job=r.id_t_job JOIN m_customer c ON c.id_m_customer=j.id_m_customer
                WHERE r.status='A' AND r.rf_refund_date >= :start AND r.rf_refund_date < :end
                ORDER BY c4, c1
                """;
            case "EXPENSE" -> """
                SELECT j.j_job_no c1, c.c_name c2, p.p_product_name c3,
                  to_char(i.ji_qty,'FM999,999,990.##') || ' ' || i.ji_unit c4,
                  to_char(p.p_cost_price,'FM999,999,999,990.00') c5,
                  to_char(i.ji_qty*p.p_cost_price,'FM999,999,999,990.00') c6,
                  i.ji_qty n1, i.ji_qty*p.p_cost_price n2, 0 flag
                FROM t_job_item i JOIN t_job j ON j.id_t_job=i.id_t_job
                JOIN m_customer c ON c.id_m_customer=j.id_m_customer
                JOIN m_product p ON p.id_m_product=i.id_m_product
                WHERE i.status='A' AND j.status='A' AND j.j_job_status<>'CANCELLED'
                  AND i.ji_item_type='PRODUCT' AND j.create_date >= :start AND j.create_date < :end
                ORDER BY j.j_job_no ASC, i.id_t_job_item
                """;
            case "TECHNICIAN" -> """
                SELECT u.u_first_name || ' ' || u.u_last_name c1,
                  count(j.id_t_job)::text c2,
                  count(j.id_t_job) FILTER (WHERE j.j_job_status='COMPLETED')::text c3,
                  count(j.id_t_job) FILTER (WHERE j.j_job_status NOT IN ('COMPLETED','CANCELLED'))::text c4,
                  count(j.id_t_job) FILTER (WHERE j.j_job_type='REWORK')::text c5,
                  to_char(coalesce(sum(j.j_grand_total) FILTER (WHERE j.j_job_status<>'CANCELLED'),0),'FM999,999,999,990.00') c6,
                  count(j.id_t_job) FILTER (WHERE j.j_job_status='COMPLETED') n1,
                  count(j.id_t_job) FILTER (WHERE j.j_job_type='REWORK') n2,
                  count(j.id_t_job) flag
                FROM m_user u JOIN m_role role ON role.id_m_role=u.id_m_role
                JOIN t_job j ON j.id_m_user=u.id_m_user
                WHERE role.r_code='TECHNICIAN' AND j.status='A' AND j.create_date >= :start AND j.create_date < :end
                GROUP BY u.id_m_user, u.u_first_name, u.u_last_name
                ORDER BY c1
                """;
            case "CUSTOMER" -> """
                SELECT c.c_customer_code c1, c.c_name c2, count(j.id_t_job)::text c3,
                  to_char(max(j.create_date),'DD/MM/') || (extract(year from max(j.create_date))::int+543) c4,
                  string_agg(DISTINCT s.s_service_name, ', ') c5,
                  to_char(coalesce(sum(j.j_grand_total) FILTER (WHERE j.j_job_status<>'CANCELLED'),0),'FM999,999,999,990.00') c6,
                  count(j.id_t_job) n1,
                  coalesce(sum(j.j_grand_total) FILTER (WHERE j.j_job_status<>'CANCELLED'),0) n2,
                  CASE WHEN count(j.id_t_job)>1 THEN 1 ELSE 0 END flag
                FROM m_customer c JOIN t_job j ON j.id_m_customer=c.id_m_customer
                JOIN m_service s ON s.id_m_service=j.id_m_service
                WHERE j.status='A' AND j.create_date >= :start AND j.create_date < :end
                GROUP BY c.id_m_customer, c.c_customer_code, c.c_name
                ORDER BY c.c_customer_code
                """;
            case "REWORK" -> """
                SELECT j.j_job_no c1, coalesce(o.j_job_no,'—') c2, c.c_name c3,
                  coalesce(nullif(j.j_rework_reason,''),'—') c4,
                  CASE WHEN j.j_under_warranty THEN 'ในประกัน' ELSE 'นอกประกัน' END c5,
                  j.j_job_status c6,
                  CASE WHEN j.j_chargeable THEN 1 ELSE 0 END n1,
                  CASE WHEN j.j_job_status='CANCELLED' THEN 0 ELSE j.j_grand_total END n2,
                  CASE WHEN j.j_under_warranty THEN 1 ELSE 0 END flag
                FROM t_job j JOIN m_customer c ON c.id_m_customer=j.id_m_customer
                LEFT JOIN t_job o ON o.id_t_job=j.id_t_original_job
                WHERE j.status='A' AND j.j_job_type='REWORK' AND j.create_date >= :start AND j.create_date < :end
                ORDER BY j.j_job_no ASC
                """;
            case "STOCK" -> """
                SELECT p.p_product_name c1,
                  CASE st.st_transaction_type WHEN 'IN' THEN 'รับเข้า' WHEN 'OUT' THEN 'เบิกใช้' WHEN 'RETURN' THEN 'คืนสต็อก' ELSE 'ปรับยอด' END c2,
                  to_char(st.st_qty,'FM999,999,990.##') || ' ' || p.p_unit c3,
                  coalesce(j.j_job_no,'—') c4,
                  to_char(st.st_qty_after,'FM999,999,990.##') || ' ' || p.p_unit c5,
                  to_char(st.create_date,'DD/MM/') || (extract(year from st.create_date)::int+543) || to_char(st.create_date,' HH24:MI') c6,
                  CASE WHEN st.st_transaction_type='IN' THEN st.st_qty ELSE 0 END n1,
                  CASE WHEN st.st_transaction_type='OUT' THEN st.st_qty ELSE 0 END n2,
                  CASE WHEN p.p_stock_qty<=p.p_min_stock THEN 1 ELSE 0 END flag
                FROM t_stock_transaction st JOIN m_product p ON p.id_m_product=st.id_m_product
                LEFT JOIN t_job j ON j.id_t_job=st.id_t_job
                WHERE st.status='A' AND st.create_date >= :start AND st.create_date < :end
                ORDER BY st.create_date DESC, st.id_t_stock_transaction DESC
                """;
            default -> throw new IllegalArgumentException("ไม่พบรายงานที่เลือก");
        };
    }
}
