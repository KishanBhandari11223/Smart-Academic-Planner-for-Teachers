"use client";

import type { ReactNode } from "react";
import {
  Bell,
  CalendarDays,
  CircleHelp,
  GraduationCap,
  Home,
  LibraryBig,
  Search,
  Settings,
} from "lucide-react";
import type { View } from "@/types";

const navigation: { id: View; label: string; icon: typeof Home }[] = [
  { id: "home", label: "Home", icon: Home },
  { id: "subjects", label: "My Subjects", icon: LibraryBig },
  { id: "calendar", label: "Calendar", icon: CalendarDays },
  { id: "notifications", label: "Notifications", icon: Bell },
  { id: "settings", label: "Settings", icon: Settings },
];

export function AppShell({
  children,
  view,
  onNavigate,
  onProfile,
  onSearch,
  searchValue,
  onHelp,
}: {
  children: ReactNode;
  view: View;
  onNavigate: (view: View) => void;
  onProfile: () => void;
  onSearch: (query: string) => void;
  searchValue: string;
  onHelp: () => void;
}) {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <button className="brand" type="button" onClick={() => onNavigate("home")} aria-label="Teaching Planner home">
          <span className="brand-icon"><GraduationCap size={19} strokeWidth={1.7} /></span>
          <span className="brand-copy"><strong>Teaching Planner</strong><small>Adaptive curriculum</small></span>
        </button>
        <div className="nav-caption">Instructional workspace</div>
        <nav className="side-nav" aria-label="Main navigation">
          {navigation.map(({ id, label, icon: Icon }) => (
            <button
              type="button"
              key={id}
              className={`nav-item ${view === id ? "nav-item-active" : ""}`}
              onClick={() => onNavigate(id)}
              aria-current={view === id ? "page" : undefined}
            >
              <Icon size={17} strokeWidth={1.8} />
              <span>{label}</span>
              {id === "notifications" && <i className="nav-dot" aria-label="Unread notification" />}
            </button>
          ))}
        </nav>
        <button className="profile-chip" type="button" onClick={onProfile}>
          <span className="avatar">KJ</span>
          <span className="profile-label"><strong>Kartikey Joshi</strong><small>Faculty · Active</small></span>
          <span className="profile-chevron">⌄</span>
        </button>
      </aside>
      <div className="app-column">
        <header className="topbar">
          <div className="term-label"><span className="status-dot" />Fall term 2026–27</div>
          <div className="topbar-tools">
            <label className="search-box">
              <Search size={15} aria-hidden="true" />
              <input
                value={searchValue}
                onChange={(event) => onSearch(event.target.value)}
                aria-label="Search subjects and sessions"
                placeholder="Search subjects, topics…"
              />
              <kbd>⌘ K</kbd>
            </label>
            <button type="button" className="icon-button help-button" onClick={onHelp} aria-label="Help">
              <CircleHelp size={17} />
            </button>
            <button type="button" className="avatar avatar-top" onClick={onProfile} aria-label="Open profile">KJ</button>
          </div>
        </header>
        <main className="main-content">{children}</main>
      </div>
      <nav className="mobile-nav" aria-label="Mobile navigation">
        {navigation.slice(0, 4).map(({ id, label, icon: Icon }) => (
          <button
            type="button"
            key={id}
            className={view === id ? "mobile-nav-active" : ""}
            onClick={() => onNavigate(id)}
            aria-current={view === id ? "page" : undefined}
          >
            <Icon size={18} />
            <span>{label}</span>
          </button>
        ))}
      </nav>
    </div>
  );
}
