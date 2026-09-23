CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE m_role (
    id_m_role BIGSERIAL PRIMARY KEY,
    r_code VARCHAR(30) NOT NULL UNIQUE,
    r_name VARCHAR(100) NOT NULL,
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'D')),
    create_by VARCHAR(100) NOT NULL DEFAULT 'system',
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NOT NULL DEFAULT 'system',
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE m_user (
    id_m_user BIGSERIAL PRIMARY KEY,
    id_m_role BIGINT NOT NULL REFERENCES m_role(id_m_role),
    u_username VARCHAR(80) NOT NULL UNIQUE,
    u_password VARCHAR(100) NOT NULL,
    u_first_name VARCHAR(100) NOT NULL,
    u_last_name VARCHAR(100) NOT NULL,
    u_phone VARCHAR(30),
    u_email VARCHAR(180),
    u_profile_image VARCHAR(500),
    u_force_password_change BOOLEAN NOT NULL DEFAULT FALSE,
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'D')),
    create_by VARCHAR(100) NOT NULL DEFAULT 'system',
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NOT NULL DEFAULT 'system',
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE m_customer (
    id_m_customer BIGSERIAL PRIMARY KEY,
    c_customer_code VARCHAR(30) NOT NULL UNIQUE,
    c_customer_type VARCHAR(20) NOT NULL CHECK (c_customer_type IN ('INDIVIDUAL', 'BUSINESS')),
    c_name VARCHAR(200) NOT NULL,
    c_company_name VARCHAR(200),
    c_tax_id VARCHAR(30),
    c_email VARCHAR(180),
    c_line_id VARCHAR(100),
    c_address TEXT,
    c_note TEXT,
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'D')),
    create_by VARCHAR(100) NOT NULL DEFAULT 'system',
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NOT NULL DEFAULT 'system',
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE m_customer_phone (
    id_m_customer_phone BIGSERIAL PRIMARY KEY,
    id_m_customer BIGINT NOT NULL REFERENCES m_customer(id_m_customer),
    cp_phone VARCHAR(30) NOT NULL,
    cp_label VARCHAR(50) NOT NULL DEFAULT 'มือถือ',
    cp_is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'D')),
    create_by VARCHAR(100) NOT NULL DEFAULT 'system',
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NOT NULL DEFAULT 'system',
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_customer_primary_phone
    ON m_customer_phone(id_m_customer) WHERE cp_is_primary AND status = 'A';

CREATE TABLE m_service (
    id_m_service BIGSERIAL PRIMARY KEY,
    s_service_code VARCHAR(30) NOT NULL UNIQUE,
    s_service_name VARCHAR(200) NOT NULL,
    s_description TEXT,
    s_default_price NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (s_default_price >= 0),
    s_default_duration_minutes INTEGER NOT NULL DEFAULT 120 CHECK (s_default_duration_minutes > 0),
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'D')),
    create_by VARCHAR(100) NOT NULL DEFAULT 'system',
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NOT NULL DEFAULT 'system',
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE m_product (
    id_m_product BIGSERIAL PRIMARY KEY,
    p_product_code VARCHAR(30) NOT NULL UNIQUE,
    p_product_name VARCHAR(200) NOT NULL,
    p_category VARCHAR(100),
    p_unit VARCHAR(50) NOT NULL,
    p_cost_price NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (p_cost_price >= 0),
    p_sale_price NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (p_sale_price >= 0),
    p_stock_qty NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (p_stock_qty >= 0),
    p_min_stock NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (p_min_stock >= 0),
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'D')),
    create_by VARCHAR(100) NOT NULL DEFAULT 'system',
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NOT NULL DEFAULT 'system',
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE t_job (
    id_t_job BIGSERIAL PRIMARY KEY,
    id_m_customer BIGINT NOT NULL REFERENCES m_customer(id_m_customer),
    id_m_service BIGINT NOT NULL REFERENCES m_service(id_m_service),
    id_m_user BIGINT REFERENCES m_user(id_m_user),
    id_t_original_job BIGINT REFERENCES t_job(id_t_job),
    j_job_no VARCHAR(40) NOT NULL UNIQUE,
    j_job_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL' CHECK (j_job_type IN ('NORMAL', 'REWORK')),
    j_title VARCHAR(250) NOT NULL,
    j_description TEXT,
    j_priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL' CHECK (j_priority IN ('NORMAL', 'URGENT')),
    j_job_status VARCHAR(30) NOT NULL DEFAULT 'NEW' CHECK (j_job_status IN ('NEW','SCHEDULED','ASSIGNED','IN_PROGRESS','WAITING_PART','COMPLETED','CANCELLED')),
    j_appointment_start TIMESTAMP NOT NULL,
    j_appointment_end TIMESTAMP NOT NULL,
    j_contact_phone VARCHAR(30),
    j_address TEXT NOT NULL,
    j_internal_note TEXT,
    j_rework_reason TEXT,
    j_under_warranty BOOLEAN NOT NULL DEFAULT FALSE,
    j_chargeable BOOLEAN NOT NULL DEFAULT TRUE,
    j_subtotal NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (j_subtotal >= 0),
    j_discount NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (j_discount >= 0),
    j_vat_mode VARCHAR(20) NOT NULL DEFAULT 'INCLUSIVE' CHECK (j_vat_mode IN ('NO_VAT','INCLUSIVE')),
    j_vat_rate NUMERIC(5,2) NOT NULL DEFAULT 7.00 CHECK (j_vat_rate >= 0),
    j_tax_base NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (j_tax_base >= 0),
    j_vat_amount NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (j_vat_amount >= 0),
    j_grand_total NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (j_grand_total >= 0),
    j_payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' CHECK (j_payment_status IN ('UNPAID','PARTIAL','PAID','REFUNDED','PARTIAL_REFUND')),
    j_public_token UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    j_completed_date TIMESTAMP,
    j_cancel_reason TEXT,
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'D')),
    create_by VARCHAR(100) NOT NULL DEFAULT 'system',
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NOT NULL DEFAULT 'system',
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (j_appointment_end > j_appointment_start),
    CHECK (j_discount <= j_subtotal)
);

