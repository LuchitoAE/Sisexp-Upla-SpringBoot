import type { ReactNode } from 'react';
import { ClipboardList } from 'lucide-react';
import styles from './EmptyState.module.css';

type EmptyStateSize = 'sm' | 'md' | 'lg';
type EmptyStateVariant = 'default' | 'brand' | 'warning';

interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
  size?: EmptyStateSize;
  variant?: EmptyStateVariant;
  className?: string;
}

const sizeClass: Record<EmptyStateSize, string> = {
  sm: styles.sizeSm,
  md: styles.sizeMd,
  lg: styles.sizeLg,
};

const variantClass: Record<EmptyStateVariant, string> = {
  default: styles.variantDefault,
  brand: styles.variantBrand,
  warning: styles.variantWarning,
};

export function EmptyState({
  icon = <ClipboardList size={40} strokeWidth={1.5} />,
  title,
  description,
  action,
  size = 'md',
  variant = 'default',
  className = '',
}: EmptyStateProps) {
  return (
    <div className={`${styles.wrapper} ${sizeClass[size]} ${variantClass[variant]} ${className}`}>
      <div className={styles.icon}>{icon}</div>
      <h3 className={styles.title}>{title}</h3>
      {description && <p className={styles.description}>{description}</p>}
      {action && <div className={styles.action}>{action}</div>}
    </div>
  );
}
