# Email OTP Authentication Documentation

This document describes the design, implementation, configuration, API contracts, and limitations of the email-based One-Time Password (OTP) authentication system in the Adaptive Teaching Planner.

---

## 1. Overview & Architecture

The authentication system is a lightweight, passwordless, server-side implementation built directly into the Next.js App Router using Route Handlers. It adheres strictly to modern web security practices:

- **Passwordless Security**: Eliminates passwords entirely, eliminating risks from credential stuffing and weak passwords.
- **Server-Only Isolation**: All security-critical code (`src/lib/server/*`), including database operations, HMAC hashing, cryptographic random generation, Resend email delivery, and JWT signing/verification, enforces `import "server-only"`. They cannot be bundled into client-side code.
- **No Plaintext Secrets Stored**: OTP codes are generated using Node.js's cryptographically secure PRNG (`crypto.randomInt`), immediately hashed using HMAC-SHA256 with the server's `JWT_SECRET` keyed and bound to the normalized email, and never written to storage or returned to the client in plaintext.
- **Timing-Safe Comparison**: OTP codes are compared against stored hashes using `crypto.timingSafeEqual` to eliminate side-channel timing attacks.
- **Concurrency & Replay Safety**: Challenge consumption is executed via an atomic database condition (`consumedAt: null`), preventing race conditions from allowing multiple concurrent logins on the same OTP.
- **Strict Cookie Storage**: Session tokens are issued as genuine signed JSON Web Tokens (JWT) using `jose` and stored in `HttpOnly`, `SameSite=Lax` cookies (with `Secure` enabled in production). Tokens are never exposed to browser `localStorage` or JavaScript.
- **CSRF Defense**: State-changing endpoints (`POST`) enforce same-origin verification (`Origin` / `Referer` headers) to guard cookie-authenticated sessions against cross-site request forgery.

---

## 2. Database Models & Schema

The system uses PostgreSQL managed via Prisma ORM with only two database models:

```prisma
datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

generator client {
  provider = "prisma-client-js"
}

model User {
  id        String   @id @default(uuid())
  email     String   @unique
  createdAt DateTime @default(now())
  updatedAt DateTime @updatedAt

  @@map("users")
}

model OtpChallenge {
  id         String    @id @default(uuid())
  email      String
  codeHash   String
  expiresAt  DateTime
  attempts   Int       @default(0)
  consumedAt DateTime?
  createdAt  DateTime  @default(now())

  @@index([email])
  @@map("otp_challenges")
}
```

### Key Security Decisions in Schema
1. **No User Created Before Verification**: The `User` model is created or fetched *only* after an OTP challenge is successfully verified. An unverified challenge cannot create a phantom user account.
2. **Nullable `consumedAt`**: Tracks single-use lifecycle and timestamp of consumption.
3. **`attempts` Counter**: Tracks failed verification attempts to lock out brute-force guessing.

---

## 3. Environment Variables & Setup

All configuration is controlled via environment variables. A template is provided in [`.env.example`](../.env.example):

| Variable | Description | Example / Default |
|---|---|---|
| `DATABASE_URL` | PostgreSQL connection string | `postgresql://postgres:postgres@localhost:5432/teaching_planner?schema=public` |
| `RESEND_API_KEY` | Resend API key for transactional email | `re_placeholder_xxxxxxxxxxxx` |
| `EMAIL_FROM` | Sender display name and address | `Teaching Planner <onboarding@resend.dev>` |
| `JWT_SECRET` | Cryptographic secret for HMAC-SHA256 and JWT signing (min 32 chars) | Random 32+ character string |
| `JWT_ISSUER` | JWT issuer (`iss`) claim | `adaptive-teaching-planner` |
| `JWT_AUDIENCE` | JWT audience (`aud`) claim | `adaptive-teaching-planner-faculty` |
| `JWT_EXPIRES_IN` | JWT session lifetime | `7d` |
| `NODE_ENV` | Runtime environment (`development`, `production`, `test`) | `development` |
| `NEXT_PUBLIC_APP_URL`| Base public application URL | `http://localhost:3000` |

