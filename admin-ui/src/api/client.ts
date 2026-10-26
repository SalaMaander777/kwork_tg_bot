// HTTP-клиент админки: сессия в cookie, CSRF-токен из cookie XSRF-TOKEN в заголовок
export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public fields?: Record<string, string>,
  ) {
    super(message)
  }
}

const BASE = '/admin/api'

let onUnauthorized: () => void = () => {}
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

function csrfToken(): string | undefined {
  return document.cookie
    .split('; ')
    .find((c) => c.startsWith('XSRF-TOKEN='))
    ?.split('=')[1]
}

export async function api<T>(path: string, init: RequestInit & { json?: unknown } = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.json !== undefined) headers.set('Content-Type', 'application/json')
  if (method !== 'GET') {
    const token = csrfToken()
    if (token) headers.set('X-XSRF-TOKEN', decodeURIComponent(token))
  }
  const res = await fetch(BASE + path, {
    ...init,
    method,
    headers,
    credentials: 'same-origin',
    body: init.json !== undefined ? JSON.stringify(init.json) : init.body,
  })
  if (res.status === 401 && !path.startsWith('/auth/')) {
    onUnauthorized()
  }
  const text = await res.text()
  const data = text ? safeJson(text) : null
  if (!res.ok) {
    const body = (data && typeof data === 'object' ? data : {}) as { error?: string; fields?: Record<string, string> }
    const message = body.error ?? (res.status === 403 ? 'Доступ запрещён — обновите страницу' : `Ошибка ${res.status}`)
    throw new ApiError(res.status, message, body.fields)
  }
  return data as T
}

function safeJson(text: string): unknown {
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

export const get = <T,>(path: string) => api<T>(path)
export const post = <T,>(path: string, json?: unknown) => api<T>(path, { method: 'POST', json: json ?? {} })
export const put = <T,>(path: string, json: unknown) => api<T>(path, { method: 'PUT', json })

export function qs(params: Record<string, string | number | boolean | undefined | null>): string {
  const s = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') s.set(k, String(v))
  })
  const str = s.toString()
  return str ? `?${str}` : ''
}
