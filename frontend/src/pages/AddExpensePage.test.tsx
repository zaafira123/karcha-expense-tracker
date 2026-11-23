import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AddExpensePage } from './AddExpensePage'
import { ToastProvider } from '../components/ToastProvider'

const api = vi.hoisted(() => ({ parseExpense: vi.fn(), createTransaction: vi.fn() }))
vi.mock('../lib/api', () => api)
function setup() { const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } }); return render(<QueryClientProvider client={client}><ToastProvider><MemoryRouter><AddExpensePage/></MemoryRouter></ToastProvider></QueryClientProvider>) }
const parsed = { amountPaise: 45000, category: 'FOOD_DINING', categoryName: 'Food and dining', merchant: 'Pizza with friends', occurredOn: '2026-11-14', note: 'paid 450 for pizza with friends', source: 'TEXT', confidence: 'HIGH', needsReview: false, method: 'RULES' }
beforeEach(() => { api.parseExpense.mockReset(); api.createTransaction.mockReset() })
describe('AddExpensePage', () => {
  it('fills editable fields after a successful parse', async () => {
    api.parseExpense.mockResolvedValue(parsed)
    setup()
    fireEvent.change(screen.getByLabelText('What did you spend?'), { target: { value: 'paid 450 for pizza with friends' } })
    fireEvent.click(screen.getByRole('button', { name: 'Read' }))
    expect(await screen.findByLabelText('Amount (₹)')).toHaveValue('450.00')
    expect(screen.getByLabelText('Merchant')).toHaveValue('Pizza with friends')
    expect(screen.getByText('Parsed from your sentence. Check the amount before saving.')).toBeInTheDocument()
  })

  it('shows manual entry after a failed parse request', async () => {
    api.parseExpense.mockRejectedValue({ error: 'AI_UNAVAILABLE', message: 'failed' })
    setup()
    fireEvent.change(screen.getByLabelText('What did you spend?'), { target: { value: 'lunch' } })
    fireEvent.click(screen.getByRole('button', { name: 'Read' }))
    expect(await screen.findByText('We could not read that sentence. Enter the details below.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Save expense' })).toBeInTheDocument()
  })

  it('saves integer paise and rejects zero', async () => {
    api.parseExpense.mockResolvedValue(parsed)
    api.createTransaction.mockResolvedValue({ id: '1' })
    setup()
    fireEvent.change(screen.getByLabelText('What did you spend?'), { target: { value: 'paid 450 for pizza with friends' } })
    fireEvent.click(screen.getByRole('button', { name: 'Read' }))
    await screen.findByLabelText('Amount (₹)')
    fireEvent.change(screen.getByLabelText('Amount (₹)'), { target: { value: '450.75' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save expense' }))
    await waitFor(() => expect(api.createTransaction.mock.calls[0][0]).toEqual(expect.objectContaining({ amountPaise: 45075, source: 'TEXT' })))
    expect(screen.getByRole('status')).toHaveTextContent('Expense saved')

    api.createTransaction.mockClear()
    fireEvent.click(screen.getByRole('button', { name: 'Enter manually instead' }))
    fireEvent.change(screen.getByLabelText('Amount (₹)'), { target: { value: '0' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save expense' }))
    expect(api.createTransaction).not.toHaveBeenCalled()
    expect(screen.getByText('Enter an amount greater than ₹0.')).toBeInTheDocument()
  })
})
