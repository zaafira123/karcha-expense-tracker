import type { ButtonHTMLAttributes } from 'react'
import { clsx } from 'clsx'
type Props = ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'ghost' | 'danger' }
export function Button({ className, variant = 'primary', ...props }: Props) {
  const styles = { primary: 'bg-accent text-white hover:bg-accent-hover border border-transparent', secondary: 'bg-canvas text-primary border border-border-strong hover:bg-hover', ghost: 'bg-transparent text-accent hover:bg-accent-soft border border-transparent', danger: 'bg-canvas text-danger-text border border-danger-text hover:bg-danger-bg' }
  return <button className={clsx('inline-flex min-h-10 items-center justify-center rounded-control px-4 body-strong disabled:cursor-not-allowed disabled:opacity-60', styles[variant], className)} {...props} />
}
