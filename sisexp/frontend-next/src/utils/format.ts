export function getInitials(name: string): string {
  return name.split(' ').map((s) => s[0]).slice(0, 2).join('') || '?';
}

const currencyFormatter = new Intl.NumberFormat('es-PE', {
  style: 'currency',
  currency: 'PEN',
  minimumFractionDigits: 2,
});

export function formatMoney(n: number | string | null | undefined): string {
  const value = typeof n === 'string' ? parseFloat(n) : (n ?? 0);
  return currencyFormatter.format(value);
}

export function timeAgo(date: string | Date): string {
  const mins = Math.floor((Date.now() - new Date(date).getTime()) / 60000);
  if (mins < 1) return 'ahora';
  if (mins < 60) return `${mins}m`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h`;
  return `${Math.floor(hrs / 24)}d`;
}
