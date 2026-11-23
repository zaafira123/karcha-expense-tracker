import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { ProtectedRoute } from './ProtectedRoute'

vi.mock('../contexts/AuthContext', () => ({ useAuth: () => ({ user: null, ready: true, sessionMessage: '' }) }))
function SignInLocation() { const location = useLocation(); const from = (location.state as { from?: { pathname?: string } })?.from; return <div>Sign in {from?.pathname}</div> }
describe('ProtectedRoute', () => {
  it('redirects signed-out users to sign in and preserves the requested route', () => {
    render(<MemoryRouter initialEntries={['/budgets']}><Routes><Route element={<ProtectedRoute/>}><Route path="/budgets" element={<div>Budgets</div>}/></Route><Route path="/signin" element={<SignInLocation/>}/></Routes></MemoryRouter>)
    expect(screen.getByText('Sign in /budgets')).toBeInTheDocument()
  })
})
