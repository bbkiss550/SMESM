package com.smeservicemanager.report;

import java.util.List;

/** Metadata for the nine report cards in the report hub. */
public final class ReportCatalog {
    private ReportCatalog() {}
    public record Report(String code, int number, String title, String description, String icon) {}
    public record Group(String key, String title, String description, String icon, List<Report> reports) {}
    public static List<Group> groups() {
        return List.of(
            new Group("service", "งานบริการ", "ข้อมูลงานบริการ งานค้าง นัดหมาย และงานแก้", "wrench-adjustable", List.of(
                new Report("SERVICE", 1, "งานบริการ", "ใบงานที่รับแจ้ง ลูกค้า บริการ ช่าง วันที่เปิดและปิดงาน และสถานะงาน", "file-earmark-text-fill"),
                new Report("OVERDUE", 2, "งานค้างและเกินกำหนด", "งานที่ยังไม่ปิด ติดตามงานค้าง และระยะเวลาที่เกินกำหนด", "clock-history"),
                new Report("APPOINTMENT", 3, "นัดหมายและการเลื่อนนัด", "ตารางนัด ประวัติการเลื่อนนัด เหตุผล และการเปลี่ยนหัวหน้าช่าง", "calendar3"),
                new Report("REWORK", 8, "งานแก้ / งานกลับมาซ่อม", "งานที่กลับมาแก้ไข เหตุผล และความเชื่อมโยงกับใบงานเดิม", "arrow-repeat"))),
            new Group("finance", "การเงินและต้นทุน", "ข้อมูลการชำระเงิน ค่าใช้จ่าย และต้นทุนงาน", "cash-coin", List.of(
                new Report("PAYMENT", 4, "รับชำระและคืนเงิน", "ยอดรับชำระ ยอดคืนเงิน วิธีชำระ และยอดรับสุทธิ", "cash-stack"),
                new Report("EXPENSE", 5, "ต้นทุนและค่าใช้จ่าย", "ค่าใช้จ่ายและต้นทุน แยกตามงานและประเภทค่าใช้จ่าย", "calculator-fill"))),
            new Group("personnel", "บุคลากร", "ข้อมูลผลงานของหัวหน้าช่าง", "people-fill", List.of(
                new Report("TECHNICIAN", 6, "ผลงานหัวหน้าช่าง", "งานที่รับผิดชอบ งานเสร็จ งานค้าง และงานแก้ของหัวหน้าช่าง", "person-gear"))),
            new Group("customer", "ลูกค้าและคลัง", "ข้อมูลลูกค้า การใช้บริการ และอะไหล่", "box-seam-fill", List.of(
                new Report("CUSTOMER", 7, "ลูกค้าและการใช้บริการ", "ลูกค้าที่ใช้บริการ ประวัติการใช้บริการ และความถี่ในการกลับมาใช้บริการ", "person-fill"),
                new Report("STOCK", 9, "สต็อกและการใช้อะไหล่", "การรับเข้า เบิกใช้ คงเหลือ และการใช้อะไหล่ในแต่ละงาน", "boxes")))
        );
    }
}
