import { ReactNode } from 'react'

const ALLOWED = new Set(['B', 'STRONG', 'I', 'EM', 'U', 'INS', 'S', 'STRIKE', 'DEL', 'CODE', 'PRE', 'A', 'BLOCKQUOTE', 'TG-SPOILER'])

/** Рендер HTML-разметки Telegram: только разрешённые теги, остальное — текстом. */
function render(node: Node, key: number): ReactNode {
  if (node.nodeType === Node.TEXT_NODE) return node.textContent
  if (node.nodeType !== Node.ELEMENT_NODE) return null
  const el = node as Element
  const children = [...el.childNodes].map((c, i) => render(c, i))
  if (!ALLOWED.has(el.tagName)) return <span key={key}>{children}</span>
  switch (el.tagName) {
    case 'B':
    case 'STRONG':
      return <b key={key}>{children}</b>
    case 'I':
    case 'EM':
      return <i key={key}>{children}</i>
    case 'U':
    case 'INS':
      return <u key={key}>{children}</u>
    case 'S':
    case 'STRIKE':
    case 'DEL':
      return <s key={key}>{children}</s>
    case 'CODE':
      return <code key={key} className="rounded bg-black/5 px-1 font-mono text-[0.9em] dark:bg-white/10">{children}</code>
    case 'PRE':
      return <pre key={key} className="my-1 overflow-x-auto rounded-lg bg-black/5 p-2 font-mono text-xs dark:bg-white/10">{children}</pre>
    case 'BLOCKQUOTE':
      return <blockquote key={key} className="my-1 border-l-[3px] border-sky-500 pl-2">{children}</blockquote>
    case 'TG-SPOILER':
      return <span key={key} className="rounded bg-slate-400/40 blur-[2px] hover:blur-0">{children}</span>
    case 'A': {
      const href = el.getAttribute('href') ?? ''
      return /^https?:\/\//.test(href) ? (
        <a key={key} href={href} target="_blank" rel="noreferrer" className="text-sky-600 underline dark:text-sky-400">
          {children}
        </a>
      ) : (
        <span key={key}>{children}</span>
      )
    }
  }
  return null
}

export function TelegramPreview({ html, buttons }: { html: string; buttons?: string[][] }) {
  const doc = new DOMParser().parseFromString(`<body>${html}</body>`, 'text/html')
  return (
    <div className="rounded-3xl bg-[#c8d7e5] bg-[radial-gradient(circle_at_20%_20%,rgba(255,255,255,.35),transparent_40%)] p-3 dark:bg-[#0e1621] sm:p-4">
      <div className="max-w-[420px]">
        <div className="whitespace-pre-wrap break-words rounded-2xl rounded-bl-md bg-white px-3 py-2 text-[14px] leading-snug text-slate-900 shadow-sm dark:bg-[#182533] dark:text-slate-100">
          {html.trim() ? [...doc.body.childNodes].map((n, i) => render(n, i)) : <span className="text-slate-400">Текст сообщения…</span>}
          <div className="mt-1 text-right text-[10px] text-slate-400">сейчас</div>
        </div>
        {buttons?.map((row, i) => (
          <div key={i} className="mt-1 flex gap-1">
            {row.map((b) => (
              <div key={b} className="flex-1 truncate rounded-xl bg-white/60 px-2 py-2 text-center text-xs font-medium text-slate-700 backdrop-blur dark:bg-[#182533]/80 dark:text-slate-200">
                {b}
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  )
}
