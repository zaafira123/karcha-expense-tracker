import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as Dialog from '@radix-ui/react-dialog'
import { useAuth } from '../contexts/AuthContext'
import { getSettings, updateAgentCap } from '../lib/api'
import { formatPaise, rupeesToPaise } from '../lib/money'
import { userMessage } from '../types/api'
import { Button } from '../components/ui/button'
import { Card } from '../components/ui/card'
import { ErrorState } from '../components/EmptyState'
import { useToast } from '../components/ToastProvider'

export function SettingsPage() {
  const { user, signOut, deleteAccount } = useAuth()
  const navigate = useNavigate()
  const client = useQueryClient()
  const { showToast } = useToast()
  const settings = useQuery({ queryKey: ['settings'], queryFn: getSettings })
  const [amount, setAmount] = useState('')
  const [emailConfirmation, setEmailConfirmation] = useState('')
  const [message, setMessage] = useState('')
  const [deleteOpen, setDeleteOpen] = useState(false)
  useEffect(() => { if (settings.data) setAmount((settings.data.agentCapPaise / 100).toFixed(2).replace(/\.00$/, '')) }, [settings.data])
  const capMutation = useMutation({ mutationFn: () => updateAgentCap(rupeesToPaise(amount)), onSuccess: async () => { await client.invalidateQueries({ queryKey: ['settings'] }); await client.invalidateQueries({ queryKey: ['auth', 'me'] }); showToast('Maximum move saved'); setMessage('') }, onError: error => setMessage(userMessage(error as unknown as Parameters<typeof userMessage>[0], 'We could not save this setting. Please try again.')) })
  const deleteMutation = useMutation({ mutationFn: () => deleteAccount(emailConfirmation), onSuccess: () => { showToast('Account deleted'); navigate('/', { replace: true }) }, onError: error => setMessage(userMessage(error as unknown as Parameters<typeof userMessage>[0], 'We could not delete your account. Please try again.')) })
  async function logout() { setMessage(''); try { await signOut(); navigate('/', { replace: true }) } catch { setMessage('We could not sign you out. Please try again.') } }
  if (settings.isLoading) return <div aria-label="Loading settings" className="space-y-5"><div className="skeleton h-12 w-48"/><div className="skeleton h-48"/><div className="skeleton h-48"/></div>
  if (settings.isError || !settings.data) return <ErrorState message="We couldn’t load your settings. Check your connection and try again." onRetry={() => void settings.refetch()} />
  const email = settings.data.email || user?.email || ''
  return <div className="mx-auto max-w-3xl space-y-6">
    <div><h1 className="h1">Settings</h1><p className="mt-1 body text-secondary">Account and agent settings.</p></div>
    {message && <p role="alert" className="small text-danger-text">{message}</p>}
    <Card><h2 className="h2">Account</h2><div className="mt-4"><p className="small text-secondary">Email</p><p className="mt-1 body-strong">{email}</p></div><Button variant="secondary" className="mt-5" onClick={() => void logout()}>Sign out</Button></Card>
    <Card><h2 className="h2">Agent</h2><form className="mt-4 max-w-sm space-y-3" onSubmit={event => { event.preventDefault(); setMessage(''); try { const paise = rupeesToPaise(amount); if (paise < 50000 || paise > 1000000) throw new Error('Enter an amount from ₹500 to ₹10,000.'); capMutation.mutate() } catch (error) { setMessage((error as Error).message) } }}>
      <label className="small text-secondary" htmlFor="agent-cap">Maximum move per suggestion (₹)</label><input id="agent-cap" inputMode="decimal" className="h-10 w-full rounded-control border border-border bg-canvas px-3 body amount" value={amount} onChange={event => setAmount(event.target.value)} aria-describedby="agent-cap-help"/><p id="agent-cap-help" className="small text-muted">Choose an amount from ₹500 to ₹10,000. Current value: {formatPaise(settings.data.agentCapPaise)}.</p><Button type="submit" disabled={capMutation.isPending}>{capMutation.isPending ? 'Saving…' : 'Save maximum move'}</Button>
    </form></Card>
    <section className="border-t border-danger-text pt-6"><Button variant="danger" onClick={() => { setMessage(''); setEmailConfirmation(''); setDeleteOpen(true) }}>Delete account</Button></section>
    <Dialog.Root open={deleteOpen} onOpenChange={setDeleteOpen}><Dialog.Portal><Dialog.Overlay className="fixed inset-0 z-40 bg-primary/25"/><Dialog.Content className="fixed left-1/2 top-1/2 z-50 w-[min(440px,calc(100%-2rem))] -translate-x-1/2 -translate-y-1/2 rounded-card border border-border bg-canvas p-6"><Dialog.Title className="h2">Delete account</Dialog.Title><Dialog.Description className="mt-3 body text-secondary">Delete your account and all expenses? This cannot be undone.</Dialog.Description><form className="mt-5 space-y-4" onSubmit={event => { event.preventDefault(); setMessage(''); deleteMutation.mutate() }}><label htmlFor="confirm-email" className="small text-secondary">Type your email to confirm</label><input id="confirm-email" type="email" required autoComplete="email" className="h-10 w-full rounded-control border border-border bg-canvas px-3 body" value={emailConfirmation} onChange={event => setEmailConfirmation(event.target.value)}/>{message && <p role="alert" className="small text-danger-text">{message}</p>}<div className="flex justify-end gap-3"><Button type="button" variant="ghost" onClick={() => setDeleteOpen(false)}>Cancel</Button><Button type="submit" variant="danger" disabled={deleteMutation.isPending || emailConfirmation.trim().toLowerCase() !== email.toLowerCase()}>{deleteMutation.isPending ? 'Deleting…' : 'Delete account'}</Button></div></form></Dialog.Content></Dialog.Portal></Dialog.Root>
  </div>
}
