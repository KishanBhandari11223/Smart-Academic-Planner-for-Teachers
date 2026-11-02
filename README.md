# Adaptive Teaching Planner

A university teaching planner with a React/Vite frontend and a Spring Boot backend. Sign-in and a versioned planner-workspace API use Spring; the current UI data is stored per teacher in MySQL while the planner screens are progressively mapped to the backend's normalized domain services.

## Implemented

- Email OTP sign-in and JWT-cookie sign-out through Spring REST endpoints.
- Dashboard, subject list, and subject workspace with the supplied subject tabs.
- Three-step subject setup for course details, sections, pace mode, and weekly teaching slots.
- Simulated syllabus extraction with file selection or manual text, plus editable units and topics.
- Pacing matrix for topic complexity, importance, and estimated teaching effort.
- Deterministic schedule generation against selected weekdays and planning dates.
- Separate schedule-map and teaching-plan-review stages.
- Explicit plan approval before proposed sessions enter the global Calendar.
- Lightweight post-class update for completed, partially covered, or not-covered outcomes.
- A proposed follow-up session after partial or missed coverage, with apply/keep-original choices.
- Per-teacher MySQL persistence for the current planner workspace; new accounts start empty, and existing account-scoped browser recovery data is imported only when that teacher has no server workspace.
- Responsive layouts, reduced-motion support, and a restrained GSAP content entrance transition.

The subject’s Tests & Revision, Analytics, and Team tabs have small explanatory placeholders; they are not expanded into invented workflows.

## Current architecture

- React 19, TypeScript, Vite, and React Router
- GSAP for one scoped, reversible screen transition
- Lucide icons
- Spring Boot 4.1.2, Spring Security, JPA/Hibernate, MySQL, and Flyway in `backend/`
- Cookie-based JWT authentication with CSRF protection for state-changing requests

The workspace API stores the current versioned `AppData` document in MySQL's `planner_workspaces` table (Flyway V6), scoped to the authenticated teacher and protected with CSRF and optimistic version checks. `localStorage` recovery copies are scoped by the signed-in email; the old unscoped browser record is not imported into a teacher account. This is a safe persistence bridge, not yet the final normalized REST integration: the existing Spring subject, syllabus, schedule, and calendar services still expose Thymeleaf controllers.

## Requirements

- Node.js 20.9 or newer
- npm
- Java 25 and Maven
- MySQL 8.4 or Docker Compose
- Mailpit for local OTP email testing (optional)

## Local setup

```bash
npm install
npm run dev
```

The Vite app runs at `http://localhost:5173` and proxies `/api` requests to `http://localhost:8081` by default. Override the proxy target with `API_PROXY_TARGET`.

For local MySQL and Mailpit, copy `.env.example` to `.env`, set local-only passwords, and start the services:

```bash
docker compose up -d
```

Copy `backend/.env.example` to `backend/.env`, use matching database credentials, and generate distinct Base64 secrets of at least 32 bytes for `JWT_SECRET_BASE64` and `OTP_HASH_SECRET_BASE64`. The examples contain placeholders only.

```bash
cd backend
mvn spring-boot:run
```

OTP messages are captured by Mailpit at `http://localhost:8025`. The backend exposes `/api/auth/csrf`, `/api/auth/send-otp`, `/api/auth/verify-otp`, `/api/auth/me`, and `/api/auth/logout`, plus `GET`/`PUT /api/planner/workspace`. On the first sign-in without a server workspace, the app imports only a valid browser recovery copy scoped to that account; otherwise, it creates an empty workspace. The browser copy is retained. Use **Settings → Clear planner data** to empty the signed-in teacher's workspace.

The default sample has two completed sessions and an approved plan. Add Subject to try the complete setup and approval journey without changing the sample subject.

## Syllabus and planning behavior

Syllabus extraction is a **local simulation**. Selecting a file only displays its name; the file is neither uploaded nor parsed. Entering text enables a simulated extraction state, after which editable sample units and topics are shown. This must not be described as real AI or document parsing.

The planner uses this deterministic heuristic:

1. Traverse topics in syllabus order.
2. Round each topic’s estimated effort up to whole teaching slots.
3. Traverse the selected date range and use the configured weekday/time slots in order.
4. Skip every blocked holiday or exam date.
5. Leave topics unscheduled when the selected period has insufficient slots; never silently omit them.
6. Keep proposed sessions out of Calendar until a teacher approves them.

The prototype’s “independent pace” choice is stored, but session generation currently places selected sections together. Adaptive follow-up proposals reserve one next available open slot and do not move unrelated sessions.

## Scripts

```bash
npm run dev        # Start the development server
npm run typecheck  # Strict TypeScript check
npm run lint       # ESLint
npm run build      # Production build
npm start          # Preview a completed Vite production build
npm test           # Frontend API-client tests
```

Run backend tests with `mvn test` from `backend/`.

## Verification performed for this bundle

- `npm run lint`, `npm run typecheck`, `npm test` (8 tests), and `npm run build` — passed for the Vite shell, auth, workspace client, and safe local-data import.
- `mvn test` — passed 10 auth/logout CSRF and workspace authentication, CSRF, validation, payload-shape, and versioned-save tests.
- Vite development-server smoke test — returned HTTP 200 and rendered the sign-in screen.
- MySQL migration execution, backend database connectivity, and full browser-to-Spring OTP/workspace flow have not been verified in this environment.
- `npm audit --omit=dev` — 0 production vulnerabilities; full `npm audit` still reports 11 moderate findings in the ESLint toolchain. The suggested `--force` remediation is breaking and was not applied.
- The frontend dependency audit should be rerun after removing the obsolete Next.js runtime dependencies.

## Project structure

```text
src/
  App.tsx           React Router route host
  main.tsx          Vite entry point and global styles
  app/              Global styles
  components/       App shell, workflow screens, shared UI
  data/             Seed course and illustrative academic dates
  lib/              API client, local persistence, and planning algorithms
  types/            Shared domain and screen types
backend/
  src/main/java/     Spring application and planner services
  src/main/resources/ Flyway migrations, properties, and existing Thymeleaf views
docs/
  design-audit.md
  user-flows.md
  architecture.md
  planning-logic.md
  implementation-plan.md
  known-limitations.md
```

## Known limitations

- Planner mutations are stored as a versioned workspace JSON document; individual subject, syllabus, schedule, and calendar flows are not yet integrated with the normalized Spring domain services.
- No production syllabus file upload, document parsing, Gemini integration, or object storage.
- Institutional holidays, exam windows, teacher, and course records are illustrative.
- The heuristic does not optimize multiple sections independently, room capacity, instructor conflicts across subjects, or partial-hour slot splitting.
- Calendar shows approved/completed sessions, not pending proposals.
- Secondary subject tabs are intentionally minimal.
- No automated browser/E2E suite is included.
