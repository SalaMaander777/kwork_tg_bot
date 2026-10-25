import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import clsx from 'clsx'
import { FlaskConical, RotateCcw, Save } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, get, post, put, qs } from '../api/client'
import type { LlmCallRow, Page, Setting } from '../api/types'
import { useToast } from '../components/Toast'
import { Badge, Button, Card, Empty, ErrorBox, Field, PageHeader, Pagination, Segmented, Skeleton, Tabs } from '../components/ui'
import { dateTime, ms, num } from '../lib/format'

type TestResult = { text: string; model: string; promptTokens: number | null; completionTokens: number | null; durationMs: number }

export default function Ai() {
  const [tab, setTab] = useState<'prompt' | 'calls'>('prompt')
  return (
    <>
      <PageHeader title="AI" subtitle="Промпт, модель и журнал вызовов GigaChat" />
      <div className="mb-4">
        <Tabs
          value={tab}
          onChange={setTab}
          items={[
            { key: 'prompt', label: 'Промпт и тест' },
            { key: 'calls', label: 'Журнал вызовов' },
          ]}
        />
      </div>
      {tab === 'prompt' ? <PromptEditor /> : <Calls />}
    </>
  )
}

function PromptEditor() {
  const client = useQueryClient()
  const toast = useToast()
  const prompt = useQuery({ queryKey: ['prompt'], queryFn: () => get<{ current: string; default: string }>('/ai/prompt') })
  const settings = useQuery({ queryKey: ['settings'], queryFn: () => get<Setting[]>('/settings') })
  const [draft, setDraft] = useState('')
  const [kworkId, setKworkId] = useState('')
  const [about, setAbout] = useState('')
  const [results, setResults] = useState<{ draft?: TestResult; current?: TestResult }>({})

  useEffect(() => {
    if (prompt.data) setDraft(prompt.data.current)
  }, [prompt.data])

  const save = useMutation({
    mutationFn: (changes: Record<string, string>) => put<Setting[]>('/settings', changes),
    onSuccess: (data) => {
      client.setQueryData(['settings'], data)
      client.invalidateQueries({ queryKey: ['prompt'] })
      toast('Сохранено')
    },
    onError: (e: ApiError) => toast(e.fields ? Object.values(e.fields).join('; ') : e.message, true),
  })

  const test = useMutation({
    mutationFn: async (compare: boolean) => {
      const body = { kworkId: kworkId ? Number(kworkId) : null, about: about || null }
      const draftResult = await post<TestResult>('/ai/test', { ...body, systemPrompt: draft })
      setResults({ draft: draftResult })
      if (compare) {
        const current = await post<TestResult>('/ai/test', { ...body, systemPrompt: prompt.data?.current })
        setResults({ draft: draftResult, current })
      }
    },
    onError: (e: Error) => toast(e.message, true),
  })

  if (prompt.isLoading || settings.isLoading) return <Skeleton className="h-96" />
  const value = (key: string) => settings.data?.find((s) => s.def.key === key)
  const model = value('ai.model')
  const temperature = value('ai.temperature')
  const dirty = prompt.data && draft !== prompt.data.current

  return (
    <div className="grid gap-4 xl:grid-cols-2">
      <div className="space-y-4">
        <Card
          title="Системный промпт"
          actions={
            <div className="flex gap-2">
              <Button size="sm" variant="ghost" icon={<RotateCcw className="h-3.5 w-3.5" />} onClick={() => setDraft(prompt.data!.default)}>
                Стандартный
              </Button>
              <Button size="sm" variant="primary" icon={<Save className="h-3.5 w-3.5" />} disabled={!dirty} loading={save.isPending} onClick={() => save.mutate({ 'ai.system-prompt': draft })}>
                Сохранить
              </Button>
            </div>
          }
        >
          <textarea className="input min-h-[340px] font-mono text-[13px] leading-relaxed" value={draft} onChange={(e) => setDraft(e.target.value)} />
          <p className="mt-2 text-xs muted">
            {draft.length} символов{dirty && ' · есть несохранённые изменения'}. В конец сообщения модели автоматически добавляются заказ и описание исполнителя.
          </p>
        </Card>
        <Card title="Модель">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Модель GigaChat">
              <select className="input" value={model?.value} onChange={(e) => save.mutate({ 'ai.model': e.target.value })}>
                {model?.def.options.map((o) => (
                  <option key={o}>{o}</option>
                ))}
              </select>
            </Field>
            <Field label={`Температура: ${temperature?.value}`} hint="Ниже — точнее и однообразнее, выше — креативнее">
              <input
                type="range"
                min={0}
                max={1.5}
                step={0.1}
                defaultValue={temperature?.value}
                className="mt-2 w-full accent-brand-600"
                onMouseUp={(e) => save.mutate({ 'ai.temperature': (e.target as HTMLInputElement).value })}
                onTouchEnd={(e) => save.mutate({ 'ai.temperature': (e.target as HTMLInputElement).value })}
                onKeyUp={(e) => save.mutate({ 'ai.temperature': (e.target as HTMLInputElement).value })}
              />
            </Field>
          </div>
        </Card>
      </div>

      <Card title="Тест на заказе">
        <div className="grid gap-3 sm:grid-cols-[160px_1fr]">
          <Field label="ID заказа" hint="Пусто — последний">
            <input className="input" type="number" value={kworkId} onChange={(e) => setKworkId(e.target.value)} placeholder="3269403" />
          </Field>
          <Field label="Описание исполнителя (необязательно)">
            <input className="input" value={about} onChange={(e) => setAbout(e.target.value)} placeholder="5 лет в Java, Spring, боты Telegram" />
          </Field>
        </div>
        <div className="mt-3 flex flex-wrap gap-2">
          <Button variant="primary" icon={<FlaskConical className="h-4 w-4" />} loading={test.isPending} onClick={() => test.mutate(false)}>
            Сгенерировать
          </Button>
          <Button disabled={!dirty} loading={test.isPending} onClick={() => test.mutate(true)}>
            Сравнить с сохранённым
          </Button>
        </div>
        <p className="mt-2 text-xs muted">Тест расходует токены и пишется в журнал вызовов, но не попадает пользователям.</p>
        <div className={clsx('mt-4 grid gap-3', results.current && 'md:grid-cols-2')}>
          {results.draft && <ResultBox title={results.current ? 'Черновик' : 'Результат'} r={results.draft} />}
          {results.current && <ResultBox title="Сохранённый промпт" r={results.current} />}
        </div>
      </Card>
    </div>
  )
}

