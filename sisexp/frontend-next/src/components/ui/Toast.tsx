import {
  createContext, useContext, useState, useCallback, useRef, useEffect,
  type ReactNode,
} from 'react';
import { CheckCircle, AlertTriangle, XCircle, Info, X } from 'lucide-react';
import styles from './Toast.module.css';

type ToastType = 'success' | 'error' | 'warning' | 'info';

interface Toast {
  id: number;
  type: ToastType;
  message: string;
  exiting?: boolean;
}

interface ToastContextValue {
  toast: {
    success: (msg: string) => void;
    error: (msg: string) => void;
    warning: (msg: string) => void;
    info: (msg: string) => void;
  };
}

const ToastContext = createContext<ToastContextValue | null>(null);

let nextId = 0;

const ICONS: Record<ToastType, ReactNode> = {
  success: <CheckCircle size={18} strokeWidth={2} />,
  error: <XCircle size={18} strokeWidth={2} />,
  warning: <AlertTriangle size={18} strokeWidth={2} />,
  info: <Info size={18} strokeWidth={2} />,
};

function ToastItem({ t, onDismiss }: { t: Toast; onDismiss: (id: number) => void }) {
  const [exiting, setExiting] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout>>(undefined);

  useEffect(() => {
    timerRef.current = setTimeout(() => {
      setExiting(true);
      setTimeout(() => onDismiss(t.id), 150);
    }, 4000);
    return () => clearTimeout(timerRef.current);
  }, [t.id, onDismiss]);

  const handleDismiss = () => {
    clearTimeout(timerRef.current);
    setExiting(true);
    setTimeout(() => onDismiss(t.id), 150);
  };

  return (
    <div
      className={`${styles.toast} ${styles[t.type]} ${exiting ? styles.toastExiting : ''}`}
      role="alert"
      aria-live="polite"
    >
      <span className={styles.icon}>{ICONS[t.type]}</span>
      <div className={styles.content}>
        <div className={styles.message}>{t.message}</div>
      </div>
      <button className={styles.dismiss} onClick={handleDismiss} aria-label="Cerrar">
        <X size={14} strokeWidth={2} />
      </button>
      <div className={styles.progress} style={{ animationDuration: '4s' }} />
    </div>
  );
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const addToast = useCallback((type: ToastType, message: string) => {
    const id = ++nextId;
    setToasts((prev) => [...prev, { id, type, message }]);
  }, []);

  const dismiss = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const value: ToastContextValue = {
    toast: {
      success: (msg: string) => addToast('success', msg),
      error: (msg: string) => addToast('error', msg),
      warning: (msg: string) => addToast('warning', msg),
      info: (msg: string) => addToast('info', msg),
    },
  };

  return (
    <ToastContext.Provider value={value}>
      {children}
      {toasts.length > 0 && (
        <div className={styles.container}>
          {toasts.map((t) => (
            <ToastItem key={t.id} t={t} onDismiss={dismiss} />
          ))}
        </div>
      )}
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}
