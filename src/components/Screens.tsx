"use client";

import { useState, type ChangeEvent, type FormEvent } from "react";
import {
  BookOpen,
  CalendarDays,
  Check,
  ChevronRight,
  FileUp,
  MoreHorizontal,
  Plus,
  RotateCcw,
  Sparkles,
  Upload,
} from "lucide-react";
import { Button, EmptyState, PageHeading, Progress, Tag } from "@/components/ui";
import { formatDate } from "@/lib/planning";
import type {
  AcademicCalendarEvent,
  AppData,
  Complexity,
  Importance,
  OutcomeKind,
  ScheduleProposal,
  Subject,
  SyllabusTopic,
  TeachingSession,
  TeachingSlot,
  View,
} from "@/types";

export function HomeScreen({
  data,
  onNavigate,
  onSubject,
  onUpdate,
  search,
}: {
  data: AppData;
  onNavigate: (view: View) => void;
  onSubject: (subjectId: string) => void;
  onUpdate: (sessionId: string) => void;
  search: string;
}) {
  const today = new Date();
  const todayKey = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
  const upcoming = data.sessions
    .filter((session) => session.status === "approved" && session.date >= todayKey)
    .sort((a, b) => `${a.date}${a.startTime}`.localeCompare(`${b.date}${b.startTime}`))
    .slice(0, 4);
  const filtered = data.subjects.filter((subject) => subject.name.toLowerCase().includes(search.toLowerCase()) || subject.code.toLowerCase().includes(search.toLowerCase()));
  const subject = data.subjects[0];
  const completedSessions = data.sessions.filter((session) => session.status === "completed").length;
  const totalSessions = data.sessions.length;
  const progress = totalSessions ? Math.round((completedSessions / totalSessions) * 100) : 0;

  return (
    <div className="screen-stack">
      <PageHeading
        eyebrow={new Date().toLocaleDateString("en-IN", { weekday: "long", month: "long", day: "numeric" })}
        title="Good morning, Kartikey."
        description="Your teaching week, at a glance."
        action={<Button onClick={() => onNavigate("create")} variant="primary" icon={<Plus size={16} />}>Add subject</Button>}
      />
      <div className="overview-grid">
        <section className="panel feature-panel">
          <div className="panel-heading">
            <div><p className="eyebrow">Curriculum realization</p><h2>Term progress</h2></div>
            <Tag tone="sage">On track</Tag>
          </div>
          <div className="progress-reading"><strong>{progress}%</strong><span>of planned sessions completed</span></div>
          <Progress value={progress} />
          <div className="metric-row">
            <div><span>Subjects</span><strong>{data.subjects.length}</strong></div>
            <div><span>Sessions taught</span><strong>{completedSessions}<small> / {totalSessions}</small></strong></div>
            <div><span>Sections</span><strong>{data.subjects.reduce((sum, item) => sum + item.sections.length, 0)}</strong></div>
          </div>
        </section>
        <section className="panel next-panel">
          <div className="panel-heading"><div><p className="eyebrow">Next up</p><h2>Teaching this week</h2></div><CalendarDays size={18} className="muted-icon" /></div>
          {upcoming.length ? (
            <div className="session-list">
              {upcoming.slice(0, 3).map((session) => {
                const owningSubject = data.subjects.find((item) => item.id === session.subjectId);
                const topic = owningSubject?.topics.find((item) => item.id === session.topicId);
                return (
                  <div className="session-row" key={session.id}>
                    <div className="date-block"><strong>{new Date(`${session.date}T12:00:00`).getDate()}</strong><span>{new Date(`${session.date}T12:00:00`).toLocaleDateString("en-IN", { month: "short" })}</span></div>
                    <button type="button" className="session-summary" onClick={() => owningSubject && onSubject(owningSubject.id)}>
                      <strong>{topic?.title ?? "Teaching session"}</strong>
                      <span>{owningSubject?.code ?? "Subject"} · {session.startTime} · {session.sectionIds.join(", ")}</span>
                    </button>
                    <button type="button" className="text-action" onClick={() => onUpdate(session.id)} aria-label={`Record outcome for ${topic?.title ?? "session"}`}>Update</button>
                  </div>
                );
              })}
            </div>
          ) : <EmptyState title="No upcoming classes" description="Approved teaching sessions will appear here." action={<Button onClick={() => onNavigate("subjects")}>View subjects</Button>} />}
          <button className="inline-link" type="button" onClick={() => onNavigate("calendar")}>Open calendar <ChevronRight size={15} /></button>
        </section>
      </div>
      <div className="section-title-row"><div><p className="eyebrow">Instructional workspace</p><h2>Your subjects</h2></div><button className="inline-link" type="button" onClick={() => onNavigate("subjects")}>View all <ChevronRight size={15} /></button></div>
      {filtered.length === 0 ? (
        <EmptyState title="No matching subjects" description="Try another search or add a new subject." action={<Button onClick={() => onNavigate("create")} variant="primary">Add subject</Button>} />
      ) : (
        <div className="subject-grid">
          {filtered.slice(0, 3).map((item, index) => {
            const sessions = data.sessions.filter((session) => session.subjectId === item.id);
            const completed = sessions.filter((session) => session.status === "completed").length;
            const pct = sessions.length ? Math.round((completed / sessions.length) * 100) : 0;
            return (
              <button type="button" className={`subject-card subject-card-${index % 3}`} key={item.id} onClick={() => onSubject(item.id)}>
                <div className="subject-card-top"><span className="course-code">{item.code}</span><MoreHorizontal size={18} /></div>
                <h3>{item.name}</h3>
                <p>{item.program} · {item.sections.join(" & ")}</p>
                <div className="subject-card-progress"><Progress value={pct} /><span>{pct}%</span></div>
                <div className="subject-card-foot"><Tag tone={item.status === "approved" ? "sage" : "amber"}>{item.status === "approved" ? "Plan approved" : "In progress"}</Tag><span>{item.topics.length} topics</span></div>
              </button>
            );
          })}
        </div>
      )}
      {subject && (
        <section className="editorial-callout">
          <div className="callout-icon"><Sparkles size={18} /></div>
          <div><p className="eyebrow">A note on your plan</p><h3>Make space for the unexpected.</h3><p>Classroom updates can suggest small schedule changes. Nothing moves until you review and apply it.</p></div>
          <Button onClick={() => onSubject(subject.id)} variant="quiet" arrow>Open subject</Button>
        </section>
      )}
    </div>
  );
}

export function SubjectsScreen({
  subjects,
  sessions,
  search,
  onCreate,
  onSubject,
}: {
  subjects: Subject[];
  sessions: TeachingSession[];
  search: string;
  onCreate: () => void;
  onSubject: (id: string) => void;
}) {
  const filtered = subjects.filter((subject) => `${subject.name} ${subject.code} ${subject.program}`.toLowerCase().includes(search.toLowerCase()));
  return (
    <div className="screen-stack">
      <PageHeading eyebrow="Instructional workspace" title="My subjects" description="Course plans, syllabus progress, and teaching pace in one place." action={<Button variant="primary" onClick={onCreate} icon={<Plus size={16} />}>Add subject</Button>} />
      {filtered.length === 0 ? <EmptyState title="No subjects found" description="Create a subject to start planning a term." action={<Button onClick={onCreate} variant="primary">Add subject</Button>} /> : (
        <div className="subject-list">
          {filtered.map((subject) => {
            const subjectSessions = sessions.filter((session) => session.subjectId === subject.id);
            const taught = subjectSessions.filter((session) => session.status === "completed").length;
            return (
              <button className="subject-list-card" type="button" key={subject.id} onClick={() => onSubject(subject.id)}>
                <span className="subject-monogram">{subject.code.slice(0, 2)}</span>
                <span className="subject-list-main"><span className="subject-card-top"><span className="course-code">{subject.code}</span><Tag tone={subject.status === "approved" ? "sage" : "amber"}>{subject.status}</Tag></span><strong>{subject.name}</strong><small>{subject.program} · {subject.sections.join(", ")}</small></span>
                <span className="subject-list-stat"><span>Teaching progress</span><strong>{taught} <small>/ {subjectSessions.length} sessions</small></strong></span>
                <ChevronRight size={18} className="muted-icon" />
              </button>
            );
          })}
        </div>
      )}
      <p className="mock-note">Example course data is stored in this browser only. It is not connected to an institution.</p>
    </div>
  );
}

