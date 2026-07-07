import { useEffect, useState, useCallback } from 'react';
import { client } from '../services/api';
import type { Notificacion } from '../types';

export function useNotifications() {
  const [count, setCount] = useState(0);
  const [notifs, setNotifs] = useState<Notificacion[]>([]);
  const [showNotifs, setShowNotifs] = useState(false);

  useEffect(() => {
    const load = () => {
      client.get<{ count: number }>('/notificaciones/count')
        .then((r) => setCount(r.count))
        .catch(() => {});
    };
    load();
    const interval = setInterval(load, 30000);
    return () => clearInterval(interval);
  }, []);

  const openNotifs = useCallback(async () => {
    setShowNotifs((prev) => !prev);
    if (!showNotifs) {
      try {
        const data = await client.get<Notificacion[]>('/notificaciones');
        setNotifs(data);
      } catch { /* ignore */ }
    }
  }, [showNotifs]);

  const markAll = useCallback(async () => {
    try {
      await client.put('/notificaciones/read-all');
      setCount(0);
      setNotifs((n) => n.map((x) => ({ ...x, leida: true })));
    } catch { /* ignore */ }
  }, []);

  const markOne = useCallback(async (id: number) => {
    try {
      await client.put(`/notificaciones/${id}/read`);
      setCount((c) => Math.max(0, c - 1));
      setNotifs((n) => n.map((x) => (x.id === id ? { ...x, leida: true } : x)));
    } catch { /* ignore */ }
  }, []);

  return { count, notifs, showNotifs, openNotifs, markAll, markOne, setShowNotifs };
}
