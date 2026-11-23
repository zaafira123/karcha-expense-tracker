import { describe, expect, it } from 'vitest'
import { formatPaise, rupeesToPaise } from './money'
describe('formatPaise', () => {
  it('formats zero rupees', () => expect(formatPaise(0)).toBe('₹0'))
  it('groups whole rupees using Indian grouping', () => expect(formatPaise(450000)).toBe('₹4,500'))
  it('formats paise and Indian grouping', () => expect(formatPaise(12345678)).toBe('₹1,23,456.78'))
})
describe('rupeesToPaise', () => {
  it('converts a decimal rupee string using integer arithmetic', () => expect(rupeesToPaise('450.75')).toBe(45075))
  it('rejects zero and invalid decimal precision', () => { expect(() => rupeesToPaise('0')).toThrow(); expect(() => rupeesToPaise('4.567')).toThrow() })
})
