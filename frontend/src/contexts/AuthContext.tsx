import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { authApi, refreshSession, setAccessToken, setTokenListener, setUnauthorizedHandler } from '../lib/api'
import type { UserProfile } from '../types/api'

type AuthContextValue = { user: UserProfile | null; accessToken: string | null; ready: boolean; sessionMessage: string; signIn: (email: string, password: string) => Promise<void>; createAccount: (email: string, password: string) => Promise<void>; signOut: () => Promise<void>; deleteAccount: (email: string) => Promise<void>; clearSessionMessage: () => void }
const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [accessToken, setAccessTokenState] = useState<string | null>(null)
  const [ready, setReady] = useState(false)
  const [sessionMessage, setSessionMessage] = useState('')

  const expire = useCallback(() => { setAccessToken(null); setUser(null); setSessionMessage('Your session has ended. Sign in to continue.') }, [])
  useEffect(() => { setTokenListener(setAccessTokenState); setUnauthorizedHandler(expire); return () => { setTokenListener(null); setUnauthorizedHandler(null) } }, [expire])
  useEffect(() => {
    let mounted = true
    refreshSession().then(() => authApi.me()).then(profile => { if (mounted) setUser(profile) }).catch(() => { setAccessToken(null) }).finally(() => { if (mounted) setReady(true) })
    return () => { mounted = false }
  }, [])

  const signIn = useCallback(async (email: string, password: string) => {
    const auth = await authApi.login(email, password)
    setAccessToken(auth.accessToken)
    setUser(await authApi.me())
  }, [])
  const createAccount = useCallback(async (email: string, password: string) => { await authApi.register(email, password); await signIn(email, password) }, [signIn])
  const signOut = useCallback(async () => { try { await authApi.logout() } finally { setAccessToken(null); setUser(null) } }, [])
  const deleteAccount = useCallback(async (email: string) => { await authApi.deleteAccount(email); setAccessToken(null); setUser(null) }, [])
  const value = useMemo(() => ({ user, accessToken, ready, sessionMessage, signIn, createAccount, signOut, deleteAccount, clearSessionMessage: () => setSessionMessage('') }), [user, accessToken, ready, sessionMessage, signIn, createAccount, signOut, deleteAccount])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() { const context = useContext(AuthContext); if (!context) throw new Error('useAuth must be used within AuthProvider'); return context }
