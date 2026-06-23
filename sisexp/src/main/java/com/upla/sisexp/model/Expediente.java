package com.upla.sisexp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.upla.sisexp.enums.EstadoExpediente;
import com.upla.sisexp.enums.Naturaleza;
import com.upla.sisexp.enums.Urgencia;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expedientes")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Expediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_poi_id", nullable = false)
    private ActividadPOI actividadPOI;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "necesidad_pap_id", nullable = false)
    private NecesidadPAP necesidadPAP;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Urgencia urgencia;

    @Enumerated(EnumType.STRING)
    private Naturaleza naturaleza;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    private EstadoExpediente estado = EstadoExpediente.Borrador;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    private LocalDate fechaLimite;

    @Column(nullable = false)
    private Integer cantidadSolicitada = 1;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costoEstimado = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por")
    private Usuario aprobadoPor;

    @OneToMany(mappedBy = "expediente", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"expediente"})
    private List<DocumentoAdjunto> documentos = new ArrayList<>();

    @OneToMany(mappedBy = "expediente", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"expediente"})
    private List<SeguimientoLog> logs = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Expediente() {}

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public ActividadPOI getActividadPOI() { return actividadPOI; }
    public void setActividadPOI(ActividadPOI actividadPOI) { this.actividadPOI = actividadPOI; }

    public NecesidadPAP getNecesidadPAP() { return necesidadPAP; }
    public void setNecesidadPAP(NecesidadPAP necesidadPAP) { this.necesidadPAP = necesidadPAP; }

    public Usuario getSolicitante() { return solicitante; }
    public void setSolicitante(Usuario solicitante) { this.solicitante = solicitante; }

    public Urgencia getUrgencia() { return urgencia; }
    public void setUrgencia(Urgencia urgencia) { this.urgencia = urgencia; }

    public Naturaleza getNaturaleza() { return naturaleza; }
    public void setNaturaleza(Naturaleza naturaleza) { this.naturaleza = naturaleza; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public EstadoExpediente getEstado() { return estado; }
    public void setEstado(EstadoExpediente estado) { this.estado = estado; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public LocalDate getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDate fechaLimite) { this.fechaLimite = fechaLimite; }

    public Integer getCantidadSolicitada() { return cantidadSolicitada; }
    public void setCantidadSolicitada(Integer cantidadSolicitada) { this.cantidadSolicitada = cantidadSolicitada; }

    public BigDecimal getCostoEstimado() { return costoEstimado; }
    public void setCostoEstimado(BigDecimal costoEstimado) { this.costoEstimado = costoEstimado; }

    public Usuario getAprobadoPor() { return aprobadoPor; }
    public void setAprobadoPor(Usuario aprobadoPor) { this.aprobadoPor = aprobadoPor; }

    public List<DocumentoAdjunto> getDocumentos() { return documentos; }
    public void setDocumentos(List<DocumentoAdjunto> documentos) { this.documentos = documentos; }

    public List<SeguimientoLog> getLogs() { return logs; }
    public void setLogs(List<SeguimientoLog> logs) { this.logs = logs; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
