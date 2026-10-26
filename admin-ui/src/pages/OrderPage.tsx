import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, ExternalLink } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { get } from '../api/client'
import type { OrderDetails } from '../api/types'
import { SendStatusBadge } from '../components/badges'
import { TelegramPreview } from '../components/TelegramPreview'
import { Card, Empty, ErrorBox, Skeleton } from '../components/ui'
import { dateTime, money } from '../lib/format'
import { ProposalCard } from './Proposals'

export default function OrderPage() {
  const id = Number(useParams().id)
  const { data, error, isLoading } = useQuery({ queryKey: ['order', id], queryFn: () => get<OrderDetails>(`/orders/${id}`) })
  if (isLoading) return <Skeleton className="h-96" />
  if (error || !data) return <ErrorBox error={error ?? 'Не найдено'} />
  const o = data.order

  return (
    <>
      <Link to="/orders" className="mb-4 inline-flex items-center gap-1 text-sm muted hover:text-slate-900 dark:hover:text-white">
        <ArrowLeft className="h-4 w-4" /> Заказы
      </Link>
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h1 className="text-xl font-semibold sm:text-2xl">{o.name}</h1>
          <p className="mt-1 text-sm muted">
            #{o.id} · {dateTime(o.createdAt)} · {o.category ?? 'без категории'}
          </p>
        </div>
        <a href={data.url} target="_blank" rel="noreferrer" className="inline-flex h-10 items-center gap-2 rounded-xl bg-brand-600 px-4 text-sm font-medium text-white hover:bg-brand-700">
          Открыть на Kwork <ExternalLink className="h-4 w-4" />
        </a>
      </div>

      <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
        {[
          ['Бюджет', money(o.price)],
          ['Срок', o.maxDays ? `${o.maxDays} дн.` : '—'],
          ['Откликов на Kwork', String(o.responses)],
          ['Разослано в боте', String(o.deliveries)],
        ].map(([l, v]) => (
          <div key={l} className="card p-4">
            <div className="text-xs muted">{l}</div>
            <div className="mt-1 text-lg font-semibold">{v}</div>
          </div>
        ))}
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card title="Описание">
          <p className="whitespace-pre-wrap text-sm leading-relaxed">{data.description || '—'}</p>
          {o.buyer && <p className="mt-4 text-sm muted">Покупатель: {o.buyer}</p>}
        </Card>
        <Card title="Карточка в Telegram">
          <TelegramPreview html={data.cardHtml} buttons={[['✍️ Отклик', '🔗 Открыть', '🙈 Скрыть']]} />
        </Card>
        <Card title={`Кому отправлен · ${data.recipients.length}`}>
          {data.recipients.length ? (
            <div className="divide-y divide-slate-100 dark:divide-slate-800">
              {data.recipients.map((r) => (
                <Link key={r.userId + r.sentAt} to={`/users/${r.userId}`} className="flex items-center justify-between gap-2 py-2.5 text-sm hover:opacity-80">
                  <span>{r.username ? '@' + r.username : `#${r.userId}`}</span>
                  <span className="flex items-center gap-2">
                    <span className="text-xs muted">{dateTime(r.sentAt)}</span>
                    <SendStatusBadge status={r.status} />
                  </span>
                </Link>
              ))}
            </div>
          ) : (
            <Empty title="Заказ никому не рассылался" hint="Нет подписчиков на эту подкатегорию или заказ пришёл до запуска бота" />
          )}
        </Card>
        <div className="space-y-3">
          <h2 className="px-1 text-sm font-semibold">Отклики · {data.proposals.length}</h2>
          {data.proposals.length ? data.proposals.map((p) => <ProposalCard key={p.id} p={p} showOrder={false} />) : <Card><Empty title="Откликов не генерировали" /></Card>}
        </div>
      </div>
    </>
  )
}