### Database Setup & Migrations

1. Ensure a PostgreSQL instance is running. (For local development with Docker):
   ```bash
   docker run --name pg-dev -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres -e POSTGRES_DB=teaching_planner -p 5432:5432 -d postgres:16-alpine
   ```
2. Copy the template and configure your connection string:
   ```bash
   cp .env.example .env
   ```
3. Run the Prisma migration:
   ```bash
   npx prisma migrate dev
   ```
4. Generate the Prisma Client:
   ```bash
   npx prisma generate
   ```

---

## 4. Resend Configuration

1. Sign up at [resend.com](https://resend.com) and retrieve an API key from the Resend Dashboard.
2. Add your key to `RESEND_API_KEY` in `.env`.
3. Configure `EMAIL_FROM` with your verified domain (or use `onboarding@resend.dev` during initial development).
4. **Development & Testing Fallback**:
   When `RESEND_API_KEY` starts with `re_placeholder` or `re_mock_test_key`, the server operates in safe development mock mode—it generates and logs verification lifecycle events without making external HTTP requests, allowing unit and integration tests to run offline.

---

## 5. End-to-End Authentication Flow

```
User (Browser)                  Route Handler (Server)               PostgreSQL / Resend
      |                                   |                                   |
      | 1. POST /api/auth/send-otp        |                                   |
      |    { email: "user@univ.edu" }     |                                   |
      | --------------------------------> | Validate & normalize email        |
      |                                   | Check resend cooldown (60s)       |
      |                                   | Invalidate prior unused codes     |
      |                                   | Generate 6-digit CSPRNG OTP       |
      |                                   | Compute HMAC-SHA256 hash          |
      |                                   | --------------------------------> | Store challenge (expires in 5m)
      |                                   | --------------------------------> | Send OTP via Resend API
      | <-------------------------------- |                                   |
      |    { success: true, message: ...} |                                   |
      |                                   |                                   |
      | 2. POST /api/auth/verify-otp      |                                   |
      |    { email, code: "123456" }      |                                   |
      | --------------------------------> | Find active challenge             |
      |                                   | Check expiry (< 5 min)            |
      |                                   | Check attempts (< 5)              |
      |                                   | Timing-safe HMAC hash check       |
      |                                   | Atomic update: consumedAt = now() |
      |                                   | --------------------------------> | Atomic UPDATE ... WHERE consumedAt IS NULL
      |                                   | --------------------------------> | Upsert User record
      |                                   | Sign JWT via `jose`               |
      | <-------------------------------- |                                   |
      |    Set-Cookie: auth_session=...   |                                   |
      |    { success: true, user: {...} } |                                   |
      |                                   |                                   |
      | 3. GET /api/auth/me               |                                   |
      |    Cookie: auth_session=...       |                                   |
      | --------------------------------> | Verify JWT signature & claims     |
      |                                   | --------------------------------> | Query user by verified ID
      | <-------------------------------- |                                   |
      |    { user: { id, email, ... } }   |                                   |
      |                                   |                                   |
      | 4. POST /api/auth/logout          |                                   |
      | --------------------------------> | Check CSRF                        |
      | <-------------------------------- | Set-Cookie: auth_session=; Max-Age=0
      |    { success: true }              |                                   |
```

---

## 6. API Contracts

### `POST /api/auth/send-otp`
Initiates an OTP challenge and sends an email.

- **Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "email": "faculty@university.edu"
  }
  ```
- **Success Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "If the email is valid, a verification code has been sent."
  }
  ```
- **Error Responses**:
  - `400 Bad Request`: `{ "error": "Please enter a valid email address." }`
  - `403 Forbidden`: `{ "error": "Invalid request origin (CSRF protection)." }`
  - `429 Too Many Requests`: `{ "error": "Please wait 60 seconds before requesting another code." }` or `{ "error": "Too many OTP requests. Please try again later." }`
  - `500 Internal Server Error`: `{ "error": "Could not send verification code. Please try again shortly." }`

