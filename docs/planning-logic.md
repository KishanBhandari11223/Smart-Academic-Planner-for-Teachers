# Planning logic

## Schedule generation

`generateSchedule(subject, blockedDates)` is deterministic and has no network or AI dependency.

1. Validate the selected date range and configured weekly slots.
2. Traverse unit topic IDs in syllabus order.
3. For each topic, consume `max(1, ceil(estimatedHours))` available teaching slots.
4. Traverse dates in the planning period; use the matching weekday slots in start-time order.
5. Skip every holiday or exam date in the supplied blocked-date set.
6. Create `proposed` sessions only. A caller must separately approve them.

If the planning period ends before all topics fit, the returned session list is shorter than the required effort; the UI flags this as a capacity warning. It does not create sessions outside the chosen period or on blocked dates.

For an already approved subject, generation only considers topics without any approved/completed session. Existing confirmed sessions are retained rather than silently overwritten. This conservative behavior means edits to an already scheduled topic do not automatically reschedule its earlier sessions.

## Adaptive update proposal

- **Completed:** marks the session completed, saves the optional note, and does not create an adjustment.
- **Partially covered:** records the outcome and proposes one open future slot for that topic.
- **Not covered:** records the missed session and proposes one open future slot for that topic.

The suggested slot is the earliest date after the source session within the planning period, on one of the instructor’s selected weekdays, not on a blocked date, and not occupied by an approved session at proposal time. The proposal is not applied automatically.

Applying a proposal adds only its proposed follow-up session. Rejecting it clears the pending proposal and leaves confirmed sessions unchanged. If there is no available slot, the update is still saved and no proposal is produced.

## Assumptions and limits

- Estimated effort is rounded up to whole slots. A 1.5-hour topic therefore reserves two configured slots; the prototype does not split a class slot into fractional sections.
- A configured slot represents one session; room conflicts and cross-subject instructor conflicts are not considered.
- Shared and independent pace modes are stored, but the allocator currently assigns selected sections together.
- Sample institutional dates and sections are illustrative. The demo uses one selected academic event per blocked date.
- This is a transparent heuristic, not an AI schedule optimizer.
