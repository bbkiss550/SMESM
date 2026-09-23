package com.smeservicemanager.web;

import com.smeservicemanager.notification.NotificationRepository;
import com.smeservicemanager.security.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

@ControllerAdvice
public class GlobalModelAdvice {
    private final UserRepository users;
    private final NotificationRepository notifications;

    public GlobalModelAdvice(UserRepository users, NotificationRepository notifications) {
        this.users = users; this.notifications = notifications;
    }

    @ModelAttribute("currentUser")
    public Object currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) return null;
        return users.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
    }

    @ModelAttribute("navNotifications")
    public Object notifications(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) return Map.of("items", java.util.List.of(), "unread", 0L);
        return Map.of("items", notifications.findTop8ByUserUsernameOrUserIsNullOrderByCreateDateDesc(authentication.getName()),
                "unread", notifications.countByUserUsernameAndReadFalseAndStatus(authentication.getName(), "A"));
    }

    @ModelAttribute("statusLabels")
    public Map<String, String> statusLabels() {
        return Map.ofEntries(
                Map.entry("NEW", "ใหม่"), Map.entry("SCHEDULED", "นัดหมายแล้ว"), Map.entry("ASSIGNED", "มอบหมายแล้ว"),
                Map.entry("IN_PROGRESS", "กำลังดำเนินการ"), Map.entry("WAITING_PART", "รออะไหล่"),
                Map.entry("COMPLETED", "เสร็จสิ้น"), Map.entry("CANCELLED", "ยกเลิก"),
                Map.entry("UNPAID", "ยังไม่ได้ชำระ"), Map.entry("PARTIAL", "ชำระบางส่วน"),
                Map.entry("PAID", "ชำระครบแล้ว"), Map.entry("REFUNDED", "คืนเงินแล้ว"),
                Map.entry("PARTIAL_REFUND", "คืนเงินบางส่วน"), Map.entry("A", "เปิดใช้งาน"), Map.entry("D", "ปิดใช้งาน")
        );
    }

    @ModelAttribute("enumLabels")
    public Map<String, String> enumLabels() {
        return Map.ofEntries(
                Map.entry("ADMIN", "ผู้ดูแลระบบ"), Map.entry("STAFF", "เจ้าหน้าที่"),
                Map.entry("TECHNICIAN", "หัวหน้าช่าง"),
                Map.entry("IN", "รับเข้า"), Map.entry("OUT", "เบิกออก"),
                Map.entry("ADJUST", "ปรับยอดคงเหลือ"), Map.entry("RETURN", "รับคืน"),
                Map.entry("SERVICE", "ค่าบริการ"), Map.entry("PRODUCT", "อะไหล่/สินค้า"),
                Map.entry("OTHER", "อื่น ๆ"),
                Map.entry("BEFORE", "ก่อนดำเนินการ"), Map.entry("DURING", "ระหว่างดำเนินการ"),
                Map.entry("AFTER", "หลังดำเนินการ"), Map.entry("DOCUMENT", "เอกสาร"),
                Map.entry("DEPOSIT", "มัดจำ"), Map.entry("BALANCE", "ชำระยอดคงเหลือ"),
                Map.entry("FULL", "ชำระเต็มจำนวน"),
                Map.entry("CASH", "เงินสด"), Map.entry("TRANSFER", "โอนเงิน"),
                Map.entry("PROMPTPAY", "พร้อมเพย์"), Map.entry("CREDIT_CARD", "บัตรเครดิต")
        );
    }
}
