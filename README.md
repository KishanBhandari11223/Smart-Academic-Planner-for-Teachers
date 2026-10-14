# Adaptive Teaching Planner

A frontend-only MVP for university instructors to set up a subject, review a syllabus, estimate topic effort, place sessions in real teaching slots, approve a plan, and adapt it after class.

The implementation follows the supplied Stitch screens and editorial-planner reference. It is a runnable Next.js application—not a hosted service and not a production SaaS.

## Implemented

- Demo sign-in with inline validation and sign-out.
- Dashboard, subject list, and subject workspace with the supplied subject tabs.
- Three-step subject setup for course details, sections, pace mode, and weekly teaching slots.
- Simulated syllabus extraction with file selection or manual text, plus editable units and topics.
- Pacing matrix for topic complexity, importance, and estimated teaching effort.
- Deterministic schedule generation against selected weekdays and planning dates.
- Separate schedule-map and teaching-plan-review stages.
- Explicit plan approval before proposed sessions enter the global Calendar.
- Lightweight post-class update for completed, partially covered, or not-covered outcomes.
- A proposed follow-up session after partial or missed coverage, with apply/keep-original choices.
- Browser-local persistence for the demo account and prototype data.
- Responsive layouts, reduced-motion support, and a restrained GSAP content entrance transition.

The subject’s Tests & Revision, Analytics, and Team tabs have small explanatory placeholders; they are not expanded into invented workflows.

## Technology

- Next.js 16 App Router
- React 19
- TypeScript with strict checking
- GSAP for one scoped, reversible screen transition
- Lucide icons
- Custom CSS design tokens and responsive styles, chosen over a component library or utility framework to stay close to the Stitch references

## Requirements

- Node.js 20.9 or newer (Node 24 is also supported)
- npm

## Install and run

```bash
npm install
npm run dev
```

Open the local URL printed by Next.js (normally `http://localhost:3000`).

## Demo access

- **Email:** `example@123`
- **Password:** `123`

This is deliberately frontend-only demo authentication. The credentials and check are visible in browser-delivered code. Do not use this mechanism to protect private information, authorize server actions, or represent a real user account.

## Data and persistence

The sample Data Mining & Warehousing course, teaching sessions, class updates, and demo sign-in are stored in browser `localStorage`. Data is per browser and is not synchronized, backed up, or stored on a server. Use **Settings → Reset demo data** to restore the original sample.

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
npm start          # Serve a completed production build
```

## Verification performed for this bundle

- `npm run lint` — passed.
- `npm run typecheck` — passed.
- `npm audit` and `npm audit --omit=dev` — zero reported vulnerabilities.
- `npm run build` — passed; the App Router entry was statically prerendered.
- Production-server HTTP smoke test — returned the app title.
- Direct schedule checks — syllabus order, holiday skipping, exhausted-period behavior, and next-open-slot proposal selection passed.

Browser-driven interaction tests were not available, so the full multi-screen click-through was not claimed as end-to-end tested.

## Project structure

```text
src/
  app/              App Router entry, layout, and design tokens
  components/       App shell, workflow screens, shared UI
  data/             Seed course and illustrative academic dates
  lib/              Local persistence and planning algorithms
  types/            Shared domain and screen types
docs/
  design-audit.md
  user-flows.md
  architecture.md
  planning-logic.md
  implementation-plan.md
  known-limitations.md
  reference/         Original briefs and Stitch export archive
```

## Known limitations

- No backend, database, real authentication, file upload, or document parsing.
- Institutional holidays, exam windows, teacher, and course records are illustrative.
- The heuristic does not optimize multiple sections independently, room capacity, instructor conflicts across subjects, or partial-hour slot splitting.
- Calendar shows approved/completed sessions, not pending proposals.
- Secondary subject tabs are intentionally minimal.
- No automated browser/E2E suite is included.

## Next steps for a backend

Replace the local demo store with authenticated server actions/API endpoints; persist subjects, sections, outcomes, and approved sessions in a database; source academic events from an institution calendar; parse uploaded syllabus files on the server; add authorization and audit history; and test scheduling rules against a dedicated unit and browser test suite.

## Design source

The original user-supplied Stitch archive and both briefs are included under `docs/reference/`. The archive has ten HTML screen exports; three of its preview images (Home, My Subjects, and Teacher Profile) are 28-byte placeholders, so their exported HTML is the available reference for those screens.
