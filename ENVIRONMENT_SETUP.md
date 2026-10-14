# Environment Setup & Deployment Guide: Adaptive Teaching Planner (Next.js)

This document provides a comprehensive, step-by-step walkthrough of setting up and running the **Adaptive Teaching Planner** project in an **Ubuntu WSL (Windows Subsystem for Linux)** environment from a Windows ZIP archive exported from Replit.

---

## 1. Project Overview

- **Project Name:** `adaptive-teaching-planner`
- **Framework:** Next.js 16.4.0 (App Router, Turbopack)
- **UI Library:** React 19.3.0
- **Language:** TypeScript 5.8.3
- **Styling & Motion:** Custom design tokens & GSAP 3.15.0
- **State & Storage:** Frontend-only state persisted in browser `localStorage`
- **Demo Credentials:**
  - Email: `example@123`
  - Password: `123`

---

## 2. WSL & Windows Filesystem Architecture

When running Ubuntu inside WSL on Windows:
- Windows drives are mounted under `/mnt/<drive-letter>/` (e.g., `C:\` is `/mnt/c/`).
- The project archive was located on Windows at:
  - `C:\Users\acer\Downloads\Adaptive-Teaching-Planner-NextJS.zip` (`/mnt/c/Users/acer/Downloads/Adaptive-Teaching-Planner-NextJS.zip`)
  - `C:\Users\acer\OneDrive\Adaptive-Teaching-Planner-NextJS.zip` (`/mnt/c/Users/acer/OneDrive/Adaptive-Teaching-Planner-NextJS.zip`)
- **Important Best Practice:** Never run Node.js / `npm install` inside `/mnt/c/` directly. The Windows NTFS filesystem translated through WSL 9P/drvfs causes heavy file I/O bottlenecks and permission conflicts with `node_modules`. Extracting into the native Linux filesystem (`/home/kartikey-xq/`) provides 10x-20x faster build times and smooth file watching.

---

## 3. Step-by-Step Setup & Command Reference

### Step 1: Verify the Archive Location in WSL
```bash
ls -la /mnt/c/Users/acer/Downloads/Adaptive-Teaching-Planner-NextJS.zip
```
- **Why it was needed:** To confirm that the Windows C: drive mount is active and that WSL has read permissions on the downloaded ZIP file.
- **Result:** File verified at `/mnt/c/Users/acer/Downloads/Adaptive-Teaching-Planner-NextJS.zip` (~3.1 MB).

---

### Step 2: Check Runtime Prerequisites (Node.js & npm)
```bash
node -v
npm -v
```
- **Why it was needed:** Next.js 16 requires Node.js version 20.9.0 or higher.
- **Result:**
  - Node.js: `v22.23.3` (satisfies requirement)
  - npm: `12.2.0`

---

### Step 3: Extract the ZIP Archive
Since `unzip` was not installed in the base Ubuntu image and installing packages via `apt` requires root credentials, Python 3's built-in `zipfile` module was used:

```bash
python3 -c "
import zipfile, os

zip_path = '/mnt/c/Users/acer/Downloads/Adaptive-Teaching-Planner-NextJS.zip'
dest_dir = '/home/kartikey-xq/Adaptive-Teaching-Planner-NextJS'

os.makedirs(dest_dir, exist_ok=True)
with zipfile.ZipFile(zip_path, 'r') as zip_ref:
    zip_ref.extractall(dest_dir)

print('Extracted successfully!')
"
```
- **Why it was needed:**
  1. Creates the target directory `/home/kartikey-xq/Adaptive-Teaching-Planner-NextJS` inside the fast native ext4 filesystem.
  2. Unpacks the complete repository (source code, configuration files, and documentation) without needing extra utility installations.

---

### Step 4: Environment Configuration & Secret Verification
```bash
cd /home/kartikey-xq/Adaptive-Teaching-Planner-NextJS
ls -la .env* 2>/dev/null || echo "No .env files"
grep -rn "process.env" src/ 2>/dev/null || echo "No process.env references"
```
- **Why it was needed:** Check whether any API keys, database connections, or secret environment variables were needed.
- **Result:** Confirmed that the application is frontend-only, uses mock seed data and browser `localStorage`, and requires no `.env` file to function.

---

### Step 5: Resolving the Replit Proxy Issue & Installing Dependencies
When running a standard `npm install`, npm failed with:
```
npm error code EALLOWREMOTE
npm error Fetching packages of type "remote" have been disabled
npm error Refusing to fetch "yocto-queue@http://package-firewall.replit.internal/npm/yocto-queue/-/yocto-queue-0.1.0.tgz"
```
- **Why this occurred:** The project was exported from Replit. Replit routes npm downloads through an internal caching proxy (`http://package-firewall.replit.internal/`). The exported `package-lock.json` contained hardcoded references to this internal Replit domain, which cannot be resolved or accessed outside of Replit's infrastructure.
- **The Solution:** Remove the Replit lockfile and reinstall so npm resolves all packages from the official public npm registry (`https://registry.npmjs.org`):

```bash
cd /home/kartikey-xq/Adaptive-Teaching-Planner-NextJS
rm package-lock.json
npm install
```
- **Result:** Clean installation of 124 packages in ~49s with 0 security vulnerabilities. A fresh, standard `package-lock.json` was generated.

---

### Step 6: Code Quality & Production Build Verification
Before launching the server, we verified TypeScript typing, ESLint checks, and production build readiness:

1. **TypeScript Typecheck:**
   ```bash
   npm run typecheck
   ```
   - *Command run:* `tsc --noEmit`
   - *Why:* Ensures strict TypeScript rules pass without any missing type declarations.
   - *Result:* Passed with 0 errors.

2. **Linting:**
   ```bash
   npm run lint
   ```
   - *Command run:* `eslint .`
   - *Why:* Ensures style consistency and React hooks compliance.
   - *Result:* Passed with 0 errors.

3. **Production Build Test:**
   ```bash
   npm run build
   ```
   - *Command run:* `next build`
   - *Why:* Tests Turbopack compilation and App Router static generation.
   - *Result:* Successfully compiled in 2.6s and generated static routes (`/` and `/_not-found`).

---

### Step 7: Checking Port Availability & Starting the Server
1. **Check Port 3000:**
   ```bash
   ss -tulpn | grep 3000
   ```
   - *Why:* Ensures no existing process is already using port 3000.
   - *Result:* Port 3000 was free.

2. **Start the Next.js Development Server:**
   ```bash
   cd /home/kartikey-xq/Adaptive-Teaching-Planner-NextJS
   npm run dev
   ```
   - *Command run:* `next dev` (with Turbopack enabled)
   - *Result:* Server initialized in 388ms:
     - Local: `http://localhost:3000`
     - Network: `http://10.255.255.254:3000`

---

### Step 8: Verifying Localhost Response
```bash
curl -I http://localhost:3000
curl -s http://localhost:3000 | grep -o "<title>.*</title>"
```
- **Why it was needed:** To verify HTTP connectivity and ensure the page is rendering properly without server-side rendering errors.
- **Result:**
  - Status: `HTTP/1.1 200 OK`
  - HTML Title: `<title>Teaching Planner — Adaptive Curriculum</title>`

---

## 4. How to Access the App

### From Windows Browser (Chrome / Edge / Firefox)
WSL automatically binds ports on `localhost` to your Windows host machine.
Simply open your browser on Windows and navigate to:
```
http://localhost:3000
```

### Sign-in Credentials:
- **Email:** `example@123`
- **Password:** `123`

---

## 5. Summary Cheat Sheet for Future Runs

| Action | Command | Directory |
|---|---|---|
| Go to project folder | `cd ~/Adaptive-Teaching-Planner-NextJS` | `~` |
| Start Dev Server | `npm run dev` | `~/Adaptive-Teaching-Planner-NextJS` |
| Check Type Errors | `npm run typecheck` | `~/Adaptive-Teaching-Planner-NextJS` |
| Run Linter | `npm run lint` | `~/Adaptive-Teaching-Planner-NextJS` |
| Build for Production | `npm run build` | `~/Adaptive-Teaching-Planner-NextJS` |
| Run Production Server | `npm start` | `~/Adaptive-Teaching-Planner-NextJS` |
| Reset Demo Data | In the app: Settings → Reset demo data | Browser |
