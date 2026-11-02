# Spring Boot backend

Java application built with Spring Boot 4.1.2, Spring Security, Spring Data JPA, MySQL, Flyway, and Thymeleaf. The root React/Vite app uses the REST auth API and a versioned per-teacher planner-workspace API. The normalized planner services still have legacy Thymeleaf controllers while their REST API is being migrated.

## Local setup

1. Start MySQL and Mailpit using the root `docker-compose.yml`, or create the `adaptive_teaching_planner` database yourself.
2. Copy `backend/.env.example` to `.env` in this folder. Use the same database credentials as the root Compose environment. Generate distinct Base64 secrets for `JWT_SECRET_BASE64` and `OTP_HASH_SECRET_BASE64`; each must decode to at least 32 bytes.
3. Keep `APP_CORS_ALLOWED_ORIGINS` set to the frontend origin. Local defaults allow `localhost` and `127.0.0.1` on Vite ports 5173 and 5175.
4. Run `mvn spring-boot:run` from this folder.

The sign-in flow uses six-digit codes that expire after five minutes, a 60-second resend cooldown, a five-request email limit per 15 minutes, a per-IP request limit, and at most five verification attempts per code. A teacher account is created only after a code is verified. Sessions use a seven-day signed JWT in an HttpOnly, SameSite=Lax cookie. Activate the `prod` profile to require Secure cookies.

The SPA auth endpoints are `GET /api/auth/csrf`, `POST /api/auth/send-otp`, `POST /api/auth/verify-otp`, `GET /api/auth/me`, and `POST /api/auth/logout`. State-changing requests require the CSRF cookie and matching `X-XSRF-TOKEN` header. The CSRF cookie is JavaScript-readable by design so the SPA can submit the matching header; the session JWT remains HttpOnly.

Planner workspace endpoints are `GET /api/planner/workspace` and `PUT /api/planner/workspace`. The document is scoped to the authenticated teacher, limited to 4 MB, and version checked; stale writes return HTTP 409. Flyway V6 adds the JSON workspace table without changing existing tables. The current UI imports a valid legacy browser-local record only when no server workspace exists and retains its local copy as a backup.

Flyway applies versioned SQL migrations at startup. Hibernate validates the resulting schema. The workspace document is an incremental persistence bridge; the existing normalized subject, syllabus, schedule, and calendar services are not yet exposed as REST endpoints.
