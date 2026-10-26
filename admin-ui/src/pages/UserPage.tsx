import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Ban, Gauge, Layers, MessageSquare, RotateCcw, ShieldCheck } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { get, post, put, qs } from '../api/client'
import type { DeliveryRow, Meta, Page, ProposalRow, UserDetails } from '../api/types'
import { Avatar, SendStatusBadge, UserStateBadge } from '../components/badges'
import { useToast } from '../components/Toast'
import { Badge, Button, Card, Empty, ErrorBox, Field, Modal, Pagination, Skeleton, Tabs } from '../components/ui'
import { CategoryPicker } from '../components/CategoryPicker'
import { ProposalCard } from './Proposals'
import { ago, dateTime, EVENT_LABELS, num, STATE_LABELS, userName } from '../lib/format'

type Tab = 'overview' | 'proposals' | 'deliveries' | 'events'

export default function UserPage() {
  const id = Number(useParams().id)
  const client = useQueryClient()
  const toast = useToast()
  const [tab, setTab] = useState<Tab>('overview')
  const [modal, setModal] = useState<'limit' | 'message' | 'state' | 'categories' | 'ban' | null>(null)
  const { data, error, isLoading } = useQuery({ queryKey: ['user', id], queryFn: () => get<UserDetails>(`/users/${id}`) })

  const action = useMutation({
    mutationFn: (fn: () => Promise<unknown>) => fn(),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['user', id] })
      client.invalidateQueries({ queryKey: ['users'] })
      setModal(null)
    },
    onError: (e: Error) => toast(e.message, true),
  })
  const run = (fn: () => Promise<unknown>, message: string) => action.mutate(fn, { onSuccess: () => toast(message) })

  if (isLoading) return <Skeleton className="h-96" />
  if (error || !data) return <ErrorBox error={error ?? 'Не найдено'} />
  const u = data.user
  const name = userName(u)

  return (
    <>
      <Link to="/users" className="mb-4 inline-flex items-center gap-1 text-sm muted hover:text-slate-900 dark:hover:text-white">
        <ArrowLeft className="h-4 w-4" /> Пользователи
      </Link>

      <div className="card mb-4 p-4 sm:p-5">
        <div className="flex flex-wrap items-start gap-4">
          <Avatar name={name} />
          <div className="min-w-0 flex-1">
            <h1 className="truncate text-xl font-semibold">{name}</h1>
            <div className="mt-1 flex flex-wrap items-center gap-2 text-sm muted">
              {u.username && (
                <a href={`https://t.me/${u.username}`} target="_blank" rel="noreferrer" className="text-brand-600 hover:underline dark:text-brand-400">
                  @{u.username}
                </a>
              )}
              <span>ID {u.id}</span>
              <UserStateBadge user={u} />
            </div>
          </div>
          <div className="flex w-full flex-wrap gap-2 sm:w-auto">
            <Button size="sm" icon={<MessageSquare className="h-4 w-4" />} onClick={() => setModal('message')}>
              Написать
            </Button>
            <Button size="sm" icon={<Layers className="h-4 w-4" />} onClick={() => setModal('categories')}>
              Категории
            </Button>
            <Button size="sm" icon={<Gauge className="h-4 w-4" />} onClick={() => setModal('limit')}>
              Лимит
            </Button>
            <Button size="sm" icon={<RotateCcw className="h-4 w-4" />} onClick={() => setModal('state')}>
              Состояние
            </Button>
            {u.banned ? (
              <Button size="sm" icon={<ShieldCheck className="h-4 w-4" />} loading={action.isPending} onClick={() => run(() => post(`/users/${id}/unban`), 'Пользователь разбанен')}>
                Разбанить
              </Button>
            ) : (
              <Button size="sm" variant="danger" icon={<Ban className="h-4 w-4" />} onClick={() => setModal('ban')}>
                Забанить
              </Button>
            )}
          </div>
        </div>
      </div>

      <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
        {[
          ['Доставлено карточек', num(u.deliveries)],
          ['Откликов', num(u.proposals)],
          ['Сегодня / лимит', `${data.proposalsToday} / ${data.dailyLimit}${u.dailyLimitOverride != null ? ' *' : ''}`],
          ['Последняя активность', ago(u.lastActivityAt)],
        ].map(([label, value]) => (
          <div key={label} className="card p-4">
            <div className="text-xs muted">{label}</div>
            <div className="mt-1 text-lg font-semibold tabular-nums">{value}</div>
          </div>
        ))}
      </div>

      <div className="mb-4">
        <Tabs
          value={tab}
          onChange={setTab}
          items={[
            { key: 'overview', label: 'Обзор' },
            { key: 'proposals', label: `Отклики · ${u.proposals}` },
            { key: 'deliveries', label: `Доставки · ${u.deliveries}` },
            { key: 'events', label: 'События' },
          ]}
        />
      </div>

      {tab === 'overview' && (
        <div className="grid gap-4 lg:grid-cols-2">
          <Card title="Профиль фрилансера" actions={data.profileUpdatedAt && <span className="text-xs muted">{dateTime(data.profileUpdatedAt)}</span>}>
            {data.profile ? <p className="whitespace-pre-wrap text-sm leading-relaxed">{data.profile}</p> : <p className="text-sm muted">Не заполнен — отклики пишутся в общем виде</p>}
          </Card>
          <Card title={`Подкатегории · ${data.subcategories.length}`}>
            {data.subcategories.length ? (
              <div className="flex flex-wrap gap-2">
                {data.subcategories.map((s) => (
                  <Badge key={s.id} tone="blue">
                    {s.name}
                  </Badge>
                ))}
              </div>
            ) : (
              <p className="text-sm muted">Не выбраны</p>
            )}
            <dl className="mt-5 grid grid-cols-2 gap-3 text-sm">
              <dt className="muted">Регистрация</dt>
              <dd>{dateTime(u.createdAt)}</dd>
              <dt className="muted">Состояние</dt>
              <dd>{STATE_LABELS[u.state]}</dd>
              <dt className="muted">Лимит в день</dt>
              <dd>{u.dailyLimitOverride != null ? `${u.dailyLimitOverride} (персональный)` : `${data.dailyLimit} (общий)`}</dd>
            </dl>
          </Card>
        </div>
      )}
      {tab === 'proposals' && <UserProposals id={id} />}
      {tab === 'deliveries' && <UserDeliveries id={id} />}
      {tab === 'events' && (
        <Card>
          {data.events.length ? (
            <ol className="relative space-y-4 border-l border-slate-200 pl-5 dark:border-slate-800">
              {data.events.map((e) => (
                <li key={e.id}>
                  <span className="absolute -left-[5px] mt-1.5 h-2.5 w-2.5 rounded-full border-2 border-white bg-brand-500 dark:border-slate-900" />
                  <div className="text-sm font-medium">
                    {EVENT_LABELS[e.type] ?? e.type}
                    {e.payload && <span className="ml-2 font-normal muted">{e.payload}</span>}
                  </div>
                  <div className="text-xs muted">{dateTime(e.createdAt)}</div>
                </li>
              ))}
            </ol>
          ) : (
            <Empty title="Событий пока нет" />
          )}
        </Card>
      )}

      {/* модалки монтируются только открытыми, чтобы их форма бралась из свежих данных */}
      {modal === 'ban' && (
        <BanModal open onClose={() => setModal(null)} name={name} loading={action.isPending} onConfirm={() => run(() => post(`/users/${id}/ban`), 'Пользователь забанен')} />
      )}
      {modal === 'limit' && (
        <LimitModal
          open
          onClose={() => setModal(null)}
          current={u.dailyLimitOverride}
          loading={action.isPending}
          onSave={(limit) => run(() => put(`/users/${id}/limit`, { limit }), 'Лимит сохранён')}
        />
      )}
      {modal === 'message' && <MessageModal open onClose={() => setModal(null)} id={id} />}
      {modal === 'state' && (
        <StateModal
          open
          onClose={() => setModal(null)}
          current={u.state}
          loading={action.isPending}
          onSave={(state) => run(() => put(`/users/${id}/state`, { state }), 'Состояние изменено')}
        />
      )}
      {modal === 'categories' && (
        <CategoriesModal
          open
          onClose={() => setModal(null)}
          selected={data.subcategories.map((s) => s.id)}
          loading={action.isPending}
          onSave={(ids) => run(() => put(`/users/${id}/categories`, ids), 'Категории сохранены')}
        />
      )}
    </>
  )
}

