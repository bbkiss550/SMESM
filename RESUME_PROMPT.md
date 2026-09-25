# Resume Prompt

คุณกำลังทำงานต่อบนโปรเจกต์ SME Service Manager ซึ่งอยู่ใน repository นี้

ก่อนทำสิ่งใด ให้ตรวจ `AGENTS.md`, `HANDOFF.md`, branch/commit/status ปัจจุบัน และอ่าน source code ของส่วนที่จะทำงานก่อน เส้นทาง handoff สร้างจาก commit `20957af3da5760364ee474784a4cf4caa36735b7`; อาจมี commit เอกสาร handoff ต่อท้ายแล้ว ให้ยึด `git log -1` และ source ปัจจุบันเป็นความจริงล่าสุด

สถานะงาน: ระบบหลักพัฒนาแล้วและไม่มี feature ค้างใน working tree ตอนเริ่ม handoff; งานล่าสุดคือจัดทำเอกสารส่งต่องาน เพิ่มกฎป้องกันการ commit secrets/dump ใหม่ และบันทึกแนวทางพัฒนาสำหรับเครื่องถัดไป ไม่ได้เริ่มออกแบบระบบใหม่

สิ่งที่ทำเสร็จแล้ว ได้แก่ UX ภาษาไทย, AJAX table search/pagination/sort, date/time picker, modal ใบงาน/ปฏิทิน, การจัดการข้อมูลหลักด้วย soft delete, ปุ่มและ modal รายละเอียดลูกค้าพร้อมประวัติบริการแบบแบ่งหน้า, modal รายละเอียดธุรกรรมแบบ read-only, การล้างฟอร์ม modal เพิ่มรายการ และตัวนับเบอร์โทร 10 ตัว ดูรายละเอียด/ข้อจำกัดใน `HANDOFF.md`

ทำต่อดังนี้:

1. ยืนยันว่า branch `main` sync กับ `origin/main` และไม่มีงานของผู้ใช้ที่ยังไม่ commit
2. ตรวจ `.gitignore` และไฟล์ handoff; มี custom-format PostgreSQL archive ชื่อ `db/db_service_demo-db_service_demo-202609231600.sql` track มาตั้งแต่ initial commit แม้ชื่อเป็น `.sql`; ผู้ใช้เคยบอกว่าข้อมูลเป็น demo แต่ยังไม่ได้ตรวจ payload ห้ามแทนที่/เผยแพร่ซ้ำโดยไม่ยืนยันข้อมูล
3. บนเครื่องใหม่ตั้ง Java 21, PostgreSQL 17 และ `.env` แบบ local; ห้ามคัดลอก credentials ลง Git หรือเอกสาร
4. รัน `.\mvnw.cmd test` บน Windows (หรือ `./mvnw test` บน Unix) โดยกำหนด DB connection variables ก่อน แล้ว smoke test หน้าหลัก
5. รอคำขอ feature ถัดไปจากผู้ใช้; อย่าคิดงานใหม่จากรายการ remaining tasks ที่เป็นเพียงการ setup/smoke test

ห้ามเริ่มออกแบบ architecture หรือเขียนระบบใหม่โดยไม่จำเป็น รักษา Spring Boot MVC + Thymeleaf + repository/service layering, coding style, ภาษาไทย, Flyway migration convention และ business rules เดิมตาม `AGENTS.md`/`HANDOFF.md`. ห้าม hard-delete, reset/discard งาน, rewrite history หรือ force-push. เมื่อมีงานใหม่ ให้แก้เฉพาะ scope ที่ร้องขอ ตรวจสอบด้วย build/tests ที่เกี่ยวข้อง แล้วรายงานผลและข้อจำกัดให้ชัดเจน
