import { useEffect, useState } from 'react';
import { Wallet, ClipboardList, AlertTriangle, Folder } from 'lucide-react';
import { dashboardApi } from '../../services/endpoints';
import { formatMoney } from '../../utils/format';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { PageLayout } from '../../components/layout/PageLayout';
import { PageHeader } from '../../components/layout/PageHeader';
import { PageSkeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import type { DashboardAlertas, DashboardSaldos } from '../../types';
import styles from './Dashboard.module.css';

type StatVariant = 'brand' | 'info' | 'danger' | 'success';

const STAT_ICON_CLASS: Record<StatVariant, string> = {
  brand: styles.statIconBrand,
  info: styles.statIconInfo,
  danger: styles.statIconDanger,
  success: styles.statIconSuccess,
};

const SEMAFORO: Record<string, { color: string; bg: string }> = {
  rojo: { color: 'var(--color-danger-500)', bg: 'var(--color-danger-50)' },
  amarillo: { color: 'var(--color-warning-500)', bg: 'var(--color-warning-50)' },
  verde: { color: 'var(--color-success-500)', bg: 'var(--color-success-50)' },
};

export default function Dashboard() {
  const [alertas, setAlertas] = useState<DashboardAlertas | null>(null);
  const [saldos, setSaldos] = useState<DashboardSaldos | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const [a, s] = await Promise.all([
          dashboardApi.alertas(),
          dashboardApi.saldos(),
        ]);
        setAlertas(a);
        setSaldos(s);
      } catch (e: unknown) {
        setError(e instanceof Error ? e.message : 'Error al cargar dashboard');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  if (loading) return <PageSkeleton />;

  if (error) {
    return (
      <PageLayout>
        <EmptyState title="Error al cargar" description={error} />
      </PageLayout>
    );
  }

  return (
    <PageLayout>
      <PageHeader title="Panel principal" description="Resumen del sistema de expedientes" />

      {/* Stats */}
      <div className={styles.statsGrid}>
        <StatsCard icon={<Wallet size={22} strokeWidth={1.5} />} variant="brand" value={saldos?.techos?.length ?? 0} label="Techos presupuestales" />
        <StatsCard icon={<ClipboardList size={22} strokeWidth={1.5} />} variant="info" value={saldos?.actividades?.length ?? 0} label="Actividades POI" />
        <StatsCard icon={<AlertTriangle size={22} strokeWidth={1.5} />} variant="danger" value={alertas?.resumen?.rojas ?? 0} label="Alertas críticas" accent />
        <StatsCard icon={<Folder size={22} strokeWidth={1.5} />} variant="success" value={alertas?.expedientes?.length ?? 0} label="Expedientes activos" />
      </div>

      <div className={styles.cols2}>
        {/* Alertas */}
        <Card>
          <div className={styles.cardTitle}>
            <span>Alertas de Actividades</span>
            {alertas?.actividades && alertas.actividades.length > 0 && (
              <span className={styles.cardTitleCount}>{alertas.actividades.length}</span>
            )}
          </div>

          {alertas?.resumen && (
            <div className={styles.alertCounts}>
              <div className={styles.alertCountItem}>
                <span className={styles.dotRed} /> {alertas.resumen.rojas} críticas
              </div>
              <div className={styles.alertCountItem}>
                <span className={styles.dotYellow} /> {alertas.resumen.amarillas} por vencer
              </div>
              <div className={styles.alertCountItem}>
                <span className={styles.dotGreen} /> {alertas.resumen.verdes} en orden
              </div>
            </div>
          )}

          {(!alertas?.actividades || alertas.actividades.length === 0) ? (
            <div className={styles.emptyText}>Sin actividades con alertas.</div>
          ) : (
            alertas.actividades.slice(0, 5).map((act) => {
              const s = SEMAFORO[act.semaforo] ?? { color: 'var(--color-neutral-400)', bg: 'var(--color-neutral-50)' };
              return (
                <div key={act.id} className={styles.alertCard}>
                  <div className={styles.alertDot} style={{ background: s.color }} />
                  <div className={styles.alertContent}>
                    <div className={styles.alertTitle}>{act.codigo} — {act.nombre}</div>
                    <div className={styles.alertSubtitle}>
                      {act.diasRestantes > 0 ? `${act.diasRestantes} días restantes` : 'Vencido'}
                      {' · '}
                      {act.pctEjecucion.toFixed(0)}% ejecutado
                    </div>
                    <div className={styles.alertMeta}>Saldo: {formatMoney(act.saldoDisponible)}</div>
                  </div>
                </div>
              );
            })
          )}
        </Card>

        {/* Techos budget */}
        <Card>
          <div className={styles.cardTitle}>
            <span>Resumen Presupuestal</span>
          </div>

          {(!saldos?.techos || saldos.techos.length === 0) ? (
            <div className={styles.emptyText}>Sin techos presupuestales registrados.</div>
          ) : (
            saldos.techos.map((t) => {
              const pct = t.montoTotal > 0 ? (t.montoUtilizado / t.montoTotal) * 100 : 0;
              return (
                <div key={t.id} className={styles.budgetRow}>
                  <div className={styles.budgetHeader}>
                    <span className={styles.budgetTitle}>Techo {t.año}</span>
                    <span className={styles.budgetAmount}>{formatMoney(t.montoUtilizado)} / {formatMoney(t.montoTotal)}</span>
                  </div>
                  <div className={styles.budgetBar}>
                    <div className={styles.budgetEjecutado} style={{ width: `${Math.min(pct, 100)}%` }} />
                  </div>
                  <div className={styles.budgetLegend}>
                    <span><span className={`${styles.budgetLegendDot} ${styles.budgetDotBrand}`} /> {pct.toFixed(1)}% utilizado</span>
                    <span><span className={`${styles.budgetLegendDot} ${styles.budgetDotNeutral}`} /> {(100 - pct).toFixed(1)}% disponible</span>
                  </div>
                </div>
              );
            })
          )}
        </Card>
      </div>

      {/* Expedientes recientes */}
      <Card>
        <div className={styles.cardTitle}>
          <span>Expedientes recientes</span>
          {alertas?.expedientes && alertas.expedientes.length > 0 && (
            <span className={styles.cardTitleCount}>{alertas.expedientes.length}</span>
          )}
        </div>

        {(!alertas?.expedientes || alertas.expedientes.length === 0) ? (
          <div className={styles.emptyText}>Sin expedientes activos.</div>
        ) : (
          <div>
            {alertas.expedientes.map((exp) => {
              const s = SEMAFORO[exp.semaforo] ?? { color: 'var(--color-neutral-400)', bg: 'var(--color-neutral-50)' };
              return (
                <div key={exp.id} className={styles.alertCard}>
                  <div className={styles.alertDot} style={{ background: s.color }} />
                  <div className={styles.alertContent}>
                    <div className={styles.alertTitle}>
                      {exp.codigo}
                      {' '}
                      <Badge variant={exp.urgencia === 'Urgente' ? 'danger' : exp.urgencia === 'No tan urgente' ? 'warning' : 'default'}>
                        {exp.urgencia.replace(/_/g, ' ')}
                      </Badge>
                    </div>
                    <div className={styles.alertSubtitle}>{exp.descripcion}</div>
                    <div className={styles.alertMeta}>
                      {exp.estado.replace(/_/g, ' ')} · {exp.diasSinMovimiento > 0 ? `${exp.diasSinMovimiento} días sin movimiento` : 'Hoy'}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </Card>
    </PageLayout>
  );
}

function StatsCard({ icon, variant, value, label, accent }: {
  icon: React.ReactNode;
  variant: StatVariant;
  value: number | string;
  label: string;
  accent?: boolean;
}) {
  const iconClass = `${styles.statIcon} ${STAT_ICON_CLASS[variant]}`;
  const valueClass = `${styles.statValue} ${accent ? styles.statValueAccent : ''}`;
  return (
    <Card padding="md">
      <div className={styles.statCard}>
        <div className={iconClass}>{icon}</div>
        <div className={valueClass}>{value}</div>
        <div className={styles.statLabel}>{label}</div>
      </div>
    </Card>
  );
}
