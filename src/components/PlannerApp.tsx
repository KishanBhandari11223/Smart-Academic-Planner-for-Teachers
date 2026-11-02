"use client";

import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { gsap } from "gsap";
import { useLocation, useNavigate } from "react-router-dom";
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
import { academicEvents } from "@/data/seed";
import {
  blockedDateSet,
  buildAdjustmentProposal,
  generateSchedule,
} from "@/lib/planning";
import { apiFetch, fetchPlannerWorkspace, responseError, savePlannerWorkspace } from "@/lib/api";
import { createEmptyAppData, isAppData, readStoredAppData, saveAppData } from "@/lib/storage";
import { accountDisplayName, accountInitials, parseAuthenticatedUser } from "@/lib/account";
import type { AppData, OutcomeKind, Subject, View } from "@/types";

type PlannerRoute = {
  view: View;
  subjectId?: string;
  sessionId?: string;
};

async function restorePlannerWorkspace(email: string): Promise<{ data: AppData; version: number }> {
  const response = await fetchPlannerWorkspace();
  if (response.status === 404) {
    const data = readStoredAppData(email) ?? createEmptyAppData();
    const version = await savePlannerWorkspace(data, null);
    return { data, version };
  }
  if (!response.ok) {
    throw new Error(await responseError(response, "Unable to load planner data."));
  }

  const body: unknown = await response.json();
  if (
    typeof body !== "object" ||
    body === null ||
    !("version" in body) ||
    typeof body.version !== "number" ||
    !Number.isInteger(body.version) ||
    !("data" in body) ||
    !isAppData(body.data)
  ) {
    throw new Error("The server returned an invalid planner workspace.");
  }
  return { data: body.data, version: body.version };
}

function plannerRouteFromPath(pathname: string): PlannerRoute | null {
  if (pathname === "/") return { view: "home" };
  if (pathname === "/subjects") return { view: "subjects" };
  if (pathname === "/subjects/new") return { view: "create" };
  if (pathname === "/calendar") return { view: "calendar" };
  if (pathname === "/adjustment") return { view: "adjustment" };
  if (pathname === "/profile") return { view: "profile" };
  if (pathname === "/notifications") return { view: "notifications" };
  if (pathname === "/settings") return { view: "settings" };

  const sessionMatch = /^\/calendar\/sessions\/([^/]+)\/update$/.exec(pathname);
  if (sessionMatch?.[1]) return { view: "update", sessionId: decodeURIComponent(sessionMatch[1]) };

  const subjectMatch = /^\/subjects\/([^/]+)(?:\/(syllabus|pacing|schedule|review))?$/.exec(pathname);
  if (subjectMatch?.[1]) {
    const subview = subjectMatch[2];
    const view: View = subview === "syllabus" || subview === "pacing" || subview === "schedule" || subview === "review"
      ? subview
      : "subject";
    return { view, subjectId: decodeURIComponent(subjectMatch[1]) };
  }

  return null;
}

function pathForPlannerView(view: View, subjectId: string, sessionId: string): string {
  const subjectPath = `/subjects/${encodeURIComponent(subjectId)}`;
  switch (view) {
    case "home": return "/";
    case "subjects": return "/subjects";
    case "subject": return subjectPath;
    case "create": return "/subjects/new";
    case "syllabus": return `${subjectPath}/syllabus`;
    case "pacing": return `${subjectPath}/pacing`;
    case "schedule": return `${subjectPath}/schedule`;
    case "review": return `${subjectPath}/review`;
    case "calendar": return "/calendar";
    case "update": return sessionId ? `/calendar/sessions/${encodeURIComponent(sessionId)}/update` : "/calendar";
    case "adjustment": return "/adjustment";
    case "profile": return "/profile";
    case "notifications": return "/notifications";
    case "settings": return "/settings";
  }
}

