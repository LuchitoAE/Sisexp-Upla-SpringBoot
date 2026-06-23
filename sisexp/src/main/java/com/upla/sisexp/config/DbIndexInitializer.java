package com.upla.sisexp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Order(1)
public class DbIndexInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DbIndexInitializer.class);
    private final JdbcTemplate jdbc;

    public DbIndexInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        log.info("Verificando indices...");

        ejecutar("CREATE INDEX IF NOT EXISTS idx_expedientes_estado ON expedientes(estado)");
        ejecutar("CREATE INDEX IF NOT EXISTS idx_expedientes_codigo ON expedientes(codigo)");
        ejecutar("CREATE INDEX IF NOT EXISTS idx_expedientes_solicitante ON expedientes(solicitante_id)");
        ejecutar("CREATE INDEX IF NOT EXISTS idx_expedientes_actividad ON expedientes(actividad_poi_id)");
        ejecutar("CREATE INDEX IF NOT EXISTS idx_expedientes_necesidad ON expedientes(necesidad_pap_id)");
        ejecutar("CREATE INDEX IF NOT EXISTS idx_expedientes_fechalimite ON expedientes(fecha_limite)");
        ejecutar("CREATE INDEX IF NOT EXISTS idx_actividades_techo ON actividades_poi(techo_presupuestal_id)");
        ejecutar("CREATE INDEX IF NOT EXISTS idx_necesidades_actividad ON necesidades_pap(actividad_poi_id)");
        ejecutar("CREATE INDEX IF NOT EXISTS idx_seguimiento_expediente ON seguimiento_logs(expediente_id)");
        ejecutar("CREATE INDEX IF NOT EXISTS idx_documentos_expediente ON documentos_adjuntos(expediente_id)");
        ejecutar("CREATE INDEX IF NOT EXISTS idx_notificaciones_usuario ON notificaciones(usuario_id, leida)");
        ejecutar("CREATE INDEX IF NOT EXISTS idx_notas_solicitante ON notas_modificatorias(solicitante_id)");
        ejecutar("CREATE INDEX IF NOT EXISTS idx_notas_estado ON notas_modificatorias(estado)");

        log.info("Indices verificados.");
    }

    private void ejecutar(String sql) {
        try {
            jdbc.execute(sql);
        } catch (Exception e) {
            log.warn("Indice: {}", e.getMessage());
        }
    }
}
