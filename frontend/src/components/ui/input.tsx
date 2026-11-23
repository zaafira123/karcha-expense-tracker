import type { InputHTMLAttributes } from 'react'
import { clsx } from 'clsx'
export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) { return <input className={clsx('h-10 w-full rounded-control border border-border bg-canvas px-3 body text-primary placeholder:text-muted focus:border-border-strong', className)} {...props} /> }
