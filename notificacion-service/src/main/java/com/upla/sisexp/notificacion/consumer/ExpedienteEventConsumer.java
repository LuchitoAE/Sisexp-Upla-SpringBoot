package com.upla.sisexp.notificacion.consumer;

import com.upla.sisexp.common.enums.TipoNotificacion;
import com.upla.sisexp.notificacion.config.RabbitMQConfig;
import com.upla.sisexp.notificacion.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ExpedienteEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(ExpedienteEventConsumer.class);
    private final NotificacionService notificacionService;

    public ExpedienteEventConsumer(NotificacionService notificacionService) { this.notificacionService = notificacionService; }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EXPEDIENTE_EVENTOS)
    public void onExpedienteEvent(Map<String, Object> evento) {
        String tipo = (String) evento.get("tipo");
        String codigo = (String) evento.get("codigo");
        Long expedienteId = evento.get("expedienteId") != null ? Long.valueOf(evento.get("expedienteId").toString()) : null;
        Long solicitanteId = evento.get("solicitanteId") != null ? Long.valueOf(evento.get("solicitanteId").toString()) : null;
        Long usuarioId = evento.get("usuarioId") != null ? Long.valueOf(evento.get("usuarioId").toString()) : null;

        log.info("Evento recibido: tipo={}, codigo={}, expedienteId={}", tipo, codigo, expedienteId);

        switch (tipo) {
            case "expediente.creado":
                if (solicitanteId != null)
                    notificacionService.crear(solicitanteId, "Expediente " + codigo + " creado exitosamente", TipoNotificacion.info, expedienteId);
                break;
            case "expediente.cambiado":
                String estadoNuevo = (String) evento.get("estadoNuevo");
                if (solicitanteId != null)
                    notificacionService.crear(solicitanteId, "Su expediente " + codigo + " cambio a " + estadoNuevo,
                        estadoNuevo.contains("Aprobado") ? TipoNotificacion.aprobacion : TipoNotificacion.info, expedienteId);
                break;
        }
    }
}