const subjectTabs = ["Overview", "Teaching Plan", "Syllabus", "Progress", "Tests & Revision", "Analytics", "Team"] as const;
type SubjectTab = (typeof subjectTabs)[number];

export function SubjectWorkspace({
  subject,
  sessions,
  outcomes,
  onNavigate,
  onUpdate,
}: {
  subject: Subject;
  sessions: TeachingSession[];
  outcomes: AppData["outcomes"];
  onNavigate: (view: View) => void;
  onUpdate: (sessionId: string) => void;
}) {
  const [tab, setTab] = useState<SubjectTab>("Overview");
  const subjectSessions = sessions.filter((session) => session.subjectId === subject.id).sort((a, b) => `${a.date}${a.startTime}`.localeCompare(`${b.date}${b.startTime}`));
  const completedTopics = new Set(subjectSessions.filter((session) => session.status === "completed").map((session) => session.topicId));
  const progress = subject.topics.length ? Math.round((completedTopics.size / subject.topics.length) * 100) : 0;

  return (
    <div className="screen-stack">
      <div className="breadcrumbs"><button type="button" onClick={() => onNavigate("subjects")}>My Subjects</button><ChevronRight size={13} /><span>{subject.name}</span></div>
      <PageHeading eyebrow={`${subject.code} · ${subject.program}`} title={subject.name} description={`${subject.academicYear} academic year · ${subject.sections.join(" & ")} · ${subject.paceMode === "shared" ? "Shared pace" : "Independent pace"}`} action={<Tag tone={subject.status === "approved" ? "sage" : "amber"}>{subject.status === "approved" ? "Plan approved" : "Planning in progress"}</Tag>} />
      <div className="subject-tabs" role="tablist" aria-label="Subject workspace tabs">
        {subjectTabs.map((name) => <button key={name} type="button" role="tab" aria-selected={tab === name} className={tab === name ? "tab-active" : ""} onClick={() => setTab(name)}>{name}</button>)}
      </div>
      {tab === "Overview" && (
        <div className="overview-grid subject-overview">
          <section className="panel">
            <div className="panel-heading"><div><p className="eyebrow">Next class</p><h2>{subject.topics.find((topic) => topic.id === subjectSessions.find((session) => session.status === "approved")?.topicId)?.title ?? "Your next teaching session"}</h2></div><Tag tone="sage">Planned</Tag></div>
            <p className="quiet-copy">{subjectSessions.find((session) => session.status === "approved") ? `${formatDate(subjectSessions.find((session) => session.status === "approved")?.date ?? "")} · ${subjectSessions.find((session) => session.status === "approved")?.startTime}` : "No approved session is scheduled."}</p>
            <div className="action-row"><Button variant="primary" onClick={() => onNavigate("schedule")}>View teaching plan</Button>{subjectSessions.some((session) => session.status === "approved") && <Button onClick={() => onUpdate(subjectSessions.find((session) => session.status === "approved")?.id ?? "")}>Record class outcome</Button>}</div>
          </section>
          <section className="panel pace-panel"><div className="panel-heading"><div><p className="eyebrow">Curriculum realization</p><h2>Syllabus progress</h2></div><strong className="big-number">{progress}%</strong></div><Progress value={progress} /><div className="metric-row"><div><span>Topics covered</span><strong>{completedTopics.size}<small> / {subject.topics.length}</small></strong></div><div><span>Sessions planned</span><strong>{subjectSessions.length}</strong></div><div><span>Class updates</span><strong>{outcomes.filter((outcome) => subjectSessions.some((session) => session.id === outcome.sessionId)).length}</strong></div></div></section>
          <section className="panel upcoming-panel"><div className="panel-heading"><div><p className="eyebrow">Term manifest</p><h2>Upcoming classes</h2></div><button className="inline-link" type="button" onClick={() => onNavigate("schedule")}>Full plan <ChevronRight size={15} /></button></div><SessionRows subject={subject} sessions={subjectSessions.slice(0, 4)} onUpdate={onUpdate} /></section>
          <section className="panel info-panel"><p className="eyebrow">Subject information</p><dl><div><dt>Course</dt><dd>{subject.program}</dd></div><div><dt>Sections</dt><dd>{subject.sections.join(", ")}</dd></div><div><dt>Teaching days</dt><dd>{subject.slots.map((slot) => ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"][slot.dayOfWeek]).join(", ") || "Not set"}</dd></div><div><dt>Syllabus state</dt><dd>{subject.topics.length ? "Reviewed" : "Awaiting syllabus"}</dd></div></dl><Button className="full-width" onClick={() => onNavigate("syllabus")}>View syllabus</Button></section>
        </div>
      )}
      {tab === "Teaching Plan" && <section className="panel"><div className="panel-heading"><div><p className="eyebrow">Session sequence</p><h2>Teaching plan</h2></div><Button onClick={() => onNavigate("schedule")} arrow>Open schedule map</Button></div><SessionRows subject={subject} sessions={subjectSessions} onUpdate={onUpdate} /></section>}
      {tab === "Syllabus" && <SyllabusSummary subject={subject} onEdit={() => onNavigate("syllabus")} />}
      {tab === "Progress" && <section className="panel"><div className="panel-heading"><div><p className="eyebrow">Classroom record</p><h2>Progress by topic</h2></div><Tag tone="sage">{completedTopics.size} topics covered</Tag></div><div className="topic-progress-list">{subject.topics.map((topic) => <div className="topic-progress-row" key={topic.id}><span><strong>{topic.title}</strong><small>{subject.units.find((unit) => unit.id === topic.unitId)?.title}</small></span><Tag tone={completedTopics.has(topic.id) ? "sage" : "neutral"}>{completedTopics.has(topic.id) ? "Covered" : "Upcoming"}</Tag></div>)}</div></section>}
      {!["Overview", "Teaching Plan", "Syllabus", "Progress"].includes(tab) && <EmptyState title={`${tab} workspace`} description="This lightweight prototype keeps this area intentionally simple. Core planning, syllabus, progress, and daily teaching workflows are ready to use." />}
    </div>
  );
}

function SessionRows({ subject, sessions, onUpdate }: { subject: Subject; sessions: TeachingSession[]; onUpdate: (sessionId: string) => void }) {
  if (sessions.length === 0) return <EmptyState title="No sessions yet" description="Generate a schedule from your teaching slots to see sessions here." />;
  return <div className="session-list">{sessions.map((session) => {
    const topic = subject.topics.find((item) => item.id === session.topicId);
    return <div className="session-row" key={session.id}><div className="date-block"><strong>{new Date(`${session.date}T12:00:00`).getDate()}</strong><span>{new Date(`${session.date}T12:00:00`).toLocaleDateString("en-IN", { month: "short" })}</span></div><div className="session-summary"><strong>{topic?.title ?? "Teaching session"}</strong><span>{formatDate(session.date)} · {session.startTime}–{session.endTime} · {session.sectionIds.join(", ")}</span></div>{session.status === "approved" ? <button className="text-action" type="button" onClick={() => onUpdate(session.id)}>Update</button> : <Tag tone={session.status === "completed" ? "sage" : "neutral"}>{session.status}</Tag>}</div>;
  })}</div>;
}

function SyllabusSummary({ subject, onEdit }: { subject: Subject; onEdit: () => void }) {
  return <section className="panel"><div className="panel-heading"><div><p className="eyebrow">Reviewed curriculum</p><h2>Syllabus structure</h2></div><Button onClick={onEdit}>Edit syllabus</Button></div>{subject.units.length === 0 ? <EmptyState title="Syllabus not added" description="Upload a syllabus or enter its contents to begin pacing." action={<Button onClick={onEdit} variant="primary">Add syllabus</Button>} /> : <div className="unit-list">{subject.units.map((unit, index) => <details className="unit-card" key={unit.id} open={index === 0}><summary><span className="unit-number">{String(index + 1).padStart(2, "0")}</span><span><strong>{unit.title}</strong><small>{unit.description}</small></span><Tag>{unit.topicIds.length} topics</Tag></summary><div className="unit-topics">{subject.topics.filter((topic) => topic.unitId === unit.id).map((topic) => <div key={topic.id}><span>{topic.title}</span><small>{topic.estimatedHours} hr</small></div>)}</div></details>)}</div>}</section>;
}

export interface SubjectDraft {
  name: string;
  code: string;
  program: string;
  semester: string;
  academicYear: string;
  sections: string;
  paceMode: Subject["paceMode"];
  slots: TeachingSlot[];
  planStart: string;
  planEnd: string;
  objectives: string;
}

const dayNames = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];

export function CreateSubjectScreen({
  draft,
  onChange,
  onFinish,
  onCancel,
}: {
  draft: SubjectDraft;
  onChange: (draft: SubjectDraft) => void;
  onFinish: () => void;
  onCancel: () => void;
}) {
  const [step, setStep] = useState(0);
  const [error, setError] = useState("");
  function change<K extends keyof SubjectDraft>(key: K, value: SubjectDraft[K]) {
    onChange({ ...draft, [key]: value });
  }
  function continueStep(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (step === 0 && (!draft.name.trim() || !draft.code.trim())) {
      setError("Add a subject name and course code to continue.");
      return;
    }
    setError("");
    if (step < 2) setStep(step + 1);
    else onFinish();
  }
  function toggleDay(day: number) {
    const existing = draft.slots.find((slot) => slot.dayOfWeek === day);
    change("slots", existing ? draft.slots.filter((slot) => slot.dayOfWeek !== day) : [...draft.slots, { dayOfWeek: day, startTime: "10:00", durationMinutes: 60, room: "Room 402" }].sort((a, b) => a.dayOfWeek - b.dayOfWeek));
  }
  function patchSlot(day: number, patch: Partial<TeachingSlot>) {
    change("slots", draft.slots.map((slot) => slot.dayOfWeek === day ? { ...slot, ...patch } : slot));
  }
  return (
    <div className="screen-stack">
      <div className="breadcrumbs"><button type="button" onClick={onCancel}>My Subjects</button><ChevronRight size={13} /><span>Add subject</span></div>
      <PageHeading eyebrow="Curriculum ingestion · Adaptive setup" title="Add a subject" description="Set up your course, sections, and teaching availability. Then add a syllabus to begin planning." action={<button type="button" className="quiet-action" onClick={onCancel}>Cancel & exit</button>} />
      <div className="wizard-steps" aria-label={`Step ${step + 1} of 3`}>
        {["Subject details", "Sections & pace", "Teaching schedule"].map((label, index) => <button type="button" className={`wizard-step ${step === index ? "wizard-active" : ""} ${step > index ? "wizard-done" : ""}`} key={label} onClick={() => index < step && setStep(index)}><span>{step > index ? <Check size={13} /> : index + 1}</span>{label}</button>)}
      </div>
      <form className="panel wizard-panel" onSubmit={continueStep}>
        <div className="panel-heading"><div><p className="eyebrow">Step {step + 1} of 3</p><h2>{["What do you teach?", "Who are you teaching?", "When do you meet?"][step]}</h2></div><BookOpen size={20} className="muted-icon" /></div>
        {step === 0 && <div className="form-grid">
          <label className="field field-wide"><span>Subject / paper name <b>Required</b></span><input value={draft.name} onChange={(event) => change("name", event.target.value)} placeholder="e.g. Distributed Systems" required /></label>
          <label className="field"><span>Course code</span><input value={draft.code} onChange={(event) => change("code", event.target.value.toUpperCase())} placeholder="e.g. COMP-5301" required /></label>
          <label className="field"><span>Programme</span><input value={draft.program} onChange={(event) => change("program", event.target.value)} placeholder="e.g. MCA · Semester 3" /></label>
          <label className="field"><span>Academic year</span><input value={draft.academicYear} onChange={(event) => change("academicYear", event.target.value)} /></label>
          <label className="field field-wide"><span>Instructional objectives <small>Optional</small></span><textarea value={draft.objectives} onChange={(event) => change("objectives", event.target.value)} placeholder="What should students be able to do by the end of this course?" rows={3} /></label>
        </div>}
        {step === 1 && <div className="form-grid">
          <label className="field field-wide"><span>Sections / cohorts <small>Separate with commas</small></span><input value={draft.sections} onChange={(event) => change("sections", event.target.value)} placeholder="MCA 3A, MCA 3B" /></label>
          <fieldset className="field field-wide">
            <legend>How should pacing work across sections?</legend>
            <div className="choice-grid">
              <label className={`choice-card ${draft.paceMode === "shared" ? "choice-selected" : ""}`}>
                <div className="choice-header">
                  <strong>Shared initial pace</strong>
                  <input
                    type="radio"
                    name="pace"
                    checked={draft.paceMode === "shared"}
                    onChange={() => change("paceMode", "shared")}
                  />
                </div>
                <small>Plan one common sequence for all selected sections.</small>
              </label>
              <label className={`choice-card ${draft.paceMode === "independent" ? "choice-selected" : ""}`}>
                <div className="choice-header">
                  <strong>Independent pace</strong>
                  <input
                    type="radio"
                    name="pace"
                    checked={draft.paceMode === "independent"}
                    onChange={() => change("paceMode", "independent")}
                  />
                </div>
                <small>Keep cohort progress separate in the prototype.</small>
              </label>
            </div>
          </fieldset>
          <label className="field"><span>Planning starts</span><input type="date" value={draft.planStart} onChange={(event) => change("planStart", event.target.value)} /></label><label className="field"><span>Planning ends</span><input type="date" value={draft.planEnd} onChange={(event) => change("planEnd", event.target.value)} /></label>
        </div>}
        {step === 2 && <div className="schedule-setup">
          <p className="quiet-copy">Choose the days you teach. These slots drive the deterministic schedule proposal; blocked dates are skipped automatically.</p>
          <div className="day-picker">{dayNames.map((day, index) => <button type="button" key={day} className={draft.slots.some((slot) => slot.dayOfWeek === index) ? "day-selected" : ""} onClick={() => toggleDay(index)} aria-pressed={draft.slots.some((slot) => slot.dayOfWeek === index)}>{day.slice(0, 3)}</button>)}</div>
          {draft.slots.length > 0 ? <div className="slot-list">{draft.slots.map((slot) => <div className="slot-row" key={slot.dayOfWeek}><strong>{dayNames[slot.dayOfWeek]}</strong><input aria-label={`${dayNames[slot.dayOfWeek]} start time`} type="time" value={slot.startTime} onChange={(event) => patchSlot(slot.dayOfWeek, { startTime: event.target.value })} /><select aria-label={`${dayNames[slot.dayOfWeek]} duration`} value={slot.durationMinutes} onChange={(event) => patchSlot(slot.dayOfWeek, { durationMinutes: Number(event.target.value) })}><option value={60}>60 minutes</option><option value={90}>90 minutes</option><option value={120}>120 minutes</option></select><input aria-label={`${dayNames[slot.dayOfWeek]} room`} value={slot.room} onChange={(event) => patchSlot(slot.dayOfWeek, { room: event.target.value })} /></div>)}</div> : <p className="inline-warning">Select at least one teaching day. You can also save this subject and add the schedule later.</p>}
          <div className="schedule-range"><label className="field"><span>Plan from</span><input type="date" value={draft.planStart} onChange={(event) => change("planStart", event.target.value)} /></label><label className="field"><span>Plan through</span><input type="date" value={draft.planEnd} onChange={(event) => change("planEnd", event.target.value)} /></label></div>
        </div>}
        {error && <p className="form-error" role="alert">{error}</p>}
        <div className="wizard-actions"><Button type="button" variant="quiet" onClick={() => step === 0 ? onCancel() : setStep(step - 1)}>{step === 0 ? "Cancel" : "Back"}</Button><Button type="submit" variant="primary" arrow>{step === 2 ? "Save subject & add syllabus" : "Continue"}</Button></div>
      </form>
      <div className="note-row"><span><Sparkles size={15} /></span><p><strong>Start simply.</strong> You can adjust sections and teaching slots later. Planning recommendations remain proposals until you approve them.</p></div>
    </div>
  );
}

export function SyllabusScreen({
  subject,
  onNext,
  onExit,
  onSave,
  onExtract,
  isExtracting,
  fileName,
  onFile,
}: {
  subject: Subject;
  onNext: () => void;
  onExit: () => void;
  onSave: (units: Subject["units"], topics: SyllabusTopic[]) => void;
  onExtract: () => void;
  isExtracting: boolean;
  fileName: string;
  onFile: (name: string) => void;
}) {
  const [text, setText] = useState("");
  const [expanded, setExpanded] = useState<string | null>(subject.units[0]?.id ?? null);
  const [error, setError] = useState("");

  function addTopic(unitId: string) {
    const id = `${unitId}-topic-${Date.now()}`;
    const topic: SyllabusTopic = { id, unitId, title: "New syllabus topic", description: "Add a short description.", complexity: "Moderate", importance: "Core", estimatedHours: 1 };
    onSave(subject.units.map((unit) => unit.id === unitId ? { ...unit, topicIds: [...unit.topicIds, id] } : unit), [...subject.topics, topic]);
  }
  function editTopic(topicId: string, patch: Partial<SyllabusTopic>) {
    onSave(subject.units, subject.topics.map((topic) => topic.id === topicId ? { ...topic, ...patch } : topic));
  }
  function removeTopic(topicId: string) {
    const topic = subject.topics.find((item) => item.id === topicId);
    if (!topic) return;
    onSave(subject.units.map((unit) => unit.id === topic.unitId ? { ...unit, topicIds: unit.topicIds.filter((id) => id !== topicId) } : unit), subject.topics.filter((item) => item.id !== topicId));
  }
  function addUnit() {
    const id = `unit-added-${Date.now()}`;
    onSave([...subject.units, { id, title: "New syllabus unit", description: "Add a unit summary.", topicIds: [] }], subject.topics);
    setExpanded(id);
  }
  function editUnit(unitId: string, patch: Partial<Subject["units"][number]>) {
    onSave(subject.units.map((unit) => unit.id === unitId ? { ...unit, ...patch } : unit), subject.topics);
  }
  function handleFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (file) onFile(file.name);
  }

  return (
    <div className="screen-stack">
      <div className="breadcrumbs"><span>My Subjects</span><ChevronRight size={13} /><span>{subject.code}</span><ChevronRight size={13} /><span>Syllabus</span></div>
      <PageHeading eyebrow={`${subject.code} · Syllabus architecture`} title="Add your syllabus" description="Upload a document or enter text. The extraction step is a local simulation; review every topic before planning." action={<button type="button" className="quiet-action" onClick={onExit}>Save & exit</button>} />
      <section className="panel upload-panel">
        <div className="upload-copy"><span className="upload-icon"><FileUp size={20} /></span><div><h2>Upload a syllabus</h2><p>PDF, DOCX, or TXT · Prototype only, no file is sent to a server.</p></div></div>
        <label className="button button-secondary file-select"><Upload size={15} /><span>{fileName ? "Change file" : "Choose file"}</span><input type="file" accept=".pdf,.doc,.docx,.txt" onChange={handleFile} /></label>
        {fileName && <p className="selected-file"><Check size={14} /> {fileName} <span>Ready for simulated extraction</span></p>}
        <div className="or-rule"><span>or paste syllabus text</span></div>
        <label className="field"><span>Syllabus text</span><textarea rows={4} value={text} onChange={(event) => setText(event.target.value)} placeholder="Paste unit titles, learning outcomes, or a short syllabus outline…" /></label>
        <div className="extract-row"><span className="mock-note">Sample extraction is generated locally and is not AI-powered.</span><Button type="button" variant="primary" icon={<Sparkles size={15} />} onClick={() => { if (!fileName && text.trim().length < 10) { setError("Choose a file or enter at least a few words of syllabus text."); return; } setError(""); onExtract(); }} disabled={isExtracting}>{isExtracting ? "Preparing sample structure…" : "Extract syllabus"}</Button></div>
        {error && <p className="form-error" role="alert">{error}</p>}
      </section>
      {subject.units.length > 0 && <>
        <div className="section-title-row"><div><p className="eyebrow">Review before planning</p><h2>Extracted syllabus structure</h2></div><div className="action-row"><Button onClick={addUnit} icon={<Plus size={15} />}>Add unit</Button><Button variant="primary" onClick={onNext} arrow>Continue to pacing</Button></div></div>
        <div className="unit-list">{subject.units.map((unit, index) => <section className="unit-card" key={unit.id}>
          <button className="unit-summary" type="button" onClick={() => setExpanded(expanded === unit.id ? null : unit.id)} aria-expanded={expanded === unit.id}><span className="unit-number">{String(index + 1).padStart(2, "0")}</span><span className="unit-title-wrap"><strong>{unit.title}</strong><small>{unit.topicIds.length} topics · {subject.topics.filter((topic) => topic.unitId === unit.id).reduce((sum, topic) => sum + topic.estimatedHours, 0)} estimated hours</small></span><ChevronRight size={16} className={expanded === unit.id ? "chevron-open" : ""} /></button>
          {expanded === unit.id && <div className="unit-body"><div className="form-grid" style={{ marginBottom: "12px" }}><label className="field"><span>Unit title</span><input value={unit.title} onChange={(event) => editUnit(unit.id, { title: event.target.value })} /></label><label className="field"><span>Unit description & scope</span><input value={unit.description} onChange={(event) => editUnit(unit.id, { description: event.target.value })} placeholder="Short summary of unit..." /></label></div><div className="topic-table">{subject.topics.filter((topic) => topic.unitId === unit.id).map((topic, topicIndex) => <div className="topic-edit-row" key={topic.id}><span className="topic-index">{index + 1}.{topicIndex + 1}</span><div className="topic-edit-fields"><input aria-label="Topic title" placeholder="Topic title" value={topic.title} onChange={(event) => editTopic(topic.id, { title: event.target.value })} /><input aria-label="Topic description" placeholder="Short description" value={topic.description} onChange={(event) => editTopic(topic.id, { description: event.target.value })} /><select aria-label="Topic complexity" value={topic.complexity} onChange={(event) => editTopic(topic.id, { complexity: event.target.value as Complexity })}><option>Foundational</option><option>Moderate</option><option>Advanced</option></select><select aria-label="Topic importance" value={topic.importance} onChange={(event) => editTopic(topic.id, { importance: event.target.value as Importance })}><option>Core</option><option>Supporting</option><option>Enrichment</option></select><input aria-label="Estimated hours" type="number" min="0.5" step="0.5" value={topic.estimatedHours} onChange={(event) => editTopic(topic.id, { estimatedHours: Number(event.target.value) || 0.5 })} /></div><button type="button" className="icon-button remove-topic" onClick={() => removeTopic(topic.id)} aria-label={`Remove ${topic.title}`}>×</button></div>)}</div><button type="button" className="inline-link add-topic" onClick={() => addTopic(unit.id)}><Plus size={14} /> Add topic</button></div>}
        </section>)}</div>
      </>}
      {subject.units.length === 0 && <div className="info-banner"><Sparkles size={17} /><span>Once an outline is added, you can review and edit every extracted unit and topic here before pacing.</span></div>}
    </div>
  );
}

