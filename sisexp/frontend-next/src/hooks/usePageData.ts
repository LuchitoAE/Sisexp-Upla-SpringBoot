import { useState, useEffect, useCallback } from 'react';

interface UsePageDataResult<T> {
  data: T | null;
  loading: boolean;
  error: string;
  refetch: () => void;
}

export function usePageData<T>(
  fetcher: () => Promise<T>,
  deps: unknown[] = [],
): UsePageDataResult<T> {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const result = await fetcher();
      setData(result);
      } catch (e: unknown) {
        setError(e instanceof Error ? e.message : 'Error al cargar datos');
    } finally {
      setLoading(false);
    }
  }, deps);

  useEffect(() => { load(); }, [load]);

  return { data, loading, error, refetch: load };
}
