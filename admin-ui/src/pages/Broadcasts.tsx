import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import clsx from 'clsx'
import { Bold, Code, Italic, Link2, Megaphone, Send, TestTube2, X } from 'lucide-react'
import { useRef, useState } from 'react'
import { get, post } from '../api/client'
import type { BroadcastRow, Meta, Segment } from '../api/types'
import { CategoryPicker } from '../components/CategoryPicker'
import { TelegramPreview } from '../components/TelegramPreview'
import { useToast } from '../components/Toast'
import { Badge, Button, Card, Empty, Field, Modal, PageHeader, Skeleton } from '../components/ui'
import { dateTime, num } from '../lib/format'
import { useDebounced } from '../lib/hooks'

const SEGMENTS: { key: Segment['type']; label: string; hint: string }[] = [
  { key: 'ALL', label: 'Все', hint: 'Кроме забаненных и заблокировавших бота' },
  { key: 'ACTIVE', label: 'Активные', hint: 'Завершили онбординг' },
  { key: 'CATEGORIES', label: 'По категориям', hint: 'Подписаны на выбранные подкатегории' },
  { key: 'INACTIVE', label: 'Неактивные', hint: 'Давно не заходили в бота' },
]

const STATUS: Record<BroadcastRow['status'], { label: string; tone: 'blue' | 'green' | 'gray' | 'red' }> = {
  RUNNING: { label: 'Идёт', tone: 'blue' },
  DONE: { label: 'Завершена', tone: 'green' },
  CANCELLED: { label: 'Отменена', tone: 'gray' },
  FAILED: { label: 'Прервана', tone: 'red' },
}

