import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { Toast } from './EmptyState'

type ToastItem = { id: number; message: string; tone: 'success' | 'error' }
type ToastContextValue = { showToast: (message: string, tone?: 'success' | 'error') => void }
const ToastContext = createContext<ToastContextValue | null>(null)
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<ToastItem | null>(null)
  const showToast = useCallback((message: string, tone: 'success' | 'error' = 'success') => setToast({ id: Date.now(), message, tone }), [])
  const value = useMemo(() => ({ showToast }), [showToast])
  return <ToastContext.Provider value={value}>{children}{toast && <Toast key={toast.id} tone={toast.tone} onDismiss={() => setToast(null)}>{toast.message}</Toast>}</ToastContext.Provider>
}
export function useToast() { const context = useContext(ToastContext); if (!context) throw new Error('useToast must be used within ToastProvider'); return context }
