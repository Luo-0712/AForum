import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

interface AuthShellProps {
  title: string;
  subtitle: string;
  children: ReactNode;
}

export default function AuthShell({ title, subtitle, children }: AuthShellProps) {
  return (
    <main className="auth-shell">
      <Link className="auth-shell__brand" to="/">
        AForum
      </Link>
      <section className="auth-shell__panel">
        <div className="auth-shell__visual" aria-hidden="true">
          <span />
          <span />
          <span />
        </div>
        <div className="auth-shell__card">
          <div className="auth-shell__heading">
            <span className="eyebrow">AForum</span>
            <h1>{title}</h1>
            <p>{subtitle}</p>
          </div>
          {children}
        </div>
      </section>
    </main>
  );
}
