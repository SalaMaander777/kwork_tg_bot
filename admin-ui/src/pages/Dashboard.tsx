import { useQuery } from '@tanstack/react-query'
import clsx from 'clsx'
import { ArrowDownRight, ArrowUpRight, RefreshCw } from 'lucide-react'
import { ReactNode, useState } from 'react'
import { Link } from 'react-router-dom'
import { get, qs } from '../api/client'
import type { CategoryStat, FunnelStep, HeatCell, Health, Point, Summary, Tokens } from '../api/types'
import { COLORS, Sparkline, TrendChart } from '../components/charts'
import { Card, PageHeader, Segmented, Skeleton, StatusDot } from '../components/ui'
import { compact, delta, ms, num, num1 } from '../lib/format'
import { PeriodKey, PERIODS } from '../lib/period'

const REFRESH = 60_000
const SERIES = ['users_new', 'active_users', 'orders', 'deliveries', 'proposals', 'llm_errors', 'llm_latency', 'tokens']

type KpiDef = { key: string; label: string; spark?: string; format?: (v: number) => string; invert?: boolean; color: string; hint?: string }

const KPIS: KpiDef[] = [
  { key: 'users_new', label: 'Новые пользователи', spark: 'users_new', color: COLORS[0] },
  { key: 'active_users', label: 'Активные пользователи', spark: 'active_users', color: COLORS[5], hint: 'Совершали действия в боте' },
  { key: 'orders', label: 'Новых заказов', spark: 'orders', color: COLORS[4], hint: 'Спарсено с Kwork' },
  { key: 'deliveries', label: 'Доставлено карточек', spark: 'deliveries', color: COLORS[1] },
  { key: 'proposals', label: 'Сгенерировано откликов', spark: 'proposals', color: COLORS[2] },
  { key: 'conversion', label: 'Конверсия в отклик', format: (v) => num1(v) + '%', color: COLORS[1], hint: 'Заказов с откликом / доставленных карточек' },
  { key: 'llm_latency', label: 'Время ответа AI', spark: 'llm_latency', format: ms, invert: true, color: COLORS[5] },
  { key: 'llm_errors', label: 'Ошибки AI', spark: 'llm_errors', invert: true, color: COLORS[3] },
]

