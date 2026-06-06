import type { ReactNode } from 'react';

interface ForumStatPillProps {
  icon: ReactNode;
  value: number | string;
  label: string;
}

export default function ForumStatPill({ icon, value, label }: ForumStatPillProps) {
  return (
    <span className="forum-stat-pill" aria-label={`${label} ${value}`}>
      <span className="forum-stat-pill__icon">{icon}</span>
      <span className="forum-stat-pill__value">{value}</span>
    </span>
  );
}
