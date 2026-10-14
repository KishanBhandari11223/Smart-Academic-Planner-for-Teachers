# Design audit

## Source reviewed

- `reference/stitch-exports.zip` — ten exported screen directories, each with `code.html`; seven full-size PNG previews and three 28-byte placeholder PNGs.
- `reference/frontend-mvp-brief.txt` — product scope, user journeys, domain behavior, and implementation requirements.
- `reference/ui-polish-brief.txt` — brand and restrained-motion direction.
- `reference` screens were compared by exported headings, visible labels, navigation, and the available PNG previews.

The design language combines an editorial serif (Newsreader) with Plus Jakarta Sans operational text, paper-tinted surfaces, fine borders, minimal elevation, and sage/amber status cues. The exports vary in sidebar width and use some cool lavender-tinted surfaces; the application normalizes these into one warm, responsive design system without changing the hierarchy or workflow.

## Screen inventory

| Export | Purpose and entry | Main exits / implemented destination |
|---|---|---|
| `teacher_home_dashboard` | Faculty landing view and quick actions | Subject workspace, Calendar, class update, add subject |
| `my_subjects` | Browse courses and progress | Subject overview or subject setup |
| `teacher_profile` | Faculty identity and teaching overview | Profile view and sign-out |
| `subject_overview_data_mining` | Selected subject overview and subject tabs | Teaching plan, syllabus, progress, class update |
| `create_subject_wizard` | Course details, section/pace setup, teaching schedule | Syllabus setup or cancel |
| `syllabus_upload_ai_extraction` | Select file/paste text, inspect editable syllabus | Add/edit units and topics, then pacing |
| `pacing_matrix_data_mining` | Adjust complexity, importance, and effort by topic | Schedule map |
| `schedule_map` | Fit syllabus work into dated weekly teaching slots | Regenerate, revise pacing, review plan |
| `teaching_plan_review` | Review proposed dated sessions before approval | Approve to Calendar or return to schedule map |
| `post_class_update_data_mining` | Record classroom coverage and optional note | Save only, or inspect an adjustment proposal |
| `plan_adjustment_preview` | Compare current outcome and suggested follow-up | Apply the one-slot proposal or retain approved plan |

There are ten distinct exports; all ten represent different views rather than duplicate screenshots. The three tiny PNG placeholders are not valid visual previews, but their HTML screen exports remain in the reference archive.

## Shared patterns and reconciliations

- Global navigation is Home, My Subjects, Calendar, Notifications, and Settings. Profile is reached through the profile chip/avatar.
- Subject navigation stays within one subject workspace and presents Overview, Teaching Plan, Syllabus, Progress, Tests & Revision, Analytics, and Team.
- The create-subject export is a three-step wizard; its pacing and schedule stages then continue into separate planning screens.
- Pacing, schedule allocation, and plan approval remain separate stages.
- Several supplied screens use `#fbf8fc` and lavender-gray token values while the design document’s narrative favors warm paper neutrals. The app uses warm paper as the canvas and keeps sage/amber semantics.
- Header term labels and subject navigation differ slightly between exports. The app uses a consistent 2026–27 label and selected-subject breadcrumbs.
- Login, global Calendar, notifications, and settings were not supplied as complete visual screens. Login was added as a simple demo entry state; Calendar and supporting screens use the shared visual system.

## Genuine workflow gaps filled

- Login is a new demo-only entry page because the required user flow includes authentication but no login screen exists in the supplied archive.
- Review is protected until a schedule proposal exists, and approval is an explicit action.
- Proposed sessions are visually marked and excluded from Calendar until approved.
- Empty/no-slot states and a sample-extraction notice explain prototype limits.
- A completed class saves without an adjustment preview; partial or not-covered outcomes can produce an optional, reviewable suggestion.
- Unrepresented subject tabs use explanatory placeholders instead of invented reporting features.