function DeltaBadge({ value, previous, invert }: { value: number; previous: number; invert?: boolean }) {
  const d = delta(value, previous)
  if (d === null) return <span className="text-xs font-medium text-emerald-600">новое</span>
  if (Math.abs(d) < 0.5) return <span className="text-xs muted">0%</span>
  const good = invert ? d < 0 : d > 0
  return (
    <span className={clsx('inline-flex items-center text-xs font-semibold', good ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400')}>
      {d > 0 ? <ArrowUpRight className="h-3.5 w-3.5" /> : <ArrowDownRight className="h-3.5 w-3.5" />}
      {num1(Math.abs(d))}%
    </span>
  )
}

function KpiCard({ def, summary, series }: { def: KpiDef; summary?: Summary; series?: Record<string, Point[]> }) {
  const kpi = summary?.[def.key]
  return (
    <div className="card flex flex-col gap-2 p-4">
      <div className="flex items-start justify-between gap-2">
        <span className="text-xs font-medium muted" title={def.hint}>
          {def.label}
        </span>
        {kpi && <DeltaBadge value={kpi.value} previous={kpi.previous} invert={def.invert} />}
      </div>
      {kpi ? (
        <div className="text-2xl font-semibold tabular-nums tracking-tight">{def.format ? def.format(kpi.value) : num(kpi.value)}</div>
      ) : (
        <Skeleton className="h-8 w-20" />
      )}
      {def.spark && series?.[def.spark] ? <Sparkline data={series[def.spark]} color={def.color} /> : <div className="h-10" />}
    </div>
  )
}

function HealthStrip() {
  const { data } = useQuery({ queryKey: ['health'], queryFn: () => get<Health[]>('/dashboard/health'), refetchInterval: REFRESH })
  if (!data) return <Skeleton className="h-20" />
  return (
    <div className="scrollbar-none -mx-4 flex gap-3 overflow-x-auto px-4 sm:mx-0 sm:grid sm:grid-cols-3 sm:px-0 xl:grid-cols-5">
      {data.map((h) => (
        <div
          key={h.title}
          className={clsx(
            'card min-w-[200px] p-3.5',
            h.status === 'error' && 'border-rose-200 dark:border-rose-500/30',
            h.status === 'warn' && 'border-amber-200 dark:border-amber-500/30',
          )}
        >
          <div className="flex items-center gap-2">
            <StatusDot status={h.status} />
            <span className="text-sm font-semibold">{h.title}</span>
          </div>
          <p className="mt-1 truncate text-xs muted" title={h.detail}>
            {h.detail}
          </p>
        </div>
      ))}
    </div>
  )
}

function Funnel({ steps }: { steps?: FunnelStep[] }) {
  if (!steps) return <Skeleton className="h-56" />
  const first = steps[0]?.count || 0
  return (
    <div className="space-y-3">
      {steps.map((s, i) => {
        const pct = first ? (s.count / first) * 100 : 0
        return (
          <div key={s.key}>
            <div className="mb-1 flex items-baseline justify-between gap-2 text-sm">
              <span className="text-slate-600 dark:text-slate-300">{s.label}</span>
              <span className="tabular-nums">
                <span className="font-semibold">{num(s.count)}</span>
                {i > 0 && <span className="ml-2 text-xs muted">{num1(pct)}%</span>}
              </span>
            </div>
            <div className="h-2.5 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800">
              <div className="h-full rounded-full bg-gradient-to-r from-brand-500 to-violet-500 transition-all" style={{ width: `${Math.max(pct, s.count ? 2 : 0)}%` }} />
            </div>
          </div>
        )
      })}
      {first === 0 && <p className="pt-2 text-xs muted">За период новых пользователей не было</p>}
    </div>
  )
}

const DAYS = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс']

function Heatmap({ cells }: { cells?: HeatCell[] }) {
  if (!cells) return <Skeleton className="h-48" />
  const max = Math.max(1, ...cells.map((c) => c.count))
  const map = new Map(cells.map((c) => [`${c.dow}-${c.hour}`, c.count]))
  return (
    <div className="overflow-x-auto">
      <div className="min-w-[560px]">
        <div className="grid grid-cols-[28px_repeat(24,1fr)] gap-[3px]">
          <span />
          {Array.from({ length: 24 }, (_, h) => (
            <span key={h} className="text-center text-[9px] muted">
              {h % 3 === 0 ? h : ''}
            </span>
          ))}
          {DAYS.map((d, di) => (
            <Row key={d} label={d}>
              {Array.from({ length: 24 }, (_, h) => {
                const v = map.get(`${di + 1}-${h}`) ?? 0
                return (
                  <div
                    key={h}
                    title={`${d}, ${h}:00 — ${v}`}
                    className="aspect-square rounded-[4px] bg-slate-100 dark:bg-slate-800"
                    style={v ? { background: `rgba(99,102,241,${0.15 + (v / max) * 0.85})` } : undefined}
                  />
                )
              })}
            </Row>
          ))}
        </div>
      </div>
      {cells.length === 0 && <p className="pt-3 text-xs muted">Событий за период нет</p>}
    </div>
  )
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <>
      <span className="self-center text-[10px] muted">{label}</span>
      {children}
    </>
  )
}

function TopCategories({ data }: { data?: CategoryStat[] }) {
  if (!data) return <Skeleton className="h-64" />
  const rows = data.filter((c) => c.orders || c.subscribers || c.proposals).slice(0, 10)
  const max = Math.max(1, ...rows.map((r) => r.orders))
  if (!rows.length) return <p className="text-sm muted">Нет данных за период</p>
  return (
    <div className="-mx-1 overflow-x-auto">
      <table className="w-full min-w-[420px] text-sm">
        <thead>
          <tr className="text-left text-xs muted">
            <th className="px-1 pb-2 font-medium">Подкатегория</th>
            <th className="px-1 pb-2 text-right font-medium">Заказов</th>
            <th className="px-1 pb-2 text-right font-medium">Подписчиков</th>
            <th className="px-1 pb-2 text-right font-medium">Откликов</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((c) => (
            <tr key={c.id} className="border-t border-slate-100 dark:border-slate-800">
              <td className="px-1 py-2">
                <div className="font-medium">{c.name}</div>
                <div className="mt-1 flex items-center gap-2">
                  <div className="h-1 w-24 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800">
                    <div className="h-full bg-sky-500" style={{ width: `${(c.orders / max) * 100}%` }} />
                  </div>
                  <span className="text-[11px] muted">{c.root}</span>
                </div>
              </td>
              <td className="px-1 py-2 text-right tabular-nums">{num(c.orders)}</td>
              <td className="px-1 py-2 text-right tabular-nums">{num(c.subscribers)}</td>
              <td className="px-1 py-2 text-right tabular-nums">{num(c.proposals)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function TokensCard({ data }: { data?: Tokens }) {
  if (!data) return <Skeleton className="h-40" />
  const used = data.allTimeTotal
  const pct = data.budget ? Math.min(100, (used / data.budget) * 100) : 0
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-3">
        <div>
          <div className="text-xs muted">За период</div>
          <div className="text-xl font-semibold tabular-nums">{compact(data.periodTotal)}</div>
        </div>
        <div>
          <div className="text-xs muted">Всего</div>
          <div className="text-xl font-semibold tabular-nums">{compact(used)}</div>
        </div>
      </div>
      {data.budget > 0 && (
        <div>
          <div className="mb-1 flex justify-between text-xs">
            <span className="muted">Бюджет {compact(data.budget)}</span>
            <span className="font-medium">осталось {compact(Math.max(0, data.budget - used))}</span>
          </div>
          <div className="h-2 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800">
            <div className={clsx('h-full rounded-full', pct > 90 ? 'bg-rose-500' : pct > 70 ? 'bg-amber-500' : 'bg-emerald-500')} style={{ width: `${pct}%` }} />
          </div>
        </div>
      )}
      <div className="space-y-2">
        {data.byModel.map((m) => (
          <div key={m.model} className="flex items-center justify-between text-sm">
            <span className="font-medium">{m.model}</span>
            <span className="tabular-nums muted">
              {num(m.calls)} выз. · {compact(m.promptTokens + m.completionTokens)} ток.
            </span>
          </div>
        ))}
        {!data.byModel.length && <p className="text-sm muted">Вызовов AI за период не было</p>}
      </div>
    </div>
  )
}

export default function Dashboard() {
  const [period, setPeriod] = useState<PeriodKey>('30')
  const p = qs({ days: period })
  const opts = { refetchInterval: REFRESH }
  const summary = useQuery({ queryKey: ['summary', period], queryFn: () => get<Summary>('/dashboard/summary' + p), ...opts })
  const series = useQuery({
    queryKey: ['series', period],
    queryFn: () => get<Record<string, Point[]>>('/dashboard/timeseries' + qs({ days: period, metrics: SERIES.join(',') })),
    ...opts,
  })
  const funnel = useQuery({ queryKey: ['funnel', period], queryFn: () => get<FunnelStep[]>('/dashboard/funnel' + p) })
  const categories = useQuery({ queryKey: ['categories', period], queryFn: () => get<CategoryStat[]>('/dashboard/categories' + p) })
  const heatmap = useQuery({ queryKey: ['heatmap', period], queryFn: () => get<HeatCell[]>('/dashboard/heatmap' + p) })
  const tokens = useQuery({ queryKey: ['tokens', period], queryFn: () => get<Tokens>('/dashboard/tokens' + p) })
  const s = series.data
  const sum = summary.data

  const refreshing = summary.isFetching || series.isFetching

  return (
    <>
      <PageHeader
        title="Дашборд"
        subtitle={
          <span className="inline-flex items-center gap-1.5">
            <RefreshCw className={clsx('h-3.5 w-3.5', refreshing && 'animate-spin')} /> Обновляется каждую минуту
          </span>
        }
        actions={<Segmented value={period} onChange={setPeriod} items={PERIODS} />}
      />

      <div className="space-y-4 sm:space-y-6">
        <HealthStrip />

        <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
          {KPIS.map((k) => (
            <KpiCard key={k.key} def={k} summary={sum} series={s} />
          ))}
        </div>

        <Card padded={false}>
          <div className="grid grid-cols-3 divide-x divide-y divide-slate-100 dark:divide-slate-800 sm:grid-cols-7 sm:divide-y-0">
            {[
              ['users_total', 'Всего'],
              ['users_active', 'Активны'],
              ['users_dau', 'DAU'],
              ['users_wau', 'WAU'],
              ['users_mau', 'MAU'],
              ['users_blocked', 'Заблокировали'],
              ['users_banned', 'Забанены'],
            ].map(([key, label]) => (
              <Link key={key} to="/users" className="px-4 py-3 transition hover:bg-slate-50 dark:hover:bg-slate-800/50">
                <div className="text-[11px] muted">{label}</div>
                <div className="text-lg font-semibold tabular-nums">{sum ? num(sum[key]?.value) : '…'}</div>
              </Link>
            ))}
          </div>
        </Card>

        <div className="grid gap-4 sm:gap-6 xl:grid-cols-3">
          <Card title="Заказы и доставки" className="xl:col-span-2">
            {s ? (
              <TrendChart
                series={[
                  { key: 'orders', label: 'Новые заказы', data: s.orders, color: COLORS[4] },
                  { key: 'deliveries', label: 'Доставлено карточек', data: s.deliveries, color: COLORS[1] },
                ]}
              />
            ) : (
              <Skeleton className="h-60" />
            )}
          </Card>
          <Card title="Воронка онбординга">
            <Funnel steps={funnel.data} />
          </Card>
        </div>

        <div className="grid gap-4 sm:gap-6 lg:grid-cols-2">
          <Card title="Пользователи">
            {s ? (
              <TrendChart
                kind="line"
                series={[
                  { key: 'active_users', label: 'Активные', data: s.active_users, color: COLORS[5] },
                  { key: 'users_new', label: 'Новые', data: s.users_new, color: COLORS[0] },
                ]}
              />
            ) : (
              <Skeleton className="h-60" />
            )}
          </Card>
          <Card title="Отклики">
            {s ? <TrendChart kind="bar" series={[{ key: 'proposals', label: 'Отклики', data: s.proposals, color: COLORS[2] }]} /> : <Skeleton className="h-60" />}
          </Card>
        </div>

        <div className="grid gap-4 sm:gap-6 lg:grid-cols-3">
          <Card title="AI: время ответа и ошибки" className="lg:col-span-2">
            {s ? (
              <div className="grid gap-4 md:grid-cols-2">
                <TrendChart height={200} kind="line" format={ms} series={[{ key: 'llm_latency', label: 'Среднее время', data: s.llm_latency, color: COLORS[5] }]} />
                <TrendChart height={200} kind="bar" series={[{ key: 'llm_errors', label: 'Ошибки', data: s.llm_errors, color: COLORS[3] }]} />
              </div>
            ) : (
              <Skeleton className="h-52" />
            )}
          </Card>
          <Card title="Токены GigaChat">
            <TokensCard data={tokens.data} />
          </Card>
        </div>

        <div className="grid gap-4 sm:gap-6 xl:grid-cols-2">
          <Card title="Топ подкатегорий">
            <TopCategories data={categories.data} />
          </Card>
          <Card title="Активность по часам">
            <Heatmap cells={heatmap.data} />
          </Card>
        </div>
      </div>
    </>
  )
}
