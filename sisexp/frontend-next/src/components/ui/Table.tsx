import type { ReactNode, HTMLAttributes } from 'react';
import { Loader2 } from 'lucide-react';
import styles from './Table.module.css';

interface Column<T> {
  key: string;
  label: string;
  render?: (item: T) => ReactNode;
  className?: string;
}

interface TableProps<T> extends HTMLAttributes<HTMLDivElement> {
  columns: Column<T>[];
  data: T[];
  onRowClick?: (item: T) => void;
  emptyMessage?: string;
  emptyIcon?: ReactNode;
  keyExtractor: (item: T) => string | number;
  striped?: boolean;
  loading?: boolean;
}

export function Table<T extends Record<string, any>>({
  columns,
  data,
  onRowClick,
  emptyMessage = 'No hay datos disponibles',
  emptyIcon,
  keyExtractor,
  striped = false,
  loading = false,
  className = '',
  ...props
}: TableProps<T>) {
  if (!loading && data.length === 0) {
    return (
      <div className={`${styles.wrapper} ${className}`} {...props}>
        <div className={styles.empty}>
          {emptyIcon}
          <span>{emptyMessage}</span>
        </div>
      </div>
    );
  }

  return (
    <div
      className={`${styles.wrapper} ${loading ? styles.loadingOverlay : ''} ${className}`}
      {...props}
    >
      <table className={`${styles.table} ${onRowClick ? styles.clickable : ''} ${striped ? styles.striped : ''}`}>
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key} className={col.className}>{col.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr>
              <td colSpan={columns.length} className={styles.loadingRow}>
                <Loader2 size={20} className={styles.loadingSpinner} />
                Cargando...
              </td>
            </tr>
          ) : (
            data.map((item) => (
              <tr
                key={keyExtractor(item)}
                onClick={() => onRowClick?.(item)}
                tabIndex={onRowClick ? 0 : undefined}
                onKeyDown={onRowClick ? (e) => { if (e.key === 'Enter') onRowClick(item); } : undefined}
              >
                {columns.map((col) => (
                  <td key={col.key} className={col.className}>
                    {col.render ? col.render(item) : String(item[col.key] ?? '')}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
