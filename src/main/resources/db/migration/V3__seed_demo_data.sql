INSERT INTO m_role (r_code, r_name) VALUES
('ADMIN', 'ผู้ดูแลระบบ'),
('STAFF', 'เจ้าหน้าที่'),
('TECHNICIAN', 'หัวหน้าช่าง');

INSERT INTO m_user (id_m_role, u_username, u_password, u_first_name, u_last_name, u_phone, u_email, create_by, update_by)
SELECT r.id_m_role, v.username, crypt('demo1234', gen_salt('bf', 10)), v.first_name, v.last_name, v.phone, v.email, 'seed', 'seed'
FROM (VALUES
    ('ADMIN','admin','สมชาย','ใจดี','081-234-5678','admin@smeservice.local'),
    ('STAFF','staff','สุดา','วงศ์สกุล','089-456-7890','staff@smeservice.local'),
    ('TECHNICIAN','technician','อนาวิน','ศรีทอง','082-345-6789','technician@smeservice.local'),
    ('TECHNICIAN','wittaya','วิทยา','ใจดี','086-789-0123','wittaya@smeservice.local'),
    ('TECHNICIAN','ekachai','เอกชัย','พร้อมงาน','095-567-8901','ekachai@smeservice.local'),
    ('TECHNICIAN','somnuk','สมนึก','ช่างดี','080-111-2222','somnuk@smeservice.local'),
    ('TECHNICIAN','kitti','กิตติ','มั่นคง','083-333-4444','kitti@smeservice.local'),
    ('STAFF','orawan','อรวรรณ','ศักดิ์ดี','064-222-3333','orawan@smeservice.local')
) AS v(role_code, username, first_name, last_name, phone, email)
JOIN m_role r ON r.r_code = v.role_code;

INSERT INTO m_customer (c_customer_code, c_customer_type, c_name, c_company_name, c_tax_id, c_email, c_line_id, c_address, c_note, create_by, update_by)
SELECT 'CUST-' || LPAD(g::text, 4, '0'),
       CASE WHEN g % 3 = 1 THEN 'BUSINESS' ELSE 'INDIVIDUAL' END,
       CASE WHEN g = 1 THEN 'คุณสมชาย ใจดี' ELSE 'ลูกค้าตัวอย่าง ' || g END,
       CASE WHEN g = 1 THEN 'ABC จำกัด' WHEN g % 3 = 1 THEN 'บริษัท ตัวอย่าง ' || g || ' จำกัด' ELSE NULL END,
       CASE WHEN g % 3 = 1 THEN LPAD((1055667700000 + g)::text, 13, '0') ELSE NULL END,
       'customer' || g || '@example.com',
       'customer_' || g,
       (10 + g) || '/1 ถนนสุขุมวิท แขวงบางนา เขตบางนา กรุงเทพมหานคร 10260',
       CASE WHEN g = 1 THEN 'ลูกค้ารายสำคัญ ดูแลงาน MA รายปี' ELSE NULL END,
       'seed', 'seed'
FROM generate_series(1, 30) g;

INSERT INTO m_customer_phone (id_m_customer, cp_phone, cp_label, cp_is_primary, create_by, update_by)
SELECT id_m_customer, '08' || LPAD((10000000 + id_m_customer)::text, 8, '0'), 'มือถือ', TRUE, 'seed', 'seed'
FROM m_customer;

INSERT INTO m_customer_phone (id_m_customer, cp_phone, cp_label, cp_is_primary, create_by, update_by)
SELECT id_m_customer, '02-' || LPAD((1000000 + id_m_customer)::text, 7, '0'), 'ที่ทำงาน', FALSE, 'seed', 'seed'
FROM m_customer WHERE c_customer_type = 'BUSINESS';

INSERT INTO m_service (s_service_code, s_service_name, s_description, s_default_price, s_default_duration_minutes, create_by, update_by) VALUES
('SRV-001','ล้างแอร์','ล้างทำความสะอาดเครื่องปรับอากาศและตรวจเช็กระบบ',800,90,'seed','seed'),
('SRV-002','ซ่อมแอร์','ตรวจวิเคราะห์และซ่อมระบบเครื่องปรับอากาศ',1200,120,'seed','seed'),
('SRV-003','ติดตั้งแอร์','ติดตั้งเครื่องปรับอากาศพร้อมทดสอบระบบ',2500,240,'seed','seed'),
('SRV-004','Maintenance','บำรุงรักษาระบบตามรอบ',1000,180,'seed','seed'),
('SRV-005','ตรวจเช็ก','ตรวจเช็กอาการและประเมินงาน',500,60,'seed','seed'),
('SRV-006','ซ่อมระบบไฟฟ้า','ตรวจและซ่อมระบบไฟฟ้าภายใน',1500,180,'seed','seed'),
('SRV-007','IT Support','แก้ไขปัญหาคอมพิวเตอร์และเครือข่าย',900,120,'seed','seed'),
('SRV-008','ติดตั้งอุปกรณ์','ติดตั้งและตั้งค่าอุปกรณ์หน้างาน',1800,180,'seed','seed');

