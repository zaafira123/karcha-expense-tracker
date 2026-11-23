import { Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { Button } from '../components/ui/button'
import { Skeleton } from '../components/Skeleton'
export function NotFoundPage() { const { user, ready } = useAuth(); if (!ready) return <main className="mx-auto flex min-h-screen max-w-lg flex-col items-center justify-center gap-4 px-6"><Skeleton className="h-10 w-3/4"/><Skeleton className="h-5 w-full"/></main>; const target = user ? '/dashboard' : '/'; return <main className="mx-auto flex min-h-screen max-w-lg flex-col items-center justify-center px-6 text-center"><h1 className="h1">Page not found</h1><p className="body mt-3 text-secondary">The page you’re looking for doesn’t exist.</p><Link className="mt-6" to={target}><Button>{user ? 'Go to dashboard' : 'Go to home'}</Button></Link></main> }
