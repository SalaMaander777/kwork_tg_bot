import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { App } from './App'
import { ToastProvider } from './components/Toast'
import './index.css'
import { applyTheme, storedTheme } from './lib/theme'

applyTheme(storedTheme())
window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
  if (storedTheme() === 'system') applyTheme('system')
})

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 15_000, retry: (count, error: any) => error?.status >= 500 && count < 2, refetchOnWindowFocus: true },
  },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <BrowserRouter basename="/admin">
          <App />
        </BrowserRouter>
      </ToastProvider>
    </QueryClientProvider>
  </React.StrictMode>,
)
