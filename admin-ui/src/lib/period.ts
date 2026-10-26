export type PeriodKey = '1' | '7' | '30' | '90'

export const PERIODS: { key: PeriodKey; label: string }[] = [
  { key: '1', label: '24 ч' },
  { key: '7', label: '7 дней' },
  { key: '30', label: '30 дней' },
  { key: '90', label: '90 дней' },
]
