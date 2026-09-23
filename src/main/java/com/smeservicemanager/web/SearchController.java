package com.smeservicemanager.web;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class SearchController {
    private final JdbcClient jdbc;
    public SearchController(JdbcClient jdbc){this.jdbc=jdbc;}
    @GetMapping("/search") public List<Map<String,Object>> search(@RequestParam String q){
        if(q==null||q.trim().length()<2)return List.of();
        return jdbc.sql("""
                (select 'ใบงาน' as type, j_job_no as code, j_title as label, '/jobs/'||id_t_job as url
                   from t_job where status='A' and (j_job_no ilike :q or j_title ilike :q or j_contact_phone ilike :q) limit 5)
                union all
                (select 'ลูกค้า', c_customer_code, coalesce(c_company_name,c_name), '/customers/'||id_m_customer
                   from m_customer where status='A' and (c_customer_code ilike :q or c_name ilike :q or coalesce(c_company_name,'') ilike :q) limit 5)
                """).param("q","%"+q.trim()+"%").query().listOfRows();
    }
}
