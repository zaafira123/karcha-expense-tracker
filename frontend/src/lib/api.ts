import type { AgentAudit, AgentProposal, AgentWhy, ApiError, AuthResponse, BudgetEntry, DashboardData, ForecastData, MonthlySummary, ParseExpenseDraft, SettingsData, SmsParseResponse, SmsSaveRequest, SmsSaveResponse, Transaction, TransactionInput, UndoResponse, UserProfile, WeeklySummary } from '../types/api'

let accessToken: string | null = null
let refreshPromise: Promise<string | null> | null = null
let unauthorizedHandler: (() => void) | null = null
let tokenListener: ((token: string | null) => void) | null = null

export function setAccessToken(token: string | null) { accessToken = token; tokenListener?.(token) }
export function setTokenListener(listener: ((token: string | null) => void) | null) { tokenListener = listener }
export function setUnauthorizedHandler(handler: (() => void) | null) { unauthorizedHandler = handler }
export function getAccessToken() { return accessToken }

async function parseError(response: Response): Promise<ApiError> {
  try { return await response.json() as ApiError }
  catch { return { error: 'REQUEST_FAILED', message: 'The request could not be completed.' } }
}

export async function refreshSession(): Promise<AuthResponse> {
  if (!refreshPromise) {
    refreshPromise = fetch('/api/auth/refresh', { method: 'POST', credentials: 'include' })
      .then(async response => {
        if (!response.ok) throw await parseError(response)
        const result = await response.json() as AuthResponse
        setAccessToken(result.accessToken)
        return result.accessToken
      })
      .finally(() => { refreshPromise = null })
  }
  const token = await refreshPromise
  if (!token) throw { error: 'UNAUTHORIZED', message: 'Your session has ended. Sign in to continue.' } satisfies ApiError
  return { accessToken: token, tokenType: 'Bearer', expiresIn: 0 }
}

export async function apiRequest<T>(path: string, init: RequestInit = {}, retried = false): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body) headers.set('Content-Type', 'application/json')
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  const response = await fetch(`/api${path}`, { ...init, headers, credentials: 'include' })
  const publicAuthRoute = ['/auth/login', '/auth/register', '/auth/refresh'].includes(path)
  if (response.status === 401 && !retried && !publicAuthRoute) {
    try { await refreshSession(); return apiRequest<T>(path, init, true) }
    catch {
      setAccessToken(null)
      unauthorizedHandler?.()
      throw { error: 'UNAUTHORIZED', message: 'Your session has ended. Sign in to continue.' } satisfies ApiError
    }
  }
  if (!response.ok) throw await parseError(response)
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const authApi = {
  login: (email: string, password: string) => apiRequest<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  register: (email: string, password: string) => apiRequest<UserProfile>('/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) }),
  me: () => apiRequest<UserProfile>('/auth/me'),
  logout: () => apiRequest<{ message: string }>('/auth/logout', { method: 'POST' }),
  deleteAccount: (email: string) => apiRequest<void>('/auth/account', { method: 'DELETE', body: JSON.stringify({ email }) }),
}
export const getDashboard = (month: string) => apiRequest<DashboardData>(`/dashboard?month=${encodeURIComponent(month)}`)
export const getWeeklySummary = () => apiRequest<WeeklySummary>('/summary/weekly')
export const getMonthlySummary = (month: string) => apiRequest<MonthlySummary>(`/summary/monthly?month=${encodeURIComponent(month)}`)
export const getSettings = () => apiRequest<SettingsData>('/settings')
export const updateAgentCap = (amountPaise: number) => apiRequest<{ agentCapPaise: number }>('/settings/agent-cap', { method: 'PUT', body: JSON.stringify({ amountPaise }) })
export const parseExpense = (text: string) => apiRequest<ParseExpenseDraft>('/expenses/parse', { method: 'POST', body: JSON.stringify({ text }) })
export const parseSmsMessages = (messages: string[]) => apiRequest<SmsParseResponse>('/imports/sms/parse', { method: 'POST', body: JSON.stringify({ messages, consentAccepted: true }) })
export const saveSmsDrafts = (request: SmsSaveRequest) => apiRequest<SmsSaveResponse>('/imports/sms/save', { method: 'POST', body: JSON.stringify(request) })
export const createTransaction = (request: TransactionInput) => apiRequest<Transaction>('/transactions', { method: 'POST', body: JSON.stringify(request) })
export const getTransactions = (month: string, category?: string) => apiRequest<Transaction[]>(`/transactions?month=${encodeURIComponent(month)}${category ? `&category=${encodeURIComponent(category)}` : ''}`)
export const updateTransaction = (id: string, request: Omit<TransactionInput, 'source'>) => apiRequest<Transaction>(`/transactions/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(request) })
export const deleteTransaction = (id: string) => apiRequest<void>(`/transactions/${encodeURIComponent(id)}`, { method: 'DELETE' })
export const getBudgets = (month: string) => apiRequest<BudgetEntry[]>(`/budgets?month=${encodeURIComponent(month)}`)
export const updateBudget = (category: string, month: string, amountPaise: number) => apiRequest<BudgetEntry>(`/budgets/${encodeURIComponent(category)}?month=${encodeURIComponent(month)}`, { method: 'PUT', body: JSON.stringify({ amountPaise }) })
export const getForecast = (month: string) => apiRequest<ForecastData>(`/forecast?month=${encodeURIComponent(month)}`)
export function getAgentProposals(filters: { month?: string; status?: string } = {}) { const params = new URLSearchParams(); if (filters.month) params.set('month', filters.month); if (filters.status) params.set('status', filters.status); return apiRequest<AgentProposal[]>(`/agent/proposals${params.size ? `?${params.toString()}` : ''}`) }
export const getAgentAudit = () => apiRequest<AgentAudit[]>('/agent/audit')
export const approveProposal = (id: string) => apiRequest<AgentProposal>(`/agent/proposals/${encodeURIComponent(id)}/approve`, { method: 'POST' })
export const rejectProposal = (id: string) => apiRequest<AgentProposal>(`/agent/proposals/${encodeURIComponent(id)}/reject`, { method: 'POST' })
export const undoProposal = (id: string) => apiRequest<UndoResponse>(`/agent/proposals/${encodeURIComponent(id)}/undo`, { method: 'POST' })
export const getProposalWhy = (id: string) => apiRequest<AgentWhy>(`/agent/proposals/${encodeURIComponent(id)}/why`)
