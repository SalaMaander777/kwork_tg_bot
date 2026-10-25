import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Download, Search, Users as UsersIcon } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { get, qs } from '../api/client'
import type { Page, UserRow } from '../api/types'
import { Avatar, UserStateBadge } from '../components/badges'
import { Card, Empty, ErrorBox, PageHeader, Pagination, Skeleton } from '../components/ui'
import { ago, dateTime, num, STATE_LABELS, userName } from '../lib/format'
import { useDebounced } from '../lib/hooks'

const SIZE = 25

export default function Users() {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [state, setState] = useState('')
  const [banned, setBanned] = useState('')
  const [sort, setSort] = useState('created')
  const [page, setPage] = useState(0)
  const q = useDebounced(query)

  const params = qs({ query: q, state, banned, sort, page, size: SIZE })
  const { data, isLoading, error } = useQuery({
    queryKey: ['users', params],
    queryFn: () => get<Page<UserRow>>('/users' + params),
    placeholderData: keepPreviousData,
  })

  const reset = (fn: () => void) => {
    fn()
    setPage(0)
  }

  return (
    <>
      <PageHeader
        title="Пользователи"
        subtitle={data ? `Всего найдено: ${num(data.total)}` : undefined}
        actions={
          <a href="/admin/api/users/export.csv" className="inline-flex h-10 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-sm font-medium hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:hover:bg-slate-800">
            <Download className="h-4 w-4" /> CSV
          </a>
        }
      />
      <Card>
        <div className="mb-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-[1fr_200px_160px_180px]">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input className="input pl-9" placeholder="Имя, @username или ID" value={query} onChange={(e) => reset(() => setQuery(e.target.value))} />
          </div>
          <select className="input" value={state} onChange={(e) => reset(() => setState(e.target.value))}>
            <option value="">Все состояния</option>
            {Object.entries(STATE_LABELS).map(([k, v]) => (
              <option key={k} value={k}>
                {v}
              </option>
            ))}
          </select>
          <select className="input" value={banned} onChange={(e) => reset(() => setBanned(e.target.value))}>
            <option value="">Бан: все</option>
            <option value="true">Забаненные</option>
            <option value="false">Не забаненные</option>
          </select>
          <select className="input" value={sort} onChange={(e) => reset(() => setSort(e.target.value))}>
            <option value="created">Сначала новые</option>
            <option value="activity">По активности</option>
            <option value="proposals">По откликам</option>
            <option value="deliveries">По доставкам</option>
          </select>
        </div>

        <ErrorBox error={error} />
        {isLoading && (
          <div className="space-y-2">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-14" />
            ))}
          </div>
        )}
        {data && data.items.length === 0 && <Empty icon={<UsersIcon className="h-10 w-10" />} title="Никого не нашли" hint="Измените фильтры или поиск" />}

        {data && data.items.length > 0 && (
          <>
            {/* таблица — планшет и десктоп */}
            <div className="-mx-5 hidden overflow-x-auto md:block">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100 text-left text-xs muted dark:border-slate-800">
                    <th className="px-5 pb-3 font-medium">Пользователь</th>
                    <th className="px-3 pb-3 font-medium">Состояние</th>
                    <th className="px-3 pb-3 text-right font-medium">Категорий</th>
                    <th className="px-3 pb-3 text-right font-medium">Доставок</th>
                    <th className="px-3 pb-3 text-right font-medium">Откликов</th>
                    <th className="px-3 pb-3 font-medium">Активность</th>
                    <th className="px-5 pb-3 font-medium">Регистрация</th>
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((u) => (
                    <tr key={u.id} onClick={() => navigate(`/users/${u.id}`)} className="cursor-pointer border-b border-slate-50 transition hover:bg-slate-50 dark:border-slate-800/60 dark:hover:bg-slate-800/40">
                      <td className="px-5 py-3">
                        <div className="flex items-center gap-3">
                          <Avatar name={userName(u)} />
                          <div className="min-w-0">
                            <div className="truncate font-medium">{userName(u)}</div>
                            <div className="truncate text-xs muted">{u.username ? '@' + u.username : '—'} · {u.id}</div>
                          </div>
                        </div>
                      </td>
                      <td className="px-3 py-3">
                        <UserStateBadge user={u} />
                      </td>
                      <td className="px-3 py-3 text-right tabular-nums">{u.subcategories}</td>
                      <td className="px-3 py-3 text-right tabular-nums">{num(u.deliveries)}</td>
                      <td className="px-3 py-3 text-right tabular-nums">{num(u.proposals)}</td>
                      <td className="px-3 py-3 text-xs muted">{ago(u.lastActivityAt)}</td>
                      <td className="px-5 py-3 text-xs muted">{dateTime(u.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* карточки — телефон */}
            <div className="space-y-2 md:hidden">
              {data.items.map((u) => (
                <Link key={u.id} to={`/users/${u.id}`} className="flex items-center gap-3 rounded-2xl border border-slate-100 p-3 active:bg-slate-50 dark:border-slate-800">
                  <Avatar name={userName(u)} />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center justify-between gap-2">
                      <span className="truncate font-medium">{userName(u)}</span>
                      <span className="shrink-0 text-[11px] muted">{ago(u.lastActivityAt)}</span>
                    </div>
                    <div className="mt-1 flex flex-wrap items-center gap-2">
                      <UserStateBadge user={u} />
                      <span className="text-xs muted">
                        {u.deliveries} карт. · {u.proposals} откл.
                      </span>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
            <Pagination page={page} size={SIZE} total={data.total} onPage={setPage} />
          </>
        )}
      </Card>
    </>
  )
}
