import React from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { App } from './App'
import { AuthProvider } from './contexts/AuthContext'
import { ToastProvider } from './components/ToastProvider'
import './index.css'
import { initializeShareTargetReceiver } from './lib/shareTarget'

initializeShareTargetReceiver()
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => { void navigator.serviceWorker.register('/sw.js') })
}

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } } })
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode><QueryClientProvider client={queryClient}><AuthProvider><ToastProvider><BrowserRouter><App/></BrowserRouter></ToastProvider></AuthProvider></QueryClientProvider></React.StrictMode>,
)