export function PacingScreen({
  subject,
  onPatchTopic,
  onNext,
  onBack,
}: {
  subject: Subject;
  onPatchTopic: (topicId: string, patch: Partial<SyllabusTopic>) => void;
  onNext: () => void;
  onBack: () => void;
}) {
  const totalHours = subject.topics.reduce((sum, topic) => sum + topic.estimatedHours, 0);
  return (
    <div className="screen-stack">
      <div className="breadcrumbs"><button type="button" onClick={() => onBack()}>My Subjects</button><ChevronRight size={13} /><span>{subject.code}</span><ChevronRight size={13} /><span>Pacing matrix</span></div>
      <PageHeading eyebrow="Planning stage 02 · Teaching effort" title="Plan your teaching pace" description="Set a realistic effort estimate for each topic. This is separate from mapping sessions to calendar dates." action={<Button variant="primary" onClick={onNext} arrow>Continue to schedule map</Button>} />
      <div className="planning-steps"><span><b>01</b> Syllabus</span><span className="step-current"><b>02</b> Pacing matrix</span><span><b>03</b> Schedule map</span><span><b>04</b> Teaching plan</span></div>
      <div className="summary-strip"><div><span>Syllabus scope</span><strong>{subject.units.length} units · {subject.topics.length} topics</strong></div><div><span>Estimated workload</span><strong>{totalHours} teaching hours</strong></div><div><span>Sections</span><strong>{subject.sections.join(" & ") || "Not set"}</strong></div><div><span>Weekly availability</span><strong>{subject.slots.length} teaching slots</strong></div></div>
      <section className="panel pacing-panel">
        <div className="panel-heading"><div><p className="eyebrow">Pacing parameters</p><h2>Topic & pedagogical module</h2></div><Button icon={<RotateCcw size={14} />} onClick={() => subject.topics.forEach((topic) => onPatchTopic(topic.id, { estimatedHours: topic.complexity === "Advanced" ? 2 : 1 }))}>Reset suggested effort</Button></div>
        <div className="pacing-table-head"><span>Unit / topic</span><span>Complexity</span><span>Importance</span><span>Effort (hrs)</span></div>
        {subject.units.map((unit, unitIndex) => (
          <div className="pacing-unit" key={unit.id}><div className="pacing-unit-heading"><span>UNIT {String(unitIndex + 1).padStart(2, "0")}</span><strong>{unit.title}</strong><small>{unit.topicIds.length} topics</small></div>
            {subject.topics.filter((topic) => topic.unitId === unit.id).map((topic, topicIndex) => <div className="pacing-row" key={topic.id}><div className="pacing-topic"><span className="topic-index">{unitIndex + 1}.{topicIndex + 1}</span><span><strong>{topic.title}</strong><small>{topic.description}</small></span></div><select aria-label={`${topic.title} complexity`} value={topic.complexity} onChange={(event) => onPatchTopic(topic.id, { complexity: event.target.value as Complexity })}><option>Foundational</option><option>Moderate</option><option>Advanced</option></select><select aria-label={`${topic.title} importance`} value={topic.importance} onChange={(event) => onPatchTopic(topic.id, { importance: event.target.value as Importance })}><option>Core</option><option>Supporting</option><option>Enrichment</option></select><label className="hours-input"><input type="number" min="0.5" max="8" step="0.5" value={topic.estimatedHours} onChange={(event) => onPatchTopic(topic.id, { estimatedHours: Math.max(0.5, Number(event.target.value) || 0.5) })} /><span>hrs</span></label></div>)}
          </div>
        ))}
      </section>
      <div className="bottom-actions"><Button variant="quiet" onClick={onBack}>Back to syllabus</Button><Button variant="primary" onClick={onNext} arrow>Continue to schedule map</Button></div>
    </div>
  );
}

