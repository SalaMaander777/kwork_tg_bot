import type { UserRow } from '../api/types'
import { STATE_LABELS } from '../lib/format'
import { Badge, Tone } from './ui'

const STATE_TONES: Record<string, Tone> = {
  ACTIVE: 'green',
  BLOCKED: 'red',
  NEW: 'gray',
  CHOOSING_CATEGORY: 'amber',
  FILLING_PROFILE: 'amber',
}

export function UserStateBadge({ user }: { user: Pick<UserRow, 'state' | 'banned'> }) {
  return (
    <span className="inline-flex flex-wrap gap-1">
      <Badge tone={STATE_TONES[user.state] ?? 'gray'}>{STATE_LABELS[user.state] ?? user.state}</Badge>
      {user.banned && <Badge tone="red">Забанен</Badge>}
    </span>
  )
}

export function SendStatusBadge({ status }: { status: string }) {
  const tone: Tone = status === 'OK' ? 'green' : status === 'BLOCKED' ? 'amber' : 'red'
  const label = { OK: 'Доставлено', BLOCKED: 'Бот заблокирован', FAILED: 'Ошибка' }[status] ?? status
  return <Badge tone={tone}>{label}</Badge>
}

export function Avatar({ name }: { name: string }) {
  const letter = name.replace(/^[@#]/, '').charAt(0).toUpperCase() || '?'
  const hue = [...name].reduce((a, c) => a + c.charCodeAt(0), 0) % 360
  return (
    <div className="grid h-9 w-9 shrink-0 place-items-center rounded-full text-sm font-semibold text-white" style={{ background: `hsl(${hue} 65% 55%)` }}>
      {letter}
    </div>
  )
}