function UserProposals({ id }: { id: number }) {
  const [page, setPage] = useState(0)
  const { data } = useQuery({ queryKey: ['user-proposals', id, page], queryFn: () => get<Page<ProposalRow>>(`/users/${id}/proposals` + qs({ page, size: 10 })) })
  if (!data) return <Skeleton className="h-60" />
  if (!data.items.length) return <Card><Empty title="Откликов пока нет" /></Card>
  return (
    <div className="space-y-3">
      {data.items.map((p) => (
        <ProposalCard key={p.id} p={p} showUser={false} />
      ))}
      <Pagination page={page} size={10} total={data.total} onPage={setPage} />
    </div>
  )
}

function UserDeliveries({ id }: { id: number }) {
  const [page, setPage] = useState(0)
  const { data } = useQuery({ queryKey: ['user-deliveries', id, page], queryFn: () => get<Page<DeliveryRow>>(`/users/${id}/deliveries` + qs({ page, size: 25 })) })
  if (!data) return <Skeleton className="h-60" />
  return (
    <Card>
      {data.items.length ? (
        <div className="divide-y divide-slate-100 dark:divide-slate-800">
          {data.items.map((d) => (
            <Link key={d.id} to={`/orders/${d.kworkId}`} className="flex items-center justify-between gap-3 py-3 hover:opacity-80">
              <div className="min-w-0">
                <div className="truncate text-sm font-medium">{d.kworkName ?? `Заказ #${d.kworkId}`}</div>
                <div className="text-xs muted">{dateTime(d.sentAt)}</div>
              </div>
              <SendStatusBadge status={d.status} />
            </Link>
          ))}
        </div>
      ) : (
        <Empty title="Карточек ещё не отправляли" />
      )}
      <Pagination page={page} size={25} total={data.total} onPage={setPage} />
    </Card>
  )
}