export function ScheduleScreen({
  subject,
  sessions,
  onChangeSubject,
  onGenerate,
  onBack,
  onReview,
}: {
  subject: Subject;
  sessions: TeachingSession[];
  onChangeSubject: (patch: Partial<Subject>) => void;
  onGenerate: () => void;
  onBack: () => void;
  onReview: () => void;
}) {
  const proposed = sessions.filter((session) => session.status === "proposed");
  const missing = subject.topics.length > 0 && proposed.length < Math.ceil(subject.topics.reduce((sum, topic) => sum + topic.estimatedHours, 0));
  return (
    <div className="screen-stack">
      <div className="breadcrumbs"><button type="button" onClick={onBack}>My Subjects</button><ChevronRight size={13} /><span>{subject.code}</span><ChevronRight size={13} /><span>Schedule map</span></div>
      <PageHeading eyebrow="Planning stage 03 · Academic calendar allocation" title="Map your teaching schedule" description="Place topics into your available weekly teaching slots. Holidays and blocked dates are skipped." action={<Button onClick={onGenerate} icon={<RotateCcw size={14} />}>Regenerate proposal</Button>} />
      <div className="planning-steps"><span><b>01</b> Syllabus</span><span><b>02</b> Pacing matrix</span><span className="step-current"><b>03</b> Schedule map</span><span><b>04</b> Teaching plan</span></div>
      <section className="panel schedule-controls"><div className="schedule-control-fields"><label className="field"><span>Planning period starts</span><input type="date" value={subject.planStart} onChange={(event) => onChangeSubject({ planStart: event.target.value })} /></label><label className="field"><span>Planning period ends</span><input type="date" value={subject.planEnd} onChange={(event) => onChangeSubject({ planEnd: event.target.value })} /></label><div className="schedule-control-meta"><span>Selected teaching slots</span><strong>{subject.slots.length ? subject.slots.map((slot) => `${dayNames[slot.dayOfWeek].slice(0, 3)} ${slot.startTime}`).join(" · ") : "None configured"}</strong></div></div><div className="info-banner"><CalendarDays size={16} /><span>Schedule logic is deterministic: syllabus order is preserved, each teaching hour uses one slot, and blocked academic dates are never selected.</span></div></section>
      <div className="section-title-row"><div><p className="eyebrow">Proposed session allocation</p><h2>{proposed.length} proposed sessions</h2></div><span className="muted-copy">{subject.topics.length} topics · {subject.sections.join(" & ")}</span></div>
      {proposed.length === 0 ? <EmptyState title="No proposal to review" description="Generate a schedule after confirming your teaching days and planning period." action={<Button onClick={onGenerate} variant="primary">Generate schedule</Button>} /> : (
        <div className="schedule-week-list">{groupByWeek(proposed).map(([week, weekSessions]) => <section className="schedule-week" key={week}><div className="week-heading"><span>WEEK {week}</span><small>{weekSessions.length} slots allocated</small></div><div className="week-session-grid">{weekSessions.map((session) => { const topic = subject.topics.find((item) => item.id === session.topicId); return <article className="week-session" key={session.id}><div className="week-session-date">{formatDate(session.date)} <Tag tone="amber">Proposed</Tag></div><strong>{topic?.title ?? "Teaching session"}</strong><p>{session.startTime}–{session.endTime} · {session.sectionIds.join(", ")}</p><small>{topic?.estimatedHours ?? 1} hr estimated effort</small></article>; })}</div></section>)}</div>
      )}
      {missing && <p className="inline-warning">Some teaching effort may not fit within the planning window. Extend the period or add teaching slots before approval.</p>}
      <div className="bottom-actions"><Button variant="quiet" onClick={onBack}>Back to pacing</Button><Button variant="primary" onClick={onReview} disabled={proposed.length === 0} arrow>Review teaching plan</Button></div>
    </div>
  );
}

