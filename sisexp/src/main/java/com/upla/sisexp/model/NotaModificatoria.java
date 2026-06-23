package com.upla.sisexp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.upla.sisexp.enums.EstadoNota;
import com.upla.sisexp.enums.Naturaleza;
import com.upla.sisexp.enums.TipoNota;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "notas_modificatorias")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NotaModificatoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 25, unique = true)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @Column(length = 200)
    private String origen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNota tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_existente_id")
    private ActividadPOI actividadExistente;

    @Column(nullable = false, length = 255)
    private String nuevoNombre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String justificacion;

    @Column(precision = 12, scale = 2)
    private BigDecimal costoEstimadoReferencial = BigDecimal.ZERO;

    @Column(length = 255)
    private String nombreArchivo;

    @Column(length = 255)
    private String nombreOriginalArchivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_origen_id")
    private ActividadPOI actividadOrigen;

    @Column(precision = 12, scale = 2)
    private BigDecimal montoTransferir;

    @Column(length = 30)
    private String nuevoClasificadorGasto;

    @Enumerated(EnumType.STRING)
    private Naturaleza nuevoTipo = Naturaleza.Bien;

    @Enumerated(EnumType.STRING)
    private EstadoNota estado = EstadoNota.pendiente;

    @Column(columnDefinition = "TEXT")
    private String observacionAdmin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por")
    private Usuario aprobadoPor;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public NotaModificatoria() {}

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public Usuario getSolicitante() { return solicitante; }
    public void setSolicitante(Usuario solicitante) { this.solicitante = solicitante; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    public TipoNota getTipo() { return tipo; }
    public void setTipo(TipoNota tipo) { this.tipo = tipo; }

    public ActividadPOI getActividadExistente() { return actividadExistente; }
    public void setActividadExistente(ActividadPOI actividadExistente) { this.actividadExistente = actividadExistente; }

    public String getNuevoNombre() { return nuevoNombre; }
    public void setNuevoNombre(String nuevoNombre) { this.nuevoNombre = nuevoNombre; }

    public String getJustificacion() { return justificacion; }
    public void setJustificacion(String justificacion) { this.justificacion = justificacion; }

    public BigDecimal getCostoEstimadoReferencial() { return costoEstimadoReferencial; }
    public void setCostoEstimadoReferencial(BigDecimal costoEstimadoReferencial) { this.costoEstimadoReferencial = costoEstimadoReferencial; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getNombreOriginalArchivo() { return nombreOriginalArchivo; }
    public void setNombreOriginalArchivo(String nombreOriginalArchivo) { this.nombreOriginalArchivo = nombreOriginalArchivo; }

    public ActividadPOI getActividadOrigen() { return actividadOrigen; }
    public void setActividadOrigen(ActividadPOI actividadOrigen) { this.actividadOrigen = actividadOrigen; }

    public BigDecimal getMontoTransferir() { return montoTransferir; }
    public void setMontoTransferir(BigDecimal montoTransferir) { this.montoTransferir = montoTransferir; }

    public String getNuevoClasificadorGasto() { return nuevoClasificadorGasto; }
    public void setNuevoClasificadorGasto(String nuevoClasificadorGasto) { this.nuevoClasificadorGasto = nuevoClasificadorGasto; }

    public Naturaleza getNuevoTipo() { return nuevoTipo; }
    public void setNuevoTipo(Naturaleza nuevoTipo) { this.nuevoTipo = nuevoTipo; }

    public EstadoNota getEstado() { return estado; }
    public void setEstado(EstadoNota estado) { this.estado = estado; }

    public String getObservacionAdmin() { return observacionAdmin; }
    public void setObservacionAdmin(String observacionAdmin) { this.observacionAdmin = observacionAdmin; }

    public Usuario getAprobadoPor() { return aprobadoPor; }
    public void setAprobadoPor(Usuario aprobadoPor) { this.aprobadoPor = aprobadoPor; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
