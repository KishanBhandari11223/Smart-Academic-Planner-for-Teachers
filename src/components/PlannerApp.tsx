"use client";

import { useEffect, useMemo, useState, type ReactNode } from "react";
import { gsap } from "gsap";
import { AppShell } from "@/components/AppShell";
import { LoginScreen } from "@/components/LoginScreen";
import {
  AdjustmentScreen,
  CalendarScreen,
  CreateSubjectScreen,
  HomeScreen,
  NotificationsScreen,
  PacingScreen,
  ProfileScreen,
  ReviewScreen,
  ScheduleScreen,
  SettingsScreen,
  SubjectWorkspace,
  SubjectsScreen,
  SyllabusScreen,
  UpdateScreen,
  clonedSampleSyllabus,
  subjectDraftDefaults,
  type SubjectDraft,
} from "@/components/Screens";
import { academicEvents, initialData } from "@/data/seed";
import {
  blockedDateSet,
  buildAdjustmentProposal,
  generateSchedule,
} from "@/lib/planning";
import { loadAppData, loadDemoAuth, saveAppData, saveDemoAuth } from "@/lib/storage";
import type { AppData, OutcomeKind, Subject, View } from "@/types";

export function PlannerApp() {
  const [data, setData] = useState<AppData>(initialData);
  const [signedIn, setSignedIn] = useState(false);
  const [ready, setReady] = useState(false);
  const [view, setView] = useState<View>("home");
  const [subjectId, setSubjectId] = useState("subject-data-mining");
  const [sessionId, setSessionId] = useState("");
  const [draft, setDraft] = useState<SubjectDraft>(subjectDraftDefaults());
  const [fileName, setFileName] = useState("");
  const [isExtracting, setIsExtracting] = useState(false);
  const [search, setSearch] = useState("");
  const [toast, setToast] = useState("");

  const subject = useMemo(() => data.subjects.find((item) => item.id === subjectId) ?? data.subjects[0], [data.subjects, subjectId]);
  const blockedDates = useMemo(() => blockedDateSet(academicEvents), []);

  useEffect(() => {
    let mounted = true;
    const frame = window.requestAnimationFrame(async () => {
      setData(loadAppData());

      // Attempt to restore authenticated server session from cookie
      try {
        const res = await fetch("/api/auth/me");
        if (res.ok) {
          const body = await res.json();
          if (mounted && body.user) {
            setSignedIn(true);
            setReady(true);
            return;
          }
        }
      } catch {
        // Fall back to demo authentication if server unreachable
      }

      if (mounted) {
        setSignedIn(loadDemoAuth());
        setReady(true);
      }
    });

    return () => {
      mounted = false;
      window.cancelAnimationFrame(frame);
    };
  }, []);

  useEffect(() => {
    if (ready) saveAppData(data);
  }, [data, ready]);

  useEffect(() => {
    if (!ready || !signedIn) return;
    const node = document.querySelector<HTMLElement>(".screen-transition");
    if (!node || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    const context = gsap.context(() => {
      gsap.fromTo(node, { y: 8, autoAlpha: 0.5 }, { y: 0, autoAlpha: 1, duration: 0.32, ease: "power2.out", clearProps: "all" });
    }, node);
    return () => context.revert();
  }, [view, ready, signedIn]);

  useEffect(() => {
    if (!toast) return;
    const timeout = window.setTimeout(() => setToast(""), 3200);
    return () => window.clearTimeout(timeout);
  }, [toast]);

  function showToast(message: string) {
    setToast(message);
  }

  function navigate(nextView: View) {
    setView(nextView);
    setSearch("");
  }

  function handleLoginSuccess(userEmail: string) {
    saveDemoAuth(true);
    setSignedIn(true);
    setView("home");
    showToast(`Signed in as ${userEmail}`);
  }

  function login(email: string, password: string): boolean {
    const valid = email === "example@123" && password === "123";
    if (valid) {
      saveDemoAuth(true);
      setSignedIn(true);
      setView("home");
    }
    return valid;
  }

  async function logout() {
    try {
      await fetch("/api/auth/logout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
      });
    } catch {
      // Ignore network errors on logout
    }
    saveDemoAuth(false);
    setSignedIn(false);
    setView("home");
  }

  function updateSubject(subjectKey: string, patch: Partial<Subject>) {
    setData((current) => ({
      ...current,
      subjects: current.subjects.map((item) => item.id === subjectKey ? { ...item, ...patch } : item),
    }));
  }

  function selectSubject(id: string, nextView: View = "subject") {
    setSubjectId(id);
    setView(nextView);
    setSearch("");
  }

  function createSubject() {
    const id = `subject-${Date.now()}`;
    const sections = draft.sections.split(",").map((item) => item.trim()).filter(Boolean);
    const nextSubject: Subject = {
      id,
      name: draft.name.trim(),
      code: draft.code.trim().toUpperCase(),
      program: draft.program.trim() || "University course",
      semester: draft.semester,
      academicYear: draft.academicYear,
      sections: sections.length ? sections : ["Section 1"],
      paceMode: draft.paceMode,
      units: [],
      topics: [],
      slots: draft.slots,
      planStart: draft.planStart,
      planEnd: draft.planEnd,
      status: "draft",
      objectives: draft.objectives,
      createdAt: new Date().toISOString(),
    };
    setData((current) => ({ ...current, subjects: [nextSubject, ...current.subjects] }));
    setSubjectId(id);
    setDraft(subjectDraftDefaults());
    setFileName("");
    setView("syllabus");
    showToast("Subject saved. Add a syllabus to begin planning.");
  }

  function extractSyllabus() {
    if (!subject) return;
    setIsExtracting(true);
    window.setTimeout(() => {
      const sample = clonedSampleSyllabus(subject.id);
      updateSubject(subject.id, { ...sample, status: "planning" });
      setIsExtracting(false);
      showToast("Sample syllabus structure is ready to review.");
    }, 750);
  }

  function generateProposal() {
    if (!subject) return;
    const existing = data.sessions.filter((session) => session.subjectId === subject.id && (session.status === "approved" || session.status === "completed"));
    const alreadyPlannedTopics = new Set(existing.map((session) => session.topicId));
    const remainingSubject: Subject = {
      ...subject,
      units: subject.units.map((unit) => ({ ...unit, topicIds: unit.topicIds.filter((id) => !alreadyPlannedTopics.has(id)) })),
      topics: subject.topics.filter((topic) => !alreadyPlannedTopics.has(topic.id)),
    };
    const generated = generateSchedule(remainingSubject, blockedDates);
    setData((current) => ({
      ...current,
      sessions: [
        ...current.sessions.filter((session) => !(session.subjectId === subject.id && session.status === "proposed")),
        ...generated,
      ],
      subjects: current.subjects.map((item) => item.id === subject.id ? { ...item, status: "planning" } : item),
    }));
    showToast(generated.length ? `${generated.length} proposed sessions generated.` : "No new topics fit the selected period and teaching slots.");
  }

  function approvePlan() {
    if (!subject) return;
    const approvedCount = data.sessions.filter((session) => session.subjectId === subject.id && session.status === "proposed").length;
    if (!approvedCount) return;
    setData((current) => ({
      ...current,
      sessions: current.sessions.map((session) => session.subjectId === subject.id && session.status === "proposed" ? { ...session, status: "approved" } : session),
      subjects: current.subjects.map((item) => item.id === subject.id ? { ...item, status: "approved" } : item),
    }));
    showToast(`${approvedCount} sessions approved and added to Calendar.`);
    setView("calendar");
  }

  function beginUpdate(id: string) {
    const session = data.sessions.find((item) => item.id === id);
    if (!session) return;
    setSessionId(id);
    setSubjectId(session.subjectId);
    setView("update");
  }

  function saveOutcome(outcome: OutcomeKind, note: string) {
    if (!subject) return;
    const targetSession = data.sessions.find((session) => session.id === sessionId);
    if (!targetSession) return;
    const finishedStatus = outcome === "not-covered" ? "missed" : "completed";
    const record = {
      id: `outcome-${Date.now()}`,
      sessionId: targetSession.id,
      outcome,
      note: note.trim(),
      savedAt: new Date().toISOString(),
    };
    const proposal = outcome === "completed" ? null : buildAdjustmentProposal(subject, data.sessions, targetSession, outcome, blockedDates);
    setData((current) => ({
      ...current,
      sessions: current.sessions.map((session) => session.id === targetSession.id ? { ...session, status: finishedStatus } : session),
      outcomes: [...current.outcomes.filter((item) => item.sessionId !== targetSession.id), record],
      lastProposal: proposal,
    }));
    if (outcome !== "completed" && proposal) {
      setView("adjustment");
      showToast("Update saved. Review the suggested change before applying it.");
    } else {
      setView("home");
      showToast(outcome === "completed" ? "Class update saved." : "Update saved; no available follow-up slot was found.");
    }
  }

  function keepOriginalPlan() {
    setData((current) => ({ ...current, lastProposal: null }));
    setView("subject");
    showToast("The approved schedule was left unchanged.");
  }

  function applyAdjustment() {
    const proposal = data.lastProposal;
    if (!proposal) return;
    setData((current) => ({
      ...current,
      sessions: [
        ...current.sessions,
        ...proposal.proposedSessions.map((session) => ({ ...session, status: "approved" as const })),
      ],
      lastProposal: null,
    }));
    setView("calendar");
    showToast("Suggested follow-up added. All other sessions stayed unchanged.");
  }

  if (!ready) return <div className="app-loading" aria-label="Loading planner"><span className="loading-mark">✳</span></div>;
  if (!signedIn) return <LoginScreen onSuccess={handleLoginSuccess} onLogin={login} />;

  let content: ReactNode;
  if (view === "home") {
    content = <HomeScreen data={data} onNavigate={navigate} onSubject={(id) => selectSubject(id)} onUpdate={beginUpdate} search={search} />;
  } else if (view === "subjects") {
    content = <SubjectsScreen subjects={data.subjects} sessions={data.sessions} search={search} onCreate={() => { setDraft(subjectDraftDefaults()); setView("create"); }} onSubject={(id) => selectSubject(id)} />;
  } else if (view === "create") {
    content = <CreateSubjectScreen draft={draft} onChange={setDraft} onFinish={createSubject} onCancel={() => setView("subjects")} />;
  } else if (view === "subject" && subject) {
    content = <SubjectWorkspace subject={subject} sessions={data.sessions} outcomes={data.outcomes} onNavigate={navigate} onUpdate={beginUpdate} />;
  } else if (view === "syllabus" && subject) {
    content = <SyllabusScreen subject={subject} isExtracting={isExtracting} fileName={fileName} onFile={setFileName} onExtract={extractSyllabus} onSave={(units, topics) => updateSubject(subject.id, { units, topics, status: "planning" })} onNext={() => {
      if (!subject.topics.length) { showToast("Add or extract syllabus topics before building a pacing matrix."); return; }
      setView("pacing");
    }} onExit={() => setView("subject")} />;
  } else if (view === "pacing" && subject) {
    content = <PacingScreen subject={subject} onPatchTopic={(topicId, patch) => updateSubject(subject.id, { topics: subject.topics.map((topic) => topic.id === topicId ? { ...topic, ...patch } : topic) })} onNext={() => setView("schedule")} onBack={() => setView("syllabus")} />;
  } else if (view === "schedule" && subject) {
    content = <ScheduleScreen subject={subject} sessions={data.sessions.filter((session) => session.subjectId === subject.id)} onChangeSubject={(patch) => updateSubject(subject.id, patch)} onGenerate={generateProposal} onBack={() => setView("pacing")} onReview={() => setView("review")} />;
  } else if (view === "review" && subject) {
    content = <ReviewScreen subject={subject} sessions={data.sessions.filter((session) => session.subjectId === subject.id)} onApprove={approvePlan} onBack={() => setView("schedule")} onEdit={() => setView("schedule")} />;
  } else if (view === "calendar") {
    content = <CalendarScreen sessions={data.sessions} subjects={data.subjects} events={academicEvents} onUpdate={beginUpdate} onSubject={(id) => selectSubject(id)} />;
  } else if (view === "update" && subject) {
    const session = data.sessions.find((item) => item.id === sessionId);
    content = session ? <UpdateScreen subject={subject} session={session} onSave={saveOutcome} onCancel={() => setView("subject")} /> : <HomeScreen data={data} onNavigate={navigate} onSubject={(id) => selectSubject(id)} onUpdate={beginUpdate} search={search} />;
  } else if (view === "adjustment" && subject && data.lastProposal) {
    content = <AdjustmentScreen subject={subject} proposal={data.lastProposal} sessions={data.sessions} onApply={applyAdjustment} onKeep={keepOriginalPlan} />;
  } else if (view === "notifications") {
    content = <NotificationsScreen onNavigate={navigate} />;
  } else if (view === "settings") {
    content = <SettingsScreen onReset={() => { setData(initialData); setView("home"); showToast("Demo data reset."); }} onLogout={logout} />;
  } else if (view === "profile") {
    content = <ProfileScreen onLogout={logout} />;
  } else {
    content = <HomeScreen data={data} onNavigate={navigate} onSubject={(id) => selectSubject(id)} onUpdate={beginUpdate} search={search} />;
  }

  const globalNavView: View = ["home", "subjects", "calendar", "notifications", "settings"].includes(view)
    ? view
    : "subjects";

  return (
    <AppShell
      view={globalNavView}
      onNavigate={navigate}
      onProfile={() => setView("profile")}
      searchValue={search}
      onSearch={(query) => { setSearch(query); setView("subjects"); }}
      onHelp={() => showToast("Use the left navigation to move between your subjects, schedule, and calendar.")}
    >
      <div className="screen-transition" key={view}>{content}</div>
      {toast && <div className="toast" role="status">{toast}</div>}
    </AppShell>
  );
}

