import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { History } from 'lucide-react'
import { useState } from 'react'
import { get, qs } from '../api/client'
import type { AuditRow, Page } from '../api/types'
import { Badge, Card, Empty, PageHeader, Pagination, Skeleton, Tone } from '../components/ui'
import { dateTime } from '../lib/format'

const ACTIONS: Record<string, { label: string; tone: Tone }> = {
  LOGIN: { label: 'Вход', tone: 'green' },
  LOGIN_FAILED: { label: 'Неудачный вход', tone: 'red' },
  SETTING: { label: 'Настройка', tone: 'blue' },
  USER_BAN: { label: 'Бан', tone: 'red' },
  USER_UNBAN: { label: 'Разбан', tone: 'green' },
  USER_LIMIT: { label: 'Лимит', tone: 'amber' },
  USER_CATEGORIES: { label: 'Категории', tone: 'blue' },
  USER_STATE: { label: 'Состояние', tone: 'amber' },
  USER_MESSAGE: { label: 'Сообщение', tone: 'blue' },
  BROADCAST_START: { label: 'Рассылка', tone: 'blue' },
  BROADCAST_CANCEL: { label: 'Отмена рассылки', tone: 'amber' },
  NOTIFIER_RUN: { label: 'Проверка заказов', tone: 'gray' },
  NOTIFIER_RESET: { label: 'Сброс курсора', tone: 'amber' },
  AI_TEST: { label: 'Тест AI', tone: 'gray' },
}

export default function Audit() {
  const [page, setPage] = useState(0)
  const { data } = useQuery({ queryKey: ['audit', page], queryFn: () => get<Page<AuditRow>>('/audit' + qs({ page, size: 50 })), placeholderData: keepPreviousData })
  return (
    <>
      <PageHeader title="Журнал" subtitle="Действия в админ-панели" />
      <Card>
        {!data ? (
          <Skeleton className="h-96" />
        ) : !data.items.length ? (
          <Empty icon={<History className="h-10 w-10" />} title="Записей нет" />
        ) : (
          <div className="divide-y divide-slate-100 dark:divide-slate-800">
            {data.items.map((a) => {
              const meta = ACTIONS[a.action] ?? { label: a.action, tone: 'gray' as Tone }
              return (
                <div key={a.id} className="flex flex-col gap-1 py-2.5 sm:flex-row sm:items-center sm:gap-4">
                  <span className="w-36 shrink-0 text-xs muted">{dateTime(a.createdAt)}</span>
                  <span className="w-40 shrink-0">
                    <Badge tone={meta.tone}>{meta.label}</Badge>
                  </span>
                  <span className="min-w-0 flex-1 break-words text-sm">
                    {a.target && <span className="font-medium">{a.target}</span>}
                    {a.details && <span className="ml-2 muted">{a.details}</span>}
                  </span>
                  <span className="text-xs muted">{a.ip}</span>
                </div>
              )
            })}
          </div>
        )}
        {data && <Pagination page={page} size={50} total={data.total} onPage={setPage} />}
      </Card>
    </>
  )
}
