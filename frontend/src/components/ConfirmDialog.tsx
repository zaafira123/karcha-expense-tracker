import * as DialogPrimitive from '@radix-ui/react-dialog'
import type { ReactNode } from 'react'
import { Button } from './ui/button'

export function ConfirmDialog({ open, onOpenChange, title, description, confirmText, onConfirm, children, danger = false, busy = false }: { open: boolean; onOpenChange: (open: boolean) => void; title: string; description?: string; confirmText: string; onConfirm: () => void; children?: ReactNode; danger?: boolean; busy?: boolean }) {
  return <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}><DialogPrimitive.Portal><DialogPrimitive.Overlay className="fixed inset-0 z-40 bg-primary/25"/><DialogPrimitive.Content className="fixed left-1/2 top-1/2 z-50 w-[calc(100%-32px)] max-w-lg -translate-x-1/2 -translate-y-1/2 rounded-card border border-border bg-canvas p-6 max-sm:p-4"><DialogPrimitive.Title className="h2">{title}</DialogPrimitive.Title>{description && <DialogPrimitive.Description className="mt-2 body text-secondary">{description}</DialogPrimitive.Description>}{children}<div className="mt-6 flex justify-end gap-3"><Button variant="ghost" onClick={() => onOpenChange(false)}>Cancel</Button><Button variant={danger ? 'danger' : 'primary'} disabled={busy} onClick={onConfirm}>{busy ? 'Please wait…' : confirmText}</Button></div></DialogPrimitive.Content></DialogPrimitive.Portal></DialogPrimitive.Root>
}
