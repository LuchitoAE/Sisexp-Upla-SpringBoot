import { AlertCircle } from 'lucide-react';
import { usePageData } from '../../hooks/usePageData';
import { techoApi } from '../../services/endpoints';
import { formatMoney } from '../../utils/format';
import { esGestionPresupuestal } from '../../utils/config';
import { useAuth } from '../../contexts/AuthContext';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Table } from '../../components/ui/Table';
import { Button } from '../../components/ui/Button';
import { PageLayout } from '../../components/layout/PageLayout';
import { PageHeader } from '../../components/layout/PageHeader';
import { PageSkeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import type { TechoPresupuestal } from '../../types';
import styles from './TechoPresupuestalPage.module.css';

function BudgetCell({ total, usado }: { total: number; usado: number }) {
  const pct = total > 0 ? (usado / total) * 100 : 0;
  const barColor = pct > 90 ? 'var(--color-danger-500)' : pct > 70 ? 'var(--color-warning-500)' : 'var(--color-brand-500)';
  return (
    <div className={styles.budgetCell}>
      <div className={styles.budgetBar}>
        <div className={styles.budgetFill} style={{ width: `${Math.min(pct, 100)}%`, background: barColor }} />
      </div>
      <span className={styles.budgetPct}>{pct.toFixed(0)}%</span>
    </div>
  );
}

export default function TechoPresupuestalPage() {
  const { user } = useAuth();
  const { data: techos, loading, error } = usePageData(() => techoApi.list(), []);
  const isAdmin = esGestionPresupuestal(user?.rol);

  if (loading) return <PageSkeleton />;

  const columnas = [
    { key: 'año', label: 'Año' },
    {
      key: 'montoTotal',
      label: 'Monto total',
      render: (t: TechoPresupuestal) => <strong>{formatMoney(t.montoTotal)}</strong>,
    },
    {
      key: 'montoUtilizado',
      label: 'Utilizado',
      render: (t: TechoPresupuestal) => <BudgetCell total={t.montoTotal} usado={t.montoUtilizado} />,
    },
    {
      key: 'activo',
      label: 'Estado',
      render: (t: TechoPresupuestal) => t.activo
        ? <Badge variant="success">Activo</Badge>
        : <Badge variant="neutral">Inactivo</Badge>,
    },
    {
      key: 'planificado',
      label: 'POI',
      render: (t: TechoPresupuestal) => t.planificado
        ? <Badge variant="success">Planificado</Badge>
        : <Badge variant="warning">Sin planificar</Badge>,
    },
  ];

  return (
    <PageLayout>
      <PageHeader title="Techos Presupuestales" description="Gestión de techos presupuestales">
        {isAdmin && (
          <Button variant="primary" size="md">+ Nuevo techo</Button>
        )}
      </PageHeader>

      {error && <div className="cardError"><AlertCircle size={14} strokeWidth={1.5} /> {error}</div>}

      {!error && (!techos || techos.length === 0) ? (
        <Card padding="lg">
          <EmptyState title="Sin techos" description="No hay techos presupuestales registrados. Registre el primer techo para comenzar la planificación." action={<Button variant="primary" size="sm">+ Nuevo techo</Button>} />
        </Card>
      ) : (
        <Card>
          <Table<TechoPresupuestal>
            columns={columnas}
            data={techos ?? []}
            keyExtractor={(t) => t.id.toString()}
            striped
          />
        </Card>
      )}
    </PageLayout>
  );
}
