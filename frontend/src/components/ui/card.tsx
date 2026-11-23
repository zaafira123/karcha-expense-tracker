import type { HTMLAttributes } from 'react'
import { clsx } from 'clsx'
export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) { return <section className={clsx('rounded-card border border-border bg-canvas p-6 max-sm:p-4', className)} {...props} /> }
