import styles from './Progress.module.css';

interface ProgressProps {
  value: number;
  max?: number;
  color?: string;
  height?: number;
  showLabel?: boolean;
}

export function Progress({
  value,
  max = 100,
  color = 'var(--color-brand-500)',
  height = 8,
  showLabel = false,
}: ProgressProps) {
  const pct = max > 0 ? Math.min(100, (value / max) * 100) : 0;
  return (
    <div>
      <div
        className={styles.track}
        style={{ height }}
        role="progressbar"
        aria-valuenow={value}
        aria-valuemin={0}
        aria-valuemax={max}
        aria-label={`${pct.toFixed(0)}%`}
      >
        <div className={styles.fill} style={{ width: `${pct}%`, background: color }} />
      </div>
      {showLabel && <div className={styles.label}>{pct.toFixed(0)}%</div>}
    </div>
  );
}

interface BudgetProgressProps {
  ejecutado: number;
  comprometido: number;
  disponible: number;
  height?: number;
}

export function BudgetProgress({
  ejecutado,
  comprometido,
  disponible,
  height = 8,
}: BudgetProgressProps) {
  const total = ejecutado + comprometido + disponible;
  if (total === 0) return <div className={styles.budgetEmpty} style={{ height }} role="progressbar" aria-valuenow={0} aria-valuemin={0} aria-valuemax={100} />;

  const ePct = (ejecutado / total) * 100;
  const cPct = (comprometido / total) * 100;
  const dPct = (disponible / total) * 100;

  return (
    <div className={styles.budgetTrack} style={{ height }} role="progressbar" aria-valuenow={ejecutado} aria-valuemin={0} aria-valuemax={total} aria-label="Progreso presupuestal">
      {ejecutado > 0 && (
        <div
          className={styles.budgetSegment}
          style={{ width: `${ePct}%`, background: 'var(--color-success-500)' }}
          title={`Ejecutado: S/ ${ejecutado.toLocaleString()}`}
        />
      )}
      {comprometido > 0 && (
        <div
          className={styles.budgetSegment}
          style={{ width: `${cPct}%`, background: 'var(--color-warning-500)' }}
          title={`Comprometido: S/ ${comprometido.toLocaleString()}`}
        />
      )}
      {disponible > 0 && (
        <div
          className={styles.budgetSegment}
          style={{ width: `${dPct}%`, background: 'var(--color-brand-500)' }}
          title={`Disponible: S/ ${disponible.toLocaleString()}`}
        />
      )}
    </div>
  );
}
