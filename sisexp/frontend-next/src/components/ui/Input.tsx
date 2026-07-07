import { forwardRef, type InputHTMLAttributes, type TextareaHTMLAttributes, type SelectHTMLAttributes } from 'react';
import styles from './Input.module.css';

interface InputWrapperProps {
  label?: string;
  error?: string;
  hint?: string;
  children: React.ReactNode;
}

function InputWrapper({ label, error, hint, children }: InputWrapperProps) {
  return (
    <div className={styles.wrapper}>
      {label && <label className={styles.label}>{label}</label>}
      {children}
      {error && <span className={styles.error} role="alert">{error}</span>}
      {hint && !error && <span className={styles.hint}>{hint}</span>}
    </div>
  );
}

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, hint, className = '', ...props }, ref) => (
    <InputWrapper label={label} error={error} hint={hint}>
      <input
        ref={ref}
        className={`${styles.input} ${className}`}
        aria-invalid={!!error}
        aria-label={label}
        {...props}
      />
    </InputWrapper>
  ),
);
Input.displayName = 'Input';

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ label, error, hint, className = '', ...props }, ref) => (
    <InputWrapper label={label} error={error} hint={hint}>
      <textarea
        ref={ref}
        className={`${styles.input} ${className}`}
        aria-invalid={!!error}
        aria-label={label}
        {...props}
      />
    </InputWrapper>
  ),
);
Textarea.displayName = 'Textarea';

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  hint?: string;
  options: Array<{ value: string; label: string }>;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, error, hint, options, className = '', ...props }, ref) => (
    <InputWrapper label={label} error={error} hint={hint}>
      <select
        ref={ref}
        className={`${styles.input} ${className}`}
        aria-invalid={!!error}
        aria-label={label}
        {...props}
      >
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
    </InputWrapper>
  ),
);
Select.displayName = 'Select';
