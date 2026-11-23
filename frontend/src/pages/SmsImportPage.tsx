import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { parseSmsMessages, saveSmsDrafts } from '../lib/api'
import { formatPaise, rupeesToPaise } from '../lib/money'
import { subscribeToSharedSms } from '../lib/shareTarget'
import { CATEGORIES, userMessage } from '../types/api'
import type { SmsImportDraft, SmsSaveRequest } from '../types/api'
import { useToast } from '../components/ToastProvider'
import { EmptyState, ErrorState } from '../components/EmptyState'
import { Skeleton } from '../components/Skeleton'
import { Button } from '../components/ui/button'
import { Card } from '../components/ui/card'
import { Input } from '../components/ui/input'
import { Label } from '../components/ui/label'

function amountField(paise: number | null) {
  if (paise == null) return ''
  return `${Math.trunc(paise / 100)}.${String(paise % 100).padStart(2, '0')}`
}

function todayInKolkata() {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kolkata', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date())
  return `${parts.find(item => item.type === 'year')?.value}-${parts.find(item => item.type === 'month')?.value}-${parts.find(item => item.type === 'day')?.value}`
}

function monthLabel(month: string) {
  const [year, number] = month.split('-').map(Number)
  return new Intl.DateTimeFormat('en', { month: 'long', year: 'numeric', timeZone: 'Asia/Kolkata' })
    .format(new Date(Date.UTC(year, number - 1, 15)))
}

function splitMessages(value: string) {
  const chunks = value.includes('\n\n') ? value.split(/\n\s*\n/) : value.split(/\r?\n/)
  return chunks.map(chunk => chunk.trim()).filter(Boolean)
}

function isReady(draft: SmsImportDraft, amount: string, category: string, merchant: string, occurredOn: string) {
  if (draft.type !== 'DEBIT' || ['DUPLICATE', 'CREDIT', 'UNKNOWN'].includes(draft.status)) return false
  try { if (rupeesToPaise(amount) <= 0) return false } catch { return false }
  return Boolean(category && merchant.trim() && occurredOn && occurredOn <= todayInKolkata())
}

async function hashDraft(amountPaise: number, occurredOn: string, merchant: string) {
  const normalizedMerchant = merchant.toLowerCase().replace(/\s+/g, ' ').trim()
  const input = `${amountPaise}|${occurredOn}|${normalizedMerchant}|DEBIT`
  const bytes = new TextEncoder().encode(input)
  const digest = await crypto.subtle.digest('SHA-256', bytes)
  return Array.from(new Uint8Array(digest), byte => byte.toString(16).padStart(2, '0')).join('')
}

function statusText(draft: SmsImportDraft, ready: boolean) {
  if (draft.status === 'DUPLICATE') return 'Duplicate'
  if (draft.status === 'CREDIT') return 'Credit, not counted'
  if (draft.status === 'UNKNOWN') return 'Could not read'
  if (ready) return 'Ready'
  return 'Check this'
}