CREATE TABLE t_job_item (
    id_t_job_item BIGSERIAL PRIMARY KEY,
    id_t_job BIGINT NOT NULL REFERENCES t_job(id_t_job),
    id_m_product BIGINT REFERENCES m_product(id_m_product),
    ji_item_type VARCHAR(20) NOT NULL CHECK (ji_item_type IN ('SERVICE','PRODUCT','OTHER')),
    ji_description VARCHAR(300) NOT NULL,
    ji_qty NUMERIC(14,2) NOT NULL CHECK (ji_qty > 0),
    ji_unit VARCHAR(50) NOT NULL,
    ji_unit_price NUMERIC(14,2) NOT NULL CHECK (ji_unit_price >= 0),
    ji_total NUMERIC(14,2) NOT NULL CHECK (ji_total >= 0),
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'D')),
    create_by VARCHAR(100) NOT NULL DEFAULT 'system',
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NOT NULL DEFAULT 'system',
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK ((ji_item_type = 'PRODUCT' AND id_m_product IS NOT NULL) OR ji_item_type <> 'PRODUCT')
);

CREATE TABLE t_job_activity (
    id_t_job_activity BIGSERIAL PRIMARY KEY,
    id_t_job BIGINT NOT NULL REFERENCES t_job(id_t_job),
    id_m_user BIGINT REFERENCES m_user(id_m_user),
    ja_activity_type VARCHAR(50) NOT NULL,
    ja_description TEXT NOT NULL,
    ja_old_value TEXT,
    ja_new_value TEXT,
    ja_activity_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'D')),
    create_by VARCHAR(100) NOT NULL DEFAULT 'system',
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NOT NULL DEFAULT 'system',
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE t_job_attachment (
    id_t_job_attachment BIGSERIAL PRIMARY KEY,
    id_t_job BIGINT NOT NULL REFERENCES t_job(id_t_job),
    jat_attachment_type VARCHAR(20) NOT NULL CHECK (jat_attachment_type IN ('BEFORE','DURING','AFTER','DOCUMENT')),
    jat_original_file_name VARCHAR(500) NOT NULL,
    jat_stored_file_name VARCHAR(100) NOT NULL UNIQUE,
    jat_file_path VARCHAR(1000) NOT NULL,
    jat_content_type VARCHAR(100) NOT NULL,
    jat_file_size BIGINT NOT NULL CHECK (jat_file_size > 0),
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'D')),
    create_by VARCHAR(100) NOT NULL DEFAULT 'system',
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NOT NULL DEFAULT 'system',
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE t_payment (
    id_t_payment BIGSERIAL PRIMARY KEY,
    id_t_job BIGINT NOT NULL REFERENCES t_job(id_t_job),
    p_payment_no VARCHAR(40) NOT NULL UNIQUE,
    p_receipt_no VARCHAR(40) NOT NULL UNIQUE,
    p_payment_date TIMESTAMP NOT NULL,
    p_amount NUMERIC(14,2) NOT NULL CHECK (p_amount > 0),
    p_payment_type VARCHAR(20) NOT NULL CHECK (p_payment_type IN ('DEPOSIT','FULL','BALANCE','OTHER')),
    p_payment_method VARCHAR(30) NOT NULL CHECK (p_payment_method IN ('CASH','TRANSFER','PROMPTPAY','CREDIT_CARD','OTHER')),
    p_reference_no VARCHAR(100),
    p_note TEXT,
    p_record_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (p_record_status IN ('ACTIVE','VOID')),
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'D')),
    create_by VARCHAR(100) NOT NULL DEFAULT 'system',
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NOT NULL DEFAULT 'system',
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE t_refund (
    id_t_refund BIGSERIAL PRIMARY KEY,
    id_t_job BIGINT NOT NULL REFERENCES t_job(id_t_job),
    rf_refund_no VARCHAR(40) NOT NULL UNIQUE,
    rf_receipt_no VARCHAR(40) NOT NULL UNIQUE,
    rf_refund_date TIMESTAMP NOT NULL,
    rf_amount NUMERIC(14,2) NOT NULL CHECK (rf_amount > 0),
    rf_refund_method VARCHAR(30) NOT NULL CHECK (rf_refund_method IN ('CASH','TRANSFER','PROMPTPAY','CREDIT_CARD','OTHER')),
    rf_reference_no VARCHAR(100),
    rf_reason TEXT NOT NULL,
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'D')),
    create_by VARCHAR(100) NOT NULL DEFAULT 'system',
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NOT NULL DEFAULT 'system',
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE t_stock_transaction (
    id_t_stock_transaction BIGSERIAL PRIMARY KEY,
    id_m_product BIGINT NOT NULL REFERENCES m_product(id_m_product),
    id_t_job BIGINT REFERENCES t_job(id_t_job),
    st_transaction_type VARCHAR(20) NOT NULL CHECK (st_transaction_type IN ('IN','OUT','ADJUST','RETURN')),
    st_qty NUMERIC(14,2) NOT NULL CHECK (st_qty > 0),
    st_qty_before NUMERIC(14,2) NOT NULL CHECK (st_qty_before >= 0),
    st_qty_after NUMERIC(14,2) NOT NULL CHECK (st_qty_after >= 0),
    st_reference VARCHAR(100),
    st_note TEXT,
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'D')),
    create_by VARCHAR(100) NOT NULL DEFAULT 'system',
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NOT NULL DEFAULT 'system',
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE t_notification (
    id_t_notification BIGSERIAL PRIMARY KEY,
    id_m_user BIGINT REFERENCES m_user(id_m_user),
    n_type VARCHAR(50) NOT NULL,
    n_title VARCHAR(200) NOT NULL,
    n_message TEXT NOT NULL,
    n_target_url VARCHAR(500),
    n_is_read BOOLEAN NOT NULL DEFAULT FALSE,
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'D')),
    create_by VARCHAR(100) NOT NULL DEFAULT 'system',
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NOT NULL DEFAULT 'system',
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE t_setting (
    id_t_setting BIGSERIAL PRIMARY KEY,
    s_key VARCHAR(120) NOT NULL UNIQUE,
    s_value TEXT,
    s_group VARCHAR(50) NOT NULL,
    s_description VARCHAR(300),
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'D')),
    create_by VARCHAR(100) NOT NULL DEFAULT 'system',
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NOT NULL DEFAULT 'system',
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE t_document_sequence (
    id_t_document_sequence BIGSERIAL PRIMARY KEY,
    ds_document_type VARCHAR(30) NOT NULL,
    ds_period VARCHAR(10) NOT NULL,
    ds_last_value BIGINT NOT NULL DEFAULT 0,
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'D')),
    create_by VARCHAR(100) NOT NULL DEFAULT 'system',
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NOT NULL DEFAULT 'system',
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(ds_document_type, ds_period)
);
