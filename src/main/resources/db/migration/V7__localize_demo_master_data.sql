UPDATE m_customer
SET c_company_name = 'บริษัท เอบีซี จำกัด',
    c_note = 'ลูกค้ารายสำคัญ ดูแลงานบำรุงรักษารายปี'
WHERE c_customer_code = 'CUST-0001'
  AND c_company_name = 'ABC จำกัด';

UPDATE m_service
SET s_service_name = 'บำรุงรักษาตามรอบ'
WHERE s_service_code = 'SRV-004'
  AND s_service_name = 'Maintenance';

UPDATE m_service
SET s_service_name = 'บริการช่วยเหลือด้านไอที'
WHERE s_service_code = 'SRV-007'
  AND s_service_name = 'IT Support';

UPDATE t_job_item
SET ji_description = 'บำรุงรักษาตามรอบ'
WHERE ji_description = 'Maintenance';

UPDATE t_job_item
SET ji_description = 'บริการช่วยเหลือด้านไอที'
WHERE ji_description = 'IT Support';

UPDATE m_product
SET p_product_name = 'คาปาซิเตอร์ 35 ไมโครฟารัด'
WHERE p_product_code = 'P-001'
  AND p_product_name = 'Capacitor 35uF';

UPDATE m_product
SET p_category = 'อุปกรณ์ไอที'
WHERE p_category = 'อุปกรณ์ IT';

UPDATE t_stock_transaction
SET st_reference = 'ยอดยกมา'
WHERE st_reference = 'OPENING';

UPDATE m_user SET create_by = 'ระบบ', update_by = 'ระบบ' WHERE create_by = 'seed' OR update_by = 'seed';
UPDATE m_customer SET create_by = 'ระบบ', update_by = 'ระบบ' WHERE create_by = 'seed' OR update_by = 'seed';
UPDATE m_customer_phone SET create_by = 'ระบบ', update_by = 'ระบบ' WHERE create_by = 'seed' OR update_by = 'seed';
UPDATE m_service SET create_by = 'ระบบ', update_by = 'ระบบ' WHERE create_by = 'seed' OR update_by = 'seed';
UPDATE m_product SET create_by = 'ระบบ', update_by = 'ระบบ' WHERE create_by = 'seed' OR update_by = 'seed';
UPDATE t_job SET create_by = 'ระบบ', update_by = 'ระบบ' WHERE create_by = 'seed' OR update_by = 'seed';
UPDATE t_job_item SET create_by = 'ระบบ', update_by = 'ระบบ' WHERE create_by IN ('seed', 'migration-fix') OR update_by IN ('seed', 'migration-fix');
UPDATE t_job_activity SET create_by = 'ระบบ', update_by = 'ระบบ' WHERE create_by IN ('seed', 'migration-fix') OR update_by IN ('seed', 'migration-fix');
UPDATE t_payment SET create_by = 'ระบบ', update_by = 'ระบบ' WHERE create_by = 'seed' OR update_by = 'seed';
UPDATE t_stock_transaction SET create_by = 'ระบบ', update_by = 'ระบบ' WHERE create_by = 'seed' OR update_by = 'seed';
UPDATE t_notification SET create_by = 'ระบบ', update_by = 'ระบบ' WHERE create_by = 'seed' OR update_by = 'seed';
UPDATE t_setting SET create_by = 'ระบบ', update_by = 'ระบบ' WHERE create_by = 'seed' OR update_by = 'seed';
