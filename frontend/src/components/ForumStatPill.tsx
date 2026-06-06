import type { MouseEventHandler, ReactNode } from 'react';

interface ForumStatPillProps {
  icon: ReactNode;
  value: number | string;
  label: string;
  active?: boolean;
  loading?: boolean;
  disabled?: boolean;
  onClick?: MouseEventHandler<HTMLButtonElement>;
}

export default function ForumStatPill({
  icon,
  value,
  label,
  active = false,
  loading = false,
  disabled = false,
  onClick,
}: ForumStatPillProps) {
  const className = [
    'forum-stat-pill',
    active ? 'is-active' : '',
    onClick ? 'is-clickable' : '',
    loading ? 'is-loading' : '',
  ]
    .filter(Boolean)
    .join(' ');

  if (onClick) {
    return (
      <button
        type="button"
        className={className}
        aria-label={`${label} ${value}`}
        aria-pressed={active}
        disabled={disabled || loading}
        onClick={onClick}
      >
        <span className="forum-stat-pill__icon">{icon}</span>
        <span className="forum-stat-pill__value">{value}</span>
      </button>
    );
  }

  return (
    <span className={className} aria-label={`${label} ${value}`}>
      <span className="forum-stat-pill__icon">{icon}</span>
      <span className="forum-stat-pill__value">{value}</span>
    </span>
  );
}
