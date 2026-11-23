import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { DashboardPage } from './DashboardPage'
import { ToastProvider } from '../components/ToastProvider'

const api = vi.hoisted(() => ({ getDashboard: vi.fn(), getForecast: vi.fn(), getAgentProposals: vi.fn(), getWeeklySummary: vi.fn(), getMonthlySummary: vi.fn() }))
vi.mock('../lib/api', () => api)
const categories = ['Food and dining', 'Groceries', 'Travel', 'Shopping', 'Bills and utilities', 'Health', 'Entertainment', 'Savings'].map((displayName, index) => ({ category: { code: String(index), displayName, isProtected: false }, budgetPaise: 10000, spentPaise: 4000, percentUsed: 40, status: 'OK' as const }))
const dashboard = { totalBudgetPaise: 80000, spentPaise: 32000, remainingPaise: 48000, daysLeft: 12, categories, recent: [] }
function setup() { const client = new QueryClient({ defaultOptions: { queries: { retry: false } } }); return render(<QueryClientProvider client={client}><ToastProvider><MemoryRouter><DashboardPage/></MemoryRouter></ToastProvider></QueryClientProvider>) }
beforeEach(() => { api.getDashboard.mockReset().mockResolvedValue(dashboard); api.getForecast.mockReset().mockResolvedValue({ status: 'OK', projectedPaise: 45000 }); api.getAgentProposals.mockReset().mockResolvedValue([]); api.getWeeklySummary.mockReset().mockResolvedValue({ summaryText: 'Spending is steady.', currentWeekSpentPaise: 10000, previousWeekSpentPaise: 8000, topCategories: [], budgetsOver80: [], tips: ['One', 'Two', 'Three'] }); api.getMonthlySummary.mockReset().mockResolvedValue({ summaryText: 'Month is steady.', currentSpentPaise: 10000, previousSpentPaise: 9000, topCategories: [], tips: ['One', 'Two', 'Three'] }) })
describe('DashboardPage', () => {
  it('renders the stat cards and eight category rows', async () => {
    setup()
    expect(await screen.findByText('Spent this month')).toBeInTheDocument()
    expect(screen.getByText('Budget remaining')).toBeInTheDocument()
    expect(screen.getByText('Projected month end')).toBeInTheDocument()
    expect(screen.getByText('Days left')).toBeInTheDocument()
    expect(await screen.findByText('Weekly summary')).toBeInTheDocument()
    expect(screen.getByText('Spending is steady.')).toBeInTheDocument()
    expect(screen.getByText('Monthly summary')).toBeInTheDocument()
    expect(screen.getByText('Month is steady.')).toBeInTheDocument()
    for (const item of categories) expect(screen.getByText(item.category.displayName)).toBeInTheDocument()
  })

  it('shows skeletons while loading', () => {
    api.getDashboard.mockImplementation(() => new Promise(() => {}))
    setup()
    expect(screen.getByLabelText('Loading dashboard')).toBeInTheDocument()
  })

  it('shows ErrorState when the request fails', async () => {
    api.getDashboard.mockRejectedValue(new Error('failed'))
    setup()
    expect(await screen.findByText('We couldn’t load your dashboard. Check your connection and try again.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
  })
})
