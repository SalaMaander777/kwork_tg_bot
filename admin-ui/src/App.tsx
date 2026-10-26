import { useQuery, useQueryClient } from '@tanstack/react-query'
import { lazy, Suspense, useEffect } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { get, post, setUnauthorizedHandler } from './api/client'
import type { Me } from './api/types'
import { Layout } from './components/Layout'
import { Skeleton } from './components/ui'
import { LoginPage } from './pages/Login'

const Dashboard = lazy(() => import('./pages/Dashboard'))
const Users = lazy(() => import('./pages/Users'))
const UserPage = lazy(() => import('./pages/UserPage'))
const Orders = lazy(() => import('./pages/Orders'))
const OrderPage = lazy(() => import('./pages/OrderPage'))
const Proposals = lazy(() => import('./pages/Proposals'))
const Ai = lazy(() => import('./pages/Ai'))
const Broadcasts = lazy(() => import('./pages/Broadcasts'))
const SettingsPage = lazy(() => import('./pages/Settings'))
const Audit = lazy(() => import('./pages/Audit'))

function PageFallback() {
  return (
    <div className="space-y-4">
      <Skeleton className="h-8 w-48" />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-28" />
        ))}
      </div>
      <Skeleton className="h-72" />
    </div>
  )
}

export function App() {
  const client = useQueryClient()
  const me = useQuery({ queryKey: ['me'], queryFn: () => get<Me>('/auth/me'), staleTime: Infinity })

  useEffect(() => {
    setUnauthorizedHandler(() => client.setQueryData<Me>(['me'], (old) => ({ enabled: old?.enabled ?? true, authenticated: false, username: null })))
  }, [client])

  if (me.isLoading) {
    return (
      <div className="grid min-h-screen place-items-center">
        <Skeleton className="h-10 w-10 rounded-full" />
      </div>
    )
  }
  if (!me.data?.authenticated) {
    return <LoginPage enabled={me.data?.enabled ?? true} />
  }

  const logout = async () => {
    await post('/auth/logout').catch(() => undefined)
    client.clear()
    await me.refetch()
  }

  return (
    <Routes>
      <Route element={<Layout username={me.data.username ?? 'admin'} onLogout={logout} />}>
        {[
          ['/', <Dashboard />],
          ['/users', <Users />],
          ['/users/:id', <UserPage />],
          ['/orders', <Orders />],
          ['/orders/:id', <OrderPage />],
          ['/proposals', <Proposals />],
          ['/ai', <Ai />],
          ['/broadcasts', <Broadcasts />],
          ['/settings', <SettingsPage />],
          ['/audit', <Audit />],
        ].map(([path, el]) => (
          <Route key={path as string} path={path as string} element={<Suspense fallback={<PageFallback />}>{el}</Suspense>} />
        ))}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
