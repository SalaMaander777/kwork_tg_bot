import clsx from 'clsx'
import { Check, ChevronDown } from 'lucide-react'
import { useState } from 'react'
import type { CategoryNode } from '../api/types'

/** Выбор подкатегорий из дерева: раскрывающиеся категории, «выбрать все». */
export function CategoryPicker({ tree, value, onChange }: { tree: CategoryNode[]; value: number[]; onChange: (ids: number[]) => void }) {
  const [open, setOpen] = useState<number | null>(tree[0]?.id ?? null)
  const selected = new Set(value)
  const toggle = (id: number) => onChange(selected.has(id) ? value.filter((v) => v !== id) : [...value, id])
  const toggleAll = (root: CategoryNode) => {
    const ids = root.children.map((c) => c.id)
    const all = ids.every((id) => selected.has(id))
    onChange(all ? value.filter((v) => !ids.includes(v)) : [...new Set([...value, ...ids])])
  }
  return (
    <div className="space-y-2">
      {tree.map((root) => {
        const count = root.children.filter((c) => selected.has(c.id)).length
        const isOpen = open === root.id
        return (
          <div key={root.id} className="rounded-2xl border border-slate-200 dark:border-slate-800">
            <button type="button" onClick={() => setOpen(isOpen ? null : root.id)} className="flex w-full items-center justify-between gap-2 px-4 py-3 text-left text-sm font-medium">
              <span>
                {root.name}
                {count > 0 && <span className="ml-2 rounded-full bg-brand-600 px-2 py-0.5 text-xs text-white">{count}</span>}
              </span>
              <ChevronDown className={clsx('h-4 w-4 text-slate-400 transition', isOpen && 'rotate-180')} />
            </button>
            {isOpen && (
              <div className="border-t border-slate-100 p-2 dark:border-slate-800">
                <button type="button" onClick={() => toggleAll(root)} className="mb-1 w-full rounded-xl px-3 py-2 text-left text-xs font-medium text-brand-600 hover:bg-brand-50 dark:text-brand-400 dark:hover:bg-brand-500/10">
                  {count === root.children.length ? 'Снять все' : 'Выбрать все'}
                </button>
                <div className="grid gap-1 sm:grid-cols-2">
                  {root.children.map((c) => (
                    <button
                      type="button"
                      key={c.id}
                      onClick={() => toggle(c.id)}
                      className={clsx(
                        'flex items-center gap-2 rounded-xl px-3 py-2 text-left text-sm transition',
                        selected.has(c.id) ? 'bg-brand-50 text-brand-800 dark:bg-brand-500/10 dark:text-brand-200' : 'hover:bg-slate-50 dark:hover:bg-slate-800/60',
                      )}
                    >
                      <span className={clsx('grid h-4 w-4 shrink-0 place-items-center rounded border', selected.has(c.id) ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-300 dark:border-slate-600')}>
                        {selected.has(c.id) && <Check className="h-3 w-3" />}
                      </span>
                      {c.name}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}
