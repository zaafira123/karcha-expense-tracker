import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { approveProposal, getAgentProposals, getDashboard, getForecast, getProposalWhy, getWeeklySummary, getMonthlySummary, rejectProposal } from '../lib/api'
import { formatPaise } from '../lib/money'
import { Button } from '../components/ui/button'
import { Card } from '../components/ui/card'
import { DashboardSkeleton } from '../components/Skeleton'
import { EmptyState, ErrorState } from '../components/EmptyState'
import type { CategoryProgress } from '../types/api'
import type { AgentProposal } from '../types/api'
import { AgentProposalActions } from '../components/AgentProposalActions'
import { useToast } from '../components/ToastProvider'
import { userMessage } from '../types/api'
import * as Dialog from '@radix-ui/react-dialog'

function currentMonth() { const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kolkata', year: 'numeric', month: '2-digit' }).formatToParts(new Date()); return `${parts.find(part => part.type === 'year')?.value}-${parts.find(part => part.type === 'month')?.value}` }
function monthLabel(month: string) { const [year, number] = month.split('-').map(Number); return new Intl.DateTimeFormat('en', { month: 'long', year: 'numeric', timeZone: 'Asia/Kolkata' }).format(new Date(Date.UTC(year, number - 1, 15))) }
function dateLabel(date: string) { return new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'Asia/Kolkata' }).format(new Date(`${date}T12:00:00+05:30`)) }

function CategoryRow({ item }: { item: CategoryProgress }) {
  const percent = Math.max(0, Math.min(item.percentUsed, 100))
  const over = item.status === 'OVER'
  const warning = item.status === 'WARNING'
  const fillColor = over ? 'bg-danger-text' : warning ? 'bg-warning-text' : 'bg-accent'
  const statusText = over ? `Over by ${formatPaise((item.spentPaise ?? 0) - (item.budgetPaise ?? 0))}` : `${item.percentUsed}% used`
  const statusColor = over ? 'text-danger-text' : warning ? 'text-warning-text' : 'text-muted'
  return <div className="border-b border-border pb-4 last:border-0 last:pb-0">
    <div className="mb-2 flex items-center justify-between gap-3"><span className="body text-primary">{item.category.displayName}{item.category.isProtected && <span className="ml-2 small text-muted">Protected</span>}</span><span className="body-strong amount whitespace-nowrap">{formatPaise(item.spentPaise ?? 0)} <span className="font-normal text-muted">/ {formatPaise(item.budgetPaise ?? 0)}</span></span></div>
    <div className="h-[6px] overflow-hidden rounded-full bg-subtle"><div className={`h-full rounded-full ${fillColor}`} style={{ width: `${percent}%` }}/></div>
    <p className={`mt-1 small ${statusColor}`}>{statusText}</p>
  </div>
}

