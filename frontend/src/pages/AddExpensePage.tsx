import { useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { parseExpense, createTransaction } from '../lib/api'
import { rupeesToPaise } from '../lib/money'
import { userMessage } from '../types/api'
import type { ParseExpenseDraft, TransactionInput } from '../types/api'
import { CATEGORIES } from '../types/api'
import { useToast } from '../components/ToastProvider'
import { Button } from '../components/ui/button'
import { Card } from '../components/ui/card'
import { ErrorState } from '../components/EmptyState'
import { Input } from '../components/ui/input'
import { Label } from '../components/ui/label'

type EntryMode = 'sentence' | 'preview' | 'manual'
function todayInKolkata() { const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kolkata', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date()); return `${parts.find(item => item.type === 'year')?.value}-${parts.find(item => item.type === 'month')?.value}-${parts.find(item => item.type === 'day')?.value}` }
function amountInput(value: number | null) { return value == null ? '' : `${Math.floor(value / 100)}.${String(value % 100).padStart(2, '0')}` }

export function AddExpensePage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const { showToast } = useToast()
  const [mode, setMode] = useState<EntryMode>('sentence')
  const [sentence, setSentence] = useState('')
  const [draft, setDraft] = useState<ParseExpenseDraft | null>(null)
  const [amount, setAmount] = useState('')
  const [category, setCategory] = useState('')
  const [merchant, setMerchant] = useState('')
  const [occurredOn, setOccurredOn] = useState('')
  const [note, setNote] = useState('')
  const [parseMessage, setParseMessage] = useState('')
  const [formMessage, setFormMessage] = useState('')
  const [fieldMessage, setFieldMessage] = useState('')

  const parseMutation = useMutation({ mutationFn: parseExpense })
  const saveMutation = useMutation({
    mutationFn: createTransaction,
    onSuccess: async () => {
      await queryClient.invalidateQueries()
      showToast('Expense saved')
      setMode('sentence'); setSentence(''); setDraft(null); setAmount(''); setCategory(''); setMerchant(''); setOccurredOn(''); setNote(''); setParseMessage(''); setFormMessage(''); setFieldMessage('')
    },
  })

  async function handleParse(event?: FormEvent) {
    event?.preventDefault()
    setParseMessage(''); setFormMessage(''); setFieldMessage('')
    if (!sentence.trim()) { setParseMessage('Enter what you spent before reading it.'); return }
    try {
      const result = await parseMutation.mutateAsync(sentence.trim())
      setDraft(result); setAmount(amountInput(result.amountPaise)); setCategory(result.category ?? ''); setMerchant(result.merchant ?? ''); setOccurredOn(result.occurredOn ?? todayInKolkata()); setNote(result.note ?? sentence.trim()); setMode('preview')
    } catch {
      setDraft(null); setMode('manual'); setAmount(''); setCategory(''); setMerchant(''); setOccurredOn(todayInKolkata()); setNote(''); setParseMessage('We could not read that sentence. Enter the details below.')
    }
  }

  async function handleSave(event: FormEvent) {
    event.preventDefault(); setFormMessage(''); setFieldMessage('')
    let amountPaise: number
    try { amountPaise = rupeesToPaise(amount) } catch (error) { setFieldMessage(error instanceof Error ? error.message : 'Enter an amount greater than ₹0.'); return }
    if (!category) { setFieldMessage('Choose a category.'); return }
    if (!merchant.trim()) { setFieldMessage('Enter a merchant or description.'); return }
    if (!occurredOn) { setFieldMessage('Choose an expense date.'); return }
    const request: TransactionInput = { amountPaise, category, merchant: merchant.trim(), occurredOn, note: note.trim() || null, source: draft && mode === 'preview' ? 'TEXT' : 'MANUAL' }
    try { await saveMutation.mutateAsync(request); navigate('/dashboard') } catch (error) { setFormMessage(userMessage(error as Parameters<typeof userMessage>[0], 'We could not save this expense. Please try again.')) }
  }

  function enterManual() { setDraft(null); setMode('manual'); setAmount(''); setCategory(''); setMerchant(''); setOccurredOn(todayInKolkata()); setNote(''); setFormMessage(''); setFieldMessage('') }
  if (parseMutation.isError && !parseMessage) return <ErrorState message="We could not read that sentence. Enter the details below." onRetry={() => void handleParse()} />

  return <div className="mx-auto max-w-3xl space-y-6">
    <div><h1 className="h1">Add expense</h1><p className="mt-1 body text-secondary">Record a transaction by describing what you spent.</p></div>
    <Card>
      <form onSubmit={handleParse} className="space-y-4">
        <Label htmlFor="expense-sentence">What did you spend?</Label>
        <div className="flex flex-col gap-3 sm:flex-row"><Input id="expense-sentence" className="h-14 border-border-strong text-[17px]" value={sentence} onChange={event => setSentence(event.target.value)} placeholder="For example, paid 450 for lunch" />{mode !== 'manual' && <Button type="submit" disabled={parseMutation.isPending} className="sm:min-w-24">{parseMutation.isPending ? 'Reading…' : 'Read'}</Button>}</div>
        {parseMessage && <p role="alert" className="small text-warning-text">{parseMessage}</p>}
        {mode !== 'manual' && <button type="button" className="min-h-10 body-strong text-accent underline" onClick={enterManual}>Enter manually instead</button>}
      </form>
    </Card>

    {(mode === 'preview' || mode === 'manual') && <Card className="border-border-strong">
      <h2 className="h2">{mode === 'preview' ? 'Parsed details' : 'Manual transaction entry'}</h2>
      {draft?.needsReview && <p role="alert" className="mt-3 small text-warning-text">Check the highlighted fields. We could not read them clearly.</p>}
      {mode === 'preview' && <p className="mt-2 small text-secondary">Parsed from your sentence. Check the amount before saving.</p>}
      {formMessage && <p role="alert" className="mt-3 rounded-control border border-danger-text bg-danger-bg p-3 small text-danger-text">{formMessage}</p>}
      {fieldMessage && <p role="alert" className="mt-3 small text-danger-text">{fieldMessage}</p>}
      <form onSubmit={handleSave} className="mt-5 grid gap-4 sm:grid-cols-2">
        <div><Label htmlFor="expense-amount">Amount (₹)</Label><Input id="expense-amount" inputMode="decimal" value={amount} onChange={event => setAmount(event.target.value)} className={`mt-1 ${draft?.needsReview && !draft.amountPaise ? 'border-danger-text' : ''}`} aria-invalid={draft?.needsReview && !draft.amountPaise ? true : undefined}/></div>
        <div><Label htmlFor="expense-category">Category</Label><select id="expense-category" className="mt-1 h-10 w-full rounded-control border border-border bg-canvas px-3 body" value={category} onChange={event => setCategory(event.target.value)}><option value="">Choose category</option>{CATEGORIES.map(item => <option key={item.code} value={item.code}>{item.name}</option>)}</select></div>
        <div><Label htmlFor="expense-merchant">Merchant</Label><Input id="expense-merchant" className="mt-1" value={merchant} onChange={event => setMerchant(event.target.value)} /></div>
        <div><Label htmlFor="expense-date">Date</Label><Input id="expense-date" className="mt-1" type="date" max={todayInKolkata()} value={occurredOn} onChange={event => setOccurredOn(event.target.value)} /></div>
        <div className="sm:col-span-2"><Label htmlFor="expense-note">Note (optional)</Label><Input id="expense-note" className="mt-1" value={note} maxLength={255} onChange={event => setNote(event.target.value)} /></div>
        <div className="flex flex-wrap gap-3 pt-2 sm:col-span-2"><Button type="submit" disabled={saveMutation.isPending}>{saveMutation.isPending ? 'Saving…' : 'Save expense'}</Button>{mode === 'preview' && <Button type="button" variant="ghost" onClick={() => { setMode('sentence'); setDraft(null) }}>Cancel</Button>}</div>
      </form>
    </Card>}
  </div>
}