function ResultBox({ title, r }: { title: string; r: TestResult }) {
  return (
    <div className="rounded-2xl border border-slate-200 p-3 dark:border-slate-800">
      <div className="mb-2 flex flex-wrap items-center gap-1.5">
        <span className="mr-auto text-xs font-semibold">{title}</span>
        <Badge tone="blue">{r.model.split(':')[0]}</Badge>
        <Badge>{ms(r.durationMs)}</Badge>
        {r.promptTokens != null && <Badge>{num((r.promptTokens ?? 0) + (r.completionTokens ?? 0))} ток.</Badge>}
      </div>
      <p className="whitespace-pre-wrap text-sm leading-relaxed">{r.text}</p>
      <p className="mt-2 text-xs muted">{r.text.length} символов</p>
    </div>
  )
}

function Calls() {
  const [status, setStatus] = useState<'' | 'OK' | 'ERROR'>('')
  const [page, setPage] = useState(0)
  const params = qs({ status, page, size: 25 })
  const { data, error } = useQuery({ queryKey: ['calls', params], queryFn: () => get<Page<LlmCallRow>>('/ai/calls' + params), placeholderData: keepPreviousData })
  return (
    <Card
      actions={
        <Segmented
          value={status}
          onChange={(v) => {
            setStatus(v)
            setPage(0)
          }}
          items={[
            { key: '', label: 'Все' },
            { key: 'OK', label: 'Успешные' },
            { key: 'ERROR', label: 'Ошибки' },
          ]}
        />
      }
      title={data ? `Вызовов: ${num(data.total)}` : 'Вызовы'}
    >
      <ErrorBox error={error} />
      {!data ? (
        <Skeleton className="h-60" />
      ) : !data.items.length ? (
        <Empty title="Вызовов нет" />
      ) : (
        <div className="divide-y divide-slate-100 dark:divide-slate-800">
          {data.items.map((c) => (
            <div key={c.id} className="flex flex-col gap-1 py-3 sm:flex-row sm:items-center sm:gap-4">
              <div className="flex items-center gap-2 sm:w-44">
                <Badge tone={c.status === 'OK' ? 'green' : 'red'}>{c.status === 'OK' ? 'OK' : 'Ошибка'}</Badge>
                <span className="text-xs muted">{dateTime(c.startedAt)}</span>
              </div>
              <div className="min-w-0 flex-1 text-sm">
                {c.error ? (
                  <span className="break-words text-rose-600 dark:text-rose-400">{c.error}</span>
                ) : (
                  <span className="muted">
                    {c.model?.split(':')[0]} · {num((c.promptTokens ?? 0) + (c.completionTokens ?? 0))} токенов
                  </span>
                )}
              </div>
              <div className="flex items-center gap-3 text-xs muted">
                <span className="tabular-nums">{ms(c.durationMs)}</span>
                {c.userId ? <Link to={`/users/${c.userId}`} className="hover:text-brand-600">польз. #{c.userId}</Link> : <span>тест</span>}
                {c.kworkId && <Link to={`/orders/${c.kworkId}`} className="hover:text-brand-600">заказ</Link>}
              </div>
            </div>
          ))}
        </div>
      )}
      {data && <Pagination page={page} size={25} total={data.total} onPage={setPage} />}
    </Card>
  )
}
