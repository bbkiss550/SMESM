# Project Handoff

ตรวจสอบสถานะเมื่อ 2026-09-25 (เวลาไทย) สำหรับส่งต่องานไปยังเครื่องอื่น

## Project Overview

SME Service Manager เป็นเว็บแอปภาษาไทยสำหรับบริหารงานบริการของ SME ครอบคลุมลูกค้า ใบงานและนัดหมาย หัวหน้าช่าง สินค้า/สต๊อก การชำระเงิน/คืนเงิน ใบเสร็จ การแจ้งเตือน รายงาน และการตั้งค่า ไฟล์แนบจัดเก็บใน local storage

## Tech Stack

- Java 21, Spring Boot 4.1.1, Maven Wrapper 3.9.16
- Spring MVC, Thymeleaf, Spring Security, Spring Data JPA, Bean Validation, Actuator
- PostgreSQL 17 และ Flyway; Hibernate ใช้ `ddl-auto: validate`
- Bootstrap 5.3.8, Bootstrap Icons, Flatpickr 4.6.13 (ภาษาไทย), Chart.js 4.5.0, SweetAlert2 11.22.4
- Docker Compose ใช้ PostgreSQL 17 Alpine และ Java 21 runtime
- Time zone ของระบบ: `Asia/Bangkok`; upload เริ่มต้นที่ `storage/uploads/jobs`

## Current Branch

`main`, track `origin/main` (`https://github.com/bbkiss550/SMESM.git`)

## Current Commit

Commit ที่ตรวจสอบก่อนจัดทำ handoff: `20957af3da5760364ee474784a4cf4caa36735b7` — `feat: improve record management and modal workflows`.

เอกสาร handoff และกฎ ignore ที่เพิ่มในรอบนี้จะถูกบันทึกใน commit ถัดจาก commit ฐานข้างต้น

## Current Working State

- ก่อนเริ่ม handoff: working tree สะอาด และ `main` ตรงกับ `origin/main` (ahead/behind = 0/0)
- ไม่มีไฟล์ feature ที่ค้างแก้ไขหรือ staged ณ จุดเริ่มต้น
- รอบ handoff เพิ่มเฉพาะ `HANDOFF.md`, `RESUME_PROMPT.md`, `AGENTS.md` และกฎ `.gitignore`; จะ commit/push เป็น commit เอกสารแยกต่างหาก
- ไม่มีงานพัฒนา feature ที่กำลังทำค้างอยู่ตามสถานะ Git ที่ตรวจพบ

## Completed Work

- ระบบจัดการลูกค้า ใบงาน ปฏิทินงาน ผู้ใช้/หัวหน้าช่าง แคตตาล็อกบริการและสินค้า สต๊อก การรับชำระ/คืนเงิน ใบเสร็จ รายงาน และตั้งค่า
- UX ภาษาไทย รวมถึง datepicker ปฏิทินไทยและการเลือกวัน/เวลาแยกช่อง
- ตารางหลักแสดง 10 รายการต่อหน้า พร้อม sort และ AJAX search/pagination เพื่อแทนที่เฉพาะตารางโดยไม่ reload ทั้งหน้า
- Modal สำหรับเพิ่มใบงานจากหน้ารายการและปฏิทิน; รายละเอียดงานจากปฏิทินเปิด modal
- หน้าลูกค้ามีปุ่มดูข้อมูลเป็น modal พร้อมประวัติการใช้บริการแบบแบ่งหน้า และปุ่มแก้ไขเป็น modal
- ข้อมูลหลักมีการแก้ไขและปิดใช้งานแบบเก็บประวัติ; ใบงานใช้ workflow ยกเลิก ส่วนธุรกรรมการเงิน/สต๊อกเป็น audit history
- ช่องเบอร์โทรจำกัด 10 ตัว พร้อมตัวนับ; modal สำหรับเพิ่มรายการล้างค่าฟอร์มเมื่อเปิดใหม่
- การจัดเก็บไฟล์แนบเป็น local storage

## Work In Progress

