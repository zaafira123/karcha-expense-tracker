import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './contexts/AuthContext'
import { AppShell } from './components/AppShell'
import { ProtectedRoute } from './components/ProtectedRoute'
import { DashboardPage } from './pages/DashboardPage'
import { LandingPage } from './pages/LandingPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { SignInPage } from './pages/SignInPage'
import { SignInSkeleton } from './components/Skeleton'
import { AddExpensePage } from './pages/AddExpensePage'
import { TransactionsPage } from './pages/TransactionsPage'
import { BudgetsPage } from './pages/BudgetsPage'
import { ForecastPage } from './pages/ForecastPage'
import { AgentPage } from './pages/AgentPage'
import { SettingsPage } from './pages/SettingsPage'
import { PrivacyPage } from './pages/PrivacyPage'
import { TermsPage } from './pages/TermsPage'
import { SmsImportPage } from './pages/SmsImportPage'

function SignInRoute() { const { user, ready } = useAuth(); if (!ready) return <SignInSkeleton/>; return user ? <Navigate to="/dashboard" replace/> : <SignInPage/> }
export function App() {
  return <Routes>
    <Route path="/" element={<LandingPage/>}/>
    <Route path="/signin" element={<SignInRoute/>}/>
    <Route path="/privacy" element={<PrivacyPage/>}/><Route path="/terms" element={<TermsPage/>}/>
    <Route element={<ProtectedRoute/>}>
      <Route element={<AppShell/>}>
        <Route path="/dashboard" element={<DashboardPage/>}/>
        <Route path="/add" element={<AddExpensePage/>}/><Route path="/transactions" element={<TransactionsPage/>}/>
        <Route path="/import" element={<SmsImportPage/>}/>
        <Route path="/budgets" element={<BudgetsPage/>}/><Route path="/forecast" element={<ForecastPage/>}/>
        <Route path="/agent" element={<AgentPage/>}/><Route path="/settings" element={<SettingsPage/>}/>
      </Route>
    </Route>
    <Route path="*" element={<NotFoundPage/>}/>
  </Routes>
}
