import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { SmsImportPage } from './SmsImportPage'
import { ToastProvider } from '../components/ToastProvider'

const api = vi.hoisted(() => ({ parseSmsMessages: vi.fn(), saveSmsDrafts: vi.fn() }))
vi.mock('../lib/api', () => api)

function setup() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(<QueryClientProvider client={client}><ToastProvider><MemoryRouter><SmsImportPage /></MemoryRouter></ToastProvider></QueryClientProvider>)
}

const debit = { index: 0, type: 'DEBIT', amountPaise: 45000, merchant: 'Zomato', occurredOn: '2026-11-14', category: 'FOOD_DINING', categoryName: 'Food and dining', status: 'READY', reason: null, messageHash: 'a'.repeat(64), referenceHint: '928271', confidence: 'HIGH' }

beforeEach(() => {
  api.parseSmsMessages.mockReset()
  api.saveSmsDrafts.mockReset()
  vi.stubGlobal('crypto', { subtle: { digest: vi.fn(async () => new Uint8Array(32).buffer) } })
})

describe('SmsImportPage', () => {
  it('requires consent before messages can be entered and parsed', async () => {
    api.parseSmsMessages.mockResolvedValue({ drafts: [debit] })
    setup()
    expect(screen.getByLabelText(/Bank or card messages/)).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Read messages' })).toBeDisabled()
    fireEvent.click(screen.getByLabelText(/I understand these messages/))
    expect(screen.getByLabelText(/Bank or card messages/)).toBeEnabled()
    fireEvent.change(screen.getByLabelText(/Bank or card messages/), { target: { value: 'INR 450 debited at Zomato on 14-11-26' } })
    fireEvent.click(screen.getByRole('button', { name: 'Read messages' }))
    expect(await screen.findByLabelText('Amount (₹)')).toHaveValue('450.00')
    expect(api.parseSmsMessages.mock.calls[0][0]).toEqual(['INR 450 debited at Zomato on 14-11-26'])
  })

  it('saves selected debits as integer paise and refreshes the user with a toast', async () => {
    api.parseSmsMessages.mockResolvedValue({ drafts: [debit] })
    api.saveSmsDrafts.mockResolvedValue({ savedCount: 1, skipped: [] })
    setup()
    fireEvent.click(screen.getByLabelText(/I understand these messages/))
    fireEvent.change(screen.getByLabelText(/Bank or card messages/), { target: { value: 'INR 450 debited at Zomato on 14-11-26' } })
    fireEvent.click(screen.getByRole('button', { name: 'Read messages' }))
    await screen.findByLabelText('Amount (₹)')
    fireEvent.change(screen.getByLabelText('Amount (₹)'), { target: { value: '450.75' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save 1 expense' }))
    await waitFor(() => expect(api.saveSmsDrafts.mock.calls[0]?.[0]).toMatchObject({ drafts: [expect.objectContaining({ amountPaise: 45075, category: 'FOOD_DINING', type: 'DEBIT' })] }))
    expect(await screen.findByText('1 expense saved.')).toBeInTheDocument()
    expect(screen.getByText('Saved. Open the matching month below to view your transactions.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View transactions for November 2026' })).toHaveAttribute('href', '/transactions?month=2026-11')
  })

  it('does not let a credit draft be selected as an expense', async () => {
    api.parseSmsMessages.mockResolvedValue({ drafts: [{ ...debit, type: 'CREDIT', status: 'CREDIT' }] })
    setup()
    fireEvent.click(screen.getByLabelText(/I understand these messages/))
    fireEvent.change(screen.getByLabelText(/Bank or card messages/), { target: { value: 'INR 450 credited from employer on 14-11-26' } })
    fireEvent.click(screen.getByRole('button', { name: 'Read messages' }))
    expect(await screen.findByText('Credit: ₹450')).toBeInTheDocument()
    expect(screen.getByLabelText('Select draft 1')).toBeDisabled()
  })

  it('disables duplicate drafts and sends only ticked debit drafts', async () => {
    api.parseSmsMessages.mockResolvedValue({
      drafts: [
        debit,
        { ...debit, index: 1, merchant: 'Uber', status: 'DUPLICATE', reason: 'This message matches an expense already imported.' }
      ]
    })
    api.saveSmsDrafts.mockResolvedValue({ savedCount: 1, skipped: [] })
    setup()
    fireEvent.click(screen.getByLabelText(/I understand these messages/))
    fireEvent.change(screen.getByLabelText(/Bank or card messages/), {
      target: { value: 'INR 450 debited at Zomato on 14-11-26\n\nINR 300 debited at Uber on 13-11-26' }
    })
    fireEvent.click(screen.getByRole('button', { name: 'Read messages' }))

    expect(await screen.findByLabelText('Select draft 2')).toBeDisabled()
    fireEvent.click(screen.getByRole('button', { name: 'Save 1 expense' }))

    await waitFor(() => expect(api.saveSmsDrafts.mock.calls[0]?.[0]).toMatchObject({
      consentAccepted: true,
      drafts: [expect.objectContaining({ index: 0, type: 'DEBIT' })]
    }))
  })
})
