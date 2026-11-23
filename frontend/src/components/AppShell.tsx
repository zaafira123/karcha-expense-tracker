import { useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Menu, X } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { Button } from './ui/button'

const nav = [
  { to: '/dashboard', label: 'Dashboard', short: 'Home' }, { to: '/add', label: 'Add expense', short: 'Add' },
  { to: '/import', label: 'Import SMS', short: 'Import' },
  { to: '/transactions', label: 'Transactions', short: 'Transactions' }, { to: '/budgets', label: 'Budgets', short: 'Budgets' },
  { to: '/forecast', label: 'Forecast', short: 'Forecast' }, { to: '/agent', label: 'Agent activity', short: 'Agent activity' },
]
const extraNav = [{ to: '/import', label: 'Import SMS' }, { to: '/forecast', label: 'Forecast' }, { to: '/agent', label: 'Agent activity' }, { to: '/settings', label: 'Settings' }]
const titles: Record<string, string> = { '/dashboard': 'Dashboard', '/add': 'Add expense', '/import': 'Import from SMS', '/transactions': 'Transactions', '/budgets': 'Budgets', '/forecast': 'Forecast', '/agent': 'Agent activity', '/settings': 'Settings' }

export function AppShell() {
  const { user, signOut } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [moreOpen, setMoreOpen] = useState(false)
  const title = titles[location.pathname] ?? 'Kharcha'
  const linkClass = ({ isActive }: { isActive: boolean }) => `flex min-h-10 items-center rounded-control px-4 body-strong ${isActive ? 'bg-accent-soft text-accent' : 'text-secondary hover:bg-hover hover:text-primary'}`
  const logout = async () => { await signOut(); navigate('/signin', { replace: true }) }
  return <div className="min-h-screen bg-canvas">
    <aside className="fixed inset-y-0 left-0 z-20 hidden w-[240px] flex-col justify-between border-r border-border bg-subtle px-4 py-6 md:flex">
      <div><a href="/dashboard" className="mb-8 block px-2 h2">Kharcha</a><nav aria-label="Main navigation" className="space-y-1">{nav.map(item => <NavLink key={item.to} to={item.to} className={linkClass}>{item.label}</NavLink>)}</nav></div>
      <div><NavLink to="/settings" className={linkClass}>Settings</NavLink><div className="mt-3 border-t border-border pt-3"><p className="truncate px-4 small text-muted" title={user?.email}>{user?.email}</p><Button variant="ghost" className="mt-2 w-full justify-start" onClick={logout}>Sign out</Button></div></div>
    </aside>
    <div className="md:pl-[240px]">
      <header className="sticky top-0 z-10 flex h-16 items-center border-b border-border bg-canvas px-4 md:px-8"><span className="h2 md:hidden">{title}</span><span className="hidden body-strong text-secondary md:block">{title}</span></header>
      <main className="mx-auto max-w-[1184px] px-4 py-6 pb-24 md:px-8 md:py-8 md:pb-8"><Outlet /></main>
    </div>
    <nav aria-label="Mobile navigation" className="fixed inset-x-0 bottom-0 z-30 grid h-[64px] grid-cols-5 border-t border-border bg-canvas md:hidden">
      {nav.filter(item => ['/dashboard', '/add', '/transactions', '/budgets'].includes(item.to)).map(item => <NavLink key={item.to} to={item.to} className={({ isActive }) => `flex flex-col items-center justify-center px-1 text-center caption ${isActive ? 'text-accent' : 'text-muted'}`}>{item.short}</NavLink>)}
      <button className={`flex flex-col items-center justify-center text-center caption ${moreOpen ? 'text-accent' : 'text-muted'}`} onClick={() => setMoreOpen(value => !value)} aria-expanded={moreOpen}><span className="flex items-center gap-1">{moreOpen ? <X size={15}/> : <Menu size={15}/>}More</span></button>
    </nav>
    {moreOpen && <div className="fixed inset-x-0 bottom-16 z-30 border-t border-border bg-canvas p-4 md:hidden"><p className="mb-2 small text-muted">More</p>{extraNav.map(item => <NavLink key={item.to} to={item.to} onClick={() => setMoreOpen(false)} className="block min-h-10 py-2 body text-primary">{item.label}</NavLink>)}<p className="mt-2 truncate small text-muted">{user?.email}</p><button onClick={logout} className="mt-2 min-h-10 body-strong text-accent">Sign out</button></div>}
  </div>
}
