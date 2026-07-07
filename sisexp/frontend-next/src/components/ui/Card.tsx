import type { ReactNode, HTMLAttributes } from 'react';
import styles from './Card.module.css';

type CardPadding = 'sm' | 'md' | 'lg';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  elevated?: boolean;
  interactive?: boolean;
  padding?: CardPadding;
  header?: ReactNode;
  footer?: ReactNode;
  children: ReactNode;
}

export function Card({
  elevated = false,
  interactive = false,
  padding = 'md',
  header,
  footer,
  children,
  className = '',
  ...props
}: CardProps) {
  const classNames = [
    styles.card,
    elevated && styles.elevated,
    interactive && styles.interactive,
    styles[`padding-${padding}`],
    className,
  ].filter(Boolean).join(' ');

  return (
    <div className={classNames} {...props}>
      {header && <div className={styles.header}>{header}</div>}
      <div className={styles.body}>{children}</div>
      {footer && <div className={styles.footer}>{footer}</div>}
    </div>
  );
}
