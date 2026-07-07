import { useState } from 'react';
import { Download, FileSpreadsheet, FileText } from 'lucide-react';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Tabs } from '../../components/ui/Tabs';
import { Select } from '../../components/ui/Input';
import { EmptyState } from '../../components/ui/EmptyState';
import { PageLayout } from '../../components/layout/PageLayout';
import { PageHeader } from '../../components/layout/PageHeader';
import styles from './ReportesPage.module.css';

const REPORTES_TABS = [
  { id: 'anual', label: 'Resumen anual' },
  { id: 'expedientes', label: 'Expedientes' },
  { id: 'poi', label: 'POI' },
  { id: 'pap', label: 'PAP' },
];

const ANIO_OPTIONS = [
  { value: '2026', label: '2026' },
  { value: '2025', label: '2025' },
  { value: '2024', label: '2024' },
];

export default function ReportesPage() {
  const [tab, setTab] = useState('anual');
  const [periodo, setPeriodo] = useState('2026');

  return (
    <PageLayout>
      <PageHeader title="Reportes" description="Reportes del sistema" />

      <Tabs tabs={REPORTES_TABS} active={tab} onChange={setTab} />

      <div className={styles.toolbar}>
        <div className={styles.selectGroup}>
          <span className={styles.selectLabel}>Período:</span>
          <Select
            value={periodo}
            onChange={(e) => setPeriodo(e.target.value)}
            options={ANIO_OPTIONS}
            aria-label="Seleccionar período"
          />
        </div>
        <div className={styles.exportGroup}>
          <Button variant="secondary" size="sm">
            <Download size={14} strokeWidth={1.5} />
            Exportar
          </Button>
          <Button variant="ghost" size="sm">
            <FileSpreadsheet size={14} strokeWidth={1.5} />
            Excel
          </Button>
          <Button variant="ghost" size="sm">
            <FileText size={14} strokeWidth={1.5} />
            PDF
          </Button>
        </div>
      </div>

      <Card>
        <div className={styles.resultsArea}>
          <EmptyState
            icon={<FileText size={32} strokeWidth={1.5} />}
            title="Seleccione un reporte"
            description="Elija una vista y período para generar el reporte."
            size="sm"
          />
        </div>
      </Card>
    </PageLayout>
  );
}
