package com.smeservicemanager.shared.service;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class DocumentNumberService {
    private final JdbcClient jdbc;

    public DocumentNumberService(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Transactional
    public String next(String type, String prefix) {
        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        jdbc.sql("""
                insert into t_document_sequence
                    (ds_document_type, ds_period, ds_last_value, status, create_by, create_date, update_by, update_date, version)
                values (:type, :period,
                        case when :type = 'JOB' then coalesce((
                            select max(substring(j_job_no from '[0-9]+$')::bigint)
                              from t_job
                             where j_job_no like :jobPattern
                        ), 0) else 0 end,
                        'A', 'system', current_timestamp, 'system', current_timestamp, 0)
                on conflict (ds_document_type, ds_period) do update
                    set ds_last_value = greatest(t_document_sequence.ds_last_value, excluded.ds_last_value)
                """).param("type", type).param("period", period)
                .param("jobPattern", prefix + "-" + period + "-%").update();

        long value = jdbc.sql("""
                update t_document_sequence
                   set ds_last_value = ds_last_value + 1,
                       update_date = current_timestamp,
                       version = version + 1
                 where ds_document_type = :type and ds_period = :period
                returning ds_last_value
                """).param("type", type).param("period", period).query(Long.class).single();
        return "%s-%s-%04d".formatted(prefix, period, value);
    }

    @Transactional
    public String nextCustomerCode() {
        jdbc.sql("""
                insert into t_document_sequence
                    (ds_document_type, ds_period, ds_last_value, status, create_by, create_date, update_by, update_date, version)
                values ('CUSTOMER', 'ALL', 30, 'A', 'system', current_timestamp, 'system', current_timestamp, 0)
                on conflict (ds_document_type, ds_period) do nothing
                """).update();
        long value = jdbc.sql("""
                update t_document_sequence
                   set ds_last_value = ds_last_value + 1, update_date = current_timestamp, version = version + 1
                 where ds_document_type = 'CUSTOMER' and ds_period = 'ALL'
                returning ds_last_value
                """).query(Long.class).single();
        return "CUST-%04d".formatted(value);
    }
}
