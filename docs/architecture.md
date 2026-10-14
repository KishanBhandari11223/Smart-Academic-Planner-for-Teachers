# Architecture

## Application structure

- `src/app/layout.tsx` — document metadata, font links, and root stylesheet.
- `src/app/page.tsx` — Next.js App Router entry.
- `src/components/PlannerApp.tsx` — client-side application state, navigation, persistence synchronization, and workflow mutations.
- `src/components/AppShell.tsx` — global navigation, search, profile access, responsive shell.
- `src/components/Screens.tsx` — view components and small screen-specific subcomponents.
- `src/components/LoginScreen.tsx` — isolated demo sign-in form.
- `src/components/ui.tsx` — buttons, tags, page headings, progress, dialog, and empty state.
- `src/types/index.ts` — shared models and view names.
- `src/data/seed.ts` — sample subject, sessions, and blocked academic dates.
- `src/lib/planning.ts` — deterministic session allocation, adaptive proposal, date helpers.
- `src/lib/storage.ts` — versioned localStorage persistence and demo sign-in.

## State and boundaries

All domain records are typed. Shared workflow state is held at the app boundary and persisted through a versioned browser key. It is intentionally not a backend: there is no database, server authorization, multi-user sync, or server-side persistence.

The setup form is a local draft until the final step. Subject, syllabus, and schedule changes update the typed state model. Calendar visibility is derived from session status rather than from the existence of a proposal.

GSAP is limited to the main screen container’s entrance transition. It is scoped with `gsap.context()` and reverted when the view changes or the component unmounts. CSS handles basic focus/hover feedback; the stylesheet also respects `prefers-reduced-motion`.

## Extension points

Replace `storage.ts` with authenticated persistence while retaining domain types. Move schedule and proposal functions to a shared server/domain layer when that exists. Keep `PlannerApp` as the orchestration boundary or introduce workflow-specific reducers if the product grows beyond this prototype.
