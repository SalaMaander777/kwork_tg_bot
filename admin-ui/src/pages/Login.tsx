import { useQueryClient } from '@tanstack/react-query'
import { Lock } from 'lucide-react'
import { FormEvent, useState } from 'react'
import { get, post } from '../api/client'
import type { Me } from '../api/types'
import { Button, ErrorBox } from '../components/ui'

export function LoginPage({ enabled }: { enabled: boolean }) {
  const client = useQueryClient()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [loading, setLoading] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      await get('/auth/me') // получить свежий CSRF-токен
      const me = await post<Me>('/auth/login', { username, password })
      client.setQueryData(['me'], me)
    } catch (err) {
      setError(err)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative grid min-h-screen place-items-center overflow-hidden px-4">
      <div className="pointer-events-none absolute -top-40 left-1/2 h-[480px] w-[480px] -translate-x-1/2 rounded-full bg-brand-500/20 blur-3xl" />
      <form onSubmit={submit} className="card relative w-full max-w-sm animate-fade-in p-6 sm:p-8">
        <div className="mb-6 flex flex-col items-center text-center">
          <div className="mb-4 grid h-12 w-12 place-items-center rounded-2xl bg-gradient-to-br from-brand-500 to-violet-600 text-white shadow-lg shadow-brand-600/30">
            <Lock className="h-5 w-5" />
          </div>
          <h1 className="text-lg font-semibold">Kwork Bot</h1>
          <p className="text-sm muted">Вход в админ-панель</p>
        </div>
        {!enabled ? (
          <ErrorBox error="Админ-панель отключена. Задайте переменные ADMIN_USERNAME и ADMIN_PASSWORD_HASH (или ADMIN_PASSWORD) и перезапустите бота." />
        ) : (
          <div className="space-y-3">
            <input className="input h-11" placeholder="Логин" autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} autoFocus />
            <input className="input h-11" placeholder="Пароль" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
            <ErrorBox error={error} />
            <Button variant="primary" className="h-11 w-full" loading={loading} disabled={!username || !password}>
              Войти
            </Button>
          </div>
        )}
      </form>
    </div>
  )
}