export function PlannerApp() {
  const location = useLocation();
  const routerNavigate = useNavigate();
  const initialRoute = plannerRouteFromPath(window.location.pathname);
  const [data, setData] = useState<AppData>(createEmptyAppData);
  const [signedIn, setSignedIn] = useState(false);
  const [userEmail, setUserEmail] = useState("");
  const [ready, setReady] = useState(false);
  const [workspaceReady, setWorkspaceReady] = useState(false);
  const [authError, setAuthError] = useState("");
  const [view, setView] = useState<View>(initialRoute?.view ?? "home");
  const [subjectId, setSubjectId] = useState(initialRoute?.subjectId ?? "subject-data-mining");
  const [sessionId, setSessionId] = useState(initialRoute?.sessionId ?? "");
  const [draft, setDraft] = useState<SubjectDraft>(subjectDraftDefaults());
  const [fileName, setFileName] = useState("");
  const [isExtracting, setIsExtracting] = useState(false);
  const [search, setSearch] = useState("");
  const [toast, setToast] = useState("");
  const workspaceVersion = useRef<number | null>(null);
  const persistedSnapshot = useRef("");
  const persistQueue = useRef<Promise<void>>(Promise.resolve());

  const userName = userEmail ? accountDisplayName(userEmail) : "";
  const userInitials = accountInitials(userName);
  const subject = useMemo(() => data.subjects.find((item) => item.id === subjectId) ?? data.subjects[0], [data.subjects, subjectId]);
  const blockedDates = useMemo(() => blockedDateSet(academicEvents), []);

  useEffect(() => {
    const route = plannerRouteFromPath(location.pathname);
    if (!route) return;
    setView(route.view);
    if (route.subjectId) setSubjectId(route.subjectId);
    if (route.sessionId) setSessionId(route.sessionId);
  }, [location.pathname]);

  useEffect(() => {
    const nextPath = pathForPlannerView(view, subjectId, sessionId);
    if (location.pathname !== nextPath) routerNavigate(nextPath);
  }, [location.pathname, routerNavigate, sessionId, subjectId, view]);

  useEffect(() => {
    let mounted = true;
    const frame = window.requestAnimationFrame(async () => {
      try {
        const response = await apiFetch("/api/auth/me");
        if (!mounted) return;
        if (response.ok) {
          const user = parseAuthenticatedUser(await response.json());
          const workspace = await restorePlannerWorkspace(user.email);
          if (!mounted) return;
          setData(workspace.data);
          setUserEmail(user.email);
          workspaceVersion.current = workspace.version;
          persistedSnapshot.current = JSON.stringify(workspace.data);
          setWorkspaceReady(true);
          setSignedIn(true);
        } else if (response.status !== 401) {
          setAuthError(await responseError(response, "Unable to verify your sign-in. Please refresh and try again."));
        }
      } catch (requestError) {
        if (mounted) {
          setAuthError(requestError instanceof Error
            ? requestError.message
            : "Unable to connect to the sign-in service.");
        }
      }

      if (mounted) {
        setReady(true);
      }
    });

    return () => {
      mounted = false;
      window.cancelAnimationFrame(frame);
    };
  }, []);

  useEffect(() => {
    if (ready && signedIn && userEmail) saveAppData(data, userEmail);
  }, [data, ready, signedIn, userEmail]);

  useEffect(() => {
    if (!ready || !signedIn || !workspaceReady) return;
    const snapshot = JSON.stringify(data);
    if (snapshot === persistedSnapshot.current) return;

    const timeout = window.setTimeout(() => {
      persistQueue.current = persistQueue.current
        .then(async () => {
          if (snapshot === persistedSnapshot.current) return;
          const version = await savePlannerWorkspace(data, workspaceVersion.current);
          workspaceVersion.current = version;
          persistedSnapshot.current = snapshot;
        })
        .catch((requestError: unknown) => {
          showToast(requestError instanceof Error
            ? `Planner data could not be saved: ${requestError.message}`
            : "Planner data could not be saved. Please try again.");
        });
    }, 350);
    return () => window.clearTimeout(timeout);
  }, [data, ready, signedIn, workspaceReady]);

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
    void restorePlannerWorkspace(userEmail)
      .then((workspace) => {
        setData(workspace.data);
        setUserEmail(userEmail);
        workspaceVersion.current = workspace.version;
        persistedSnapshot.current = JSON.stringify(workspace.data);
        setWorkspaceReady(true);
        setSignedIn(true);
        setAuthError("");
        setView("home");
        showToast(`Signed in as ${userEmail}`);
      })
      .catch((requestError: unknown) => {
        setAuthError(requestError instanceof Error
          ? requestError.message
          : "Unable to load planner data. Please try again.");
      });
  }

  async function logout() {
    try {
      const response = await apiFetch("/api/auth/logout", { method: "POST" });
      if (!response.ok) {
        const body: unknown = await response.json();
        const message = typeof body === "object" && body !== null && "error" in body && typeof body.error === "string"
          ? body.error
          : `The server rejected sign-out (${response.status}).`;
        throw new Error(message);
      }
    } catch (requestError) {
      showToast(requestError instanceof Error
        ? `Sign-out failed: ${requestError.message}`
        : "Sign-out failed. Please try again.");
      return;
    }
    setSignedIn(false);
    setWorkspaceReady(false);
    setUserEmail("");
    setView("home");
    showToast("You have signed out.");
  }

  function updateSubject(subjectKey: string, patch: Partial<Subject>) {
    setData((current) => ({
      ...current,
      subjects: current.subjects.map((item) => item.id === subjectKey ? { ...item, ...patch } : item),
    }));
  }

  function patchSession(sessionId: string, patch: Partial<import("@/types").TeachingSession>) {
    setData((current) => ({
      ...current,
      sessions: current.sessions.map((item) => item.id === sessionId ? { ...item, ...patch } : item),
    }));
  }

  function removeSession(sessionId: string) {
    setData((current) => ({
      ...current,
      sessions: current.sessions.filter((item) => item.id !== sessionId),
    }));
    showToast("Session excluded from plan.");
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
  if (!signedIn) return <LoginScreen onSuccess={handleLoginSuccess} initialError={authError} />;

  let content: ReactNode;
  if (view === "home") {
    content = <HomeScreen data={data} accountName={userName} onNavigate={navigate} onSubject={(id) => selectSubject(id)} onUpdate={beginUpdate} search={search} />;
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
    content = <ReviewScreen subject={subject} sessions={data.sessions.filter((session) => session.subjectId === subject.id)} onApprove={approvePlan} onBack={() => setView("schedule")} onEdit={() => setView("schedule")} onPatchSession={patchSession} onRemoveSession={removeSession} />;
  } else if (view === "calendar") {
    content = <CalendarScreen sessions={data.sessions} subjects={data.subjects} events={academicEvents} onUpdate={beginUpdate} onSubject={(id) => selectSubject(id)} />;
  } else if (view === "update" && subject) {
    const session = data.sessions.find((item) => item.id === sessionId);
    content = session ? <UpdateScreen subject={subject} session={session} onSave={saveOutcome} onCancel={() => setView("subject")} /> : <HomeScreen data={data} accountName={userName} onNavigate={navigate} onSubject={(id) => selectSubject(id)} onUpdate={beginUpdate} search={search} />;
  } else if (view === "adjustment" && subject && data.lastProposal) {
    content = <AdjustmentScreen subject={subject} proposal={data.lastProposal} sessions={data.sessions} onApply={applyAdjustment} onKeep={keepOriginalPlan} />;
  } else if (view === "notifications") {
    content = <NotificationsScreen onNavigate={navigate} />;
  } else if (view === "settings") {
    content = <SettingsScreen onReset={() => { setData(createEmptyAppData()); setView("home"); showToast("Planner data cleared."); }} onLogout={logout} />;
  } else if (view === "profile") {
    content = <ProfileScreen accountName={userName} accountEmail={userEmail} accountAvatar={userInitials} onLogout={logout} />;
  } else {
    content = <HomeScreen data={data} accountName={userName} onNavigate={navigate} onSubject={(id) => selectSubject(id)} onUpdate={beginUpdate} search={search} />;
  }

  const globalNavView: View = ["home", "subjects", "calendar", "notifications", "settings"].includes(view)
    ? view
    : "subjects";

  return (
    <AppShell
      view={globalNavView}
      accountName={userName}
      accountEmail={userEmail}
      accountAvatar={userInitials}
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
