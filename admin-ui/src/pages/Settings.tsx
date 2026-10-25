import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Play, RotateCcw, Save } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, get, post, put } from '../api/client'
import type { Setting, SystemInfo } from '../api/types'
import { useToast } from '../components/Toast'
import { Button, Card, Field, Modal, PageHeader, Skeleton, Switch } from '../components/ui'
import { dateTime, duration, num } from '../lib/format'

const HIDDEN = new Set(['ai.system-prompt', 'ai.model', 'ai.temperature']) // редактируются на странице AI

export default function SettingsPage() {
  const client = useQueryClient()
  const toast = useToast()
  const { data } = useQuery({ queryKey: ['settings'], queryFn: () => get<Setting[]>('/settings') })
  const [draft, setDraft] = useState<Record<string, string>>({})
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [resetConfirm, setResetConfirm] = useState(false)

  useEffect(() => {
    if (data) setDraft(Object.fromEntries(data.map((s) => [s.def.key, s.value])))
  }, [data])

  const changes = useMemo(() => Object.fromEntries((data ?? []).filter((s) => draft[s.def.key] !== undefined && draft[s.def.key] !== s.value).map((s) => [s.def.key, draft[s.def.key]])), [data, draft])
  const dirty = Object.keys(changes).length > 0

  const save = useMutation({
    mutationFn: (c: Record<string, string>) => put<Setting[]>('/settings', c),
    onSuccess: (d) => {
      client.setQueryData(['settings'], d)
      client.invalidateQueries({ queryKey: ['health'] })
      setErrors({})
      toast('Настройки сохранены')
    },
    onError: (e: ApiError) => {
      setErrors(e.fields ?? {})
      toast(e.message, true)
    },
  })
  // переключатели сохраняются сразу
  const toggle = (key: string, v: boolean) => {
    setDraft({ ...draft, [key]: String(v) })
    save.mutate({ [key]: String(v) })
  }
  const runNow = useMutation({
    mutationFn: () => post<{ processed: number }>('/notifier/run-now'),
    onSuccess: (r) => toast(r.processed < 0 ? 'Проверка уже идёт' : `Новых заказов обработано: ${r.processed}`),
    onError: (e: Error) => toast(e.message, true),
  })
  const resetCursor = useMutation({
    mutationFn: () => post<{ cursor: number }>('/notifier/reset-cursor'),
    onSuccess: (r) => {
      toast(`Курсор сдвинут на #${r.cursor}`)
      setResetConfirm(false)
    },
    onError: (e: Error) => toast(e.message, true),
  })

  if (!data) return <Skeleton className="h-96" />
  const groups = [...new Set(data.map((s) => s.def.group))]

  return (
    <>
      <PageHeader title="Настройки" subtitle="Применяются сразу, без перезапуска бота" />
      <div className="grid gap-4 lg:grid-cols-2">
        {groups.map((group) => (
          <Card key={group} title={group}>
            <div className="space-y-5">
              {data
                .filter((s) => s.def.group === group && !HIDDEN.has(s.def.key))
                .map((s) => (
                  <SettingControl key={s.def.key} s={s} value={draft[s.def.key] ?? s.value} error={errors[s.def.key]} onChange={(v) => setDraft({ ...draft, [s.def.key]: v })} onToggle={(v) => toggle(s.def.key, v)} />
                ))}
              {group === 'AI' && (
                <p className="text-sm muted">
                  Модель, температура и промпт — на странице{' '}
                  <Link to="/ai" className="text-brand-600 hover:underline dark:text-brand-400">
                    AI
                  </Link>
                  .
                </p>
              )}
              {group === 'Рассылка' && (
                <div className="flex flex-wrap gap-2 border-t border-slate-100 pt-4 dark:border-slate-800">
                  <Button size="sm" icon={<Play className="h-3.5 w-3.5" />} loading={runNow.isPending} onClick={() => runNow.mutate()}>
                    Проверить заказы сейчас
                  </Button>
                  <Button size="sm" variant="ghost" icon={<RotateCcw className="h-3.5 w-3.5" />} onClick={() => setResetConfirm(true)}>
                    Пропустить накопившиеся
                  </Button>
                </div>
              )}
            </div>
          </Card>
        ))}
        <SystemCard />
      </div>

      {dirty && (
        <div className="fixed inset-x-0 bottom-16 z-20 px-4 lg:bottom-6 lg:pl-64">
          <div className="mx-auto flex max-w-xl animate-fade-in items-center justify-between gap-3 rounded-2xl bg-slate-900 px-4 py-3 text-white shadow-2xl dark:bg-white dark:text-slate-900">
            <span className="text-sm">Изменено: {Object.keys(changes).length}</span>
            <div className="flex gap-2">
              <Button size="sm" variant="ghost" className="text-white dark:text-slate-900" onClick={() => setDraft(Object.fromEntries(data.map((s) => [s.def.key, s.value])))}>
                Отменить
              </Button>
              <Button size="sm" variant="primary" icon={<Save className="h-3.5 w-3.5" />} loading={save.isPending} onClick={() => save.mutate(changes)}>
                Сохранить
              </Button>
            </div>
          </div>
        </div>
      )}

      <Modal
        open={resetConfirm}
        onClose={() => setResetConfirm(false)}
        title="Пропустить накопившиеся заказы?"
        footer={
          <Button variant="primary" loading={resetCursor.isPending} onClick={() => resetCursor.mutate()}>
            Пропустить
          </Button>
        }
      >
        <p className="text-sm">Заказы, которые уже есть в базе, но ещё не разосланы, не будут отправлены пользователям. Рассылка продолжится с новых заказов.</p>
      </Modal>
    </>
  )
}

