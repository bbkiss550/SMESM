WITH job_balances AS (
    SELECT job.id_t_job,
           job.j_subtotal,
           SUM(item.ji_total) AS displayed_total
    FROM t_job job
    JOIN t_job_item item ON item.id_t_job = job.id_t_job AND item.status = 'A'
    GROUP BY job.id_t_job, job.j_subtotal
    HAVING SUM(item.ji_total) > job.j_subtotal
), adjustment_targets AS (
    SELECT balance.id_t_job,
           balance.displayed_total - balance.j_subtotal AS excess,
           (
               SELECT item.id_t_job_item
               FROM t_job_item item
               WHERE item.id_t_job = balance.id_t_job
                 AND item.status = 'A'
               ORDER BY item.ji_total DESC, item.id_t_job_item
               LIMIT 1
           ) AS id_t_job_item
    FROM job_balances balance
)
UPDATE t_job_item item
SET ji_total = item.ji_total - target.excess,
    ji_unit_price = ROUND((item.ji_total - target.excess) / item.ji_qty, 2),
    update_by = 'migration-v6',
    update_date = CURRENT_TIMESTAMP,
    version = item.version + 1
FROM adjustment_targets target
WHERE item.id_t_job_item = target.id_t_job_item
  AND item.ji_total >= target.excess;
