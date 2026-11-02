"use client";

import { BookOpenCheck, CalendarDays, Sparkles, Mail, KeyRound, ArrowLeft } from "lucide-react";
import { useState, useEffect, type FormEvent } from "react";
import { Button } from "@/components/ui";
import { parseAuthenticatedUser } from "@/lib/account";
import { apiFetch, responseError } from "@/lib/api";

interface LoginScreenProps {
  onSuccess: (email: string) => void;
  initialError?: string;
}

export function LoginScreen({ onSuccess, initialError = "" }: LoginScreenProps) {
  const [step, setStep] = useState<"email" | "verify">("email");
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [error, setError] = useState(initialError);
  const [info, setInfo] = useState("");
  const [loading, setLoading] = useState(false);
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setInterval(() => {
      setCooldown((prev) => Math.max(0, prev - 1));
    }, 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  useEffect(() => {
    setError(initialError);
  }, [initialError]);

  async function handleSendOtp(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const cleanEmail = email.trim();
    if (!cleanEmail) return;

    setError("");
    setInfo("");
    setLoading(true);

    try {
      const res = await apiFetch("/api/auth/send-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: cleanEmail }),
      });

      if (!res.ok) {
        setError(await responseError(res, `Failed to send verification code (${res.status}).`));
      } else {
        const data: unknown = await res.json();
        if (typeof data !== "object" || data === null || !("message" in data) || typeof data.message !== "string") {
          throw new Error("The server returned an invalid response when requesting a sign-in code.");
        }
        setStep("verify");
        setCooldown(60);
        setInfo(data.message);
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not send the verification code.");
    } finally {
      setLoading(false);
    }
  }

  async function handleVerifyOtp(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const cleanEmail = email.trim();
    const cleanOtp = otp.trim();

    if (!cleanOtp) return;

    setError("");
    setInfo("");
    setLoading(true);

    try {
      const res = await apiFetch("/api/auth/verify-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: cleanEmail, code: cleanOtp }),
      });

      if (!res.ok) {
        setError(await responseError(res, `Could not verify the sign-in code (${res.status}).`));
      } else {
        const user = parseAuthenticatedUser(await res.json());
        if (user.email.toLocaleLowerCase() !== cleanEmail.toLocaleLowerCase()) {
          throw new Error("The authenticated account does not match the email used for verification.");
        }
        onSuccess(user.email);
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not verify the sign-in code.");
    } finally {
      setLoading(false);
    }
  }

  async function handleResend() {
    if (cooldown > 0 || loading) return;
    const cleanEmail = email.trim();
    setError("");
    setInfo("");
    setLoading(true);

    try {
      const res = await apiFetch("/api/auth/send-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: cleanEmail }),
      });

      if (!res.ok) {
        setError(await responseError(res, `Failed to resend verification code (${res.status}).`));
      } else {
        setCooldown(60);
        setInfo("A new verification code has been sent.");
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not resend the sign-in code.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-page">
      <div className="login-art" aria-hidden="true">
        <div className="art-orbit orbit-one" />
        <div className="art-orbit orbit-two" />
        <div className="art-paper">
          <span className="paper-kicker">COURSE DESIGN · 2026–27</span>
          <h2>A thoughtful plan<br />for every class.</h2>
          <div className="paper-line" />
          <div className="paper-row"><span /><i /><b /></div>
          <div className="paper-row"><span /><i /><b /></div>
          <div className="paper-row"><span /><i /><b /></div>
          <div className="paper-note"><Sparkles size={14} /> Built around your teaching week</div>
        </div>
        <div className="art-caption">Make room for the teaching that matters.</div>
      </div>
      <section className="login-panel">
        <div className="login-brand"><span className="brand-icon"><BookOpenCheck size={19} /></span><span>Teaching Planner</span></div>
        <div className="login-content">
          <span className="eyebrow">Adaptive curriculum · Faculty workspace</span>
          <h1>Plan your term<br /><em>with intention.</em></h1>
          <p className="page-description">Bring your syllabus, teaching schedule, and classroom progress into one calm workspace.</p>

          {step === "email" ? (
            <form className="login-form" onSubmit={handleSendOtp}>
              <label className="field-label" htmlFor="login-email">Faculty email</label>
              <input
                id="login-email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="you@university.edu"
                required
                disabled={loading}
              />
              {error && <p className="form-error" role="alert">{error}</p>}
              {info && <p style={{ color: "var(--sage)", fontSize: "10px", marginTop: "4px" }} role="status">{info}</p>}
              <Button type="submit" variant="primary" className="login-submit" arrow disabled={loading} icon={<Mail size={15} />}>
                {loading ? "Sending code..." : "Send verification code"}
              </Button>
            </form>
          ) : (
            <form className="login-form" onSubmit={handleVerifyOtp}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "4px" }}>
                <label className="field-label" htmlFor="login-otp" style={{ margin: 0 }}>Verification code</label>
                <button
                  type="button"
                  onClick={() => { setStep("email"); setError(""); setInfo(""); }}
                  style={{ display: "inline-flex", alignItems: "center", gap: "4px", background: "none", border: "none", color: "var(--sage)", fontSize: "9px", cursor: "pointer" }}
                >
                  <ArrowLeft size={11} /> Change email ({email})
                </button>
              </div>
              <input
                id="login-otp"
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                pattern="\d{6}"
                value={otp}
                onChange={(event) => setOtp(event.target.value.replace(/\D/g, ""))}
                placeholder="Enter 6-digit code"
                required
                autoFocus
                disabled={loading}
              />
              {error && <p className="form-error" role="alert">{error}</p>}
              {info && <p style={{ color: "var(--sage)", fontSize: "10px", marginTop: "4px" }} role="status">{info}</p>}
              <Button type="submit" variant="primary" className="login-submit" arrow disabled={loading} icon={<KeyRound size={15} />}>
                {loading ? "Verifying..." : "Verify code & continue"}
              </Button>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "10px" }}>
                <button
                  type="button"
                  onClick={handleResend}
                  disabled={cooldown > 0 || loading}
                  style={{
                    background: "none",
                    border: "none",
                    color: cooldown > 0 ? "var(--muted)" : "var(--navy)",
                    cursor: cooldown > 0 ? "default" : "pointer",
                    fontSize: "10px",
                    textDecoration: cooldown > 0 ? "none" : "underline",
                  }}
                >
                  {cooldown > 0 ? `Resend code in ${cooldown}s` : "Resend code"}
                </button>
              </div>
            </form>
          )}

          <div className="demo-credentials">
            <span>PASSWORDLESS OTP</span>
            <code>Single-use email link</code>
          </div>

          <div className="login-footnotes">
            <span><CalendarDays size={14} /> Your availability, respected</span>
            <span><Sparkles size={14} /> Plans you stay in control of</span>
          </div>
        </div>
        <span className="login-legal">Single-use secure email OTP · HttpOnly session cookie</span>
      </section>
    </main>
  );
}
