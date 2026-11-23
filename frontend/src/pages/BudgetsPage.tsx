import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getBudgets, updateBudget } from '../lib/api'
import { formatPaise, rupeesToPaise } from '../lib/money'
import type { BudgetEntry } from '../types/api'
import { userMessage } from '../types/api'
import { ErrorState } from '../components/EmptyState'
import { Skeleton } from '../components/Skeleton'
import { useToast } from '../components/ToastProvider'
import { Button } from '../components/ui/button'
import { Input } from '../components/ui/input'
import { Label } from '../components/ui/label'

function currentMonth() { const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kolkata', year: 'numeric', month: '2-digit' }).formatToParts(new Date()); return `${parts.find(item => item.type === 'year')?.value}-${parts.find(item => item.type === 'month')?.value}` }
function monthsBefore(month: string) { const [year, number] = month.split('-').map(Number); return Array.from({ length: 12 }, (_, offset) => { const date = new Date(Date.UTC(year, number - 1 - offset, 1)); return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}` }) }
function monthName(month: string) { const [year, number] = month.split('-').map(Number); return new Intl.DateTimeFormat('en', { month: 'long', year: 'numeric', timeZone: 'Asia/Kolkata' }).format(new Date(Date.UTC(year, number - 1, 15))) }
function inputAmount(paise: number) { return `${Math.floor(paise / 100)}.${String(paise % 100).padStart(2, '0')}` }

export function BudgetsPage() {
  const current = useMemo(currentMonth, [])
  const [month, setMonth] = useState(current)
  const [editing, setEditing] = useState<string | null>(null)
  const [draft, setDraft] = useState('')
  const [message, setMessage] = useState('')
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const query = useQuery({ queryKey: ['budgets', month], queryFn: () => getBudgets(month) })
  const mutation = useMutation({ mutationFn: ({ code, value }: { code: string; value: number }) => updateBudget(code, month, value), onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['budgets'] }); await queryClient.invalidateQueries({ queryKey: ['dashboard'] }); await queryClient.invalidateQueries({ queryKey: ['forecast'] }); await queryClient.invalidateQueries({ queryKey: ['agent'] }); setEditing(null); setMessage(''); showToast('Budget saved') } })

  function beginEdit(item: BudgetEntry) { setEditing(item.category.code); setDraft(inputAmount(item.amountPaise ?? 0)); setMessage('') }
  function cancelEdit() { setEditing(null); setDraft(''); setMessage('') }
  async function save(code: string) { setMessage(''); try { await mutation.mutateAsync({ code, value: rupeesToPaise(draft, true) }) } catch (error) { setMessage(userMessage(error as Parameters<typeof userMessage>[0], 'We could not save this budget. Please try again.')) } }

  if (query.isLoading) return <div className="space-y-4" aria-label="Loading budgets"><Skeleton className="h-10 w-1/2"/>{Array.from({ length: 8 }, (_, i) => <Skeleton key={i} className="h-20 w-full"/>)}</div>
  if (query.isError) return <ErrorState message="We couldn’t load budgets. Check your connection and try again." onRetry={() => void query.refetch()} />
  const rows = query.data ?? []
  const totalBudget = rows.reduce((sum, item) => sum + (item.amountPaise ?? 0), 0)
  const totalSpent = rows.reduce((sum, item) => sum + (item.spentPaise ?? 0), 0)
  return <div className="mx-auto max-w-4xl space-y-6">
    <div className="flex flex-wrap items-end justify-between gap-4"><div><h1 className="h1">Budgets</h1><p className="mt-1 body text-secondary">Monthly limits for {monthName(month)}.</p></div><div className="flex items-center gap-3"><Label htmlFor="budget-month">Month</Label><select id="budget-month" className="h-10 rounded-control border border-border bg-canvas px-3 body" value={month} onChange={event => { setMonth(event.target.value); cancelEdit() }}>{monthsBefore(current).map(value => <option key={value} value={value}>{monthName(value)}</option>)}</select></div></div>
    <p className="body-strong">Total budget {formatPaise(totalBudget)} <span className="mx-2 text-muted">·</span> Total spent {formatPaise(totalSpent)}</p>
    {message && <p role="alert" className="small text-danger-text">{message}</p>}
    <div className="overflow-hidden rounded-card border border-border bg-canvas">{rows.map(item => {
      const spent = item.spentPaise ?? 0; const budget = item.amountPaise ?? 0; const used = budget > 0 ? Math.floor((spent * 100) / budget) : spent > 0 ? 100 : 0; const fill = spent > budget ? 'bg-danger-text' : used >= 80 ? 'bg-warning-text' : 'bg-accent'; const status = spent > budget ? `Over by ${formatPaise(spent - budget)}` : `${used}% used`; const statusColor = spent > budget ? 'text-danger-text' : used >= 80 ? 'text-warning-text' : 'text-muted'
      return <div key={item.category.code} className="grid gap-3 border-b border-border p-4 last:border-b-0 sm:grid-cols-[minmax(180px,1fr)_minmax(220px,2fr)_auto] sm:items-center sm:p-5">
        <div><p className="body-strong">{item.category.displayName}{item.category.isProtected && <span className="ml-2 small text-muted">Protected</span>}</p><p className="small text-secondary">{formatPaise(spent)} of {formatPaise(budget)} used</p></div>
        <div className="space-y-1"><div className="h-[6px] overflow-hidden rounded-full bg-subtle"><div className={`h-full ${fill}`} style={{ width: `${Math.max(0, Math.min(100, used))}%` }}/></div><p className={`small ${statusColor}`}>{status}</p></div>
        {editing === item.category.code ? <div className="flex flex-wrap items-end gap-2"><div><Label htmlFor={`budget-${item.category.code}`}>Amount (₹)</Label><Input id={`budget-${item.category.code}`} className="mt-1 w-32" value={draft} onChange={event => setDraft(event.target.value)}/></div><Button disabled={mutation.isPending} onClick={() => void save(item.category.code)}>Save</Button><Button variant="ghost" onClick={cancelEdit}>Cancel</Button></div> : <Button variant="ghost" className="justify-self-start sm:justify-self-end" onClick={() => beginEdit(item)}>Edit</Button>}
      </div>
    })}</div>
  </div>
}