export function DashboardPage() {
  const navigate = useNavigate()
  const client = useQueryClient()
  const { showToast } = useToast()
  const current = useMemo(currentMonth, [])
  const [month, setMonth] = useState(current)
  const months = useMemo(() => Array.from({ length: 12 }, (_, index) => { const [year, mon] = current.split('-').map(Number); const date = new Date(Date.UTC(year, mon - 1 - index, 1)); return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}` }), [current])
  const query = useQuery({ queryKey: ['dashboard', month], queryFn: () => getDashboard(month) })
  const forecastQuery = useQuery({ queryKey: ['forecast', month], queryFn: () => getForecast(month) })
  const proposalQuery = useQuery({ queryKey: ['agent', 'proposals', 'PENDING'], queryFn: () => getAgentProposals({ status: 'PENDING' }) })
  const weeklyQuery = useQuery({ queryKey: ['summary', 'weekly'], queryFn: getWeeklySummary })
  const monthlyQuery = useQuery({ queryKey: ['summary', 'monthly', month], queryFn: () => getMonthlySummary(month) })
  const [selectedProposal, setSelectedProposal] = useState<AgentProposal | null>(null)
  const [actionMessage, setActionMessage] = useState('')
  const whyQuery = useQuery({ queryKey: ['agent', 'why', selectedProposal?.id], queryFn: () => getProposalWhy(selectedProposal!.id), enabled: !!selectedProposal })
  const proposalMutation = useMutation({ mutationFn: async ({ action, id }: { action: 'approve'|'reject'; id: string }) => action === 'approve' ? approveProposal(id) : rejectProposal(id), onSuccess: async (_, variables) => { await client.invalidateQueries({ queryKey: ['agent'] }); await client.invalidateQueries({ queryKey: ['dashboard'] }); await client.invalidateQueries({ queryKey: ['budgets'] }); showToast(variables.action === 'approve' ? 'Suggestion approved' : 'Suggestion rejected'); setActionMessage('') }, onError: error => setActionMessage(userMessage(error as unknown as Parameters<typeof userMessage>[0], 'The action could not be completed. Please try again.')) })
  if (query.isLoading) return <DashboardSkeleton />
  if (query.isError || !query.data) return <ErrorState message="We couldn’t load your dashboard. Check your connection and try again." onRetry={() => void query.refetch()} />
  const data = query.data
  const suggested = proposalQuery.data?.[0]
  return <>
    {proposalQuery.isError && <div className="mb-6"><ErrorState message="We couldn’t load your pending suggestions." onRetry={() => void proposalQuery.refetch()}/></div>}
    {proposalQuery.isLoading && <section aria-label="Loading suggested move" className="mb-6 rounded-card border border-border p-5"><div className="skeleton mb-3 h-4 w-36"/><div className="skeleton mb-2 h-6 w-2/3"/><div className="skeleton h-10 w-48"/></section>}
    {suggested && <section className="mb-6 rounded-card border border-border bg-accent-soft p-5"><div className="flex flex-wrap items-start justify-between gap-4"><div><p className="caption text-accent">SUGGESTED MOVE</p><h2 className="mt-1 h2">Move {formatPaise(suggested.amountPaise)} from {suggested.fromCategoryName} to {suggested.toCategoryName}</h2><p className="mt-1 small text-secondary">Review the reason before deciding.</p></div><AgentProposalActions proposal={suggested} busy={proposalMutation.isPending} onApprove={() => proposalMutation.mutate({ action: 'approve', id: suggested.id })} onReject={() => proposalMutation.mutate({ action: 'reject', id: suggested.id })} onWhy={() => setSelectedProposal(suggested)}/></div>{actionMessage && <p role="alert" className="mt-3 small text-danger-text">{actionMessage}</p>}</section>}
    <section aria-label="Weekly summary" className="mb-6"><Card><div className="flex flex-wrap items-start justify-between gap-4"><div><h2 className="h2">Weekly summary</h2>{weeklyQuery.isLoading ? <div aria-label="Loading weekly summary" className="skeleton mt-3 h-5 w-64"/> : weeklyQuery.isError || !weeklyQuery.data ? <p className="mt-2 small text-danger-text">We couldn’t load your weekly summary.</p> : <><p className="mt-2 body text-secondary">{weeklyQuery.data.summaryText}</p><p className="mt-1 small text-muted">This week {formatPaise(weeklyQuery.data.currentWeekSpentPaise)} · Previous week {formatPaise(weeklyQuery.data.previousWeekSpentPaise)}</p>{weeklyQuery.data.topCategories.length > 0 && <p className="mt-2 small text-secondary">Top categories: {weeklyQuery.data.topCategories.map(item => `${item.categoryName} ${formatPaise(item.amountPaise)}`).join(' · ')}</p>}{weeklyQuery.data.budgetsOver80.length > 0 && <p className="mt-2 small text-warning-text">Over 80% of budget: {weeklyQuery.data.budgetsOver80.map(item => `${item.categoryName} (${item.percentUsed}%)`).join(' · ')}</p>}<ul className="mt-3 list-disc space-y-1 pl-5 small text-secondary">{(weeklyQuery.data.tips ?? []).map(tip => <li key={tip}>{tip}</li>)}</ul></>}</div>{weeklyQuery.isError && <Button variant="secondary" onClick={() => void weeklyQuery.refetch()}>Try again</Button>}</div></Card></section>
    <section aria-label="Monthly summary" className="mb-6"><Card><div className="flex flex-wrap items-start justify-between gap-4"><div><h2 className="h2">Monthly summary</h2>{monthlyQuery.isLoading ? <div aria-label="Loading monthly summary" className="skeleton mt-3 h-5 w-64"/> : monthlyQuery.isError || !monthlyQuery.data ? <p className="mt-2 small text-danger-text">We couldn’t load your monthly summary.</p> : <><p className="mt-2 body text-secondary">{monthlyQuery.data.summaryText}</p>{monthlyQuery.data.topCategories.length > 0 && <p className="mt-1 small text-muted">Top categories: {monthlyQuery.data.topCategories.map(item => `${item.categoryName} ${formatPaise(item.amountPaise)}`).join(' · ')}</p>}<ul className="mt-3 list-disc space-y-1 pl-5 small text-secondary">{(monthlyQuery.data.tips ?? []).map(tip => <li key={tip}>{tip}</li>)}</ul></>}</div>{monthlyQuery.isError && <Button variant="secondary" onClick={() => void monthlyQuery.refetch()}>Try again</Button>}</div></Card></section>
    <div className="mb-6 flex flex-wrap items-center justify-between gap-4"><div className="flex items-center gap-3"><h1 className="h1">{monthLabel(month)}</h1><label className="sr-only" htmlFor="dashboard-month">Choose month</label><select id="dashboard-month" aria-label="Choose month" className="h-10 max-w-[150px] rounded-control border border-border bg-canvas px-3 small" value={month} onChange={event => setMonth(event.target.value)}>{months.map(value => <option value={value} key={value}>{monthLabel(value)}</option>)}</select></div><Link to="/add"><Button>Add expense</Button></Link></div>
    <div className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <Stat label="Spent this month" value={formatPaise(data.spentPaise ?? 0)} note={data.totalBudgetPaise ? `${Math.floor(((data.spentPaise ?? 0) * 100) / data.totalBudgetPaise)}% of budget` : 'No budget set'} />
      <Stat label="Budget remaining" value={formatPaise(data.remainingPaise ?? 0)} note="Across 8 categories" />
      <Stat label="Projected month end" value={forecastQuery.data?.projectedPaise == null ? '—' : formatPaise(forecastQuery.data.projectedPaise)} note={forecastQuery.isError ? 'Forecast unavailable' : forecastQuery.data?.status === 'OVER_BUDGET_PROJECTED' ? 'Projected over budget' : forecastQuery.data ? 'Based on recorded spending' : 'Loading forecast'} />
      <Stat label="Days left" value={String(data.daysLeft)} note={`Until ${monthLabel(month)}`} />
    </div>
    <div className="grid gap-6 lg:grid-cols-12">
      <Card className="space-y-5 lg:col-span-7"><h2 className="h2">Spending by category</h2>{data.categories.length ? <div className="space-y-4">{data.categories.map(item => <CategoryRow key={item.category.code} item={item}/>)}</div> : <EmptyState text="No expenses recorded for this month yet." action="Add expense" onAction={() => navigate('/add')} />}</Card>
      <Card className="flex flex-col lg:col-span-5"><h2 className="h2 mb-4">Recent transactions</h2>{data.recent.length ? <div className="divide-y divide-border">{data.recent.map(item => <div key={item.id} className="flex items-center justify-between gap-3 py-4"><div className="min-w-0"><p className="body-strong truncate">{item.merchant}</p><p className="small text-muted">{dateLabel(item.occurredOn)} · {item.category.displayName}</p></div><span className="body-strong amount whitespace-nowrap">{formatPaise(item.amountPaise)}</span></div>)}</div> : <EmptyState text="No expenses recorded for this month yet." action="Add expense" onAction={() => navigate('/add')} />}<Link className="mt-auto pt-4 body-strong text-accent underline" to="/transactions">View all transactions</Link></Card>
    </div>
    <Dialog.Root open={!!selectedProposal} onOpenChange={open => !open && setSelectedProposal(null)}><Dialog.Portal><Dialog.Overlay className="fixed inset-0 z-40 bg-primary/25"/><Dialog.Content className="fixed inset-y-0 right-0 z-50 w-[400px] max-w-full overflow-y-auto border-l border-border bg-canvas p-6 max-sm:w-full"><Dialog.Title className="h2">Why this suggestion?</Dialog.Title><Dialog.Description className="mt-2 body text-secondary">{selectedProposal ? `${selectedProposal.fromCategoryName} → ${selectedProposal.toCategoryName} · ${formatPaise(selectedProposal.amountPaise)}` : ''}</Dialog.Description>{whyQuery.isLoading ? <p className="mt-6 small text-muted">Loading explanation…</p> : whyQuery.isError ? <div className="mt-6 space-y-3"><p role="alert" className="small text-danger-text">{userMessage(whyQuery.error as unknown as Parameters<typeof userMessage>[0])}</p><Button variant="secondary" onClick={() => void whyQuery.refetch()}>Try again</Button></div> : <p className="mt-6 body text-secondary">{whyQuery.data?.explanation}</p>}{selectedProposal && <div className="mt-6"><AgentProposalActions proposal={selectedProposal} busy={proposalMutation.isPending} onApprove={() => proposalMutation.mutate({ action: 'approve', id: selectedProposal.id })} onReject={() => proposalMutation.mutate({ action: 'reject', id: selectedProposal.id })}/></div>}</Dialog.Content></Dialog.Portal></Dialog.Root>
  </>
}
function Stat({ label, value, note }: { label: string; value: string; note: string }) { return <Card className="flex min-h-[132px] flex-col gap-1"><span className="small text-secondary">{label}</span><span className="display amount">{value}</span><span className="caption text-muted">{note}</span></Card> }
