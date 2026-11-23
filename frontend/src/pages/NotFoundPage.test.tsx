import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { NotFoundPage } from './NotFoundPage'

const mock = vi.hoisted(() => ({ useAuth: vi.fn() }))
vi.mock('../contexts/AuthContext', () => ({ useAuth: mock.useAuth }))
describe('NotFoundPage', () => {
  it('offers the dashboard when signed in and home when signed out', () => {
    mock.useAuth.mockReturnValue({ user: { email: 'person@example.com' }, ready: true })
    const { rerender } = render(<MemoryRouter><NotFoundPage/></MemoryRouter>)
    expect(screen.getByRole('link', { name: 'Go to dashboard' })).toHaveAttribute('href', '/dashboard')
    mock.useAuth.mockReturnValue({ user: null, ready: true })
    rerender(<MemoryRouter><NotFoundPage/></MemoryRouter>)
    expect(screen.getByRole('link', { name: 'Go to home' })).toHaveAttribute('href', '/')
  })
})
