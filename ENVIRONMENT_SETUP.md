# Local environment setup

The current frontend is React + TypeScript + Vite. Spring Boot provides email OTP/JWT authentication and stores the current planner workspace in MySQL. Detailed planner screens are still being connected to the existing normalized Spring services.

## Requirements

- Node.js 20.9 or newer and npm
- Java 25 and Maven
- MySQL 8.4, or Docker Desktop with Compose

## Start local services

Copy `.env.example` to `.env`, replace the local-only MySQL passwords, then run:

```powershell
docker compose up -d
```

MySQL is available on port `3306`. Mailpit accepts SMTP on `1025` and its inbox UI is at `http://localhost:8025`.

Copy `backend/.env.example` to `backend/.env`. Use the same `MYSQL_DATABASE`, `MYSQL_USER`, and `MYSQL_PASSWORD` values as in the root `.env`. Generate two distinct Base64 secrets, each decoding to at least 32 bytes, for `JWT_SECRET_BASE64` and `OTP_HASH_SECRET_BASE64`.

## Start the application

In one terminal:

```powershell
Set-Location backend
mvn spring-boot:run
```

In another terminal:

```powershell
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` requests to `http://localhost:8081`; set `API_PROXY_TARGET` in the root `.env` to override it.

## Verification commands

```powershell
npm run typecheck
npm test
npm run lint
npm run build
```

Run backend tests with `mvn test` from `backend/`.

## Migration notes

- Sign-in uses single-use email OTP and an HttpOnly JWT cookie; Mailpit captures local messages.
- SPA mutations fetch a Spring CSRF token and send it in `X-XSRF-TOKEN`. Keep the frontend and backend on the same site, or configure `APP_CORS_ALLOWED_ORIGINS` and production cookie settings for the deployment.
- The database uses additive Flyway migrations and Hibernate schema validation. Flyway V6 adds `planner_workspaces` to the existing `adaptive_teaching_planner` schema; do not drop or recreate the database as part of setup.
- On the first authenticated load, if MySQL has no workspace for that teacher, the app imports only a valid browser recovery copy scoped to that account email; if none exists, it creates an empty workspace. The old unscoped browser record and sample planner data are never imported into a new account. Subsequent saves use workspace version checks; a 409 means another browser session changed the workspace and the latest data should be reloaded.
- The old Next.js and Prisma runtime code, schema, and migration files have been removed from this source tree. This does not drop or modify any PostgreSQL or MySQL database. If PostgreSQL account data might still be needed, verify and back it up before retiring that database; see `docs/migration-plan.md`.
- Syllabus extraction is still simulated. No Gemini API or object storage integration is present.
