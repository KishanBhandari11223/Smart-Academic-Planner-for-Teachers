# Known limitations

- Demo authentication is entirely client-side and is not secure.
- All course, calendar, and teacher details are illustrative.
- localStorage is scoped to one browser and may be cleared by the user or browser.
- Syllabus files are not read or uploaded; extraction produces example data after a short simulated loading state.
- No database, server, institutional calendar sync, real user roles, or multi-user collaboration.
- Independent cohort schedules are not generated separately.
- Schedule allocation consumes whole weekly slots and does not optimize room, teacher, or class-capacity conflicts.
- A schedule regeneration does not revise an already approved topic; approved work is preserved.
- Calendar is a list view rather than a full month/week grid.
- Tests & Revision, Analytics, and Team subject tabs are intentionally placeholders.
- No automated end-to-end/browser tests are included.
