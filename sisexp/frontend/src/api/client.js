const API_URL = '/api';
const CACHE_TTL = 30_000;

const cache = new Map();

function cacheGet(path) {
  const entry = cache.get(path);
  if (entry && Date.now() - entry.time < CACHE_TTL) return entry.data;
  cache.delete(path);
  return undefined;
}

function cacheSet(path, data) {
  if (path === '/notificaciones/count') return data;
  cache.set(path, { data, time: Date.now() });
  return data;
}

export function invalidarCache(prefijo) {
  if (!prefijo) { cache.clear(); return; }
  for (const key of cache.keys()) {
    if (key.startsWith(prefijo)) cache.delete(key);
  }
}

export async function refreshGet(path, _token) {
  cache.delete(path);
  return get(path, _token);
}

function errorDeRed(err) {
  if (!navigator.onLine || err.message === 'Failed to fetch') {
    return new Error('Sin conexion al servidor. Verifique su red o intente mas tarde.');
  }
  return err;
}

async function handleResponse(res) {
  if (res.status === 204) return null;
  if (res.status === 401) {
    window.location.href = '/login';
    throw new Error('Sesion expirada');
  }
  if (res.status === 403) {
    const text = await res.text();
    let data;
    try { data = text ? JSON.parse(text) : null; } catch { data = text; }
    throw new Error((data && data.error) || data || 'Sistema fuera de horario laboral (8am-8pm, Peru).');
  }
  const text = await res.text();
  let data;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (!res.ok) throw new Error((data && data.error) || res.statusText || 'Error de red');
  return data;
}

async function get(path, _token) {
  const cached = cacheGet(path);
  if (cached !== undefined) return cached;
  try {
    const res = await fetch(`${API_URL}${path}`, { credentials: 'include' });
    const data = await handleResponse(res);
    return cacheSet(path, data);
  } catch (err) { throw errorDeRed(err); }
}

async function post(path, body, _token) {
  try {
    const res = await fetch(`${API_URL}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(body)
    });
    const data = await handleResponse(res);
    invalidarCache(path.split('/').slice(0, 2).join('/'));
    return data;
  } catch (err) { throw errorDeRed(err); }
}

async function put(path, body, _token) {
  try {
    const res = await fetch(`${API_URL}${path}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(body)
    });
    const data = await handleResponse(res);
    invalidarCache(path.split('/').slice(0, 2).join('/'));
    return data;
  } catch (err) { throw errorDeRed(err); }
}

async function del(path, _token) {
  try {
    const res = await fetch(`${API_URL}${path}`, {
      method: 'DELETE',
      credentials: 'include'
    });
    const data = await handleResponse(res);
    invalidarCache(path.split('/').slice(0, 2).join('/'));
    return data;
  } catch (err) { throw errorDeRed(err); }
}

async function patch(path, body, _token) {
  try {
    const res = await fetch(`${API_URL}${path}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(body)
    });
    const data = await handleResponse(res);
    invalidarCache(path.split('/').slice(0, 2).join('/'));
    return data;
  } catch (err) { throw errorDeRed(err); }
}

async function upload(path, file, _token, fieldName = 'archivo', extraFields = {}) {
  const fd = new FormData();
  fd.append(fieldName, file);
  Object.entries(extraFields).forEach(([k, v]) => fd.append(k, v));
  try {
    const res = await fetch(`${API_URL}${path}`, {
      method: 'POST',
      credentials: 'include',
      body: fd
    });
    const data = await handleResponse(res);
    invalidarCache(path.split('/').slice(0, 2).join('/'));
    return data;
  } catch (err) { throw errorDeRed(err); }
}

export const client = { get, post, put, patch, del, upload, invalidarCache, refreshGet };
export { API_URL };