export function SmsImportPage() {
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [consent, setConsent] = useState(false)
  const [messageText, setMessageText] = useState('')
  const [drafts, setDrafts] = useState<SmsImportDraft[]>([])
  const [values, setValues] = useState<Record<number, { amount: string; category: string; merchant: string; occurredOn: string }>>({})
  const [selected, setSelected] = useState<Record<number, boolean>>({})
  const [fieldMessage, setFieldMessage] = useState('')
  const [parseError, setParseError] = useState('')
  const [saveError, setSaveError] = useState('')
  const [savedNotice, setSavedNotice] = useState<{ count: number; months: string[] } | null>(null)
  const parseMutation = useMutation({ mutationFn: parseSmsMessages })
  const saveMutation = useMutation({ mutationFn: saveSmsDrafts })
  const messages = useMemo(() => splitMessages(messageText), [messageText])

  useEffect(() => subscribeToSharedSms(text => {
    setMessageText(current => current ? `${current}\n\n${text}` : text)
    setDrafts([])
    setSelected({})
    setSavedNotice(null)
  }), [])

  async function parse(event?: FormEvent) {
    event?.preventDefault()
    setFieldMessage('')
    setParseError('')
    setSavedNotice(null)
    if (!consent) { setFieldMessage('Confirm the consent statement before reading messages.'); return }
    if (messages.length < 1 || messages.length > 20 || messages.some(item => item.length < 10 || item.length > 1000)) {
      setFieldMessage('Enter 1 to 20 messages, each between 10 and 1,000 characters.')
      return
    }
    try {
      const result = await parseMutation.mutateAsync(messages)
      setDrafts(result.drafts)
      const nextValues: typeof values = {}
      const nextSelected: typeof selected = {}
      result.drafts.forEach(draft => {
        nextValues[draft.index] = { amount: amountField(draft.amountPaise), category: draft.category ?? '', merchant: draft.merchant ?? '', occurredOn: draft.occurredOn ?? todayInKolkata() }
        nextSelected[draft.index] = draft.status === 'READY'
      })
      setValues(nextValues)
      setSelected(nextSelected)
    } catch (error) {
      setParseError(userMessage(error as Parameters<typeof userMessage>[0], 'We could not read those messages. Try again.'))
    }
  }

  function editDraft(index: number, key: 'amount' | 'category' | 'merchant' | 'occurredOn', value: string) {
    setValues(current => ({ ...current, [index]: { ...current[index], [key]: value } }))
    setFieldMessage('')
  }

  async function save() {
    setSaveError('')
    setFieldMessage('')
    const selectedDrafts = drafts.filter(draft => selected[draft.index])
    if (!selectedDrafts.length) { setFieldMessage('Select at least one ready debit to save.'); return }
    try {
      const payloadDrafts: SmsSaveRequest['drafts'] = []
      for (const draft of selectedDrafts) {
        const value = values[draft.index]
        if (!isReady(draft, value.amount, value.category, value.merchant, value.occurredOn)) {
          setFieldMessage('Complete the highlighted debit details before saving.')
          return
        }
        const amountPaise = rupeesToPaise(value.amount)
        payloadDrafts.push({ index: draft.index, amountPaise, category: value.category, merchant: value.merchant.trim(), occurredOn: value.occurredOn, messageHash: await hashDraft(amountPaise, value.occurredOn, value.merchant), type: 'DEBIT' })
      }
      const result = await saveMutation.mutateAsync({ consentAccepted: true, drafts: payloadDrafts })
      await queryClient.invalidateQueries({ queryKey: ['transactions'] })
      await queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      await queryClient.invalidateQueries({ queryKey: ['forecast'] })
      await queryClient.invalidateQueries({ queryKey: ['budgets'] })
      await queryClient.invalidateQueries({ queryKey: ['summary'] })
      await queryClient.invalidateQueries({ queryKey: ['agent'] })
      showToast(`${result.savedCount} ${result.savedCount === 1 ? 'expense' : 'expenses'} saved.`)
      const skippedIndexes = new Set(result.skipped.map(item => item.index))
      const savedMonths = [...new Set(payloadDrafts
        .filter(draft => !skippedIndexes.has(draft.index))
        .map(draft => draft.occurredOn.slice(0, 7)))].sort().reverse()
      setSavedNotice({ count: result.savedCount, months: savedMonths })
      setDrafts([])
      setValues({})
      setSelected({})
      setMessageText('')
    } catch (error) {
      setSaveError(userMessage(error as Parameters<typeof userMessage>[0], 'We could not save the selected expenses. Try again.'))
    }
  }

  const readyCount = drafts.filter(draft => selected[draft.index] && isReady(draft, values[draft.index]?.amount ?? '', values[draft.index]?.category ?? '', values[draft.index]?.merchant ?? '', values[draft.index]?.occurredOn ?? '')).length

  return <div className="mx-auto max-w-3xl space-y-6">
    <div>
      <h1 className="h1">Import from SMS</h1>
      <p className="mt-1 body text-secondary">Paste bank or card messages. We read them and show drafts for you to check. Nothing is saved until you confirm.</p>
    </div>

    <Card>
      <form onSubmit={parse} className="space-y-4">
        <div className="flex items-start gap-3">
          <input id="sms-consent" type="checkbox" className="mt-1 h-4 w-4 accent-accent focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent" checked={consent} onChange={event => setConsent(event.target.checked)} />
          <Label htmlFor="sms-consent" className="body text-secondary">I understand these messages are read to create transactions. The full message is not saved.</Label>
        </div>
        <div>
          <Label htmlFor="sms-messages">Bank or card messages, one per line or separated by a blank line</Label>
          <textarea id="sms-messages" value={messageText} onChange={event => setMessageText(event.target.value)} disabled={!consent} rows={6} className="mt-1 w-full resize-y rounded-control border border-border bg-canvas px-3 py-3 body text-primary focus:border-border-strong focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent disabled:bg-subtle disabled:text-muted" />
        </div>
        {fieldMessage && <p role="alert" className="small text-danger-text">{fieldMessage}</p>}
        <Button type="submit" disabled={!consent || parseMutation.isPending}>{parseMutation.isPending ? 'Reading messages…' : 'Read messages'}</Button>
      </form>
    </Card>

    {parseError && <ErrorState message={parseError} onRetry={() => void parse()} />}
    {saveError && <p role="alert" className="small text-danger-text">{saveError}</p>}
    {savedNotice && <Card>
      <p className="body-strong" role="status">Saved. Open the matching month below to view your transactions.</p>
      {savedNotice.months.length > 0 && <div className="mt-2 flex flex-wrap gap-x-4 gap-y-2">
        {savedNotice.months.map(month => <Link key={month} className="body-strong text-accent underline" to={`/transactions?month=${encodeURIComponent(month)}`}>
          View transactions for {monthLabel(month)}
        </Link>)}
      </div>}
    </Card>}
    {parseMutation.isPending && <div className="space-y-3" aria-label="Reading messages"><Skeleton className="h-28 w-full"/><Skeleton className="h-28 w-full"/></div>}

    {!parseMutation.isPending && !parseError && drafts.length === 0 && <Card><EmptyState text="Paste or share messages to review possible transactions. The message text is used for parsing and is not saved." /></Card>}

    {drafts.length > 0 && <section aria-label="Message drafts" className="space-y-4">
      <div><h2 className="h2">Review drafts</h2><p className="mt-1 small text-secondary">Credits are listed separately. Only ready debit drafts can be saved.</p></div>
      {drafts.map(draft => {
        const value = values[draft.index] ?? { amount: '', category: '', merchant: '', occurredOn: todayInKolkata() }
        const ready = isReady(draft, value.amount, value.category, value.merchant, value.occurredOn)
        const checked = Boolean(selected[draft.index])
        const editable = draft.type === 'DEBIT' && draft.status !== 'DUPLICATE'
        return <Card key={draft.index} className={draft.status === 'DUPLICATE' ? 'border-border-strong' : ''}>
          <div className="flex items-start gap-3">
            <input aria-label={`Select draft ${draft.index + 1}`} type="checkbox" className="mt-1 h-4 w-4 accent-accent focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent" checked={checked} disabled={!ready} onChange={event => setSelected(current => ({ ...current, [draft.index]: event.target.checked }))} />
            <div className="min-w-0 flex-1">
              <p className="body-strong">Message {draft.index + 1}</p>
              <p className="mt-1 small text-secondary" role="status">{statusText(draft, ready)}</p>
              {(draft.reason || draft.status === 'NEEDS_REVIEW') && !ready && <p className="mt-1 small text-warning-text">{draft.reason ?? 'Review the transaction details.'}</p>}
            </div>
          </div>
          {draft.type === 'CREDIT' && <div className="mt-4 rounded-control border border-border bg-subtle p-3"><p className="body-strong">Credit: {draft.amountPaise == null ? 'Amount unclear' : formatPaise(draft.amountPaise)}</p><p className="mt-1 small text-secondary">{draft.merchant ?? 'Merchant unclear'}{draft.occurredOn ? ` · ${draft.occurredOn}` : ''}</p><p className="mt-1 small text-secondary">Credits are not counted as expenses.</p></div>}
          {draft.type === 'DEBIT' && <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <div><Label htmlFor={`sms-amount-${draft.index}`}>Amount (₹)</Label><Input id={`sms-amount-${draft.index}`} className="mt-1" inputMode="decimal" value={value.amount} disabled={!editable} onChange={event => editDraft(draft.index, 'amount', event.target.value)} /></div>
            <div><Label htmlFor={`sms-category-${draft.index}`}>Category</Label><select id={`sms-category-${draft.index}`} className="mt-1 h-10 w-full rounded-control border border-border bg-canvas px-3 body focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent" value={value.category} disabled={!editable} onChange={event => editDraft(draft.index, 'category', event.target.value)}><option value="">Choose a category</option>{CATEGORIES.map(category => <option key={category.code} value={category.code}>{category.name}</option>)}</select></div>
            <div><Label htmlFor={`sms-merchant-${draft.index}`}>Merchant</Label><Input id={`sms-merchant-${draft.index}`} className="mt-1" value={value.merchant} disabled={!editable} onChange={event => editDraft(draft.index, 'merchant', event.target.value)} /></div>
            <div><Label htmlFor={`sms-date-${draft.index}`}>Date</Label><Input id={`sms-date-${draft.index}`} className="mt-1" type="date" max={todayInKolkata()} value={value.occurredOn} disabled={!editable} onChange={event => editDraft(draft.index, 'occurredOn', event.target.value)} /></div>
          </div>}
          {draft.status === 'DUPLICATE' && <p className="mt-4 small text-secondary">This message matches an expense already imported.</p>}
        </Card>
      })}
      <div className="flex flex-wrap items-center gap-3"><Button disabled={readyCount === 0 || saveMutation.isPending} onClick={() => void save()}>{saveMutation.isPending ? 'Saving…' : `Save ${readyCount} ${readyCount === 1 ? 'expense' : 'expenses'}`}</Button><Link className="body-strong text-accent underline" to="/transactions">Transactions</Link></div>
    </section>}
  </div>
}
