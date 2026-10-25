// DTO бэкенда (org.example.telegramkworkbot.admin.*)
export type Page<T> = { items: T[]; total: number; page: number; size: number }

export type Me = { authenticated: boolean; enabled: boolean; username: string | null }

export type Kpi = { value: number; previous: number }
export type Summary = Record<string, Kpi>
export type Point = { t: string; value: number }
export type FunnelStep = { key: string; label: string; count: number }
export type CategoryStat = { id: number; name: string; root: string; orders: number; subscribers: number; proposals: number }
export type HeatCell = { dow: number; hour: number; count: number }
export type Tokens = {
  byModel: { model: string; calls: number; promptTokens: number; completionTokens: number }[]
  periodTotal: number
  allTimeTotal: number
  budget: number
}
export type HealthStatus = 'ok' | 'warn' | 'error'
export type Health = { status: HealthStatus; title: string; detail: string; data: Record<string, unknown> }

export type UserState = 'NEW' | 'CHOOSING_CATEGORY' | 'FILLING_PROFILE' | 'ACTIVE' | 'BLOCKED'
export type UserRow = {
  id: number
  username: string | null
  firstName: string | null
  lastName: string | null
  state: UserState
  banned: boolean
  createdAt: string | null
  lastActivityAt: string | null
  dailyLimitOverride: number | null
  subcategories: number
  deliveries: number
  proposals: number
}
export type UserDetails = {
  user: UserRow
  profile: string | null
  profileUpdatedAt: string | null
  subcategories: { id: number; name: string; root: string }[]
  dailyLimit: number
  proposalsToday: number
  events: { id: number; type: string; createdAt: string; payload: string | null }[]
}
export type DeliveryRow = { id: number; kworkId: number; kworkName: string | null; status: string; sentAt: string }

export type OrderRow = {
  id: number
  name: string
  price: number | null
  maxDays: number | null
  categoryId: number | null
  category: string | null
  createdAt: string | null
  buyer: string | null
  responses: number
  deliveries: number
  proposals: number
}
export type OrderDetails = {
  order: OrderRow
  description: string | null
  url: string
  cardHtml: string
  recipients: { userId: number; username: string | null; status: string; sentAt: string }[]
  proposals: ProposalRow[]
}

export type ProposalRow = {
  id: number
  userId: number
  username: string | null
  kworkId: number
  kworkName: string | null
  text: string
  createdAt: string
  model: string | null
  durationMs: number | null
  tokens: number | null
}
export type LlmCallRow = {
  id: number
  userId: number | null
  kworkId: number | null
  model: string | null
  startedAt: string
  durationMs: number
  status: 'OK' | 'ERROR'
  promptTokens: number | null
  completionTokens: number | null
  error: string | null
}

export type Segment = { type: 'ALL' | 'ACTIVE' | 'CATEGORIES' | 'INACTIVE'; subcategoryIds?: number[]; inactiveDays?: number }
export type BroadcastRow = {
  id: number
  text: string
  segment: string
  status: 'RUNNING' | 'DONE' | 'CANCELLED' | 'FAILED'
  total: number
  sent: number
  failed: number
  createdAt: string
  finishedAt: string | null
}

export type SettingType = 'BOOLEAN' | 'INTEGER' | 'DECIMAL' | 'TEXT' | 'CHOICE'
export type Setting = {
  def: {
    key: string
    type: SettingType
    group: string
    label: string
    description: string | null
    defaultValue: string
    min: number | null
    max: number | null
    options: string[]
  }
  value: string
  overridden: boolean
}

export type AuditRow = { id: number; action: string; target: string | null; details: string | null; ip: string | null; createdAt: string }

export type CategoryNode = { id: number; name: string; children: CategoryNode[] }
export type Meta = { categories: CategoryNode[]; states: UserState[]; models: string[]; adminTelegramId: number | null }

export type SystemInfo = {
  version: string
  java: string
  startedAt: string
  uptimeSeconds: number
  memoryUsedMb: number
  memoryMaxMb: number
  threads: number
  dbSizeMb: number
  tables: Record<string, { rows: number; sizeKb: number }>
}
