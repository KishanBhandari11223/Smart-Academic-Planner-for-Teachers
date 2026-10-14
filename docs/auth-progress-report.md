# Authentication Implementation Progress Report

**Date:** October 14, 2026  
**Status:** Completed  
**Branch / Target:** Next.js App Router (TypeScript, Node 22, React 19)

---

## 1. What Was Implemented

### Database & ORM
- **Prisma & PostgreSQL Setup**: Installed and configured Prisma 6 with PostgreSQL. Created initial migration `20261014144405_init_auth`.
- **Database Models**: Created exactly two models in `prisma/schema.prisma`:
  - `User`: `id` (UUID string), `email` (unique string), `createdAt`, `updatedAt`.
  - `OtpChallenge`: `id` (UUID string), `email`, `codeHash`, `expiresAt`, `attempts` (default 0), `consumedAt` (nullable DateTime), `createdAt`.
- **Zero Scope Creep**: No subject, syllabus, schedule, or application models were introduced into the database.

### Server-Side Security Modules (`src/lib/server/*`)
All server modules enforce `import "server-only"` to guarantee they cannot be bundled into client-side code:
1. **`src/lib/server/env.ts`**: Validates server environment variables (`DATABASE_URL`, `JWT_SECRET`, `JWT_ISSUER`, `JWT_AUDIENCE`, `JWT_EXPIRES_IN`, `RESEND_API_KEY`, `EMAIL_FROM`, `NODE_ENV`).
2. **`src/lib/server/prisma.ts`**: Global singleton PrismaClient instance avoiding socket exhaustion across Next.js dev reloads.
3. **`src/lib/server/otp.ts`**:
   - CSPRNG 6-digit OTP generation via Node.js `crypto.randomInt`.
   - HMAC-SHA256 hashing bound to the recipient email and server secret.
   - Timing-safe constant-time hash comparison via `crypto.timingSafeEqual` preventing side-channel attacks.
   - Email format validation and lowercase/trim normalization.
   - Expiry set strictly to 5 minutes.
4. **`src/lib/server/jwt.ts`**:
   - Genuine signed JWT token issuance using `jose` with HS256 algorithm.
   - Token verification against issuer, audience, and secret.
   - Cookie configuration enforcing `HttpOnly`, `SameSite=Lax`, and `Secure` (in production).
5. **`src/lib/server/email.ts`**:
   - Server-side email delivery using `resend` package.
   - Safe mock mode for local dev/testing without active API keys.
   - Secure error handling that never leaks secrets or raw OTPs.
6. **`src/lib/server/csrf.ts`**:
   - Same-origin verification (`Origin` / `Referer` vs `Host`) for cookie-authenticated state-changing requests.

### Route Handlers
1. **`POST /api/auth/send-otp`**:
   - Validates and normalizes email.
   - Enforces 60-second resend cooldown and rolling window rate limit (max 5 per 15 min).
   - Invalidates previous unused challenges for the same email.
   - Hashes and saves the challenge to PostgreSQL.
   - Delivers code via Resend.
   - Returns generic response to prevent account enumeration.
2. **`POST /api/auth/verify-otp`**:
   - Validates email and 6-digit code.
   - Finds active challenge; enforces 5-minute expiry and 5-attempt limit.
   - Verifies OTP hash using timing-safe comparison.
   - Concurrency protection: atomically updates `consumedAt` using `updateMany({ where: { consumedAt: null } })` to prevent race conditions.
   - Upserts `User` record *only* upon successful OTP verification.
   - Issues genuine signed JWT in an HttpOnly session cookie.
3. **`GET /api/auth/me`**:
   - Extracts session cookie from request.
   - Verifies JWT signature and claims via `jose`.
   - Fetches authenticated user's safe public fields (`id`, `email`, `createdAt`).
   - Returns 401 if unauthenticated or expired.
4. **`POST /api/auth/logout`**:
   - CSRF check.
   - Clears session cookie with matching path, domain, and `Max-Age=0`.

### UI Integration
- **`src/components/LoginScreen.tsx`**: Preserved layout, styling, and animations while adding two-step email OTP entry, cooldown timer, resend button, error handling, and demo shortcut.
- **`src/components/PlannerApp.tsx`**: Added automatic session restoration via `GET /api/auth/me` on initial load, wired `handleLoginSuccess`, and connected `POST /api/auth/logout` to sign-out actions.

---

## 2. What Was Tested

An automated test suite was implemented in [`tests/auth.test.ts`](../tests/auth.test.ts) using Vitest against the running PostgreSQL instance:

1. **OTP Security & Hashing**:
   - Verified 6-digit numeric generation (`/^\d{6}$/`).
   - Email normalization (`FACULTY@University.EDU` -> `faculty@university.edu`) and RFC validation.
   - Timing-safe HMAC hash matching and cross-account rejection.
2. **JWT Verification**:
   - Signed and verified 3-part JWT tokens.
   - Verified rejection of tampered tokens, malformed strings, and missing claims.
3. **Unauthenticated Requests**:
   - Verified `GET /api/auth/me` returns 401 when cookie is missing.
   - Verified `GET /api/auth/me` returns 401 when cookie is invalid or forged.