function SettingControl({ s, value, error, onChange, onToggle }: { s: Setting; value: string; error?: string; onChange: (v: string) => void; onToggle: (v: boolean) => void }) {
  const { def } = s
  if (def.type === 'BOOLEAN') {
    return (
      <div className="flex items-start justify-between gap-4">
        <div>
          <div className="text-sm font-medium">{def.label}</div>
          {def.description && <div className="text-xs muted">{def.description}</div>}
        </div>
        <Switch checked={value === 'true'} onChange={onToggle} />
      </div>
    )
  }
  const hint = [def.description, def.min != null && def.max != null && def.max < 1e9 ? `от ${def.min} до ${def.max}` : null].filter(Boolean).join(' · ')
  return (
    <Field label={def.label} hint={hint || undefined} error={error}>
      {def.type === 'TEXT' ? (
        <textarea className="input min-h-[80px]" value={value} onChange={(e) => onChange(e.target.value)} />
      ) : def.type === 'CHOICE' ? (
        <select className="input" value={value} onChange={(e) => onChange(e.target.value)}>
          {def.options.map((o) => (
            <option key={o}>{o}</option>
          ))}
        </select>
      ) : (
        <input className="input" type="number" step={def.type === 'DECIMAL' ? 0.1 : 1} value={value} onChange={(e) => onChange(e.target.value)} />
      )}
    </Field>
  )
}

function SystemCard() {
  const { data } = useQuery({ queryKey: ['system'], queryFn: () => get<SystemInfo>('/system'), refetchInterval: 30_000 })
  if (!data) return <Skeleton className="h-64" />
  return (
    <Card title="Система">
      <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
        <dt className="muted">Версия</dt>
        <dd>{data.version}</dd>
        <dt className="muted">Java</dt>
        <dd>{data.java}</dd>
        <dt className="muted">Запущен</dt>
        <dd>
          {dateTime(data.startedAt)} <span className="muted">({duration(data.uptimeSeconds)})</span>
        </dd>
        <dt className="muted">Память</dt>
        <dd>
          {data.memoryUsedMb} / {data.memoryMaxMb} МБ
        </dd>
        <dt className="muted">База данных</dt>
        <dd>{data.dbSizeMb} МБ</dd>
      </dl>
      <div className="mt-4 overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-xs muted">
              <th className="pb-2 font-medium">Таблица</th>
              <th className="pb-2 text-right font-medium">Строк</th>
              <th className="pb-2 text-right font-medium">Размер</th>
            </tr>
          </thead>
          <tbody>
            {Object.entries(data.tables).map(([name, t]) => (
              <tr key={name} className="border-t border-slate-100 dark:border-slate-800">
                <td className="py-1.5 font-mono text-xs">{name}</td>
                <td className="py-1.5 text-right tabular-nums">{num(t.rows)}</td>
                <td className="py-1.5 text-right tabular-nums muted">{t.sizeKb >= 1024 ? `${(t.sizeKb / 1024).toFixed(1)} МБ` : `${t.sizeKb} КБ`}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  )
}
