import clsx from 'clsx'
import {
  Bot,
  ClipboardList,
  FileText,
  LayoutDashboard,
  LogOut,
  Megaphone,
  Monitor,
  Moon,
  MoreHorizontal,
  Settings,
  Sparkles,
  Sun,
  Users,
} from 'lucide-react'
import { ReactNode, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { applyTheme, storedTheme, Theme } from '../lib/theme'
import { Modal } from './ui'

type NavItem = { to: string; label: string; icon: ReactNode; mobile?: boolean }

const NAV: NavItem[] = [
  { to: '/', label: 'Дашборд', icon: <LayoutDashboard className="h-5 w-5" />, mobile: true },
  { to: '/users', label: 'Пользователи', icon: <Users className="h-5 w-5" />, mobile: true },
  { to: '/orders', label: 'Заказы', icon: <ClipboardList className="h-5 w-5" />, mobile: true },
  { to: '/proposals', label: 'Отклики', icon: <FileText className="h-5 w-5" /> },
  { to: '/ai', label: 'AI', icon: <Sparkles className="h-5 w-5" /> },
  { to: '/broadcasts', label: 'Рассылки', icon: <Megaphone className="h-5 w-5" />, mobile: true },
  { to: '/settings', label: 'Настройки', icon: <Settings className="h-5 w-5" /> },
  { to: '/audit', label: 'Журнал', icon: <Bot className="h-5 w-5" /> },
]

function ThemeSwitch() {
  const [theme, setTheme] = useState<Theme>(storedTheme())
  const next: Record<Theme, Theme> = { light: 'dark', dark: 'system', system: 'light' }
  const icon = { light: <Sun className="h-4 w-4" />, dark: <Moon className="h-4 w-4" />, system: <Monitor className="h-4 w-4" /> }[theme]
  const label = { light: 'Светлая', dark: 'Тёмная', system: 'Авто' }[theme]
  return (
    <button
      onClick={() => {
        const t = next[theme]
        setTheme(t)
        applyTheme(t)
      }}
      className="flex h-9 items-center gap-2 rounded-xl px-3 text-sm text-slate-500 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800"
      title={`Тема: ${label}`}
    >
      {icon}
      <span className="hidden xl:inline">{label}</span>
    </button>
  )
}

function Logo() {
  return (
    <div className="flex items-center gap-2.5">
      <div className="grid h-9 w-9 place-items-center rounded-xl bg-gradient-to-br from-brand-500 to-violet-600 text-sm font-bold text-white shadow-lg shadow-brand-600/30">
        K
      </div>
      <div className="leading-tight">
        <div className="text-sm font-semibold">Kwork Bot</div>
        <div className="text-[11px] muted">админ-панель</div>
      </div>
    </div>
  )
}

export function Layout({ username, onLogout }: { username: string; onLogout: () => void }) {
  const [more, setMore] = useState(false)
  const location = useLocation()
  const link = (it: NavItem) =>
    clsx(
      'flex items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium transition',
      (it.to === '/' ? location.pathname === '/' : location.pathname.startsWith(it.to))
        ? 'bg-brand-50 text-brand-700 dark:bg-brand-500/10 dark:text-brand-300'
        : 'text-slate-600 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800/70',
    )

  return (
    <div className="min-h-screen lg:pl-64">
      {/* боковое меню — десктоп */}
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col border-r border-slate-200/70 bg-white/80 px-4 py-5 backdrop-blur dark:border-slate-800 dark:bg-slate-950/80 lg:flex">
        <Logo />
        <nav className="mt-8 flex flex-1 flex-col gap-1">
          {NAV.map((it) => (
            <NavLink key={it.to} to={it.to} end={it.to === '/'} className={link(it)}>
              {it.icon}
              {it.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-slate-200/70 pt-4 dark:border-slate-800">
          <div className="mb-2 truncate px-3 text-xs muted">Вы вошли как {username}</div>
          <div className="flex items-center justify-between">
            <ThemeSwitch />
            <button onClick={onLogout} className="flex h-9 items-center gap-2 rounded-xl px-3 text-sm text-slate-500 hover:bg-rose-50 hover:text-rose-600 dark:hover:bg-rose-500/10">
              <LogOut className="h-4 w-4" /> Выйти
            </button>
          </div>
        </div>
      </aside>

      {/* шапка — мобильная */}
      <header className="sticky top-0 z-30 flex items-center justify-between border-b border-slate-200/70 bg-white/80 px-4 py-3 backdrop-blur dark:border-slate-800 dark:bg-slate-950/80 lg:hidden">
        <Logo />
        <ThemeSwitch />
      </header>

      <main className="mx-auto max-w-7xl px-4 pb-28 pt-5 sm:px-6 lg:px-8 lg:pb-10 lg:pt-8">
        <Outlet />
      </main>

      {/* нижняя навигация — мобильная */}
      <nav className="pb-safe fixed inset-x-0 bottom-0 z-30 border-t border-slate-200/70 bg-white/90 backdrop-blur dark:border-slate-800 dark:bg-slate-950/90 lg:hidden">
        <div className="grid grid-cols-5">
          {NAV.filter((it) => it.mobile).map((it) => (
            <NavLink
              key={it.to}
              to={it.to}
              end={it.to === '/'}
              className={({ isActive }) =>
                clsx('flex flex-col items-center gap-1 py-2 text-[10px] font-medium', isActive ? 'text-brand-600 dark:text-brand-400' : 'text-slate-500')
              }
            >
              {it.icon}
              {it.label}
            </NavLink>
          ))}
          <button onClick={() => setMore(true)} className="flex flex-col items-center gap-1 py-2 text-[10px] font-medium text-slate-500">
            <MoreHorizontal className="h-5 w-5" />
            Ещё
          </button>
        </div>
      </nav>

      <Modal open={more} onClose={() => setMore(false)} title="Разделы">
        <div className="grid grid-cols-2 gap-2">
          {NAV.filter((it) => !it.mobile).map((it) => (
            <NavLink key={it.to} to={it.to} onClick={() => setMore(false)} className="flex items-center gap-3 rounded-2xl border border-slate-200 p-4 text-sm font-medium dark:border-slate-800">
              {it.icon}
              {it.label}
            </NavLink>
          ))}
        </div>
        <button onClick={onLogout} className="mt-4 flex w-full items-center justify-center gap-2 rounded-2xl bg-rose-50 p-3 text-sm font-medium text-rose-600 dark:bg-rose-500/10">
          <LogOut className="h-4 w-4" /> Выйти ({username})
        </button>
      </Modal>
    </div>
  )
}