export default function Broadcasts() {
  const client = useQueryClient()
  const toast = useToast()
  const textRef = useRef<HTMLTextAreaElement>(null)
  const [text, setText] = useState('')
  const [segment, setSegment] = useState<Segment>({ type: 'ALL', subcategoryIds: [], inactiveDays: 7 })
  const [confirm, setConfirm] = useState(false)
  const [picker, setPicker] = useState(false)
  const meta = useQuery({ queryKey: ['meta'], queryFn: () => get<Meta>('/meta'), staleTime: Infinity })
  const seg = useDebounced(segment, 250)
  const preview = useQuery({
    queryKey: ['bc-preview', seg],
    queryFn: () => post<{ recipients: number }>('/broadcasts/preview', seg),
    enabled: seg.type !== 'CATEGORIES' || (seg.subcategoryIds?.length ?? 0) > 0,
  })
  const list = useQuery({
    queryKey: ['broadcasts'],
    queryFn: () => get<BroadcastRow[]>('/broadcasts'),
    refetchInterval: (q) => (q.state.data?.some((b) => b.status === 'RUNNING') ? 2000 : false),
  })

  const test = useMutation({
    mutationFn: () => post<{ status: string }>('/broadcasts/test', { text }),
    onSuccess: (r) => (r.status === 'OK' ? toast('Тест отправлен вам в Telegram') : toast('Telegram не принял сообщение — проверьте разметку', true)),
    onError: (e: Error) => toast(e.message, true),
  })
  const start = useMutation({
    mutationFn: () => post<{ id: number }>('/broadcasts', { text, segment }),
    onSuccess: () => {
      toast('Рассылка запущена')
      setConfirm(false)
      setText('')
      client.invalidateQueries({ queryKey: ['broadcasts'] })
    },
    onError: (e: Error) => toast(e.message, true),
  })
  const cancel = useMutation({
    mutationFn: (id: number) => post(`/broadcasts/${id}/cancel`),
    onSuccess: () => client.invalidateQueries({ queryKey: ['broadcasts'] }),
  })

  const wrap = (open: string, close: string) => {
    const el = textRef.current
    if (!el) return
    const { selectionStart: a, selectionEnd: b } = el
    const next = text.slice(0, a) + open + text.slice(a, b) + close + text.slice(b)
    setText(next)
    requestAnimationFrame(() => {
      el.focus()
      el.setSelectionRange(a + open.length, b + open.length)
    })
  }
  const recipients = preview.data?.recipients
  const subNames = meta.data?.categories.flatMap((r) => r.children).filter((c) => segment.subcategoryIds?.includes(c.id)) ?? []

  return (
    <>
      <PageHeader title="Рассылки" subtitle="Сообщение всем пользователям или сегменту" />
      <div className="grid gap-4 xl:grid-cols-[1fr_440px]">
        <Card title="Новая рассылка">
          <div className="space-y-4">
            <div>
              <div className="mb-1.5 flex items-center justify-between">
                <span className="label mb-0">Текст</span>
                <div className="flex gap-1">
                  {[
                    [<Bold className="h-4 w-4" />, '<b>', '</b>', 'Жирный'],
                    [<Italic className="h-4 w-4" />, '<i>', '</i>', 'Курсив'],
                    [<Code className="h-4 w-4" />, '<code>', '</code>', 'Код'],
                    [<Link2 className="h-4 w-4" />, '<a href="https://">', '</a>', 'Ссылка'],
                  ].map(([icon, o, c, title]) => (
                    <button key={title as string} type="button" title={title as string} onClick={() => wrap(o as string, c as string)} className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800">
                      {icon}
                    </button>
                  ))}
                </div>
              </div>
              <textarea ref={textRef} className="input min-h-[180px]" value={text} onChange={(e) => setText(e.target.value)} placeholder="Привет! У нас обновление…" maxLength={4000} />
              <div className="mt-1 text-right text-xs muted">{text.length} / 4000</div>
            </div>

            <div>
              <span className="label">Получатели</span>
              <div className="grid gap-2 sm:grid-cols-2">
                {SEGMENTS.map((s) => (
                  <button
                    key={s.key}
                    type="button"
                    onClick={() => setSegment({ ...segment, type: s.key })}
                    className={clsx(
                      'rounded-2xl border p-3 text-left transition',
                      segment.type === s.key ? 'border-brand-500 bg-brand-50 dark:bg-brand-500/10' : 'border-slate-200 hover:border-slate-300 dark:border-slate-700',
                    )}
                  >
                    <div className="text-sm font-medium">{s.label}</div>
                    <div className="text-xs muted">{s.hint}</div>
                  </button>
                ))}
              </div>
              {segment.type === 'CATEGORIES' && (
                <div className="mt-3 flex flex-wrap items-center gap-2">
                  {subNames.map((c) => (
                    <Badge key={c.id} tone="blue">
                      {c.name}
                    </Badge>
                  ))}
                  <Button size="sm" onClick={() => setPicker(true)}>
                    {subNames.length ? 'Изменить' : 'Выбрать подкатегории'}
                  </Button>
                </div>
              )}
              {segment.type === 'INACTIVE' && (
                <div className="mt-3 max-w-xs">
                  <Field label="Не заходили, дней">
                    <input className="input" type="number" min={1} max={365} value={segment.inactiveDays} onChange={(e) => setSegment({ ...segment, inactiveDays: Number(e.target.value) })} />
                  </Field>
                </div>
              )}
            </div>

            <div className="flex flex-wrap items-center gap-2 rounded-2xl bg-slate-50 p-3 dark:bg-slate-800/50">
              <span className="mr-auto text-sm">
                Получат: <b className="tabular-nums">{recipients == null ? '…' : num(recipients)}</b>
              </span>
              <Button icon={<TestTube2 className="h-4 w-4" />} disabled={!text.trim() || !meta.data?.adminTelegramId} loading={test.isPending} onClick={() => test.mutate()} title={meta.data?.adminTelegramId ? '' : 'Задайте ADMIN_TELEGRAM_ID'}>
                Тест себе
              </Button>
              <Button variant="primary" icon={<Send className="h-4 w-4" />} disabled={!text.trim() || !recipients} onClick={() => setConfirm(true)}>
                Запустить
              </Button>
            </div>
            {!meta.data?.adminTelegramId && <p className="text-xs muted">Чтобы отправлять тест себе, задайте переменную ADMIN_TELEGRAM_ID.</p>}
          </div>
        </Card>

        <div className="space-y-4">
          <Card title="Предпросмотр">
            <TelegramPreview html={text} />
          </Card>
        </div>
      </div>

      <Card title="История" className="mt-4">
        {!list.data ? (
          <Skeleton className="h-40" />
        ) : !list.data.length ? (
          <Empty icon={<Megaphone className="h-10 w-10" />} title="Рассылок ещё не было" />
        ) : (
          <div className="divide-y divide-slate-100 dark:divide-slate-800">
            {list.data.map((b) => {
              const done = b.sent + b.failed
              const pct = b.total ? (done / b.total) * 100 : 0
              return (
                <div key={b.id} className="py-3">
                  <div className="flex flex-wrap items-center gap-2">
                    <Badge tone={STATUS[b.status].tone}>{STATUS[b.status].label}</Badge>
                    <span className="text-xs muted">
                      {dateTime(b.createdAt)} · {b.segment}
                    </span>
                    <span className="ml-auto text-xs tabular-nums muted">
                      ✅ {b.sent} · ❌ {b.failed} · из {b.total}
                    </span>
                    {b.status === 'RUNNING' && (
                      <Button size="sm" variant="ghost" icon={<X className="h-3.5 w-3.5" />} onClick={() => cancel.mutate(b.id)}>
                        Остановить
                      </Button>
                    )}
                  </div>
                  <p className="mt-1 line-clamp-2 text-sm text-slate-600 dark:text-slate-300">{b.text.replace(/<[^>]+>/g, '')}</p>
                  {b.status === 'RUNNING' && (
                    <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800">
                      <div className="h-full rounded-full bg-brand-500 transition-all" style={{ width: `${pct}%` }} />
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </Card>

      <Modal
        open={confirm}
        onClose={() => setConfirm(false)}
        title="Запустить рассылку?"
        footer={
          <>
            <Button onClick={() => setConfirm(false)}>Отмена</Button>
            <Button variant="primary" loading={start.isPending} onClick={() => start.mutate()}>
              Отправить {num(recipients ?? 0)} получателям
            </Button>
          </>
        }
      >
        <p className="mb-3 text-sm">Сообщение уйдёт сразу. Остановить можно в истории, но уже отправленные сообщения не отзываются.</p>
        <TelegramPreview html={text} />
      </Modal>

      <Modal open={picker} onClose={() => setPicker(false)} wide title="Подкатегории" footer={<Button variant="primary" onClick={() => setPicker(false)}>Готово</Button>}>
        {meta.data && <CategoryPicker tree={meta.data.categories} value={segment.subcategoryIds ?? []} onChange={(ids) => setSegment({ ...segment, subcategoryIds: ids })} />}
      </Modal>
    </>
  )
}