function groupByWeek(sessions: TeachingSession[]): [number, TeachingSession[]][] {
  const groups = new Map<number, TeachingSession[]>();
  for (const session of sessions) {
    const key = Math.ceil(new Date(`${session.date}T12:00:00`).getDate() / 7);
    const group = groups.get(key) ?? [];
    group.push(session);
    groups.set(key, group);
  }
  return [...groups.entries()];
}

export function ReviewScreen({
  subject,
  sessions,
  onApprove,
  onBack,
  onEdit,
  onPatchSession,
  onRemoveSession,
}: {
  subject: Subject;
  sessions: TeachingSession[];
  onApprove: () => void;
  onBack: () => void;
  onEdit: () => void;
  onPatchSession?: (sessionId: string, patch: Partial<TeachingSession>) => void;
  onRemoveSession?: (sessionId: string) => void;
}) {
  const [editingId, setEditingId] = useState<string | null>(null);
  const pending = sessions.filter((session) => session.status === "proposed");
  const visible = sessions.filter((session) => session.status === "proposed" || session.status === "approved").sort((a, b) => `${a.date}${a.startTime}`.localeCompare(`${b.date}${b.startTime}`));
  return (
    <div className="screen-stack">
      <div className="breadcrumbs"><span>My Subjects</span><ChevronRight size={13} /><span>{subject.code}</span><ChevronRight size={13} /><span>Teaching plan review</span></div>
      <PageHeading eyebrow={`${subject.code} · Course architecture`} title="Review your teaching plan" description="Check the dated sessions before confirming. You can adjust session dates, topics, times, or exclude sessions before approving." action={<div className="action-row"><Button onClick={onEdit}>Back to schedule map</Button><Button variant="primary" onClick={onApprove} disabled={pending.length === 0} icon={<Check size={15} />}>Approve plan</Button></div>} />
      <div className="summary-strip"><div><span>Proposed sessions</span><strong>{pending.length} to approve</strong></div><div><span>Sections</span><strong>{subject.sections.join(" & ") || "Not set"}</strong></div><div><span>Teaching days</span><strong>{subject.slots.length} weekly slots</strong></div><div><span>Approval state</span><strong>{pending.length ? "Awaiting your review" : "No proposed changes"}</strong></div></div>
      {pending.length === 0 && <div className="info-banner"><Check size={16} /><span>There are no unapproved sessions for this subject. Regenerate a schedule to review a new proposal.</span></div>}
      <section className="panel review-list"><div className="panel-heading"><div><p className="eyebrow">Detailed session schedule</p><h2>Sessions in syllabus order</h2></div><Tag tone={pending.length ? "amber" : "sage"}>{pending.length ? "Awaiting approval" : "Reviewed"}</Tag></div>
        {visible.length ? <div className="session-list">{visible.map((session, index) => {
          const topic = subject.topics.find((item) => item.id === session.topicId);
          const isEditing = editingId === session.id;
          return (
            <div key={session.id}>
              <div className="review-session">
                <span className={`review-number ${session.status === "approved" ? "review-approved" : ""}`}>{String(index + 1).padStart(2, "0")}</span>
                <div className="date-block"><strong>{new Date(`${session.date}T12:00:00`).getDate()}</strong><span>{new Date(`${session.date}T12:00:00`).toLocaleDateString("en-IN", { month: "short" })}</span></div>
                <div className="review-session-main"><strong>{topic?.title ?? "Teaching session"}</strong><span>{formatDate(session.date)} · {session.startTime}–{session.endTime} · {session.sectionIds.join(", ")}</span></div>
                <div className="review-session-actions">
                  <Tag tone={session.status === "approved" ? "sage" : "amber"}>{session.status === "approved" ? "Confirmed" : "Proposed"}</Tag>
                  {session.status === "proposed" && onPatchSession && (
                    <button type="button" className="text-action" onClick={() => setEditingId(isEditing ? null : session.id)} style={{ fontSize: "10px", padding: "2px 6px", cursor: "pointer", background: "none", border: "none", color: "var(--sage)" }}>
                      {isEditing ? "Close" : "Edit"}
                    </button>
                  )}
                </div>
              </div>
              {isEditing && onPatchSession && (
                <div className="review-session-edit">
                  <div className="review-session-edit-grid">
                    <label className="field"><span>Assigned topic</span><select value={session.topicId} onChange={(event) => onPatchSession(session.id, { topicId: event.target.value })}>{subject.topics.map((t) => <option key={t.id} value={t.id}>{t.title}</option>)}</select></label>
                    <label className="field"><span>Session date</span><input type="date" value={session.date} onChange={(event) => onPatchSession(session.id, { date: event.target.value })} /></label>
                    <label className="field"><span>Start time</span><input type="time" value={session.startTime} onChange={(event) => onPatchSession(session.id, { startTime: event.target.value })} /></label>
                    <label className="field"><span>End time</span><input type="time" value={session.endTime} onChange={(event) => onPatchSession(session.id, { endTime: event.target.value })} /></label>
                  </div>
                  <div className="review-session-edit-actions">
                    {onRemoveSession && <Button type="button" variant="danger" onClick={() => { onRemoveSession(session.id); setEditingId(null); }}>Exclude from plan</Button>}
                    <Button type="button" variant="secondary" onClick={() => setEditingId(null)}>Done</Button>
                  </div>
                </div>
              )}
            </div>
          );
        })}</div> : <EmptyState title="No sessions to review" description="Create a subject and generate a schedule first." />}
      </section>
      <div className="bottom-actions"><Button variant="quiet" onClick={onBack}>Back</Button><Button variant="primary" onClick={onApprove} disabled={pending.length === 0} icon={<Check size={15} />}>Approve {pending.length} sessions</Button></div>
    </div>
  );
}

