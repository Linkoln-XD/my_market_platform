import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import type { AuthSession } from './types';
import { fetchDemoToken } from './api';

const STORAGE_KEY = 'market.demo.auth';

type AuthContextValue = {
  session: AuthSession | null;
  loginAsUser: (subject: string) => Promise<void>;
  loginAsAdmin: (subject: string) => Promise<void>;
  logout: () => void;
  isAdmin: boolean;
};

const AuthContext = createContext<AuthContextValue | null>(null);

function loadStored(): AuthSession | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as AuthSession) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => loadStored());

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      isAdmin: session?.roles.includes('ADMIN') ?? false,
      async loginAsUser(subject) {
        const next = await fetchDemoToken(subject, ['USER']);
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next));
        setSession(next);
      },
      async loginAsAdmin(subject) {
        const next = await fetchDemoToken(subject, ['ADMIN']);
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next));
        setSession(next);
      },
      logout() {
        sessionStorage.removeItem(STORAGE_KEY);
        setSession(null);
      },
    }),
    [session],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth outside AuthProvider');
  }
  return ctx;
}
