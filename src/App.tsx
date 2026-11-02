import { Navigate, Route, Routes } from "react-router-dom";
import { PlannerApp } from "@/components/PlannerApp";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<PlannerApp />} />
      <Route path="/subjects/*" element={<PlannerApp />} />
      <Route path="/calendar/*" element={<PlannerApp />} />
      <Route path="/adjustment" element={<PlannerApp />} />
      <Route path="/profile" element={<PlannerApp />} />
      <Route path="/notifications" element={<PlannerApp />} />
      <Route path="/settings" element={<PlannerApp />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
