# User flows

```text
Demo sign-in
  └─ Home
      ├─ My Subjects → Subject Overview
      │                    ├─ Teaching Plan → Schedule Map → Plan Review → Approve → Calendar
      │                    ├─ Syllabus → Pacing Matrix → Schedule Map
      │                    └─ Approved session → Post-Class Update
      │                                              ├─ Completed → Save → Home
      │                                              └─ Partial / Not covered → Adjustment Preview
      │                                                                      ├─ Apply → Calendar
      │                                                                      └─ Keep original → Subject
      ├─ Add Subject → Details → Sections & Pace → Teaching Schedule
      │                             → Syllabus Upload & Review → Pacing Matrix
      │                             → Schedule Map → Plan Review → Approve
      └─ Calendar / Notifications / Settings / Profile
```

## Screen behavior

- **Home:** summarizes sample progress, approved upcoming sessions, and subjects. The subject search filters the subject list.
- **Subject setup:** stores course information, sections, pace mode, plan dates, and weekly slots in browser-local state. Saving sends the teacher to syllabus setup.
- **Syllabus:** choosing a file displays its filename; text entry enables a simulated extraction state. Extracted unit/topic examples can be renamed, edited, added, or removed.
- **Pacing:** edits topic complexity, importance, and estimated hours.
- **Schedule Map:** regenerates a proposed sequence using remaining topics, selected dates, weekly slots, and blocked academic dates.
- **Plan Review:** lists proposed and already approved sessions separately. Approving changes only pending sessions to approved and marks the subject plan approved.
- **Calendar:** includes only approved or completed sessions. Holidays and exam windows are a distinct list, not teaching sessions.
- **Post-class update:** offers Completed, Partially covered, or Not covered; a note is optional and revealed on demand.
- **Adjustment Preview:** one proposed follow-up appears for a partial/missed session. Apply adds that follow-up as an approved event; Keep original clears the proposal without changing other sessions.
- **Refresh:** demo sign-in and application data are restored from localStorage. The current screen resets to Home on a full refresh.
