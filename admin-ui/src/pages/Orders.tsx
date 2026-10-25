import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { ClipboardList, Search } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { get, qs } from '../api/client'
import type { Meta, OrderRow, Page } from '../api/types'
import { Badge, Card, Empty, ErrorBox, PageHeader, Pagination, Skeleton } from '../components/ui'
import { dateTime, money, num } from '../lib/format'
import { useDebounced } from '../lib/hooks'

const SIZE = 25

export default function Orders() {
  const [query, setQuery] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [priceMin, setPriceMin] = useState('')
  const [delivered, setDelivered] = useState(false)
  const [page, setPage] = useState(0)
  const q = useDebounced(query)
  const price = useDebounced(priceMin)
  const meta = useQuery({ queryKey: ['meta'], queryFn: () => get<Meta>('/meta'), staleTime: Infinity })
  const params = qs({ query: q, categoryId, priceMin: price, delivered: delivered || undefined, page, size: SIZE })
  const { data, isLoading, error } = useQuery({
    queryKey: ['orders', params],
    queryFn: () => get<Page<OrderRow>>('/orders' + params),
    placeholderData: keepPreviousData,
  })
  const reset = (fn: () => void) => {
    fn()
    setPage(0)
  }

  return (
    <>
      <PageHeader title="Заказы" subtitle={data ? `Найдено: ${num(data.total)}` : 'Заказы Kwork, собранные парсером'} />
      <Card>
        <div className="mb-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-[1fr_260px_160px_auto]">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input className="input pl-9" placeholder="Название, описание или ID" value={query} onChange={(e) => reset(() => setQuery(e.target.value))} />
          </div>
          <select className="input" value={categoryId} onChange={(e) => reset(() => setCategoryId(e.target.value))}>
            <option value="">Все подкатегории</option>
            {meta.data?.categories.map((root) => (
              <optgroup key={root.id} label={root.name}>
                {root.children.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </optgroup>
            ))}
          </select>
          <input className="input" type="number" min={0} placeholder="Бюджет от, ₽" value={priceMin} onChange={(e) => reset(() => setPriceMin(e.target.value))} />
          <label className="flex h-10 items-center gap-2 whitespace-nowrap px-1 text-sm">
            <input type="checkbox" className="h-4 w-4 accent-brand-600" checked={delivered} onChange={(e) => reset(() => setDelivered(e.target.checked))} />
            Только разосланные
          </label>
        </div>
        <ErrorBox error={error} />
        {isLoading && <Skeleton className="h-96" />}
        {data && !data.items.length && <Empty icon={<ClipboardList className="h-10 w-10" />} title="Заказов не найдено" />}
        {data && data.items.length > 0 && (
          <div className="divide-y divide-slate-100 dark:divide-slate-800">
            {data.items.map((o) => (
              <Link key={o.id} to={`/orders/${o.id}`} className="-mx-2 flex flex-col gap-2 rounded-xl px-2 py-3 transition hover:bg-slate-50 dark:hover:bg-slate-800/40 sm:flex-row sm:items-center">
                <div className="min-w-0 flex-1">
                  <div className="font-medium sm:truncate">{o.name}</div>
                  <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs muted">
                    <span>#{o.id}</span>
                    <span>{dateTime(o.createdAt)}</span>
                    {o.category && <span>{o.category}</span>}
                    {o.buyer && <span>👤 {o.buyer}</span>}
                  </div>
                </div>
                <div className="flex shrink-0 flex-wrap items-center gap-2 sm:justify-end">
                  <span className="text-sm font-semibold tabular-nums">{money(o.price)}</span>
                  <Badge tone="gray">💬 {o.responses}</Badge>
                  {o.deliveries > 0 && <Badge tone="green">📨 {o.deliveries}</Badge>}
                  {o.proposals > 0 && <Badge tone="blue">✍️ {o.proposals}</Badge>}
                </div>
              </Link>
            ))}
          </div>
        )}
        {data && <Pagination page={page} size={SIZE} total={data.total} onPage={setPage} />}
      </Card>
    </>
  )
}
