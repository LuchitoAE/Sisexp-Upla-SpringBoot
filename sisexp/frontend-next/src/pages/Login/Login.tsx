import { useState, type FormEvent } from 'react';
import { AlertCircle, Clock } from 'lucide-react';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import styles from './Login.module.css';

interface LoginProps {
  onLogin: (email: string, password: string) => Promise<unknown>;
}

export default function Login({ onLogin }: LoginProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Ingrese su correo y contraseña');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await onLogin(email, password);
    } catch (e: unknown) {
        setError(e instanceof Error ? e.message : 'Credenciales inválidas');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.wrapper}>
      <div className={styles.card}>
        <div className={styles.logo}>S</div>
        <h1 className={styles.title}>SISEXP UPLA</h1>
        <p className={styles.subtitle}>Sistema de Seguimiento y Control de Expedientes</p>

        {error && (
          <div className={styles.error}>
            <AlertCircle size={16} strokeWidth={1.5} />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className={styles.form}>
          <Input
            label="Correo institucional"
            type="email"
            placeholder="usuario@upla.edu.pe"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
            autoFocus
          />
          <Input
            label="Contraseña"
            type="password"
            placeholder="••••••••"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />
          <Button
            type="submit"
            variant="primary"
            size="lg"
            loading={loading}
            className={styles.submitBtn}
          >
            Ingresar
          </Button>
        </form>

        <div className={styles.horarioInfo}>
          <Clock size={14} strokeWidth={1.5} />
          <span>Horario laboral: 8:00 - 20:00 (lun–vie)</span>
        </div>

        <div className={styles.footer}>
          Universidad Peruana Los Andes — v2.0
        </div>
      </div>
    </div>
  );
}
