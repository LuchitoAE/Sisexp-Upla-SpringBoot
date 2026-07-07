const API_URL = '/api';
const CACHE_TTL = 30_000;

const cache = new Map<string, { data: unknown; time: number }>();

function cacheGet<T>(path: string): T | undefined {
  const entry = cache.get(path);
  if (entry && Date.now() - entry.time < CACHE_TTL) return entry.data as T;
  cache.delete(path);
  return undefined;
}

function cacheSet<T>(path: string, data: T): T {
  if (path === '/notificaciones/count') return data;
  cache.set(path, { data, time: Date.now() });
  return data;
}

export function invalidarCache(prefijo?: string): void {
  if (!prefijo) { cache.clear(); return; }
  for (const key of cache.keys()) {
    if (key.startsWith(prefijo)) cache.delete(key);
  }
}

export async function refreshGet<T>(path: string): Promise<T> {
  cache.delete(path);
  return get<T>(path);
}

class ApiError extends Error {
  status?: number;
  constructor(message: string, status?: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

async function handleResponse(res: Response): Promise<unknown> {
  if (res.status === 204) return null;
  if (res.status === 401) {
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
    throw new ApiError('Sesión expirada', 401);
  }
  if (res.status === 403) {
    const text = await res.text();
    let data: { error?: string } | string | null = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = text; }
    const msg = (data && typeof data === 'object' && 'error' in data)
      ? (data as { error: string }).error
      : typeof data === 'string' ? data : 'Sistema fuera de horario laboral (8am-8pm, Peru).';
    throw new ApiError(msg, 403);
  }
  const text = await res.text();
  let data: unknown;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (!res.ok) {
    const msg = data && typeof data === 'object' && 'error' in (data as Record<string, unknown>)
      ? ((data as Record<string, string>).error)
      : res.statusText || 'Error de red';
    throw new ApiError(msg, res.status);
  }
  return data;
}

async function get<T>(path: string): Promise<T> {
  const cached = cacheGet<T>(path);
  if (cached !== undefined) return cached;
  try {
    const res = await fetch(`${API_URL}${path}`, { credentials: 'include' });
    const data = await handleResponse(res) as T;
    return cacheSet(path, data);
  } catch (err) {
    if (err instanceof ApiError) throw err;
    if (!navigator.onLine || (err instanceof TypeError && err.message === 'Failed to fetch')) {
      throw new ApiError('Sin conexión al servidor. Verifique su red o intente más tarde.');
    }
    throw err;
  }
}

async function post<T>(path: string, body?: unknown): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await handleResponse(res) as T;
  invalidarCache(path.split('/').slice(0, 2).join('/'));
  return data;
}

async function put<T>(path: string, body?: unknown): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await handleResponse(res) as T;
  invalidarCache(path.split('/').slice(0, 2).join('/'));
  return data;
}

async function del<T>(path: string): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    method: 'DELETE',
    credentials: 'include',
  });
  const data = await handleResponse(res) as T;
  invalidarCache(path.split('/').slice(0, 2).join('/'));
  return data;
}

async function patch<T>(path: string, body?: unknown): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await handleResponse(res) as T;
  invalidarCache(path.split('/').slice(0, 2).join('/'));
  return data;
}

async function upload<T>(
  path: string,
  file: File,
  fieldName = 'archivo',
  extraFields: Record<string, string> = {},
): Promise<T> {
  const fd = new FormData();
  fd.append(fieldName, file);
  Object.entries(extraFields).forEach(([k, v]) => fd.append(k, v));
  const res = await fetch(`${API_URL}${path}`, {
    method: 'POST',
    credentials: 'include',
    body: fd,
  });
  const data = await handleResponse(res) as T;
  invalidarCache(path.split('/').slice(0, 2).join('/'));
  return data;
}

export const client = { get, post, put, patch, del, upload, invalidarCache, refreshGet };
