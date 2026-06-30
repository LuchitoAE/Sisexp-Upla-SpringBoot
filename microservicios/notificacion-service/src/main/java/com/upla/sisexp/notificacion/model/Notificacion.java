package com.upla.sisexp.notificacion.model;

import com.upla.sisexp.common.enums.TipoNotificacion;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
public class Notificacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long usuarioId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;
    @Enumerated(EnumType.STRING)
    private TipoNotificacion tipo;
    @Column(nullable = false)
    private Boolean leida = false;
    private Long expedienteId;
    private LocalDateTime createdAt;

    public Notificacion() {}
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public TipoNotificacion getTipo() { return tipo; }
    public void setTipo(TipoNotificacion tipo) { this.tipo = tipo; }
    public Boolean getLeida() { return leida; }
    public void setLeida(Boolean leida) { this.leida = leida; }
    public Long getExpedienteId() { return expedienteId; }
    public void setExpedienteId(Long expedienteId) { this.expedienteId = expedienteId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
