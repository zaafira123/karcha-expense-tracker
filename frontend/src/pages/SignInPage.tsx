import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Eye, EyeOff } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { Button } from '../components/ui/button'
import { Card } from '../components/ui/card'
import { Input } from '../components/ui/input'
import { Label } from '../components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs'
import type { ApiError } from '../types/api'

type PageState = { from?: { pathname?: string; search?: string }; message?: string }
export function SignInPage() {
  const { signIn, createAccount, clearSessionMessage } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const pageState = location.state as PageState | null
  const [tab, setTab] = useState('signin')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [emailError, setEmailError] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const redirect = () => { clearSessionMessage(); navigate(`${pageState?.from?.pathname ?? '/dashboard'}${pageState?.from?.search ?? ''}`, { replace: true }) }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setEmailError(''); setPasswordError('')
    const normalizedEmail = email.trim()
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) { setEmailError('Enter a valid email address.'); return }
    if (password.length < 8) { setPasswordError('Password must be at least 8 characters.'); return }
    setBusy(true)
    try {
      if (tab === 'create') await createAccount(normalizedEmail, password)
      else await signIn(normalizedEmail, password)
      redirect()
    } catch (caught) {
      const apiError = caught as ApiError
      if (apiError.error === 'EMAIL_ALREADY_EXISTS' || apiError.message?.toLowerCase().includes('already exists')) setEmailError('An account with this email already exists.')
      else if (apiError.error === 'INVALID_CREDENTIALS' || apiError.error === 'UNAUTHORIZED' || tab === 'signin') setPasswordError('Check your email and password and try again.')
      else if (apiError.fields?.email) setEmailError(apiError.fields.email)
      else if (apiError.fields?.password) setPasswordError(apiError.fields.password)
      else setError(apiError.message || 'Unable to create your account. Please try again.')
    } finally { setBusy(false) }
  }

  return <div className="min-h-screen bg-subtle">
    <header className="flex h-[72px] items-center justify-between border-b border-border bg-canvas px-6 md:px-10"><Link className="h2" to="/">Kharcha</Link><Link to="/signin" className="small text-accent">Sign in</Link></header>
    <main className="flex min-h-[calc(100vh-72px)] flex-col items-center justify-center px-4 py-10">
      <Card className="w-full max-w-[440px] p-8 max-sm:p-5">
        <Tabs value={tab} onValueChange={value => { setTab(value); setError(''); setEmailError(''); setPasswordError('') }}>
          <TabsList className="mb-6"><TabsTrigger value="signin">Sign in</TabsTrigger><TabsTrigger value="create">Create account</TabsTrigger></TabsList>
          <TabsContent value="signin"><h1 className="h2">Sign in to Kharcha</h1><p className="mt-1 small text-muted">Welcome back. Enter your account details.</p></TabsContent>
          <TabsContent value="create"><h1 className="h2">Create your account</h1><p className="mt-1 small text-muted">Start keeping track of your monthly expenses.</p></TabsContent>
          {pageState?.message && <p role="status" className="mt-4 rounded-control border border-border bg-subtle p-3 small text-secondary">{pageState.message}</p>}
          {error && <p role="alert" className="mt-4 rounded-control border border-danger-text bg-danger-bg p-3 small text-danger-text">{error}</p>}
          <form className="mt-6 space-y-4" onSubmit={submit} noValidate>
            <div><Label htmlFor="email">Email</Label><Input id="email" type="email" autoComplete="email" value={email} onChange={event => setEmail(event.target.value)} aria-invalid={!!emailError} aria-describedby={emailError ? 'email-error' : undefined} className={emailError ? 'border-danger-text' : ''} />{emailError && <p id="email-error" className="mt-1 small text-danger-text">{emailError}</p>}</div>
            <div><Label htmlFor="password">Password</Label><div className="relative mt-1"><Input id="password" type={showPassword ? 'text' : 'password'} autoComplete={tab === 'create' ? 'new-password' : 'current-password'} value={password} onChange={event => setPassword(event.target.value)} aria-invalid={!!passwordError} aria-describedby={passwordError ? 'password-error' : tab === 'create' ? 'password-hint' : undefined} className={`pr-16 ${passwordError ? 'border-danger-text' : ''}`} /><button type="button" className="absolute inset-y-0 right-2 min-w-12 text-small text-secondary" onClick={() => setShowPassword(value => !value)} aria-label={showPassword ? 'Hide password' : 'Show password'}>{showPassword ? <span className="flex items-center gap-1"><EyeOff size={16}/>Hide</span> : <span className="flex items-center gap-1"><Eye size={16}/>Show</span>}</button></div>{tab === 'create' && !passwordError && <p id="password-hint" className="mt-1 small text-muted">At least 8 characters</p>}{passwordError && <p id="password-error" className="mt-1 small text-danger-text">{passwordError}</p>}</div>
            <Button type="submit" disabled={busy} className="w-full">{busy ? 'Please wait…' : tab === 'create' ? 'Create account' : 'Sign in'}</Button>
          </form>
          <p className="mt-5 text-center small text-muted">{tab === 'create' ? <>Already have an account? <button type="button" className="font-medium text-accent underline" onClick={() => setTab('signin')}>Sign in</button></> : <>New to Kharcha? <button type="button" className="font-medium text-accent underline" onClick={() => setTab('create')}>Create an account</button></>}</p>
        </Tabs>
      </Card>
    </main>
  </div>
}