function BanModal({ open, onClose, onConfirm, name, loading }: { open: boolean; onClose: () => void; onConfirm: () => void; name: string; loading: boolean }) {
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Забанить пользователя?"
      footer={
        <>
          <Button onClick={onClose}>Отмена</Button>
          <Button variant="danger" loading={loading} onClick={onConfirm}>
            Забанить
          </Button>
        </>
      }
    >
      <p className="text-sm">
        <b>{name}</b> перестанет получать заказы, а бот не будет отвечать на его сообщения. Разбанить можно в любой момент.
      </p>
    </Modal>
  )
}

function LimitModal({ open, onClose, current, onSave, loading }: { open: boolean; onClose: () => void; current: number | null; onSave: (v: number | null) => void; loading: boolean }) {
  const [value, setValue] = useState(current?.toString() ?? '')
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Дневной лимит откликов"
      footer={
        <>
          <Button onClick={() => onSave(null)} loading={loading}>
            Сбросить на общий
          </Button>
          <Button variant="primary" loading={loading} disabled={value === ''} onClick={() => onSave(Number(value))}>
            Сохранить
          </Button>
        </>
      }
    >
      <Field label="Откликов в день" hint="Персональный лимит вместо общего из настроек; 0 — запретить генерацию">
        <input className="input" type="number" min={0} max={1000} value={value} onChange={(e) => setValue(e.target.value)} />
      </Field>
    </Modal>
  )
}

function StateModal({ open, onClose, current, onSave, loading }: { open: boolean; onClose: () => void; current: string; onSave: (v: string) => void; loading: boolean }) {
  const [value, setValue] = useState(current)
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Состояние пользователя"
      footer={
        <Button variant="primary" loading={loading} onClick={() => onSave(value)}>
          Сохранить
        </Button>
      }
    >
      <div className="space-y-2">
        {Object.entries(STATE_LABELS).map(([k, v]) => (
          <label key={k} className="flex cursor-pointer items-center gap-3 rounded-xl border border-slate-200 p-3 text-sm has-[:checked]:border-brand-500 has-[:checked]:bg-brand-50 dark:border-slate-700 dark:has-[:checked]:bg-brand-500/10">
            <input type="radio" name="state" checked={value === k} onChange={() => setValue(k)} className="accent-brand-600" />
            {v}
          </label>
        ))}
      </div>
      <p className="mt-3 text-xs muted">Например, «Новый» — пользователь пройдёт онбординг заново при следующем /start.</p>
    </Modal>
  )
}

function MessageModal({ open, onClose, id }: { open: boolean; onClose: () => void; id: number }) {
  const toast = useToast()
  const [text, setText] = useState('')
  const send = useMutation({
    mutationFn: () => post<{ status: string }>(`/users/${id}/message`, { text }),
    onSuccess: (r) => {
      if (r.status === 'OK') {
        toast('Сообщение отправлено')
        setText('')
        onClose()
      } else toast(r.status === 'BLOCKED' ? 'Пользователь заблокировал бота' : 'Не удалось отправить', true)
    },
    onError: (e: Error) => toast(e.message, true),
  })
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Сообщение от бота"
      footer={
        <Button variant="primary" loading={send.isPending} disabled={!text.trim()} onClick={() => send.mutate()}>
          Отправить
        </Button>
      }
    >
      <Field label="Текст" hint="Поддерживается HTML-разметка Telegram: <b>, <i>, <a href>, <code>, <blockquote>">
        <textarea className="input min-h-[140px]" value={text} onChange={(e) => setText(e.target.value)} />
      </Field>
    </Modal>
  )
}

function CategoriesModal({ open, onClose, selected, onSave, loading }: { open: boolean; onClose: () => void; selected: number[]; onSave: (ids: number[]) => void; loading: boolean }) {
  const meta = useQuery({ queryKey: ['meta'], queryFn: () => get<Meta>('/meta'), staleTime: Infinity })
  const [value, setValue] = useState<number[]>(selected)
  return (
    <Modal
      open={open}
      onClose={onClose}
      wide
      title="Подкатегории пользователя"
      footer={
        <Button variant="primary" loading={loading} onClick={() => onSave(value)}>
          Сохранить ({value.length})
        </Button>
      }
    >
      {meta.data ? <CategoryPicker tree={meta.data.categories} value={value} onChange={setValue} /> : <Skeleton className="h-60" />}
    </Modal>
  )
}