export function CalendarScreen({
  sessions,
  subjects,
  events,
  onUpdate,
  onSubject,
}: {
  sessions: TeachingSession[];
  subjects: Subject[];
  events: AcademicCalendarEvent[];
  onUpdate: (sessionId: string) => void;
  onSubject: (subjectId: string) => void;
}) {
  const [filter, setFilter] = useState("all");
  const confirmed = sessions.filter((session) => session.status === "approved" || session.status === "completed");
  const filtered = confirmed.filter((session) => filter === "all" || session.subjectId === filter).sort((a, b) => `${a.date}${a.startTime}`.localeCompare(`${b.date}${b.startTime}`));
  const upcoming = filtered.filter((session) => session.status === "approved");
  return (
    <div className="screen-stack">
      <PageHeading eyebrow="Institution calendar" title="Calendar" description="Confirmed teaching sessions and academic dates across your subjects." action={<label className="select-filter"><span className="sr-only">Filter calendar by subject</span><select value={filter} onChange={(event) => setFilter(event.target.value)}><option value="all">All subjects</option>{subjects.map((subject) => <option key={subject.id} value={subject.id}>{subject.code}</option>)}</select></label>} />
      <div className="calendar-legend"><span><i className="legend-teaching" /> Confirmed teaching</span><span><i className="legend-holiday" /> Holiday / recess</span><span><i className="legend-exam" /> Exam period</span><span className="muted-copy">Proposed sessions are hidden until approval.</span></div>
      <section className="panel calendar-panel"><div className="panel-heading"><div><p className="eyebrow">Upcoming · {upcoming.length} sessions</p><h2>Teaching schedule</h2></div><CalendarDays size={19} className="muted-icon" /></div>
        {filtered.length === 0 ? <EmptyState title="No confirmed events" description="Approve a teaching plan to add its sessions to your calendar." /> : <div className="calendar-list">{filtered.map((session) => { const subject = subjects.find((item) => item.id === session.subjectId); const topic = subject?.topics.find((item) => item.id === session.topicId); return <article className="calendar-event" key={session.id}><div className="event-date">{formatDate(session.date, { weekday: "short", month: "short", day: "numeric" })}<small>{session.startTime}–{session.endTime}</small></div><span className={`event-line ${session.status === "completed" ? "event-complete" : ""}`} /><div className="event-main"><button type="button" onClick={() => subject && onSubject(subject.id)}><strong>{topic?.title ?? "Teaching session"}</strong></button><span>{subject?.name} · {session.sectionIds.join(", ")}</span></div>{session.status === "approved" ? <Button onClick={() => onUpdate(session.id)}>Record outcome</Button> : <Tag tone="sage">Taught</Tag>}</article>; })}</div>}
      </section>
      <section className="panel academic-dates"><div className="panel-heading"><div><p className="eyebrow">Academic calendar</p><h2>Blocked dates</h2></div></div>{events.map((event) => <div className="date-event" key={event.id}><span className={`event-type event-type-${event.kind}`}>{event.kind === "holiday" ? "Recess" : "Exam"}</span><strong>{event.title}</strong><span>{formatDate(event.date)}</span></div>)}<p className="mock-note">Illustrative dates only; replace with your institution’s calendar.</p></section>
    </div>
  );
}

