import { beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { SignInPage } from './SignInPage'

const mocks = vi.hoisted(() => ({ signIn: vi.fn(), createAccount: vi.fn(), clearSessionMessage: vi.fn() }))
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => mocks }))

beforeEach(() => { mocks.signIn.mockReset(); mocks.createAccount.mockReset(); mocks.clearSessionMessage.mockReset() })
describe('SignInPage', () => {
  it('shows a server credential error under password', async () => {
    mocks.signIn.mockRejectedValue({ error: 'INVALID_CREDENTIALS', message: 'Invalid email or password' })
    render(<MemoryRouter><SignInPage/></MemoryRouter>)
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'user@example.com' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password123' } })
    fireEvent.click(screen.getByRole('button', { name: /^Sign in$/ }))
    await waitFor(() => expect(screen.getByText('Check your email and password and try again.')).toBeInTheDocument())
  })

  it('disables the submit button while submitting', async () => {
    let finish!: () => void
    mocks.signIn.mockImplementation(() => new Promise<void>(resolve => { finish = resolve }))
    render(<MemoryRouter><SignInPage/></MemoryRouter>)
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'user@example.com' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password123' } })
    fireEvent.click(screen.getByRole('button', { name: /^Sign in$/ }))
    expect(screen.getByRole('button', { name: 'Please wait…' })).toBeDisabled()
    await act(async () => { finish() })
  })

  it('returns to the originally requested page after sign in', async () => {
    mocks.signIn.mockResolvedValue(undefined)
    render(<MemoryRouter initialEntries={[{ pathname: '/signin', state: { from: { pathname: '/budgets' } } }]}><Routes><Route path="/signin" element={<SignInPage/>}/><Route path="/budgets" element={<p>Budgets page</p>}/></Routes></MemoryRouter>)
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'user@example.com' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password123' } })
    fireEvent.click(screen.getByRole('button', { name: /^Sign in$/ }))
    expect(await screen.findByText('Budgets page')).toBeInTheDocument()
  })
})
