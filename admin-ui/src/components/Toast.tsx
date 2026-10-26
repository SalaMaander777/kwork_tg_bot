import clsx from 'clsx'
import { CheckCircle2, XCircle } from 'lucide-react'
import { createContext, ReactNode, useCallback, useContext, useState } from 'react'

type ToastItem = { id: number; text: string; error?: boolean }
const ToastContext = createContext<(text: string, error?: boolean) => void>(() => {})

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])
  const show = useCallback((text: string, error?: boolean) => {
    const id = Date.now() + Math.random()
    setItems((list) => [...list, { id, text, error }])
    setTimeout(() => setItems((list) => list.filter((t) => t.id !== id)), 3500)
  }, [])
  return (
    <ToastContext.Provider value={show}>
      {children}
      <div className="pointer-events-none fixed inset-x-0 bottom-20 z-[60] flex flex-col items-center gap-2 px-4 lg:bottom-6">
        {items.map((t) => (
          <div
            key={t.id}
            className={clsx(
              'pointer-events-auto flex max-w-md animate-fade-in items-center gap-2 rounded-2xl px-4 py-3 text-sm font-medium shadow-xl',
              t.error ? 'bg-rose-600 text-white' : 'bg-slate-900 text-white dark:bg-white dark:text-slate-900',
            )}
          >
            {t.error ? <XCircle className="h-4 w-4 shrink-0" /> : <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-400 dark:text-emerald-600" />}
            {t.text}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)
