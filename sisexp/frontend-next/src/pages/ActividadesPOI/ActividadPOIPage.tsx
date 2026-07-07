import { useState } from 'react';
import { usePageData } from '../../hooks/usePageData';
import { techoApi, actividadApi } from '../../services/endpoints';
import { formatMoney } from '../../utils/format';
import { esGestionPresupuestal } from '../../utils/config';
import { useAuth } from '../../contexts/AuthContext';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Table } from '../../components/ui/Table';
import { Button } from '../../components/ui/Button';
import { Tabs } from '../../components/ui/Tabs';
import { PageLayout } from '../../components/layout/PageLayout';
import { PageHeader } from '../../components/layout/PageHeader';
import { PageSkeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import type { ActividadPOI } from '../../types';
import styles from './ActividadPOIPage.module.css';

const ESTADO_CONFIG: Record<string, { label: string; variant: 'success' | 'warning' | 'info' | 'default' }> = {
  Pendiente: { label: 'Pendiente', variant: 'default' },
  En_proceso: { label: 'En proceso', variant: 'info' },
  Finalizada: { label: 'Finalizada', variant: 'success' },
  Extemporanea: { label: 'Extemporánea', variant: 'warning' },
};

export default function ActividadPOIPage() {
  const { user } = useAuth();
  const { data: techos, loading } = usePageData(() => techoApi.list(), []);
  const [techoActivo, setTechoActivo] = useState<number | null>(null);
  const { data: actividades } = usePageData(
    () => (techoActivo ? actividadApi.listByTecho(techoActivo) : Promise.resolve([] as ActividadPOI[])),
    [techoActivo],
  );

  const techoSeleccionado = techoActivo ?? techos?.[0]?.id ?? null;
  const techosTabs = (techos ?? []).map((t) => ({ id: t.id.toString(), label: `${t.año}` }));
  const isAdmin = esGestionPresupuestal(user?.rol);

  if (loading) return <PageSkeleton />;

  const columnas = [
    { key: 'codigo', label: 'Código' },
    { key: 'nombre', label: 'Nombre' },
    {
      key: 'presupuestoAsignado',
      label: 'Asignado',
      render: (a: ActividadPOI) => <strong>{formatMoney(a.presupuestoAsignado)}</strong>,
    },
    {
      key: 'disponible',
      label: 'Disponible',
      render: (a: ActividadPOI) => (
        <span style={{ color: a.disponible < 0 ? 'var(--color-danger-600)' : 'var(--color-success-600)', fontWeight: 600 }}>
          {formatMoney(a.disponible)}
        </span>
      ),
    },
    {
      key: 'estado',
      label: 'Estado',
      render: (a: ActividadPOI) => {
        const e = ESTADO_CONFIG[a.estado] ?? { label: a.estado, variant: 'default' };
        return <Badge variant={e.variant}>{e.label}</Badge>;
      },
    },
    {
      key: 'fechaLimite',
      label: 'Fecha límite',
      render: (a: ActividadPOI) => a.fechaLimite
        ? <span className="textMuted">{new Date(a.fechaLimite).toLocaleDateString('es-PE')}</span>
        : <span className="textMuted">&mdash;</span>,
    },
  ];

  return (
    <PageLayout>
      <PageHeader title="Actividades POI" description="Plan operativo institucional">
        {isAdmin && (
          <Button variant="primary" size="md">+ Nueva actividad</Button>
        )}
      </PageHeader>

      {techosTabs.length > 0 && (
        <div className={styles.filterBar}>
          <Tabs
            tabs={techosTabs}
            active={(techoSeleccionado ?? techosTabs[0]?.id ?? '').toString()}
            onChange={(id) => setTechoActivo(Number(id))}
          />
        </div>
      )}

      {!actividades || actividades.length === 0 ? (
        <Card padding="lg">
          <EmptyState
            title="Sin actividades"
            description="No hay actividades POI para este techo presupuestal. Cree la primera actividad para comenzar."
            action={<Button variant="primary" size="sm">+ Nueva actividad</Button>}
          />
        </Card>
      ) : (
        <Card>
          <Table<ActividadPOI>
            columns={columnas}
            data={actividades}
            keyExtractor={(a) => a.id.toString()}
            striped
          />
        </Card>
      )}
    </PageLayout>
  );
}
