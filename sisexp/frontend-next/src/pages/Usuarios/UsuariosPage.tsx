import { AlertCircle } from 'lucide-react';
import { usePageData } from '../../hooks/usePageData';
import { usuarioApi } from '../../services/endpoints';
import { useAuth } from '../../contexts/AuthContext';
import { Card } from '../../components/ui/Card';
import { Badge, RoleBadge } from '../../components/ui/Badge';
import { Table } from '../../components/ui/Table';
import { Button } from '../../components/ui/Button';
import { PageLayout } from '../../components/layout/PageLayout';
import { PageHeader } from '../../components/layout/PageHeader';
import { PageSkeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import { getInitials } from '../../utils/format';
import type { Usuario } from '../../types';
import styles from './UsuariosPage.module.css';

export default function UsuariosPage() {
  const { user } = useAuth();
  const { data: usuarios, loading, error } = usePageData(() => usuarioApi.list(), []);

  if (loading) return <PageSkeleton />;

  if (user?.rol !== 'Administrador') {
    return (
      <PageLayout>
        <Card padding="lg">
          <div className={styles.restricted}>
            Acceso restringido. Solo administradores pueden gestionar usuarios.
          </div>
        </Card>
      </PageLayout>
    );
  }

  const columnas = [
    {
      key: 'nombre',
      label: 'Nombre',
      render: (u: Usuario) => (
        <div className={styles.avatarCell}>
          <div className={styles.avatar}>
            {getInitials(u.nombre)}
          </div>
          <div className={styles.avatarInfo}>
            <span className={styles.avatarName}>{u.nombre}</span>
            <span className={styles.avatarEmail}>{u.email}</span>
          </div>
        </div>
      ),
    },
    {
      key: 'rol',
      label: 'Rol',
      render: (u: Usuario) => <RoleBadge rol={u.rol} />,
    },
    {
      key: 'activo',
      label: 'Estado',
      render: (u: Usuario) => u.activo
        ? <Badge variant="success">Activo</Badge>
        : <Badge variant="danger">Inactivo</Badge>,
    },
    {
      key: 'horarioRestringido',
      label: 'Horario',
      render: (u: Usuario) => u.horarioRestringido
        ? <Badge variant="warning">Restringido</Badge>
        : <Badge variant="success">24/7</Badge>,
    },
  ];

  return (
    <PageLayout>
      <PageHeader title="Usuarios" description={`${usuarios?.length ?? 0} usuarios registrados`}>
        <Button variant="primary" size="md">+ Nuevo usuario</Button>
      </PageHeader>

      {error && <div className="cardError"><AlertCircle size={14} strokeWidth={1.5} /> {error}</div>}

      {!error && (!usuarios || usuarios.length === 0) ? (
        <Card padding="lg">
          <EmptyState title="Sin usuarios" description="No hay usuarios registrados en el sistema. Agregue el primer usuario para comenzar." action={<Button variant="primary" size="sm">+ Nuevo usuario</Button>} />
        </Card>
      ) : (
        <Card>
          <Table<Usuario>
            columns={columnas}
            data={usuarios ?? []}
            keyExtractor={(u) => u.id.toString()}
            striped
          />
        </Card>
      )}
    </PageLayout>
  );
}
