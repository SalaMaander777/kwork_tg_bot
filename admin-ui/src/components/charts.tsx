import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { useId } from 'react'
import { compact, num1 } from '../lib/format'
import type { Point } from '../api/types'

export const COLORS = ['#6366f1', '#10b981', '#f59e0b', '#f43f5e', '#0ea5e9', '#8b5cf6']

export type Series = { key: string; label: string; data: Point[]; color?: string }

function merge(series: Series[]) {
  const rows = new Map<string, Record<string, number | string>>()
  series.forEach((s) =>
    s.data.forEach((p) => {
      const row = rows.get(p.t) ?? { t: p.t }
      row[s.key] = p.value
      rows.set(p.t, row)
    }),
  )
  return [...rows.values()].sort((a, b) => String(a.t).localeCompare(String(b.t)))
}

function tick(t: string, hourly: boolean) {
  const d = new Date(t)
  return hourly
    ? d.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
    : d.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' })
}

function isHourly(series: Series[]) {
  const pts = series[0]?.data ?? []
  return pts.length > 1 && new Date(pts[1].t).getTime() - new Date(pts[0].t).getTime() <= 3600_000
}

function ChartTooltip({ active, payload, label, hourly, format }: any) {
  if (!active || !payload?.length) return null
  const d = new Date(label)
  return (
    <div className="rounded-xl border border-slate-200 bg-white/95 px-3 py-2 text-xs shadow-lg backdrop-blur dark:border-slate-700 dark:bg-slate-900/95">
      <div className="mb-1 font-medium text-slate-500">
        {d.toLocaleString('ru-RU', hourly ? { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' } : { weekday: 'short', day: 'numeric', month: 'long' })}
      </div>
      {payload.map((p: any) => (
        <div key={p.dataKey} className="flex items-center gap-2">
          <span className="h-2 w-2 rounded-full" style={{ background: p.color }} />
          <span className="text-slate-600 dark:text-slate-300">{p.name}</span>
          <span className="ml-auto pl-3 font-semibold tabular-nums">{format ? format(p.value) : num1(p.value)}</span>
        </div>
      ))}
    </div>
  )
}

export function TrendChart({ series, height = 240, kind = 'area', format }: {
  series: Series[]
  height?: number
  kind?: 'area' | 'bar' | 'line'
  format?: (v: number) => string
}) {
  const data = merge(series)
  const hourly = isHourly(series)
  const common = {
    data,
    margin: { top: 8, right: 8, left: -12, bottom: 0 },
  }
  const axes = [
    <CartesianGrid key="g" vertical={false} strokeDasharray="3 3" />,
    <XAxis key="x" dataKey="t" tickFormatter={(t) => tick(t, hourly)} tickLine={false} axisLine={false} minTickGap={24} />,
    <YAxis key="y" tickFormatter={(v) => (format ? format(v) : compact(v))} tickLine={false} axisLine={false} width={48} allowDecimals={false} />,
    <Tooltip key="t" content={<ChartTooltip hourly={hourly} format={format} />} />,
    series.length > 1 ? <Legend key="l" iconType="circle" iconSize={8} wrapperStyle={{ fontSize: 12 }} /> : null,
  ]
  return (
    <div style={{ height }} className="w-full">
      <ResponsiveContainer>
        {kind === 'bar' ? (
          <BarChart {...common}>
            {axes}
            {series.map((s, i) => (
              <Bar key={s.key} dataKey={s.key} name={s.label} fill={s.color ?? COLORS[i]} radius={[6, 6, 0, 0]} maxBarSize={28} />
            ))}
          </BarChart>
        ) : kind === 'line' ? (
          <LineChart {...common}>
            {axes}
            {series.map((s, i) => (
              <Line key={s.key} type="monotone" dataKey={s.key} name={s.label} stroke={s.color ?? COLORS[i]} strokeWidth={2} dot={false} />
            ))}
          </LineChart>
        ) : (
          <AreaChart {...common}>
            <defs>
              {series.map((s, i) => (
                <linearGradient key={s.key} id={`g-${s.key}`} x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor={s.color ?? COLORS[i]} stopOpacity={0.3} />
                  <stop offset="100%" stopColor={s.color ?? COLORS[i]} stopOpacity={0} />
                </linearGradient>
              ))}
            </defs>
            {axes}
            {series.map((s, i) => (
              <Area
                key={s.key}
                type="monotone"
                dataKey={s.key}
                name={s.label}
                stroke={s.color ?? COLORS[i]}
                strokeWidth={2}
                fill={`url(#g-${s.key})`}
              />
            ))}
          </AreaChart>
        )}
      </ResponsiveContainer>
    </div>
  )
}

export function Sparkline({ data, color = COLORS[0] }: { data: Point[]; color?: string }) {
  const id = 's' + useId().replace(/:/g, '')
  return (
    <div className="h-10 w-full">
      <ResponsiveContainer>
        <AreaChart data={data} margin={{ top: 2, right: 0, left: 0, bottom: 0 }}>
          <defs>
            <linearGradient id={id} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={color} stopOpacity={0.35} />
              <stop offset="100%" stopColor={color} stopOpacity={0} />
            </linearGradient>
          </defs>
          <Area type="monotone" dataKey="value" stroke={color} strokeWidth={1.75} fill={`url(#${id})`} isAnimationActive={false} />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
