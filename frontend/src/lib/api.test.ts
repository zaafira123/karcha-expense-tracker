import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest, approveProposal, authApi, getSettings, getWeeklySummary, setAccessToken, setUnauthorizedHandler, updateAgentCap } from './api'

afterEach(() => { vi.unstubAllGlobals(); setAccessToken(null); setUnauthorizedHandler(null) })

describe('apiRequest refresh handling', () => {
  it('refreshes once after a 401 and retries once', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response('{}', { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ accessToken: 'new-token', tokenType: 'Bearer', expiresIn: 900 }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ ok: true }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    setAccessToken('old-token')
    await expect(apiRequest('/dashboard?month=2026-10')).resolves.toEqual({ ok: true })
    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(fetchMock.mock.calls[1][0]).toBe('/api/auth/refresh')
  })

  it('clears the session when refresh fails', async () => {
    const onUnauthorized = vi.fn()
    const fetchMock = vi.fn().mockResolvedValueOnce(new Response('{}', { status: 401 })).mockResolvedValueOnce(new Response('{}', { status: 401 }))
    vi.stubGlobal('fetch', fetchMock)
    setAccessToken('expired')
    setUnauthorizedHandler(onUnauthorized)
    await expect(apiRequest('/dashboard?month=2026-10')).rejects.toMatchObject({ message: 'Your session has ended. Sign in to continue.' })
    expect(onUnauthorized).toHaveBeenCalledOnce()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})

describe('agent proposal API helpers', () => {
  it('accepts a successful empty approval response', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(new Response(null, { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    setAccessToken('token')

    await expect(approveProposal('proposal-1')).resolves.toBeUndefined()
    expect(fetchMock.mock.calls[0][0]).toBe('/api/agent/proposals/proposal-1/approve')
    expect(fetchMock.mock.calls[0][1].method).toBe('POST')
    expect(fetchMock.mock.calls[0][1].body).toBeUndefined()
  })
})

describe('account, settings, and weekly summary API helpers', () => {
  it('uses the new endpoints with paise values and email confirmation', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ email: 'user@example.com', agentCapPaise: 200000 }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ agentCapPaise: 250000 }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ summaryText: 'Summary' }), { status: 200 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)
    setAccessToken('token')
    await expect(getSettings()).resolves.toMatchObject({ agentCapPaise: 200000 })
    await expect(updateAgentCap(250000)).resolves.toEqual({ agentCapPaise: 250000 })
    await expect(getWeeklySummary()).resolves.toMatchObject({ summaryText: 'Summary' })
    await expect(authApi.deleteAccount('user@example.com')).resolves.toBeUndefined()
    expect(fetchMock.mock.calls[1][0]).toBe('/api/settings/agent-cap')
    expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual({ amountPaise: 250000 })
    expect(fetchMock.mock.calls[3][0]).toBe('/api/auth/account')
    expect(JSON.parse(fetchMock.mock.calls[3][1].body)).toEqual({ email: 'user@example.com' })
  })
})