export function UpdateScreen({
  subject,
  session,
  onSave,
  onCancel,
}: {
  subject: Subject;
  session: TeachingSession;
  onSave: (outcome: OutcomeKind, note: string) => void;
  onCancel: () => void;
}) {
  const [outcome, setOutcome] = useState<OutcomeKind | null>(null);
  const [showNote, setShowNote] = useState(false);
  const [note, setNote] = useState("");
  const topic = subject.topics.find((item) => item.id === session.topicId);
  return (
    <div className="screen-stack">
      <div className="breadcrumbs"><span>{subject.name}</span><ChevronRight size={13} /><span>Class update</span></div>
      <PageHeading eyebrow="Class reflection · 10–20 seconds" title="How did the class go?" description="Record what happened so your upcoming plan can stay realistic." />
      <section className="panel update-panel"><div className="session-context"><div><p className="eyebrow">Scheduled session</p><strong>{formatDate(session.date)} · {session.startTime}–{session.endTime}</strong><span>{subject.code} · {session.sectionIds.join(", ")}</span></div><div><p className="eyebrow">Topic</p><strong>{topic?.title ?? "Teaching session"}</strong><span>{subject.units.find((unit) => unit.id === topic?.unitId)?.title}</span></div></div>
        <fieldset className="outcome-field"><legend>How much did you cover?</legend><div className="outcome-options">{([{ id: "completed", title: "Completed", note: "Covered as planned", mark: "✓" }, { id: "partial", title: "Partially covered", note: "Needs a little more time", mark: "◐" }, { id: "not-covered", title: "Not covered", note: "Move it to a later slot", mark: "○" }] as const).map((item) => <button type="button" className={`outcome-option ${outcome === item.id ? "outcome-selected" : ""}`} key={item.id} onClick={() => setOutcome(item.id)} aria-pressed={outcome === item.id}><span className="outcome-mark">{item.mark}</span><strong>{item.title}</strong><small>{item.note}</small></button>)}</div></fieldset>
        <div className="optional-note">{showNote ? <label className="field"><span>Teacher note <small>Optional</small></span><textarea rows={3} value={note} onChange={(event) => setNote(event.target.value)} placeholder="A short note for your own planning…" /></label> : <button type="button" className="inline-link" onClick={() => setShowNote(true)}><Plus size={14} /> Add a note (optional)</button>}</div>
        {outcome && outcome !== "completed" && <p className="info-banner"><Sparkles size={16} /><span>Saving this update will prepare a suggested schedule adjustment for your review. Your approved plan will not change yet.</span></p>}
        <div className="wizard-actions"><Button variant="quiet" onClick={onCancel}>Cancel</Button><Button variant="primary" disabled={!outcome} onClick={() => outcome && onSave(outcome, note)} arrow>Save class update</Button></div>
      </section>
    </div>
  );
}

