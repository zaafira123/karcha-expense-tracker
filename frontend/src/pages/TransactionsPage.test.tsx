import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { TransactionsPage } from './TransactionsPage'
import { ToastProvider } from '../components/ToastProvider'

const api = vi.hoisted(() => ({ getTransactions: vi.fn(), deleteTransaction: vi.fn(), updateTransaction: vi.fn() }))
vi.mock('../lib/api', () => api)
const row = { id: 'tx-1', amountPaise: 45000, category: { code: 'FOOD_DINING', displayName: 'Food and dining', isProtected: false }, merchant: 'Cafe', occurredOn: '2026-11-12', note: null, source: 'MANUAL', createdAt: '2026-11-12T10:00:00Z' }
function setup(initialEntry = '/transactions') { const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } }); return render(<QueryClientProvider client={client}><ToastProvider><MemoryRouter initialEntries={[initialEntry]}><TransactionsPage/></MemoryRouter></ToastProvider></QueryClientProvider>) }
beforeEach(() => { api.getTransactions.mockReset().mockResolvedValue([row]); api.deleteTransaction.mockReset(); api.updateTransaction.mockReset() })
describe('TransactionsPage', () => {
  it('requires delete confirmation and filters by category', async () => {
    setup()
    expect((await screen.findAllByText('Cafe')).length).toBeGreaterThan(0)
    fireEvent.click(screen.getAllByRole('button', { name: 'Delete' })[0])
    expect(screen.getByText('Delete this expense of ₹450? This cannot be undone.')).toBeInTheDocument()
    expect(api.deleteTransaction).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Confirm delete' }))
    await waitFor(() => expect(api.deleteTransaction.mock.calls[0][0]).toBe('tx-1'))
    fireEvent.click(screen.getByRole('button', { name: 'Groceries' }))
    await waitFor(() => expect(api.getTransactions).toHaveBeenLastCalledWith(expect.any(String), 'GROCERIES'))
  })

  it('opens the month requested by the SMS import link', async () => {
    setup('/transactions?month=2026-08')
    expect(await screen.findAllByText('Cafe')).toHaveLength(2)
    expect(screen.getByLabelText('Month')).toHaveValue('2026-08')
    expect(api.getTransactions).toHaveBeenCalledWith('2026-08', undefined)
    expect(screen.getByText('Expenses recorded for August 2026.')).toBeInTheDocument()
  })
})
