const nf = new Intl.NumberFormat('ru-RU')
const nf1 = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 1 })

export const num = (v: number | null | undefined) => (v == null ? '—' : nf.format(Math.round(v)))
export const num1 = (v: number | null | undefined) => (v == null ? '—' : nf1.format(v))

/** 12 345 → 12,3 тыс. */
export function compact(v: number): string {
  if (Math.abs(v) >= 1_000_000) return nf1.format(v / 1_000_000) + ' млн'
  if (Math.abs(v) >= 10_000) return nf1.format(v / 1000) + ' тыс.'
  return nf.format(Math.round(v))
}

export const money = (v: number | null | undefined) => (v == null ? '—' : nf.format(v) + ' ₽')

export function ms(v: number | null | undefined): string {
  if (v == null) return '—'
  return v >= 1000 ? nf1.format(v / 1000) + ' с' : Math.round(v) + ' мс'
}

/** Instant (с Z) или LocalDateTime (без зоны) → «25 окт, 14:05» */
export function dateTime(v: string | null | undefined): string {
  if (!v) return '—'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return v
  const sameYear = d.getFullYear() === new Date().getFullYear()
  return d.toLocaleString('ru-RU', {
    day: 'numeric',
    month: 'short',
    year: sameYear ? undefined : 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function ago(v: string | null | undefined): string {
  if (!v) return 'никогда'
  const diff = (Date.now() - new Date(v).getTime()) / 1000
  if (diff < 60) return 'только что'
  if (diff < 3600) return `${Math.floor(diff / 60)} мин назад`
  if (diff < 86400) return `${Math.floor(diff / 3600)} ч назад`
  if (diff < 86400 * 30) return `${Math.floor(diff / 86400)} дн назад`
  return dateTime(v)
}

export function duration(seconds: number): string {
  const d = Math.floor(seconds / 86400)
  const h = Math.floor((seconds % 86400) / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  return [d && `${d} д`, h && `${h} ч`, `${m} мин`].filter(Boolean).join(' ')
}

/** Изменение в процентах относительно прошлого периода */
export function delta(value: number, previous: number): number | null {
  if (previous === 0) return value === 0 ? 0 : null
  return ((value - previous) / previous) * 100
}

export function userName(u: { username: string | null; firstName: string | null; lastName: string | null; id: number }) {
  const full = [u.firstName, u.lastName].filter(Boolean).join(' ')
  return full || (u.username ? '@' + u.username : `#${u.id}`)
}

export const STATE_LABELS: Record<string, string> = {
  NEW: 'Новый',
  CHOOSING_CATEGORY: 'Выбирает категории',
  FILLING_PROFILE: 'Заполняет профиль',
  ACTIVE: 'Активен',
  BLOCKED: 'Заблокировал бота',
}

export const EVENT_LABELS: Record<string, string> = {
  START: 'Нажал /start',
  CATEGORIES_SAVED: 'Сохранил категории',
  PROFILE_SAVED: 'Заполнил профиль',
  PROFILE_SKIPPED: 'Пропустил профиль',
  CARD_HIDDEN: 'Скрыл карточку',
  PROPOSAL_VIEWED: 'Открыл отклик',
  PROPOSAL_GENERATED: 'Сгенерировал отклик',
  LIMIT_HIT: 'Упёрся в лимит',
  COMMAND: 'Команда',
}