### `POST /api/auth/verify-otp`
Verifies the submitted 6-digit code, marks the challenge consumed atomically, creates the user record if new, and sets the session cookie.

- **Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "email": "faculty@university.edu",
    "code": "123456"
  }
  ```
- **Success Response (200 OK)**:
  - **Headers**: `Set-Cookie: auth_session=<JWT>; Path=/; HttpOnly; SameSite=Lax; Max-Age=604800`
  - **Body**:
    ```json
    {
      "success": true,
      "user": {
        "id": "c7a8b688-6627-4665-b13c-73ca41cfb2ea",
        "email": "faculty@university.edu",
        "createdAt": "2026-10-14T14:45:00.000Z"
      }
    }
    ```
- **Error Responses**:
  - `400 Bad Request`:
    - Code expired: `{ "error": "Verification code has expired. Please request a new code." }`
    - Invalid code: `{ "error": "Invalid verification code. Please check and try again." }`
    - Exceeded attempts: `{ "error": "Maximum verification attempts exceeded. Please request a new code." }`
    - Already used: `{ "error": "This verification code has already been used. Please request a new code." }`
  - `403 Forbidden`: `{ "error": "Invalid request origin (CSRF protection)." }`

### `GET /api/auth/me`
Returns public information for the currently authenticated user.

- **Headers**: `Cookie: auth_session=<JWT>`
- **Success Response (200 OK)**:
  ```json
  {
    "user": {
      "id": "c7a8b688-6627-4665-b13c-73ca41cfb2ea",
      "email": "faculty@university.edu",
      "createdAt": "2026-10-14T14:45:00.000Z"
    }
  }
  ```
- **Error Responses**:
  - `401 Unauthorized`: `{ "error": "Authentication required." }` or `{ "error": "Invalid or expired session." }`

### `POST /api/auth/logout`
Terminates the session by clearing the HttpOnly cookie.

- **Headers**: `Content-Type: application/json`
- **Success Response (200 OK)**:
  - **Headers**: `Set-Cookie: auth_session=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Max-Age=0`
  - **Body**:
    ```json
    {
      "success": true,
      "message": "Logged out successfully."
    }
    ```
- **Error Responses**:
  - `403 Forbidden`: `{ "error": "Invalid request origin (CSRF protection)." }`

---

## 7. Security Mitigations

1. **Enumeration Resistance**: `POST /api/auth/send-otp` returns identical success messages regardless of whether the email previously existed.
2. **Brute-Force Protection**: Each challenge tracks failed attempts. After 5 incorrect attempts, the challenge is invalidated.
3. **Resend Flooding Defense**: Cooldown of 60 seconds between OTP requests per email and maximum 5 challenges per 15-minute rolling window.
4. **Race Condition Elimination**: Challenge consumption checks `consumedAt: null` in an atomic SQL statement (`UPDATE ... WHERE id = ... AND "consumedAt" IS NULL`), preventing duplicate redemptions under concurrency.
5. **No LocalStorage**: Tokens are never accessible to client JavaScript, preventing theft through XSS.

---

## 8. Explicit Limitations & Boundaries

The current implementation is strictly scoped to email OTP authentication:
- **No Refresh Tokens**: Sessions are bounded to the 7-day JWT expiration. Expired sessions require a quick single-click email OTP sign-in.
- **No Role-Based Access Control (RBAC)**: All authenticated users currently access the single faculty workspace.
- **No Password Authentication**: Passwords and password recovery mechanisms are not implemented.
- **No Third-Party OAuth**: Social sign-in (Google, GitHub, etc.) is not configured.
- **Frontend State Storage**: Course syllabus and teaching planning data remains client-side in browser storage as designed; only authentication and user records are stored in PostgreSQL.
