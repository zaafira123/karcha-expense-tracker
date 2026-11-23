import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { ToastProvider } from '../components/ToastProvider'
import { SettingsPage } from './SettingsPage'

const api = vi.hoisted(() => ({ getSettings: vi.fn(), updateAgentCap: vi.fn() }))
const auth = vi.hoisted(() => ({ signOut: vi.fn(), deleteAccount: vi.fn() }))
vi.mock('../lib/api', () => api)
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => ({ user: { id: 'u-1', email: 'person@example.com' }, ...auth }) }))
function setup() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={client}><ToastProvider><MemoryRouter><SettingsPage/></MemoryRouter></ToastProvider></QueryClientProvider>)
}
describe('SettingsPage', () => {
  it('loads the account and agent cap from the API', async () => {
    api.getSettings.mockResolvedValue({ email: 'person@example.com', agentCapPaise: 200000 })
    setup()
    expect(await screen.findByText('person@example.com')).toBeInTheDocument()
    expect(screen.getByLabelText('Maximum move per suggestion (₹)')).toHaveValue('2000')
    expect(screen.queryByText('Coming soon')).not.toBeInTheDocument()
  })

  it('saves the rupee cap as integer paise', async () => {
    api.getSettings.mockResolvedValue({ email: 'person@example.com', agentCapPaise: 200000 })
    api.updateAgentCap.mockResolvedValue({ agentCapPaise: 250050 })
    setup()
    const input = await screen.findByLabelText('Maximum move per suggestion (₹)')
    fireEvent.change(input, { target: { value: '2500.50' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save maximum move' }))
    await waitFor(() => expect(api.updateAgentCap).toHaveBeenCalledWith(250050))
    expect(await screen.findByText('Maximum move saved')).toBeInTheDocument()
  })

  it('requires matching email before deleting the account', async () => {
    api.getSettings.mockResolvedValue({ email: 'person@example.com', agentCapPaise: 200000 })
    auth.deleteAccount.mockResolvedValue(undefined)
    setup()
    fireEvent.click(await screen.findByRole('button', { name: 'Delete account' }))
    expect(screen.getByText('Delete your account and all expenses? This cannot be undone.')).toBeInTheDocument()
    const confirm = screen.getByLabelText('Type your email to confirm')
    fireEvent.change(confirm, { target: { value: 'person@example.com' } })
    fireEvent.click(screen.getAllByRole('button', { name: 'Delete account' }).at(-1)!)
    await waitFor(() => expect(auth.deleteAccount).toHaveBeenCalledWith('person@example.com'))
  })
})
