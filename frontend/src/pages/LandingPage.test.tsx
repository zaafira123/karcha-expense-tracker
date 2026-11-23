import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { LandingPage } from './LandingPage'

describe('LandingPage', () => {
  it('has a complete FAQ with no placeholder copy', () => {
    render(<MemoryRouter><LandingPage/></MemoryRouter>)
    expect(screen.getByText('Yes. Kharcha is free to use while it is in its first version.')).toBeInTheDocument()
    const text = document.body.textContent?.toLowerCase() ?? ''
    expect(text).not.toMatch(/placeholder|todo|\[/)
  })
})