export function AdjustmentScreen({
  subject,
  proposal,
  sessions,
  onApply,
  onKeep,
}: {
  subject: Subject;
  proposal: ScheduleProposal;
  sessions: TeachingSession[];
  onApply: () => void;
  onKeep: () => void;
}) {
  const source = sessions.find((session) => session.id === proposal.sourceSessionId);
  const originalTopic = subject.topics.find((topic) => topic.id === source?.topicId);
  const suggested = proposal.proposedSessions[0];
  return (
    <div className="screen-stack">
      <div className="breadcrumbs"><span>{subject.code}</span><ChevronRight size={13} /><span>Teaching plan</span><ChevronRight size={13} /><span>Suggested changes</span></div>
      <PageHeading eyebrow="Adaptive schedule · Review before applying" title="Adjust your upcoming plan" description="One classroom update created a proposed change. No other sessions will move." action={<Tag tone="amber">1 session affected</Tag>} />
      <section className="panel proposal-reason"><div className="proposal-icon"><Sparkles size={18} /></div><div><p className="eyebrow">Post-class update triggered</p><h2>{proposal.outcome === "partial" ? "Topic partially covered" : "Session not covered"}</h2><p>{proposal.reason}</p></div><div className="proposal-scope"><span>Adjustment scope</span><strong>1 new session</strong><small>0 other sessions changed</small></div></section>
      <div className="diff-grid"><section className="panel"><p className="eyebrow">Original session</p><h2>{source ? formatDate(source.date) : "Source session"}</h2><div className="diff-card"><strong>{originalTopic?.title ?? "Topic"}</strong><span>{source?.startTime ?? "—"}–{source?.endTime ?? ""}</span><Tag tone="neutral">{proposal.outcome === "partial" ? "Partially covered" : "Not covered"}</Tag></div></section><section className="panel diff-suggested"><p className="eyebrow">Suggested follow-up</p><h2>{suggested ? formatDate(suggested.date) : "No available slot"}</h2>{suggested ? <div className="diff-card"><strong>{originalTopic?.title ?? "Topic"} · follow-up</strong><span>{suggested.startTime}–{suggested.endTime} · {suggested.sectionIds.join(", ")}</span><Tag tone="sage">Next open slot</Tag></div> : <p className="inline-warning">No available slot remains in the planning period. Extend the schedule period before applying.</p>}</section></div>
      <div className="info-banner"><Check size={16} /><span>All unrelated approved sessions stay exactly where they are. This suggestion remains pending until you apply it.</span></div>
      <div className="bottom-actions"><Button variant="quiet" onClick={onKeep}>Keep original plan</Button><span className="mock-note">You can review changes later in the subject’s schedule.</span><Button variant="primary" onClick={onApply} disabled={!suggested} icon={<Check size={15} />}>Apply suggested change</Button></div>
    </div>
  );
}

export function NotificationsScreen({ onNavigate }: { onNavigate: (view: View) => void }) {
  return <div className="screen-stack"><PageHeading eyebrow="Faculty workspace" title="Notifications" description="Updates that may need your attention." /><section className="panel notification-list"><article><span className="notification-indicator" /><div><strong>Your weekly teaching plan is ready</strong><p>Review the proposed sessions before they appear in Calendar.</p><small>Planning · Today</small></div><Button onClick={() => onNavigate("subjects")}>Review</Button></article><article><span className="notification-indicator notification-muted" /><div><strong>Term calendar dates are illustrative</strong><p>Replace the sample recess and exam dates with your institution’s calendar.</p><small>Calendar · This week</small></div><Button onClick={() => onNavigate("calendar")}>Open calendar</Button></article></section></div>;
}

export function SettingsScreen({ onReset, onLogout }: { onReset: () => void; onLogout: () => void }) {
  const [confirm, setConfirm] = useState(false);
  return <div className="screen-stack"><PageHeading eyebrow="Preferences" title="Settings" description="Prototype preferences and local demo data." /><section className="panel settings-section"><div><p className="eyebrow">Data & privacy</p><h2>Browser-only prototype</h2><p className="quiet-copy">Subjects, plans, class updates, and demo sign-in are stored in this browser’s local storage. Nothing is synced to a server.</p></div><div className="settings-actions">{confirm ? <><span className="inline-warning">This will replace your local demo data with the original sample.</span><Button variant="danger" onClick={() => { onReset(); setConfirm(false); }}>Confirm reset</Button><Button onClick={() => setConfirm(false)}>Cancel</Button></> : <Button onClick={() => setConfirm(true)} icon={<RotateCcw size={14} />}>Reset demo data</Button>}</div></section><section className="panel settings-section"><div><p className="eyebrow">Session</p><h2>Demo access</h2><p className="quiet-copy">The sign-in screen is a convenience for this prototype. It is not security or authorization.</p></div><Button onClick={onLogout}>Sign out</Button></section></div>;
}

export function ProfileScreen({ onLogout }: { onLogout: () => void }) {
  return <div className="screen-stack"><PageHeading eyebrow="Faculty profile" title="Kartikey Joshi" description="Department of Computer Science & Engineering · Faculty workspace." /><section className="panel profile-page"><div className="large-avatar">KJ</div><div><p className="eyebrow">Teaching overview</p><h2>University faculty</h2><p className="quiet-copy">Your profile details are sample information for this prototype. Edit and institutional directory features are not connected.</p></div><div className="profile-details"><div><span>Academic year</span><strong>2026–27</strong></div><div><span>Subjects</span><strong>Adaptive teaching</strong></div><div><span>Workspace</span><strong>Faculty</strong></div></div><Button onClick={onLogout}>Sign out</Button></section></div>;
}

export function subjectDraftDefaults(): SubjectDraft {
  return {
    name: "",
    code: "",
    program: "MCA · Semester 3",
    semester: "Semester 3",
    academicYear: "2026–27",
    sections: "MCA 3A, MCA 3B",
    paceMode: "shared",
    slots: [
      { dayOfWeek: 1, startTime: "10:00", durationMinutes: 60, room: "Room 402" },
      { dayOfWeek: 3, startTime: "10:00", durationMinutes: 60, room: "Room 402" },
      { dayOfWeek: 5, startTime: "10:00", durationMinutes: 60, room: "Room 402" },
    ],
    planStart: "2026-10-12",
    planEnd: "2026-12-18",
    objectives: "",
  };
}

export function clonedSampleSyllabus(prefix: string): Pick<Subject, "units" | "topics"> {
  const units: Subject["units"] = [
    { id: `${prefix}-unit-1`, title: "Foundations & System Architecture", description: "Core concepts, motivation, and system components.", topicIds: [] },
    { id: `${prefix}-unit-2`, title: "Data Quality & Warehouse Integration", description: "Data preparation, warehouse architecture, and OLAP.", topicIds: [] },
    { id: `${prefix}-unit-3`, title: "Pattern Discovery & Evaluation", description: "Association patterns, clustering, and practical evaluation.", topicIds: [] },
  ];
  const specs = [
    ["Foundations of the subject", "A shared vocabulary, key concepts, and motivating examples.", 1, 0],
    ["Core models and architecture", "The major components and how they work together.", 2, 0],
    ["Data quality and preparation", "Cleaning, transformation, and integration workflows.", 1.5, 1],
    ["Warehouse and analytical models", "Schemas, measures, and common query patterns.", 2, 1],
    ["Methods and practical applications", "A guided comparison of central techniques.", 2, 2],
    ["Evaluation and case study", "Interpret results, discuss limitations, and solve an applied case.", 1.5, 2],
  ] as const;
  const topics: SyllabusTopic[] = specs.map(([title, description, estimatedHours, unitIndex], index) => {
    const id = `${prefix}-topic-${index + 1}`;
    units[unitIndex]?.topicIds.push(id);
    return {
      id,
      title,
      description,
      unitId: units[unitIndex]?.id ?? units[0]?.id ?? "",
      complexity: index === 1 || index === 4 ? "Advanced" : index === 2 ? "Foundational" : "Moderate",
      importance: index === 5 ? "Supporting" : "Core",
      estimatedHours,
    };
  });
  return { units, topics };
}

export function accessibleTopicTitle(subject: Subject, topicId: string): string {
  return subject.topics.find((topic) => topic.id === topicId)?.title ?? "Teaching topic";
}

