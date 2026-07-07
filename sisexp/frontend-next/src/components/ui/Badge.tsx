import type { ReactNode } from 'react';
import styles from './Badge.module.css';

type BadgeVariant = 'default' | 'success' | 'warning' | 'danger' | 'info' | 'neutral';

interface BadgeProps {
  children: ReactNode;
  variant?: BadgeVariant;
  className?: string;
}

const variantClass: Record<BadgeVariant, string> = {
  default: styles.default,
  success: styles.success,
  warning: styles.warning,
  danger: styles.danger,
  info: styles.info,
  neutral: styles.neutral,
};

export function Badge({ children, variant = 'default', className = '' }: BadgeProps) {
  return (
    <span className={`${styles.badge} ${variantClass[variant]} ${className}`} role="status">
      {children}
    </span>
  );
}

// ─── Estado badge (expediente estados) ───

const estadoVariant: Record<string, BadgeVariant> = {
  Borrador: 'default',
  En_revision: 'warning',
  Aprobado: 'success',
  Rechazado: 'danger',
  Finalizado: 'default',
  Observado: 'warning',
  Derivado: 'info',
};

export function EstadoBadge({ estado }: { estado: string }) {
  const v = estadoVariant[estado] || 'default';
  return (
    <span className={`${styles.badge} ${variantClass[v]}`} role="status">
      {estado.replace(/_/g, ' ')}
    </span>
  );
}

// ─── Urgencia badge ───

const urgenciaVariant: Record<string, BadgeVariant> = {
  Urgente: 'danger',
  'No tan urgente': 'warning',
  'Puede esperar': 'success',
};

export function UrgenciaBadge({ urgencia }: { urgencia: string }) {
  const v = urgenciaVariant[urgencia] || 'default';
  return (
    <span className={`${styles.badge} ${variantClass[v]}`}>
      {urgencia}
    </span>
  );
}

// ─── Role badge ───

const roleVariant: Record<string, BadgeVariant> = {
  Administrador: 'danger',
  Coordinacion: 'info',
  Secretaria: 'info',
  Director: 'info',
  Laboratorio: 'warning',
  Decanato: 'default',
};

export function RoleBadge({ rol }: { rol: string }) {
  const v = roleVariant[rol] || 'default';
  return (
    <span className={`${styles.badge} ${variantClass[v]}`}>
      {rol}
    </span>
  );
}