INSERT INTO m_product (p_product_code, p_product_name, p_category, p_unit, p_cost_price, p_sale_price, p_stock_qty, p_min_stock, create_by, update_by)
SELECT 'P-' || LPAD(g::text, 3, '0'),
       CASE WHEN g = 1 THEN 'Capacitor 35uF' WHEN g = 2 THEN 'น้ำยาแอร์ R32' ELSE 'อะไหล่ตัวอย่าง ' || g END,
       CASE WHEN g <= 10 THEN 'อะไหล่แอร์' WHEN g <= 20 THEN 'อุปกรณ์ไฟฟ้า' ELSE 'อุปกรณ์ IT' END,
       CASE WHEN g = 2 THEN 'กิโลกรัม' ELSE 'ชิ้น' END,
       80 + (g * 20), 150 + (g * 35),
       CASE WHEN g % 11 = 0 THEN 0 WHEN g % 7 = 0 THEN 3 ELSE 15 + g END,
       5, 'seed', 'seed'
FROM generate_series(1, 30) g;

INSERT INTO t_job (
    id_m_customer, id_m_service, id_m_user, j_job_no, j_title, j_description, j_priority,
    j_job_status, j_appointment_start, j_appointment_end, j_contact_phone, j_address,
    j_subtotal, j_discount, j_tax_base, j_vat_amount, j_grand_total, j_payment_status,
    j_completed_date, create_by, update_by, create_date, update_date)
SELECT ((g - 1) % 30) + 1,
       ((g - 1) % 8) + 1,
       3 + ((g - 1) % 5),
       'JOB-' || TO_CHAR(CURRENT_DATE, 'YYYYMM') || '-' || LPAD(g::text, 4, '0'),
       CASE (g % 6)
         WHEN 0 THEN 'แอร์สำนักงานไม่เย็น'
         WHEN 1 THEN 'ล้างแอร์ประจำปี'
         WHEN 2 THEN 'ติดตั้งเครื่องปรับอากาศ'
         WHEN 3 THEN 'ตรวจเช็กระบบไฟฟ้า'
         WHEN 4 THEN 'ซ่อมคอมพิวเตอร์สำนักงาน'
         ELSE 'บำรุงรักษาอุปกรณ์'
       END,
       'รายละเอียดงานตัวอย่างสำหรับสาธิตระบบ SME Service Manager',
       CASE WHEN g % 13 = 0 THEN 'URGENT' ELSE 'NORMAL' END,
       CASE
         WHEN g <= 47 THEN 'COMPLETED'
         WHEN g <= 59 THEN 'IN_PROGRESS'
         WHEN g <= 67 THEN 'WAITING_PART'
         WHEN g <= 85 THEN 'ASSIGNED'
         WHEN g <= 108 THEN 'SCHEDULED'
         WHEN g <= 123 THEN 'NEW'
         ELSE 'CANCELLED'
       END,
       CURRENT_DATE - INTERVAL '180 days' + (g * INTERVAL '34 hours'),
       CURRENT_DATE - INTERVAL '180 days' + (g * INTERVAL '34 hours') + INTERVAL '2 hours',
       '08' || LPAD((10000000 + (((g - 1) % 30) + 1))::text, 8, '0'),
       (10 + (((g - 1) % 30) + 1)) || '/1 ถนนสุขุมวิท แขวงบางนา เขตบางนา กรุงเทพมหานคร 10260',
       1000 + (g * 125),
       CASE WHEN g % 10 = 0 THEN 200 ELSE 0 END,
       ROUND(((1000 + (g * 125) - CASE WHEN g % 10 = 0 THEN 200 ELSE 0 END) * 100 / 107)::numeric, 2),
       ROUND(((1000 + (g * 125) - CASE WHEN g % 10 = 0 THEN 200 ELSE 0 END) * 7 / 107)::numeric, 2),
       1000 + (g * 125) - CASE WHEN g % 10 = 0 THEN 200 ELSE 0 END,
       CASE WHEN g <= 35 THEN 'PAID' WHEN g <= 47 THEN 'PARTIAL' ELSE 'UNPAID' END,
       CASE WHEN g <= 47 THEN CURRENT_DATE - INTERVAL '170 days' + (g * INTERVAL '34 hours') ELSE NULL END,
       'seed', 'seed',
       CURRENT_DATE - INTERVAL '185 days' + (g * INTERVAL '34 hours'),
       CURRENT_DATE - INTERVAL '180 days' + (g * INTERVAL '34 hours')
FROM generate_series(1, 128) g;

INSERT INTO t_job_item (id_t_job, id_m_product, ji_item_type, ji_description, ji_qty, ji_unit, ji_unit_price, ji_total, create_by, update_by)
SELECT j.id_t_job, NULL, 'SERVICE', s.s_service_name, 1, 'งาน', s.s_default_price, s.s_default_price, 'seed', 'seed'
FROM t_job j JOIN m_service s ON s.id_m_service = j.id_m_service;

