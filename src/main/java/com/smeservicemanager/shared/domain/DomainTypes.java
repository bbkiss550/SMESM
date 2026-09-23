package com.smeservicemanager.shared.domain;

public final class DomainTypes {
    private DomainTypes() {}

    public enum CustomerType { INDIVIDUAL, BUSINESS }
    public enum JobType { NORMAL, REWORK }
    public enum JobPriority { NORMAL, URGENT }
    public enum JobStatus { NEW, SCHEDULED, ASSIGNED, IN_PROGRESS, WAITING_PART, COMPLETED, CANCELLED }
    public enum JobItemType { SERVICE, PRODUCT, OTHER }
    public enum AttachmentType { BEFORE, DURING, AFTER, DOCUMENT }
    public enum PaymentStatus { UNPAID, PARTIAL, PAID, REFUNDED, PARTIAL_REFUND }
    public enum PaymentType { DEPOSIT, FULL, BALANCE, OTHER }
    public enum PaymentMethod { CASH, TRANSFER, PROMPTPAY, CREDIT_CARD, OTHER }
    public enum VatMode { NO_VAT, INCLUSIVE }
    public enum StockTransactionType { IN, OUT, ADJUST, RETURN }
}
