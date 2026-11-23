import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { PrivacyPage } from './PrivacyPage'
import { TermsPage } from './TermsPage'

describe('legal pages', () => {
  it('states the privacy practices and update date', () => {
    render(<MemoryRouter initialEntries={['/privacy']}><Routes><Route path="/privacy" element={<PrivacyPage/>}/></Routes></MemoryRouter>)
    expect(screen.getByText('Last updated 14 Nov 2026')).toBeInTheDocument()
    expect(screen.getByText(/No bank data is accessed/)).toBeInTheDocument()
  })
  it('renders plain-language terms without placeholder copy', () => {
    render(<MemoryRouter initialEntries={['/terms']}><Routes><Route path="/terms" element={<TermsPage/>}/></Routes></MemoryRouter>)
    expect(screen.getByText('Last updated 14 Nov 2026')).toBeInTheDocument()
    expect(document.body.textContent?.toLowerCase()).not.toMatch(/placeholder|todo|\[/)
  })
})