INSERT INTO t_job_activity (id_t_job, id_m_user, ja_activity_type, ja_description, ja_activity_date, create_by, update_by)
SELECT id_t_job, NULL, 'JOB_CREATED', 'สร้างใบงานในระบบ', create_date, 'seed', 'seed' FROM t_job;

INSERT INTO t_job_activity (id_t_job, id_m_user, ja_activity_type, ja_description, ja_activity_date, create_by, update_by)
SELECT id_t_job, id_m_user, 'TECHNICIAN_ASSIGNED', 'มอบหมายหัวหน้าช่าง', create_date + INTERVAL '15 minutes', 'seed', 'seed'
FROM t_job WHERE id_m_user IS NOT NULL;

INSERT INTO t_job_activity (id_t_job, id_m_user, ja_activity_type, ja_description, ja_activity_date, create_by, update_by)
SELECT id_t_job, id_m_user, 'JOB_COMPLETED', 'งานบริการเสร็จสิ้น', j_completed_date, 'seed', 'seed'
FROM t_job WHERE j_job_status = 'COMPLETED';

INSERT INTO t_payment (id_t_job, p_payment_no, p_receipt_no, p_payment_date, p_amount, p_payment_type, p_payment_method, p_reference_no, create_by, update_by)
SELECT id_t_job,
       'PAY-' || TO_CHAR(CURRENT_DATE, 'YYYYMM') || '-' || LPAD(id_t_job::text, 4, '0'),
       'RCPT-' || TO_CHAR(CURRENT_DATE, 'YYYYMM') || '-' || LPAD(id_t_job::text, 4, '0'),
       COALESCE(j_completed_date, update_date),
       CASE WHEN j_payment_status = 'PAID' THEN j_grand_total ELSE ROUND(j_grand_total * 0.40, 2) END,
       CASE WHEN j_payment_status = 'PAID' THEN 'FULL' ELSE 'DEPOSIT' END,
       CASE (id_t_job % 4) WHEN 0 THEN 'CASH' WHEN 1 THEN 'TRANSFER' WHEN 2 THEN 'PROMPTPAY' ELSE 'CREDIT_CARD' END,
       'DEMO-' || LPAD(id_t_job::text, 6, '0'), 'seed', 'seed'
FROM t_job WHERE j_payment_status IN ('PAID','PARTIAL');

INSERT INTO t_stock_transaction (id_m_product, st_transaction_type, st_qty, st_qty_before, st_qty_after, st_reference, st_note, create_by, update_by)
SELECT id_m_product, 'IN', p_stock_qty, 0, p_stock_qty, 'OPENING', 'ยอดยกมาสำหรับข้อมูลสาธิต', 'seed', 'seed'
FROM m_product WHERE p_stock_qty > 0;

INSERT INTO t_notification (id_m_user, n_type, n_title, n_message, n_target_url, n_is_read, create_by, update_by) VALUES
((SELECT id_m_user FROM m_user WHERE u_username='admin'),'NEW_JOB','มีใบงานใหม่','มีใบงานใหม่รอการตรวจสอบ','/jobs',FALSE,'seed','seed'),
((SELECT id_m_user FROM m_user WHERE u_username='admin'),'LOW_STOCK','สินค้าใกล้หมด','มีสินค้าในสต๊อกต่ำกว่าจุดสั่งซื้อ','/stock',FALSE,'seed','seed'),
((SELECT id_m_user FROM m_user WHERE u_username='technician'),'JOB_ASSIGNED','ได้รับมอบหมายงาน','คุณได้รับมอบหมายใบงานใหม่','/my-jobs',FALSE,'seed','seed');

INSERT INTO t_setting (s_key, s_value, s_group, s_description, create_by, update_by) VALUES
('company.name','บริษัท เอสเอ็มอี เซอร์วิส จำกัด','COMPANY','ชื่อบริษัท','seed','seed'),
('company.taxId','0105566012345','COMPANY','เลขประจำตัวผู้เสียภาษี','seed','seed'),
('company.address','99/9 ถนนสุขุมวิท แขวงจตุจักร เขตจตุจักร กรุงเทพมหานคร 10900','COMPANY','ที่อยู่บริษัท','seed','seed'),
('company.phone','02-123-4567','COMPANY','โทรศัพท์','seed','seed'),
('company.email','contact@example.com','COMPANY','อีเมล','seed','seed'),
('company.website','https://example.com','COMPANY','เว็บไซต์','seed','seed'),
('tax.enabled','true','TAX','เปิดใช้งาน VAT','seed','seed'),
('tax.rate','7.00','TAX','อัตรา VAT แบบรวมในราคา','seed','seed'),
('notification.newJob','true','NOTIFICATION','แจ้งเตือนงานใหม่','seed','seed'),
('notification.appointment','true','NOTIFICATION','แจ้งเตือนใกล้นัดหมาย','seed','seed'),
('notification.payment','true','NOTIFICATION','แจ้งเตือนการชำระเงิน','seed','seed'),
('notification.dailySummary','true','NOTIFICATION','สรุปรายงานประจำวัน','seed','seed'),
('line.status','NOT_CONNECTED','INTEGRATION','สถานะ LINE OA','seed','seed');
