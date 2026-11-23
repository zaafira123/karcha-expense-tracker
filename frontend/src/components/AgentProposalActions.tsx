import { Button } from './ui/button'
import type { AgentProposal } from '../types/api'

export function AgentProposalActions({ proposal, onApprove, onReject, onUndo, onWhy, busy = false }: { proposal: AgentProposal; onApprove?: () => void; onReject?: () => void; onUndo?: () => void; onWhy?: () => void; busy?: boolean }) {
  const pending = proposal.status === 'PENDING'
  const applied = proposal.status === 'APPLIED'
  const undoDate = proposal.appliedAt ? new Date(new Date(proposal.appliedAt).getTime() + 7 * 24 * 60 * 60 * 1000) : null
  const undoAvailable = !!undoDate && undoDate.getTime() > Date.now()
  return <div className="flex flex-wrap items-center gap-2">
    {pending && <><Button className="px-3 text-small" disabled={busy} onClick={onApprove}>Approve</Button><Button variant="ghost" className="px-3 text-small" disabled={busy} onClick={onReject}>Reject</Button></>}
    {applied && onUndo && undoAvailable && <><Button variant="ghost" className="px-3 text-small" disabled={busy} onClick={onUndo}>Undo</Button><span className="small text-muted">Undo until {new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'Asia/Kolkata' }).format(undoDate!)}</span></>}
    {onWhy && <Button variant="ghost" className="px-3 text-small" onClick={onWhy}>Why?</Button>}
  </div>
}
