WITH technician_pool AS (
    SELECT u.id_m_user,
           ROW_NUMBER() OVER (ORDER BY u.id_m_user) AS position,
           COUNT(*) OVER () AS pool_size
    FROM m_user u
    JOIN m_role r ON r.id_m_role = u.id_m_role
    WHERE r.r_code = 'TECHNICIAN'
      AND u.status = 'A'
), invalid_jobs AS (
    SELECT j.id_t_job,
           ((ROW_NUMBER() OVER (ORDER BY j.id_t_job) - 1) % (SELECT MAX(pool_size) FROM technician_pool)) + 1 AS technician_position
    FROM t_job j
    LEFT JOIN m_user u ON u.id_m_user = j.id_m_user
    LEFT JOIN m_role r ON r.id_m_role = u.id_m_role
    WHERE j.id_m_user IS NOT NULL
      AND COALESCE(r.r_code, '') <> 'TECHNICIAN'
), reassignment AS (
    SELECT invalid_jobs.id_t_job, technician_pool.id_m_user
    FROM invalid_jobs
    JOIN technician_pool ON technician_pool.position = invalid_jobs.technician_position
)
UPDATE t_job j
SET id_m_user = reassignment.id_m_user,
    update_by = 'migration-v4',
    update_date = CURRENT_TIMESTAMP,
    version = j.version + 1
FROM reassignment
WHERE j.id_t_job = reassignment.id_t_job;

UPDATE t_job_activity activity
SET id_m_user = job.id_m_user,
    update_by = 'migration-v4',
    update_date = CURRENT_TIMESTAMP,
    version = activity.version + 1
FROM t_job job
WHERE activity.id_t_job = job.id_t_job
  AND activity.ja_activity_type = 'TECHNICIAN_ASSIGNED'
  AND activity.id_m_user IS DISTINCT FROM job.id_m_user;