ไม่มีงานค้างที่ระบุได้จาก working tree ณ วันที่ตรวจสอบ

## Remaining Tasks

- ไม่มี backlog หรือ issue tracker ที่ผูกกับ repository ให้ตรวจสอบในรอบ handoff นี้; ยืนยันงานถัดไปกับเจ้าของโปรเจกต์ก่อนเริ่ม feature ใหม่
- บนเครื่องใหม่ให้ตั้ง Java/PostgreSQL และ `.env` แล้วรัน smoke test ตามหัวข้อด้านล่าง โดยเฉพาะ modal ลูกค้า, ประวัติบริการ, AJAX pagination/search และการ login
- เปลี่ยนรหัสผ่านบัญชีสาธิตก่อนใช้กับข้อมูลหรือเครือข่ายจริง

## Known Bugs / Issues

- ไม่พบ bug ที่ยืนยันได้และยังเปิดค้างจากข้อมูลใน repository/การตรวจครั้งนี้
- `SmeServiceManagerApplicationTests` เริ่ม Spring Context และต้องเชื่อมต่อ PostgreSQL; การรันโดยไม่มี `DB_PASSWORD`/ค่าฐานข้อมูลที่ถูกต้องทำให้ context test ล้มเหลวจาก PostgreSQL SCRAM authentication ไม่ใช่จาก test logic
- พบไฟล์ `db/db_service_demo-db_service_demo-202609231600.sql` ขนาดประมาณ 98 KB ซึ่งขึ้นต้นด้วย PostgreSQL custom-archive marker (`PGDMP`) แม้นามสกุลเป็น `.sql`; เป็นไฟล์ที่ track อยู่ตั้งแต่ initial commit และอยู่ใน remote แล้ว ผู้ใช้เคยยืนยันว่าข้อมูลที่นำขึ้น Git เป็นข้อมูลสมมติ แต่ archive นี้ไม่ได้ถูกแก้หรือถอดจาก Git ใน handoff นี้ และเครื่องมือ `pg_restore` ไม่มีใน PATH จึงไม่ได้ตรวจรายการ/ข้อมูลภายใน ห้ามใช้เป็น backup จริงหรือส่งต่อไปยังระบบภายนอกโดยไม่ยืนยันแหล่งข้อมูลก่อน
- `.gitignore` เดิม ignore `.env`, build/cache และ uploads อยู่แล้ว; รอบ handoff เพิ่ม ignore สำหรับ credential/certificate และ dump ใหม่ กฎ ignore ใหม่ไม่ยกเลิกการ track ไฟล์ archive ที่อยู่ใน Git อยู่ก่อนแล้ว

## Important Files

- `pom.xml`, `.mvn/wrapper/maven-wrapper.properties` — dependencies และ Maven Wrapper
- `src/main/resources/application.yml`, `.env.example` — configuration; ใส่ค่าจริงเฉพาะใน `.env` ที่ ignored
- `src/main/resources/db/migration/V1__create_schema.sql` ถึง `V7__localize_demo_master_data.sql` — schema, indexes และข้อมูลสาธิต
- `src/main/java/com/smeservicemanager/` — โค้ดแยกตามโดเมน เช่น `job`, `customer`, `calendar`, `catalog`, `payment`, `stock`, `security`
- `src/main/resources/templates/` — Thymeleaf UI; `templates/fragments/layout.html` เป็นส่วน layout กลาง
- `src/main/resources/static/assets/js/app.js`, `assets/css/app.css` — พฤติกรรมและสไตล์ UI กลาง
- `src/test/java/` — context, การคำนวณ VAT และการประกอบวัน/เวลา
- `docker-compose.yml`, `Dockerfile` — วิธีรันผ่าน Docker
- `README.md` — คู่มือเริ่มต้นและข้อกำหนดธุรกิจที่บันทึกไว้

## Important Design Decisions

