import { useState, useMemo } from 'react';
import { AlertCircle } from 'lucide-react';
import { usePageData } from '../../hooks/usePageData';
import { expedienteApi } from '../../services/endpoints';
import { formatMoney, timeAgo } from '../../utils/format';
import { puede } from '../../utils/config';
import { useAuth } from '../../contexts/AuthContext';
import { Card } from '../../components/ui/Card';
import { EstadoBadge, UrgenciaBadge } from '../../components/ui/Badge';
import { Table } from '../../components/ui/Table';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Tabs } from '../../components/ui/Tabs';
import { PageLayout } from '../../components/layout/PageLayout';
import { PageHeader } from '../../components/layout/PageHeader';
import { PageSkeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import type { Expediente } from '../../types';
import styles from './ExpedienteList.module.css';

const FILTROS_ESTADO = [
  { id: 'todos', label: 'Todos' },
  { id: 'Borrador', label: 'Borrador' },
  { id: 'En_revision', label: 'En revisión' },
  { id: 'Aprobado', label: 'Aprobado' },
  { id: 'Rechazado', label: 'Rechazado' },
  { id: 'Observado', label: 'Observado' },
  { id: 'Derivado', label: 'Derivado' },
  { id: 'Finalizado', label: 'Finalizado' },
];

export default function ExpedienteList() {
  const { user } = useAuth();
  const { data: expedientes, loading, error } = usePageData(() => expedienteApi.list(), []);
  const [filtroEstado, setFiltroEstado] = useState('todos');
  const [search, setSearch] = useState('');

  const filtrados = useMemo(() => {
    if (!expedientes) return [];
    let list = expedientes;
    if (filtroEstado !== 'todos') {
      list = list.filter((e) => e.estado === filtroEstado);
    }
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(
        (e) =>
          e.codigo.toLowerCase().includes(q) ||
          e.descripcion.toLowerCase().includes(q) ||
          e.solicitante?.nombre?.toLowerCase().includes(q),
      );
    }
    return list;
  }, [expedientes, filtroEstado, search]);

  const puedeCrear = puede(user?.rol, 'crearExpediente');

  if (loading) return <PageSkeleton />;

  const columnas = [
    { key: 'codigo', label: 'Código' },
    {
      key: 'descripcion',
      label: 'Descripción',
      render: (item: Expediente) => (
        <div className={styles.descCell}>
          <div className={styles.descCode}>{item.codigo}</div>
          <div className={styles.descSolicitante}>{item.solicitante?.nombre ?? '—'}</div>
        </div>
      ),
    },
    {
      key: 'urgencia',
      label: 'Urgencia',
      render: (item: Expediente) => <UrgenciaBadge urgencia={item.urgencia} />,
    },
    {
      key: 'estado',
      label: 'Estado',
      render: (item: Expediente) => <EstadoBadge estado={item.estado} />,
    },
    {
      key: 'costoEstimado',
      label: 'Costo est.',
      render: (item: Expediente) => <span className={styles.costoCell}>{formatMoney(item.costoEstimado)}</span>,
    },
    {
      key: 'createdAt',
      label: 'Creado',
      render: (item: Expediente) => <span className="textMuted">{timeAgo(item.createdAt)}</span>,
    },
  ];

  return (
    <PageLayout>
      <PageHeader title="Expedientes" description={search ? `Resultados para "${search}"` : 'Gestión de expedientes y documentos'}>
        {puedeCrear && (
          <Button variant="primary" size="md">+ Nuevo expediente</Button>
        )}
      </PageHeader>

      <div className={styles.filterBar}>
        <div className={styles.searchInput}>
          <Input
            placeholder="Buscar por código, descripción..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <Tabs tabs={FILTROS_ESTADO} active={filtroEstado} onChange={setFiltroEstado} />
      </div>

      {error && (
        <div className="cardError">
          <AlertCircle size={14} strokeWidth={1.5} />
          <span>{error}</span>
        </div>
      )}

      {!error && filtrados.length === 0 ? (
        <Card padding="lg">
          <EmptyState
            title="Sin expedientes"
            description={search ? 'No se encontraron expedientes con ese filtro.' : 'No hay expedientes registrados aún. Cree el primer expediente para comenzar el seguimiento.'}
          />
        </Card>
      ) : (
        <Card>
          <Table<Expediente>
            columns={columnas}
            data={filtrados}
            keyExtractor={(e) => e.id.toString()}
            onRowClick={() => {}}
            striped
          />
        </Card>
      )}
    </PageLayout>
  );
}
