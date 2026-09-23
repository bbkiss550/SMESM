WITH item_totals AS (
    SELECT job.id_t_job,
           job.j_subtotal,
           COALESCE(SUM(item.ji_total) FILTER (WHERE item.status = 'A'), 0) AS displayed_total
    FROM t_job job
    LEFT JOIN t_job_item item ON item.id_t_job = job.id_t_job
    GROUP BY job.id_t_job, job.j_subtotal
), missing_amounts AS (
    SELECT id_t_job, j_subtotal - displayed_total AS amount
    FROM item_totals
    WHERE j_subtotal > displayed_total
)
INSERT INTO t_job_item (
    id_t_job,
    id_m_product,
    ji_item_type,
    ji_description,
    ji_qty,
    ji_unit,
    ji_unit_price,
    ji_total,
    create_by,
    update_by
)
SELECT id_t_job,
       NULL,
       'OTHER',
       'ค่าใช้จ่ายเพิ่มเติม (ข้อมูลตัวอย่าง)',
       1,
       'รายการ',
       amount,
       amount,
       'migration-v5',
       'migration-v5'
FROM missing_amounts;