- UI เบื้องต้นเป็นภาษาไทยทั้งหมด และออกแบบให้ใช้ local storage สำหรับไฟล์แนบ
- ใช้ soft delete/audit fields แทน hard delete; payment/refund และ stock transactions เป็นประวัติที่ไม่ควรแก้ย้อนหลัง
- การยกเลิกงานเป็นกระบวนการเดียวกับการเลือกคืนเงินและสร้างหลักฐานคืนเงิน
- หัวหน้าช่างหนึ่งคนรับงานซ้อนเวลาไม่ได้; งานก่อนมีสิทธิ์ก่อน
- งานที่ปิดแล้วไม่เปิดกลับ; งานแก้ต้องสร้างเป็นใบงานใหม่และเชื่อมงานเดิม
- VAT 7% คำนวณย้อนจากราคารวมสุทธิ ไม่บวกเพิ่มกับลูกค้า
- ใช้ Flyway เป็นเจ้าของ schema; Hibernate ตรวจ schema ด้วย `validate`
- รายการตารางและการค้นหาใช้ AJAX เพื่อไม่ reload หน้า; modal รายละเอียดควรโหลดข้อมูลจาก endpoint/fragment เดิมของแต่ละโดเมน

## Database / Migration Status

- ฐานข้อมูลพัฒนา: PostgreSQL `db_service_demo`; ค่าพอร์ต/ผู้ใช้ต้องตั้งตามเครื่องและอยู่ใน `.env`
- migration ที่มีใน source: V1–V7; ตอนรันระบบครั้งล่าสุดที่ตรวจไว้เมื่อ 2026-09-24 Flyway อยู่ที่ V7 และ schema up to date
- PostgreSQL ที่รันในเครื่องก่อนหน้านี้รายงานเวอร์ชัน 17.10; Docker Compose ระบุ `postgres:17-alpine`
- ห้ามแก้ migration เก่าที่ถูก apply แล้ว ให้เพิ่ม migration หมายเลขถัดไป
- ฐานข้อมูลกับ dump ไม่จำเป็นต้องย้ายด้วยการ commit credentials; ใช้ฐานข้อมูลว่างแล้วให้ Flyway สร้าง schema และ seed demo data หรือย้ายข้อมูลผ่านช่องทางที่ปลอดภัยตามที่เจ้าของยืนยัน

## How To Run

ต้องมี JDK 21 และ PostgreSQL 17 (หรือ Docker Engine) พร้อมฐานข้อมูล `db_service_demo`

```powershell
Copy-Item .env.example .env
# แก้ .env และกำหนด DB_PASSWORD จริงในเครื่องนี้ ห้าม commit
.\mvnw.cmd spring-boot:run
```

ค่าเริ่มต้นเว็บคือ `http://localhost:8080`; ตั้ง `SERVER_PORT=8081` ใน `.env` หากต้องการพอร์ต 8081. ตรวจ health ได้ที่ `/actuator/health`.

บัญชี demo ตาม README: `admin`, `staff`, `technician` ใช้รหัสเริ่มต้น `demo1234`; เปลี่ยนรหัสก่อนใช้งานจริง

Docker Compose:

```powershell
$env:POSTGRES_PASSWORD = 'กำหนดรหัสผ่านเฉพาะเครื่อง'
docker compose up --build
```

## How To Test

```powershell
.\mvnw.cmd test
```

ต้องตั้ง `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` ให้เชื่อม PostgreSQL ได้ก่อน เพราะ context test โหลด application context และ Flyway. ผลทดสอบล่าสุดที่ทราบจาก 2026-09-24 ผ่านเมื่อกำหนด DB environment; การทดสอบรอบ handoff นี้ไม่ได้รันซ้ำ

## Next Recommended Step

บนเครื่องใหม่: clone/pull branch `main`, อ่าน `AGENTS.md` และเอกสารนี้, สร้าง `.env` จาก `.env.example`, เตรียม PostgreSQL, จากนั้นรัน `mvnw test` และเปิดระบบเพื่อตรวจหน้า dashboard, ลูกค้า/ประวัติบริการ, ตาราง AJAX และปฏิทิน ก่อนรับงาน feature ถัดไปจากเจ้าของโปรเจกต์
