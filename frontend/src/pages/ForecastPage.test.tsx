import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { ForecastPage } from './ForecastPage'
import { ToastProvider } from '../components/ToastProvider'

const api = vi.hoisted(() => ({ getForecast: vi.fn() }))
vi.mock('../lib/api', () => api)
const base = { month: '2026-11', status: 'OK', spentPaise: 3000000, projectedPaise: 4500000, lowPaise: 4000000, highPaise: 5000000, budgetPaise: 4200000, avgDailyPaise: 82900, daysLeft: 12, series: [], categories: [{ category: 'GROCERIES', categoryName: 'Groceries', projectedPaise: 900000, budgetPaise: 800000, overByPaise: 100000 }, { category: 'FOOD_DINING', categoryName: 'Food and dining', projectedPaise: 900000, budgetPaise: 600000, overByPaise: 300000 }, { category: 'TRAVEL', categoryName: 'Travel', projectedPaise: 1200000, budgetPaise: 500000, overByPaise: 700000 }] }
function setup() { const client = new QueryClient({ defaultOptions: { queries: { retry: false } } }); return render(<QueryClientProvider client={client}><ToastProvider><MemoryRouter><ForecastPage/></MemoryRouter></ToastProvider></QueryClientProvider>) }
beforeEach(() => api.getForecast.mockReset())
describe('ForecastPage', () => {
  it('shows insufficient data without a chart', async () => {
    api.getForecast.mockResolvedValue({ ...base, status: 'INSUFFICIENT_DATA', projectedPaise: null, series: [] })
    setup()
    expect(await screen.findByText('Add a few more days of expenses for a forecast.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Cumulative spend forecast chart')).not.toBeInTheDocument()
  })
  it('shows over-budget status and the biggest driver', async () => {
    api.getForecast.mockResolvedValue({ ...base, status: 'OVER_BUDGET_PROJECTED' })
    setup()
    expect(await screen.findByText('Projected to exceed your budget by ₹3,000.')).toBeInTheDocument()
    expect(screen.getByText('Travel')).toBeInTheDocument()
  })
})
