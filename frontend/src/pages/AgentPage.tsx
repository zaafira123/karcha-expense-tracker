import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as Dialog from '@radix-ui/react-dialog'
import { approveProposal, getAgentAudit, getAgentProposals, getProposalWhy, rejectProposal, undoProposal } from '../lib/api'
import { formatPaise } from '../lib/money'
import type { AgentProposal } from '../types/api'
import { userMessage } from '../types/api'
import { AgentProposalActions } from '../components/AgentProposalActions'
import { EmptyState, ErrorState } from '../components/EmptyState'
import { Skeleton } from '../components/Skeleton'
import { useToast } from '../components/ToastProvider'
import { Button } from '../components/ui/button'

function currentMonth() { const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kolkata', year: 'numeric', month: '2-digit' }).formatToParts(new Date()); return `${parts.find(item => item.type === 'year')?.value}-${parts.find(item => item.type === 'month')?.value}` }
function dateLabel(date: string) { return new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'Asia/Kolkata' }).format(new Date(date)) }
function eventLabel(event: string) { return event.toLowerCase().replaceAll('_', ' ') }

export function AgentPage() {
  const month = useMemo(currentMonth, [])
  const [selected, setSelected] = useState<AgentProposal | null>(null)
  const [message, setMessage] = useState('')
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const proposalsQuery = useQuery({ queryKey: ['agent', 'proposals', month], queryFn: () => getAgentProposals({ month }) })
  const auditQuery = useQuery({ queryKey: ['agent', 'audit'], queryFn: getAgentAudit })
  const whyQuery = useQuery({ queryKey: ['agent', 'why', selected?.id], queryFn: () => getProposalWhy(selected!.id), enabled: !!selected })
  const actionMutation = useMutation({ mutationFn: async ({ action, id }: { action: 'approve' | 'reject' | 'undo'; id: string }) => action === 'approve' ? approveProposal(id) : action === 'reject' ? rejectProposal(id) : undoProposal(id), onSuccess: async (result, variables) => { await queryClient.invalidateQueries({ queryKey: ['agent'] }); await queryClient.invalidateQueries({ queryKey: ['budgets'] }); await queryClient.invalidateQueries({ queryKey: ['dashboard'] }); setMessage(''); if (variables.action === 'undo' && 'budgetsChangedSinceApply' in result && result.budgetsChangedSinceApply) showToast('Undone. Your budgets had changed since, so the amounts were adjusted by the same move.'); else showToast(variables.action === 'approve' ? 'Suggestion approved' : variables.action === 'reject' ? 'Suggestion rejected' : 'Suggestion undone') }, onError: error => setMessage(userMessage(error as unknown as Parameters<typeof userMessage>[0], 'The action could not be completed. Please try again.')) })

  if (proposalsQuery.isLoading || auditQuery.isLoading) return <div className="space-y-4" aria-label="Loading agent activity"><Skeleton className="h-10 w-1/2"/>{Array.from({ length: 6 }, (_, i) => <Skeleton key={i} className="h-14 w-full"/>)}</div>
  if (proposalsQuery.isError || auditQuery.isError) return <ErrorState message="We couldn’t load agent activity. Check your connection and try again." onRetry={() => { void proposalsQuery.refetch(); void auditQuery.refetch() }} />
  const proposals = proposalsQuery.data ?? []
  const audit = [...(auditQuery.data ?? [])].sort((a, b) => b.createdAt.localeCompare(a.createdAt))
  const act = (action: 'approve' | 'reject' | 'undo', proposal: AgentProposal) => { setMessage(''); actionMutation.mutate({ action, id: proposal.id }) }
  const openWhy = (proposal: AgentProposal) => setSelected(proposal)
  return <div className="space-y-8">
    <div><h1 className="h1">Agent activity</h1><p className="mt-1 body text-secondary">Review suggested budget moves and their history.</p></div>
    {message && <p role="alert" className="rounded-control border border-danger-text bg-danger-bg p-3 small text-danger-text">{message}</p>}
    <section><h2 className="h2 mb-4">Suggestions</h2>{proposals.length === 0 ? <div className="rounded-card border border-border bg-canvas p-6"><EmptyState text="No suggestions this month. Your budgets are on track."/></div> : <><div className="hidden overflow-x-auto rounded-card border border-border bg-canvas md:block"><table className="w-full border-collapse"><thead className="bg-subtle text-left caption text-secondary"><tr><th className="px-4 py-3">Date</th><th className="px-4 py-3">Move</th><th className="px-4 py-3 text-right">Amount</th><th className="px-4 py-3">Status</th><th className="px-4 py-3">Action</th></tr></thead><tbody className="divide-y divide-border">{proposals.map(proposal => <tr key={proposal.id} className="h-16"><td className="px-4 small text-muted">{dateLabel(proposal.createdAt)}</td><td className="px-4 body-strong"><button className="text-left underline decoration-transparent hover:decoration-current" onClick={() => openWhy(proposal)}>{proposal.fromCategoryName} → {proposal.toCategoryName}</button></td><td className="px-4 text-right body-strong tabular-nums">{formatPaise(proposal.amountPaise)}</td><td className="px-4 body text-secondary">{eventLabel(proposal.status)}</td><td className="px-4"><AgentProposalActions proposal={proposal} busy={actionMutation.isPending} onApprove={() => act('approve', proposal)} onReject={() => act('reject', proposal)} onUndo={() => act('undo', proposal)} onWhy={() => openWhy(proposal)}/></td></tr>)}</tbody></table></div><div className="space-y-3 md:hidden">{proposals.map(proposal => <article key={proposal.id} className="rounded-card border border-border bg-canvas p-4"><div className="flex items-start justify-between gap-3"><div><p className="small text-muted">{dateLabel(proposal.createdAt)}</p><button className="mt-1 min-h-10 text-left body-strong underline" onClick={() => openWhy(proposal)}>{proposal.fromCategoryName} → {proposal.toCategoryName}</button></div><p className="body-strong tabular-nums">{formatPaise(proposal.amountPaise)}</p></div><p className="mt-1 small capitalize text-secondary">Status: {eventLabel(proposal.status)}</p><div className="mt-3"><AgentProposalActions proposal={proposal} busy={actionMutation.isPending} onApprove={() => act('approve', proposal)} onReject={() => act('reject', proposal)} onUndo={() => act('undo', proposal)} onWhy={() => openWhy(proposal)}/></div></article>)}</div></>}</section>
    <section><h2 className="h2 mb-4">Audit log</h2>{audit.length ? <div className="divide-y divide-border rounded-card border border-border bg-canvas px-4">{audit.map(event => <p key={event.id} className="py-3 small text-secondary"><time className="mr-3 text-muted" dateTime={event.createdAt}>{dateLabel(event.createdAt)}</time><span className="font-medium capitalize">{eventLabel(event.event)}</span>{event.detail ? ` · ${event.detail}` : ''}</p>)}</div> : <p className="rounded-card border border-border bg-canvas p-4 small text-muted">No agent activity recorded yet.</p>}</section>
    <Dialog.Root open={!!selected} onOpenChange={open => !open && setSelected(null)}><Dialog.Portal><Dialog.Overlay className="fixed inset-0 z-40 bg-primary/25"/><Dialog.Content className="fixed inset-y-0 right-0 z-50 w-[400px] max-w-full overflow-y-auto border-l border-border bg-canvas p-6 max-sm:w-full"><Dialog.Title className="h2">Why this suggestion?</Dialog.Title><Dialog.Description className="mt-2 small text-muted">{selected ? `${selected.fromCategoryName} → ${selected.toCategoryName} · ${formatPaise(selected.amountPaise)}` : ''}</Dialog.Description>{whyQuery.isLoading ? <Skeleton className="mt-6 h-16 w-full"/> : whyQuery.isError ? <p role="alert" className="mt-6 small text-danger-text">{userMessage(whyQuery.error as unknown as Parameters<typeof userMessage>[0])}</p> : <p className="mt-6 body text-secondary">{whyQuery.data?.explanation}</p>}{selected?.status === 'PENDING' && <div className="mt-6"><AgentProposalActions proposal={selected} busy={actionMutation.isPending} onApprove={() => act('approve', selected)} onReject={() => act('reject', selected)}/></div>}<Button variant="ghost" className="mt-6" onClick={() => setSelected(null)}>Close</Button></Dialog.Content></Dialog.Portal></Dialog.Root>
  </div>
}
