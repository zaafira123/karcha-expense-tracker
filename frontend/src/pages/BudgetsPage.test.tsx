import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { BudgetsPage } from './BudgetsPage'
import { ToastProvider } from '../components/ToastProvider'

const api = vi.hoisted(() => ({ getBudgets: vi.fn(), updateBudget: vi.fn() }))
vi.mock('../lib/api', () => api)
const budgets = [{ category: { code: 'FOOD_DINING', displayName: 'Food and dining', isProtected: false }, amountPaise: 600000, spentPaise: 420000 }]
function setup() { const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } }); return render(<QueryClientProvider client={client}><ToastProvider><MemoryRouter><BudgetsPage/></MemoryRouter></ToastProvider></QueryClientProvider>) }
beforeEach(() => { api.getBudgets.mockReset().mockResolvedValue(budgets); api.updateBudget.mockReset().mockResolvedValue(budgets[0]) })
describe('BudgetsPage', () => {
  it('saves rupee values as integer paise and cancel restores the old amount', async () => {
    setup()
    expect(await screen.findByText('Food and dining')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Edit' }))
    const input = screen.getByLabelText('Amount (₹)')
    fireEvent.change(input, { target: { value: '7000.25' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    await waitFor(() => expect(api.updateBudget).toHaveBeenCalledWith('FOOD_DINING', expect.any(String), 700025))
    fireEvent.click(screen.getByRole('button', { name: 'Edit' }))
    fireEvent.change(screen.getByLabelText('Amount (₹)'), { target: { value: '9000' } })
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(document.body.textContent).toContain('₹6,000')
  })
})