4. **`POST /api/auth/send-otp`**:
   - Verified challenge creation with 5-minute expiry.
   - Verified OTP is never exposed in response body.
   - Verified 60-second resend cooldown returns 429.
5. **`POST /api/auth/verify-otp` Failure Modes**:
   - Verified rejection of incorrect OTP and incrementing of attempt counter.
   - Verified rejection of expired OTP (older than 5 minutes).
   - Verified lockout after exceeding maximum failed attempts (5).
   - Verified rejection of reused/consumed OTPs.
6. **End-to-End Successful Login Flow**:
   - Verified full cycle: `send-otp` -> `verify-otp` -> user creation in PostgreSQL -> HttpOnly cookie issuance -> `GET /api/auth/me` verification -> `POST /api/auth/logout` cookie clearance.

---

## 3. Actual Command Execution Results

### 1. Typecheck (`npm run typecheck`)
```
> adaptive-teaching-planner@1.0.0 typecheck
> tsc --noEmit
Exit code: 0
```

### 2. Linting (`npm run lint`)
```
> adaptive-teaching-planner@1.0.0 lint
> eslint .
Exit code: 0
```

### 3. Automated Test Suite (`npm test`)
```
> adaptive-teaching-planner@1.0.0 test
> vitest run

 RUN  v5.0.4 /home/kartikey-xq/Adaptive-Teaching-Planner-NextJS

 ✓ tests/auth.test.ts (15 tests) 149ms
   ✓ Email OTP Authentication Test Suite (15)
     ✓ Unit: OTP Security & Hashing (3)
       ✓ generates a 6-digit cryptographically secure OTP string 44ms
       ✓ correctly normalizes and validates email addresses 3ms
       ✓ verifies hash matches correct OTP and rejects incorrect OTP in constant time 3ms
     ✓ Unit: JWT Verification (3)
       ✓ signs and genuinely verifies a JWT session token 6ms
       ✓ rejects tampered or forged JWT tokens 3ms
       ✓ rejects empty or malformed token strings 2ms
     ✓ Endpoint: Unauthenticated Requests (2)
       ✓ GET /api/auth/me returns 401 when no session cookie is provided 5ms
       ✓ GET /api/auth/me returns 401 when an invalid cookie is supplied 2ms
     ✓ Endpoint: POST /api/auth/send-otp (2)
       ✓ validates email and creates an unconsumed challenge with 5-minute expiry without exposing OTP 16ms
       ✓ enforces resend cooldown limit when requested again immediately 10ms
     ✓ Endpoint: POST /api/auth/verify-otp (Failure Modes) (4)
       ✓ rejects invalid OTP code and increments attempt counter 9ms
       ✓ rejects expired OTP challenge 6ms
       ✓ enforces attempt limits and rejects after maximum failed attempts 6ms
       ✓ prevents OTP reuse once already consumed 7ms
     ✓ End-to-End: Successful Login, Session Check, and Logout (1)
       ✓ completes full authentication flow: send -> verify -> me -> logout 19ms

 Test Files  1 passed (1)
      Tests  15 passed (15)
   Duration  427ms
Exit code: 0
```

### 4. Production Build (`npm run build`)
```
> adaptive-teaching-planner@1.0.0 build
> next build

▲ Next.js 16.4.0 (Turbopack)
- Environments: .env
✓ Running next.config.ts took 33ms

  Creating an optimized production build ...
✓ Compiled successfully in 289ms
  Finished TypeScript in 1692ms    ✓ Finished TypeScript in 1692ms 
  Collecting page data using 8 workers in 610ms    ✓ Collecting page data using 8 workers in 610ms 
✓ Generating static pages using 8 workers (7/7) in 575ms
  Finalizing page optimization in 10ms    ✓ Finalizing page optimization in 10ms 

Route (app)
┌ ○ /
├ ○ /_not-found
├ ƒ /api/auth/logout
├ ƒ /api/auth/me
├ ƒ /api/auth/send-otp
└ ƒ /api/auth/verify-otp

○  (Static)   prerendered as static content
ƒ  (Dynamic)  server-rendered on demand

Exit code: 0
```

### 5. Database Migration
```
> prisma migrate dev --name init_auth
Applying migration `20261014144405_init_auth`
The following migration(s) have been created and applied from new schema changes:
prisma/migrations/
  └─ 20261014144405_init_auth/
    └─ migration.sql
Your database is now in sync with your schema.
Exit code: 0
```

---

## 4. What Remains / Future Considerations (Out of Scope)

The requested scope (email OTP authentication only) is 100% complete and operational. Future production enhancements not within the current scope include:

1. **Production Resend Domain Verification**: Update `EMAIL_FROM` with an institutional domain (e.g. `auth@university.edu`) once verified in Resend DNS.
2. **Refresh Token Rotation**: Optional sliding session extension if longer continuous login periods are needed without re-verification.
3. **Role-Based Access Control**: Adding role metadata if distinct faculty vs admin permissions are introduced.
4. **Database-backed Planner Data**: Migrating subjects, schedules, and syllabus from browser `localStorage` to PostgreSQL tables linked to `User.id`.
