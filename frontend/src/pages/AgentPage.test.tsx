import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AgentPage } from './AgentPage'
import { ToastProvider } from '../components/ToastProvider'

const api = vi.hoisted(() => ({ getAgentProposals: vi.fn(), getAgentAudit: vi.fn(), approveProposal: vi.fn(), rejectProposal: vi.fn(), undoProposal: vi.fn(), getProposalWhy: vi.fn() }))
vi.mock('../lib/api', () => api)
const proposal = { id: 'p-1', month: '2026-11', fromCategory: 'GROCERIES', fromCategoryName: 'Groceries', toCategory: 'FOOD_DINING', toCategoryName: 'Food and dining', amountPaise: 180000, status: 'PENDING', createdAt: '2026-11-12T10:00:00Z', appliedAt: null, expiresAt: '2026-11-19T10:00:00Z' }
function setup() { const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } }); return render(<QueryClientProvider client={client}><ToastProvider><MemoryRouter><AgentPage/></MemoryRouter></ToastProvider></QueryClientProvider>) }
beforeEach(() => { api.getAgentProposals.mockReset().mockResolvedValue([proposal]); api.getAgentAudit.mockReset().mockResolvedValue([]); api.approveProposal.mockReset().mockResolvedValue(proposal); api.rejectProposal.mockReset(); api.undoProposal.mockReset(); api.getProposalWhy.mockReset().mockResolvedValue({ proposalId: 'p-1', explanation: 'Groceries has room.' }) })
describe('AgentPage', () => {
  it('approves a suggestion and refreshes the proposals list', async () => {
    setup()
    expect((await screen.findAllByRole('button', { name: 'Groceries → Food and dining' })).length).toBeGreaterThan(0)
    fireEvent.click((await screen.findAllByRole('button', { name: 'Approve' }))[0])
    await waitFor(() => expect(api.approveProposal).toHaveBeenCalledWith('p-1'))
    await waitFor(() => expect(api.getAgentProposals).toHaveBeenCalledTimes(2))
  })

  it('approves proposals and shows API messages for expired proposals', async () => {
    api.approveProposal.mockRejectedValueOnce({ error: 'PROPOSAL_EXPIRED', message: 'This proposal has expired.' })
    setup()
    expect((await screen.findAllByRole('button', { name: 'Groceries → Food and dining' })).length).toBeGreaterThan(0)
    expect(screen.getAllByText('₹1,800').length).toBeGreaterThan(0)
    fireEvent.click((await screen.findAllByRole('button', { name: 'Approve' }))[0])
    expect(await screen.findByText('This proposal has expired.')).toBeInTheDocument()
    expect(screen.queryByText('PROPOSAL_EXPIRED')).not.toBeInTheDocument()
  })

  it('shows the changed-budgets undo note', async () => {
    api.getAgentProposals.mockResolvedValue([{ ...proposal, status: 'APPLIED', appliedAt: '2026-11-20T10:00:00Z' }])
    api.undoProposal.mockResolvedValue({ proposal: { ...proposal, status: 'UNDONE' }, budgetsChangedSinceApply: true })
    setup()
    fireEvent.click((await screen.findAllByRole('button', { name: 'Undo' }))[0])
    await waitFor(() => expect(api.undoProposal).toHaveBeenCalledWith('p-1'))
    expect(await screen.findByText('Undone. Your budgets had changed since, so the amounts were adjusted by the same move.')).toBeInTheDocument()
  })
})
