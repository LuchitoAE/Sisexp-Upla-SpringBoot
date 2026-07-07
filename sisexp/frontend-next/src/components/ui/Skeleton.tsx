import styles from './Skeleton.module.css';

interface SkeletonProps {
  width?: string | number;
  height?: string | number;
  borderRadius?: string | number;
  className?: string;
}

export function Skeleton({
  width = '100%',
  height = 16,
  borderRadius = 'var(--radius-sm)',
  className = '',
}: SkeletonProps) {
  return (
    <div
      className={`${styles.skeleton} ${className}`}
      style={{ width, height, borderRadius }}
      aria-hidden="true"
    />
  );
}

export function PageSkeleton() {
  return (
    <div className={styles.pageSkeleton}>
      <div className={styles.titleRow}>
        <Skeleton width={240} height={28} borderRadius="var(--radius-md)" />
        <div className={styles.spacer} />
        <Skeleton width={120} height={36} borderRadius="var(--radius-md)" />
      </div>
      <div className={styles.grid}>
        {[1, 2, 3, 4, 5, 6].map((i) => (
          <Skeleton key={i} height={100} borderRadius="var(--radius-lg)" />
        ))}
      </div>
    </div>
  );
}

export function DashboardSkeleton() {
  return (
    <div className={styles.pageSkeleton}>
      <div className={styles.titleRow}>
        <Skeleton width={200} height={28} borderRadius="var(--radius-md)" />
      </div>
      <div className={styles.grid}>
        {[1, 2, 3, 4].map((i) => (
          <Skeleton key={i} height={110} borderRadius="var(--radius-lg)" />
        ))}
      </div>
      <div className={styles.dashboardRow}>
        <Skeleton height={200} borderRadius="var(--radius-lg)" />
        <Skeleton height={200} borderRadius="var(--radius-lg)" />
      </div>
      <div className={styles.dashboardBottom}>
        <Skeleton height={180} borderRadius="var(--radius-lg)" />
      </div>
    </div>
  );
}

export function TableSkeleton({ rows = 5, columns = 4 }: { rows?: number; columns?: number }) {
  return (
    <div className={styles.tableSkeleton}>
      <div className={styles.tableHeader}>
        {Array.from({ length: columns }).map((_, i) => (
          <Skeleton key={i} height={14} width={`${Math.max(60, 100 / columns)}%`} />
        ))}
      </div>
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className={styles.tableRow}>
          {Array.from({ length: columns }).map((_, j) => (
            <Skeleton key={j} height={12} width={`${Math.max(60, 100 / columns)}%`} />
          ))}
        </div>
      ))}
    </div>
  );
}
