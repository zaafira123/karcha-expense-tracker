import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as Dialog from '@radix-ui/react-dialog'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { deleteTransaction, getTransactions, updateTransaction } from '../lib/api'
import { formatPaise, rupeesToPaise } from '../lib/money'
import type { Transaction } from '../types/api'
import { CATEGORIES, userMessage } from '../types/api'
import { useToast } from '../components/ToastProvider'
import { ErrorState, EmptyState } from '../components/EmptyState'
import { Skeleton } from '../components/Skeleton'
import { Button } from '../components/ui/button'
import { Input } from '../components/ui/input'
import { Label } from '../components/ui/label'

function currentMonth() { const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kolkata', year: 'numeric', month: '2-digit' }).formatToParts(new Date()); return `${parts.find(item => item.type === 'year')?.value}-${parts.find(item => item.type === 'month')?.value}` }
function monthsBefore(month: string) { const [year, number] = month.split('-').map(Number); return Array.from({ length: 12 }, (_, offset) => { const date = new Date(Date.UTC(year, number - 1 - offset, 1)); return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}` }) }
function validMonth(value: string | null): value is string { if (!value || !/^\d{4}-(0[1-9]|1[0-2])$/.test(value)) return false; return true }
function monthName(month: string) { const [year, number] = month.split('-').map(Number); return new Intl.DateTimeFormat('en', { month: 'long', year: 'numeric', timeZone: 'Asia/Kolkata' }).format(new Date(Date.UTC(year, number - 1, 15))) }
function dateLabel(date: string) { return new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'Asia/Kolkata' }).format(new Date(`${date}T12:00:00+05:30`)) }
function amountField(paise: number) { return `${Math.trunc(paise / 100)}.${String(paise % 100).padStart(2, '0')}` }

export function TransactionsPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const monthNow = useMemo(currentMonth, [])
  const [month, setMonth] = useState(() => {
    const linkedMonth = searchParams.get('month')
    return validMonth(linkedMonth) ? linkedMonth : monthNow
  })
  const [category, setCategory] = useState('')
  const [toDelete, setToDelete] = useState<Transaction | null>(null)
  const [toEdit, setToEdit] = useState<Transaction | null>(null)
  const [editAmount, setEditAmount] = useState('')
  const [editCategory, setEditCategory] = useState('')
  const [editMerchant, setEditMerchant] = useState('')
  const [editDate, setEditDate] = useState('')
  const [editNote, setEditNote] = useState('')
  const [message, setMessage] = useState('')
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const query = useQuery({ queryKey: ['transactions', month, category], queryFn: () => getTransactions(month, category || undefined) })
  const deleteMutation = useMutation({ mutationFn: deleteTransaction, onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['transactions'] }); setToDelete(null); showToast('Expense deleted') } })
  const editMutation = useMutation({ mutationFn: ({ id, data }: { id: string; data: { amountPaise: number; category: string; merchant: string; occurredOn: string; note: string | null } }) => updateTransaction(id, data), onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['transactions'] }); await queryClient.invalidateQueries({ queryKey: ['dashboard'] }); setToEdit(null); showToast('Expense updated') } })

  function beginEdit(item: Transaction) { setToEdit(item); setEditAmount(amountField(item.amountPaise)); setEditCategory(item.category.code); setEditMerchant(item.merchant); setEditDate(item.occurredOn); setEditNote(item.note ?? ''); setMessage('') }
  async function saveEdit(event: React.FormEvent) { event.preventDefault(); if (!toEdit) return; setMessage(''); try { const amountPaise = rupeesToPaise(editAmount); await editMutation.mutateAsync({ id: toEdit.id, data: { amountPaise, category: editCategory, merchant: editMerchant.trim(), occurredOn: editDate, note: editNote.trim() || null } }) } catch (error) { setMessage(userMessage(error as Parameters<typeof userMessage>[0], 'We could not update this expense. Please try again.')) } }
  async function confirmDelete() { if (!toDelete) return; setMessage(''); try { await deleteMutation.mutateAsync(toDelete.id) } catch (error) { setMessage(userMessage(error as Parameters<typeof userMessage>[0], 'We could not delete this expense. Please try again.')) } }

  if (query.isLoading) return <div className="space-y-4" aria-label="Loading transactions"><Skeleton className="h-10 w-1/2"/>{Array.from({ length: 7 }, (_, i) => <Skeleton key={i} className="h-14 w-full"/>)}</div>
  if (query.isError) return <ErrorState message="We couldn’t load transactions. Check your connection and try again." onRetry={() => void query.refetch()} />
  const rows = query.data ?? []
  const monthOptions = [...new Set([...monthsBefore(monthNow), month])].sort().reverse()
  return <div className="space-y-6">
    <div className="flex flex-wrap items-center justify-between gap-4"><div><h1 className="h1">Transactions</h1><p className="mt-1 body text-secondary">Expenses recorded for {monthName(month)}.</p></div><Link to="/add"><Button>Add expense</Button></Link></div>
    <div className="flex flex-wrap items-center gap-4"><label className="small text-secondary" htmlFor="transaction-month">Month</label><select id="transaction-month" className="h-10 rounded-control border border-border bg-canvas px-3 body" value={month} onChange={event => setMonth(event.target.value)}>{monthOptions.map(value => <option key={value} value={value}>{monthName(value)}</option>)}</select><div className="flex flex-wrap gap-2" aria-label="Filter by category"><button aria-pressed={category === ''} className={`min-h-10 rounded-full border px-3 small ${category === '' ? 'border-accent bg-accent-soft text-accent' : 'border-border text-secondary'}`} onClick={() => setCategory('')}>All</button>{CATEGORIES.map(item => <button key={item.code} aria-pressed={category === item.code} className={`min-h-10 rounded-full border px-3 small ${category === item.code ? 'border-accent bg-accent-soft text-accent' : 'border-border text-secondary'}`} onClick={() => setCategory(item.code)}>{item.name}</button>)}</div></div>
    {message && <p role="alert" className="small text-danger-text">{message}</p>}
    {!rows.length ? <section className="rounded-card border border-border bg-canvas p-6"><EmptyState text="No expenses recorded for this month yet." action="Add expense" onAction={() => navigate('/add')}/></section> : <>
      <div className="hidden overflow-x-auto rounded-card border border-border md:block"><table className="w-full border-collapse"><thead className="bg-subtle text-left caption text-secondary"><tr><th className="px-4 py-3">Date</th><th className="px-4 py-3">Merchant</th><th className="px-4 py-3">Category</th><th className="px-4 py-3 text-right">Amount</th><th className="px-4 py-3 text-right">Actions</th></tr></thead><tbody className="divide-y divide-border">{rows.map(item => <tr key={item.id} className="group h-14"><td className="px-4 small text-muted">{dateLabel(item.occurredOn)}</td><td className="px-4 body-strong">{item.merchant}</td><td className="px-4 small text-secondary">{item.category.displayName}</td><td className="px-4 text-right body-strong tabular-nums">{formatPaise(item.amountPaise)}</td><td className="px-4 text-right"><div className="flex justify-end gap-2 opacity-0 group-hover:opacity-100 group-focus-within:opacity-100"><Button variant="ghost" className="h-9 px-2 text-small" onClick={() => beginEdit(item)}>Edit</Button><Button variant="ghost" className="h-9 px-2 text-small" onClick={() => setToDelete(item)}>Delete</Button></div></td></tr>)}</tbody></table></div>
      <div className="space-y-3 md:hidden">{rows.map(item => <div key={item.id} className="rounded-card border border-border p-4"><div className="flex items-center justify-between gap-3"><p className="body-strong">{item.merchant}</p><p className="body-strong tabular-nums">{formatPaise(item.amountPaise)}</p></div><p className="mt-1 small text-secondary">{item.category.displayName} · {dateLabel(item.occurredOn)}</p><div className="mt-2 flex gap-3"><Button variant="ghost" className="min-h-10 px-1 text-small" onClick={() => beginEdit(item)}>Edit</Button><Button variant="ghost" className="min-h-10 px-1 text-small" onClick={() => setToDelete(item)}>Delete</Button></div></div>)}</div>
    </>}
    <Dialog.Root open={!!toDelete} onOpenChange={open => !open && setToDelete(null)}><Dialog.Portal><Dialog.Overlay className="fixed inset-0 z-40 bg-primary/25"/><Dialog.Content className="fixed left-1/2 top-1/2 z-50 w-[calc(100%-32px)] max-w-lg -translate-x-1/2 -translate-y-1/2 rounded-card border border-border bg-canvas p-6"><Dialog.Title className="h2">Delete expense</Dialog.Title><Dialog.Description className="mt-2 body text-secondary">Delete this expense of {toDelete ? formatPaise(toDelete.amountPaise) : ''}? This cannot be undone.</Dialog.Description>{message && <p role="alert" className="mt-3 small text-danger-text">{message}</p>}<div className="mt-6 flex justify-end gap-3"><Button variant="ghost" onClick={() => setToDelete(null)}>Cancel</Button><Button variant="danger" disabled={deleteMutation.isPending} onClick={() => void confirmDelete()}>{deleteMutation.isPending ? 'Deleting…' : 'Confirm delete'}</Button></div></Dialog.Content></Dialog.Portal></Dialog.Root>
    <Dialog.Root open={!!toEdit} onOpenChange={open => !open && setToEdit(null)}><Dialog.Portal><Dialog.Overlay className="fixed inset-0 z-40 bg-primary/25"/><Dialog.Content className="fixed left-1/2 top-1/2 z-50 max-h-[90vh] w-[calc(100%-32px)] max-w-xl -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-card border border-border bg-canvas p-6"><Dialog.Title className="h2">Edit expense</Dialog.Title><form onSubmit={saveEdit} className="mt-5 grid gap-4 sm:grid-cols-2"><div><Label htmlFor="edit-amount">Amount (₹)</Label><Input id="edit-amount" className="mt-1" value={editAmount} onChange={event => setEditAmount(event.target.value)}/></div><div><Label htmlFor="edit-category">Category</Label><select id="edit-category" className="mt-1 h-10 w-full rounded-control border border-border bg-canvas px-3 body" value={editCategory} onChange={event => setEditCategory(event.target.value)}>{CATEGORIES.map(item => <option key={item.code} value={item.code}>{item.name}</option>)}</select></div><div><Label htmlFor="edit-merchant">Merchant</Label><Input id="edit-merchant" className="mt-1" value={editMerchant} onChange={event => setEditMerchant(event.target.value)}/></div><div><Label htmlFor="edit-date">Date</Label><Input id="edit-date" type="date" className="mt-1" value={editDate} onChange={event => setEditDate(event.target.value)}/></div><div className="sm:col-span-2"><Label htmlFor="edit-note">Note (optional)</Label><Input id="edit-note" className="mt-1" value={editNote} onChange={event => setEditNote(event.target.value)}/></div>{message && <p role="alert" className="small text-danger-text sm:col-span-2">{message}</p>}<div className="flex justify-end gap-3 sm:col-span-2"><Button type="button" variant="ghost" onClick={() => setToEdit(null)}>Cancel</Button><Button type="submit" disabled={editMutation.isPending}>{editMutation.isPending ? 'Saving…' : 'Save changes'}</Button></div></form></Dialog.Content></Dialog.Portal></Dialog.Root>
  </div>
}
