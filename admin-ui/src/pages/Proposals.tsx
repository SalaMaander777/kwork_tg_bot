import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Check, Copy, FileText } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { get, qs } from '../api/client'
import type { Page, ProposalRow } from '../api/types'
import { Badge, Card, Empty, ErrorBox, PageHeader, Pagination, Skeleton } from '../components/ui'
import { dateTime, ms, num } from '../lib/format'

export function ProposalCard({ p, showUser = true, showOrder = true }: { p: ProposalRow; showUser?: boolean; showOrder?: boolean }) {
  const [copied, setCopied] = useState(false)
  const [expanded, setExpanded] = useState(false)
  const long = p.text.length > 420
  const copy = async () => {
    await navigator.clipboard?.writeText(p.text)
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }
  return (
    <div className="card p-4">
      <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
        <div className="min-w-0 text-sm">
          {showOrder && (
            <Link to={`/orders/${p.kworkId}`} className="block truncate font-medium hover:text-brand-600">
              {p.kworkName ?? `Заказ #${p.kworkId}`}
            </Link>
          )}
          <div className="flex flex-wrap items-center gap-2 text-xs muted">
            {showUser && (
              <Link to={`/users/${p.userId}`} className="hover:text-brand-600">
                {p.username ? '@' + p.username : `#${p.userId}`}
              </Link>
            )}
            <span>{dateTime(p.createdAt)}</span>
          </div>
        </div>
        <div className="flex items-center gap-1.5">
          {p.model && <Badge tone="blue">{p.model.split(':')[0]}</Badge>}
          {p.durationMs != null && <Badge>{ms(p.durationMs)}</Badge>}
          {p.tokens != null && <Badge>{num(p.tokens)} ток.</Badge>}
          <button onClick={copy} className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-700 dark:hover:bg-slate-800" title="Копировать">
            {copied ? <Check className="h-4 w-4 text-emerald-500" /> : <Copy className="h-4 w-4" />}
          </button>
        </div>
      </div>
      <blockquote className="whitespace-pre-wrap border-l-[3px] border-brand-400 pl-3 text-sm leading-relaxed text-slate-700 dark:text-slate-300">
        {long && !expanded ? p.text.slice(0, 420) + '…' : p.text}
      </blockquote>
      {long && (
        <button onClick={() => setExpanded(!expanded)} className="mt-2 text-xs font-medium text-brand-600 dark:text-brand-400">
          {expanded ? 'Свернуть' : 'Показать полностью'}
        </button>
      )}
    </div>
  )
}

export default function Proposals() {
  const [page, setPage] = useState(0)
  const params = qs({ page, size: 20 })
  const { data, isLoading, error } = useQuery({
    queryKey: ['proposals', params],
    queryFn: () => get<Page<ProposalRow>>('/proposals' + params),
    placeholderData: keepPreviousData,
  })
  return (
    <>
      <PageHeader title="Отклики" subtitle={data ? `Всего: ${num(data.total)}` : 'Все сгенерированные отклики'} />
      <ErrorBox error={error} />
      {isLoading && <Skeleton className="h-96" />}
      {data && !data.items.length && (
        <Card>
          <Empty icon={<FileText className="h-10 w-10" />} title="Откликов пока нет" hint="Они появятся, когда пользователи нажмут «✍️ Отклик» под заказом" />
        </Card>
      )}
      <div className="grid gap-3 xl:grid-cols-2">{data?.items.map((p) => <ProposalCard key={p.id} p={p} />)}</div>
      {data && <Pagination page={page} size={20} total={data.total} onPage={setPage} />}
    </>
  )
}
