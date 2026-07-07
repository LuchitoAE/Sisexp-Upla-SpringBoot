import { useState } from 'react';
import { usePageData } from '../../hooks/usePageData';
import { techoApi, actividadApi, necesidadApi } from '../../services/endpoints';
import { formatMoney } from '../../utils/format';
import { esGestionPresupuestal } from '../../utils/config';
import { useAuth } from '../../contexts/AuthContext';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Table } from '../../components/ui/Table';
import { Button } from '../../components/ui/Button';
import { Select } from '../../components/ui/Input';
import { PageLayout } from '../../components/layout/PageLayout';
import { PageHeader } from '../../components/layout/PageHeader';
import { PageSkeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import type { NecesidadPAP, ActividadPOI } from '../../types';
import styles from './NecesidadPAPPage.module.css';

export default function NecesidadPAPPage() {
  const { user } = useAuth();
  const { data: techos, loading: loadingTechos } = usePageData(() => techoApi.list(), []);
  const [techoId, setTechoId] = useState<number | null>(null);
  const [actividadId, setActividadId] = useState<number | null>(null);

  const { data: actividades } = usePageData(
    () => (techoId ? actividadApi.listByTecho(techoId) : Promise.resolve([] as ActividadPOI[])),
    [techoId],
  );
  const { data: necesidades } = usePageData(
    () => (actividadId ? necesidadApi.listByActividad(actividadId) : Promise.resolve([] as NecesidadPAP[])),
    [actividadId],
  );

  const isAdmin = esGestionPresupuestal(user?.rol);

  if (loadingTechos) return <PageSkeleton />;

  const techoOptions = [
    { value: '', label: 'Seleccionar techo' },
    ...(techos ?? []).map((t) => ({ value: t.id.toString(), label: `Techo ${t.año}` })),
  ];

  const actividadOptions = [
    { value: '', label: 'Seleccionar actividad' },
    ...(actividades ?? []).map((a) => ({ value: a.id.toString(), label: `${a.codigo} — ${a.nombre}` })),
  ];

  const columnas = [
    { key: 'nombre', label: 'Nombre' },
    { key: 'unidad', label: 'Unidad' },
    {
      key: 'cantidad',
      label: 'Cantidad',
      render: (n: NecesidadPAP) => `${n.cantidad}${n.cantidadDisponible ? ` (${n.cantidadDisponible} disp.)` : ''}`,
    },
    {
      key: 'precioEstimado',
      label: 'Precio est.',
      render: (n: NecesidadPAP) => formatMoney(n.precioEstimado),
    },
    {
      key: 'montoDisponible',
      label: 'Monto disp.',
      render: (n: NecesidadPAP) => (
        <span style={{ color: n.montoDisponible < 0 ? 'var(--color-danger-600)' : 'var(--color-success-600)', fontWeight: 600 }}>
          {formatMoney(n.montoDisponible)}
        </span>
      ),
    },
    {
      key: 'tipo',
      label: 'Tipo',
      render: (n: NecesidadPAP) => n.tipo === 'Bien'
        ? <Badge variant="info">Bien</Badge>
        : <Badge variant="warning">Servicio</Badge>,
    },
    { key: 'oficinaLaboratorio', label: 'Oficina/Lab' },
  ];

  return (
    <PageLayout>
      <PageHeader title="Necesidades PAP" description="Plan anual de pagos">
        {isAdmin && (
          <Button variant="primary" size="md">+ Nueva necesidad</Button>
        )}
      </PageHeader>

      <div className={styles.filterBar}>
        <Select
          value={techoId?.toString() ?? ''}
          onChange={(e) => { setTechoId(e.target.value ? Number(e.target.value) : null); setActividadId(null); }}
          options={techoOptions}
          aria-label="Filtrar por techo"
        />
        {actividades && actividades.length > 0 && (
          <Select
            value={actividadId?.toString() ?? ''}
            onChange={(e) => setActividadId(e.target.value ? Number(e.target.value) : null)}
            options={actividadOptions}
            aria-label="Filtrar por actividad"
          />
        )}
      </div>

      {!necesidades || necesidades.length === 0 ? (
        <Card padding="lg">
          <EmptyState
            title="Sin necesidades"
            description={actividadId ? 'No hay necesidades PAP para esta actividad.' : 'Seleccione un techo y actividad para ver necesidades.'}
            action={actividadId ? <Button variant="primary" size="sm">+ Nueva necesidad</Button> : undefined}
          />
        </Card>
      ) : (
        <Card>
          <Table<NecesidadPAP>
            columns={columnas}
            data={necesidades}
            keyExtractor={(n) => n.id.toString()}
            striped
          />
        </Card>
      )}
    </PageLayout>
  );
}
