import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { Skeleton } from './Skeleton'
export function ProtectedRoute() {
  const { user, ready, sessionMessage } = useAuth()
  const location = useLocation()
  if (!ready) return <main className="mx-auto max-w-5xl space-y-4 p-8" aria-label="Loading session"><Skeleton className="h-10 w-1/3"/><Skeleton className="h-48 w-full"/></main>
  if (!user) return <Navigate to="/signin" replace state={{ from: location, message: sessionMessage }} />
  return <Outlet />
}
