# ATP architecture migration plan

## Initial audit findings

- The root application is a Next.js 16 / React 19 / TypeScript prototype. Its planner data and demo sign-in are stored in browser `localStorage`; syllabus extraction is simulated.
- The legacy application used PostgreSQL for OTP authentication and account records. Its Prisma schema and migration files have since been removed from this source tree; this does not establish whether the old database still contains data.
- `backend/` is already a separate Spring Boot 4.1.2 / Java 25 application using Spring Security, JPA, MySQL, and Flyway. Its five migrations cover authentication, subjects and sections, syllabus, teaching plans and sessions, and class updates and schedule adjustments.
- The Spring application already implements planner business logic and OTP/JWT authentication, but its planner and auth controllers return Thymeleaf pages/forms rather than a REST API. The database-backed Spring workflows and the browser-local prototype are not currently integrated.
- The root prototype includes scheduling, syllabus editing, review/approval, calendar, class outcomes, adjustment proposals, profile, notifications, and settings. The Spring backend implements overlapping planner workflows with a different data model.
- No Gemini integration, real syllabus file parsing/upload, or object-storage feature was found. These should not be represented as existing functionality or silently simulated as production-ready services.
- At the initial audit, `mvn test` reported no backend tests and Node.js/npm were unavailable. Four backend auth/CSRF tests and frontend checks have since been added and run.

## Migration sequence

1. **Establish the target frontend architecture.** Convert the root React/TypeScript application to Vite, retain its existing visual components and CSS, and add URL-backed client routing. The Vite shell and Spring REST authentication are now in place.
2. **Persist the current frontend safely.** Flyway V6 adds a versioned, per-teacher JSON workspace table in the existing `adaptive_teaching_planner` schema. The SPA sends CSRF-protected `PUT` requests, rejects stale versions, and migrates an existing valid browser record only when no server workspace exists. Browser data remains as a local recovery copy.
3. **Expose the normalized Spring domain through REST APIs.** The current Spring subject, syllabus, schedule, and calendar services still have Thymeleaf controllers. Add request/response DTOs and endpoints around those services, then migrate the workspace document into the normalized tables before removing the bridge.
4. **Preserve and reconcile data safely.** Keep the existing MySQL/Flyway schema and avoid destructive schema changes. If the legacy PostgreSQL database may contain accounts that need to be retained, verify and back it up before migration or retirement. Do not claim browser `localStorage` data has been migrated unless a successful import is confirmed.
5. **Configure local services and documentation.** Add Vite/Spring/MySQL/Mailpit setup, Docker Compose where appropriate, and placeholder-only environment examples. Keep production secrets out of source.
6. **Remove obsolete runtime code.** The Next.js and Prisma runtime code, Prisma schema, and migrations have been removed from the source tree. This source cleanup does not drop, modify, or verify the state of any PostgreSQL database.
7. **Verify each cutover.** Add backend and frontend tests for auth, CSRF, planner API behavior, and routing; run frontend type-check/build/tests and backend tests/build; smoke-test the configured database and email services when available.

## PostgreSQL account-data retention

The Spring MySQL schema has a `users` table. The legacy PostgreSQL database is separate, and its current contents have not been verified. If it contains accounts that must be retained:

1. Back up the PostgreSQL database and stop writes to the old auth routes.
2. Export only `users` (`id`, `email`, `created_at`, `updated_at`) to a protected CSV or SQL dump. Do not copy OTP challenge hashes; challenges are short-lived and should be invalidated during cutover.
3. Before importing, compare every email against MySQL `users`. Resolve duplicate IDs/emails explicitly instead of overwriting records.
4. Import the verified users in a transaction, compare row counts and sampled emails, and verify those accounts can use OTP sign-in.
5. Keep the backup until the owner confirms the imported accounts. Do not drop the PostgreSQL database as part of this code migration.

## Guardrails

- Preserve the current UI and implemented planning behavior rather than replacing them with a generic scaffold.
- Do not add password/role, AI, parsing, or storage behavior that the existing application does not implement without confirming the product requirements.
- Do not delete the untracked `backend/` source, project guide, or design references; these are existing project resources.
- Do not drop PostgreSQL or MySQL databases during source-code cleanup. The workspace persistence bridge does not mean the normalized planner cutover is complete.

## Implementation checkpoint

- The root React/TypeScript UI now boots through Vite and React Router, with URL-backed paths for its existing views.
- The frontend no longer offers the hard-coded demo password; it restores sign-in from the Spring session cookie and uses a CSRF-aware API client for OTP and logout requests.
- Spring now exposes the OTP/JWT auth endpoints, a readable CSRF token for the SPA, credentialed CORS configuration, and JSON errors for API authentication/CSRF failures. Both SPA logout and the existing Thymeleaf logout form have CSRF regression tests.
- Spring now exposes a versioned, teacher-scoped workspace API backed by Flyway V6. It validates top-level data, enforces a 4 MB cap, requires CSRF for writes, and rejects stale versions.
- The React app loads planner state from this API and preserves the local browser record as a recovery copy; on first use it migrates a valid local record when no server workspace exists.
- **Not cut over yet:** the UI workspace is still a JSON document rather than the normalized subject/syllabus/schedule tables; the normalized Spring planner workflows remain Thymeleaf-only. The Next.js and Prisma runtime, schema, and migration files have been removed from the source tree; no database was modified as part of that cleanup.
- Frontend type-check, lint, 8 tests, and production build pass; all 10 backend tests pass, including workspace request handling and CSRF. MySQL migration execution and browser-to-backend login remain unverified.
- Docker Compose files are present but were not exercised in this environment. A live database/email integration test remains outstanding.
