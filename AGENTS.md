# Project Development Instructions

- Read `HANDOFF.md` and the relevant source files before making changes. Preserve the current Spring Boot architecture and conventions; do not redesign or replace working subsystems without a concrete requirement.
- The application is Java 21 / Spring Boot MVC with Thymeleaf. Keep business rules in services, persistence in repositories, and UI behavior in the existing templates and `static/assets/js/app.js` / `app.css` patterns.
- Keep user-facing text in Thai. Use `Asia/Bangkok` for user-facing date/time behavior and retain the existing separate date/time fields and Thai calendar conventions.
- Do not hard-delete business data. Master records use audit fields and soft deactivation. Payment/refund and stock movement records are audit history and should remain read-only; job cancellation and refunds must use the established cancellation workflow.
- Respect the established business rules: technician bookings cannot overlap (the earlier booking wins); completed jobs are not reopened and corrections become a new repair job linked to the original; VAT is calculated inclusively from the customer total and is not added on top.
- Database schema changes must be added as a new Flyway migration under `src/main/resources/db/migration`. Do not edit or reorder migrations already applied to a database. Hibernate schema mode is `validate`.
- Never add credentials, tokens, private keys, `.env`, or real database exports to Git. Configure local secrets through ignored `.env`; `.env.example` must contain placeholders only. The existing tracked database archive is documented in `HANDOFF.md` and must not be replaced casually.
- Use the Maven Wrapper (`.\mvnw.cmd` on Windows, `./mvnw` elsewhere). Run relevant tests/build checks after code changes; the context test requires PostgreSQL connection settings.
- Preserve unrelated working-tree changes. Do not reset, discard, rewrite history, force-push, or commit/push unless the user explicitly requests it.
