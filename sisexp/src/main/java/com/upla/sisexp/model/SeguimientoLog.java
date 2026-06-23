package com.upla.sisexp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seguimiento_logs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SeguimientoLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @Column(length = 30)
    private String estadoAnterior;

    @Column(nullable = false, length = 30)
    private String estadoNuevo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Lob
    private String observacion;

    @Lob
    private String metadata;

    private LocalDateTime createdAt;

    public SeguimientoLog() {}

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Expediente getExpediente() { return expediente; }
    public void setExpediente(Expediente expediente) { this.expediente = expediente; }

    public String getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(String estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public String getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(String estadoNuevo) { this.estadoNuevo = estadoNuevo; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
