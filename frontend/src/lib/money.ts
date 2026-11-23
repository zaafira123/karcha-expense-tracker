export function formatPaise(amountPaise: number): string {
  const paise = Number.isFinite(amountPaise) ? Math.trunc(amountPaise) : 0
  const rupees = Math.trunc(paise / 100)
  const remainder = Math.abs(paise % 100)
  const grouped = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 0 }).format(rupees)
  return `₹${grouped}${remainder ? `.${String(remainder).padStart(2, '0')}` : ''}`
}

export function rupeesToPaise(value: string, allowZero = false): number {
  const match = value.trim().replaceAll(',', '').match(/^(\d+)(?:\.(\d{1,2}))?$/)
  if (!match) throw new Error('Enter a valid amount in rupees.')
  const paise = BigInt(match[1]) * 100n + BigInt((match[2] ?? '').padEnd(2, '0') || '0')
  if (paise < 0n || (!allowZero && paise === 0n)) throw new Error('Enter an amount greater than ₹0.')
  if (paise > BigInt(Number.MAX_SAFE_INTEGER)) throw new Error('Enter a smaller amount.')
  return Number(paise)
}
