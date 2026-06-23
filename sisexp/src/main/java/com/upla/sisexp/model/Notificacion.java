package com.upla.sisexp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.upla.sisexp.enums.TipoNotificacion;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacion tipo = TipoNotificacion.info;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id")
    private Expediente expediente;

    @Column(nullable = false)
    private Boolean leida = false;

    private LocalDateTime createdAt;

    public Notificacion() {}

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public TipoNotificacion getTipo() { return tipo; }
    public void setTipo(TipoNotificacion tipo) { this.tipo = tipo; }

    public Expediente getExpediente() { return expediente; }
    public void setExpediente(Expediente expediente) { this.expediente = expediente; }

    public Boolean getLeida() { return leida; }
    public void setLeida(Boolean leida) { this.leida = leida; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
