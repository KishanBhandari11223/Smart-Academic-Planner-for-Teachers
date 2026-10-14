import type { ButtonHTMLAttributes, ReactNode } from "react";
import { ArrowRight, Check, X } from "lucide-react";

type ButtonVariant = "primary" | "secondary" | "quiet" | "danger";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  icon?: ReactNode;
  arrow?: boolean;
}

export function Button({
  children,
  variant = "secondary",
  icon,
  arrow = false,
  className = "",
  ...props
}: ButtonProps) {
  return (
    <button className={`button button-${variant} ${className}`} {...props}>
      {icon}
      <span>{children}</span>
      {arrow && <ArrowRight size={15} aria-hidden="true" />}
    </button>
  );
}

export function PageHeading({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="page-heading">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
        {description && <p className="page-description">{description}</p>}
      </div>
      {action && <div className="heading-action">{action}</div>}
    </div>
  );
}

export function Tag({
  children,
  tone = "neutral",
}: {
  children: ReactNode;
  tone?: "neutral" | "sage" | "amber" | "rose";
}) {
  return <span className={`tag tag-${tone}`}>{children}</span>;
}

export function Progress({ value, label }: { value: number; label?: string }) {
  const safeValue = Math.max(0, Math.min(100, value));
  return (
    <div className="progress-wrap" aria-label={label ?? `${safeValue}% complete`}>
      <div className="progress-track">
        <span className="progress-value" style={{ width: `${safeValue}%` }} />
      </div>
      {label && <span className="progress-label">{label}</span>}
    </div>
  );
}

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className="empty-state">
      <span className="empty-mark" aria-hidden="true">✳</span>
      <h3>{title}</h3>
      <p>{description}</p>
      {action}
    </div>
  );
}

export function Modal({
  title,
  children,
  onClose,
}: {
  title: string;
  children: ReactNode;
  onClose: () => void;
}) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose();
    }}>
      <section className="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
        <div className="modal-heading">
          <h2 id="modal-title">{title}</h2>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Close dialog">
            <X size={17} />
          </button>
        </div>
        {children}
      </section>
    </div>
  );
}

export function SuccessMark({ label }: { label: string }) {
  return (
    <span className="success-mark">
      <Check size={13} aria-hidden="true" />
      {label}
    </span>
  );
}
