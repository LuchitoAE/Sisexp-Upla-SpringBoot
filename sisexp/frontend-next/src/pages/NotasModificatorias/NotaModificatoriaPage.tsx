import { AlertCircle } from 'lucide-react';
import { usePageData } from '../../hooks/usePageData';
import { notaApi } from '../../services/endpoints';
import { formatMoney, timeAgo } from '../../utils/format';
import { esGestionPresupuestal } from '../../utils/config';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Table } from '../../components/ui/Table';
import { Button } from '../../components/ui/Button';
import { useAuth } from '../../contexts/AuthContext';
import { PageLayout } from '../../components/layout/PageLayout';
import { PageHeader } from '../../components/layout/PageHeader';
import { PageSkeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import type { NotaModificatoria } from '../../types';
import styles from './NotaModificatoriaPage.module.css';

const estadoVariants: Record<string, 'default' | 'warning' | 'success' | 'danger'> = {
  pendiente: 'warning',
  configurada: 'success',
  rechazada: 'danger',
};

export default function NotaModificatoriaPage() {
  const { user } = useAuth();
  const { data: notas, loading, error } = usePageData(() => notaApi.list(), []);
  const isAdmin = esGestionPresupuestal(user?.rol);

  if (loading) return <PageSkeleton />;

  const columnas = [
    { key: 'codigo', label: 'Código' },
    {
      key: 'tipo',
      label: 'Tipo',
      render: (n: NotaModificatoria) => (
        <Badge variant={n.tipo === 'inclusion_item' ? 'info' : 'default'}>
          {n.tipo === 'inclusion_item' ? 'Inclusión ítem' : 'Inclusión actividad'}
        </Badge>
      ),
    },
    {
      key: 'nuevoNombre',
      label: 'Descripción',
      render: (n: NotaModificatoria) => (
        <div className={styles.descCell}>
          <span className={styles.descName}>{n.nuevoNombre}</span>
          <span className={styles.descMeta}>{n.origen}</span>
        </div>
      ),
    },
    {
      key: 'montoTransferir',
      label: 'Monto',
      render: (n: NotaModificatoria) => <strong>{formatMoney(n.montoTransferir)}</strong>,
    },
    {
      key: 'estado',
      label: 'Estado',
      render: (n: NotaModificatoria) => (
        <Badge variant={estadoVariants[n.estado] ?? 'default'}>{n.estado}</Badge>
      ),
    },
    {
      key: 'createdAt',
      label: 'Creado',
      render: (n: NotaModificatoria) => <span className="textMuted">{timeAgo(n.createdAt)}</span>,
    },
  ];

  return (
    <PageLayout>
      <PageHeader title="Notas Modificatorias" description="Modificaciones presupuestales">
        {isAdmin && (
          <Button variant="primary" size="md">+ Nueva nota</Button>
        )}
      </PageHeader>

      {error && <div className="cardError"><AlertCircle size={14} strokeWidth={1.5} /> {error}</div>}

      {!error && (!notas || notas.length === 0) ? (
        <Card padding="lg">
          <EmptyState title="Sin notas" description="No hay notas modificatorias registradas." action={<Button variant="primary" size="sm">+ Nueva nota</Button>} />
        </Card>
      ) : (
        <Card>
          <Table<NotaModificatoria>
            columns={columnas}
            data={notas ?? []}
            keyExtractor={(n) => n.id.toString()}
            striped
          />
        </Card>
      )}
    </PageLayout>
  );
}
