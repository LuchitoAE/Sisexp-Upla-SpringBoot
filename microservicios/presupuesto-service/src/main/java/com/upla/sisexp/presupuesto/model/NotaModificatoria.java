package com.upla.sisexp.presupuesto.model;

import com.upla.sisexp.common.enums.EstadoNota;
import com.upla.sisexp.common.enums.Naturaleza;
import com.upla.sisexp.common.enums.TipoNota;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "notas_modificatorias")
public class NotaModificatoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 30)
    private String codigo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNota tipo;
    @Column(nullable = false, length = 255)
    private String nuevoNombre;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String justificacion;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costoEstimadoReferencial = BigDecimal.ZERO;
    @Column(length = 255)
    private String origen;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoNota estado = EstadoNota.pendiente;
    @Column(precision = 12, scale = 2)
    private BigDecimal montoTransferir;
    private Long actividadExistenteId;
    private Long actividadOrigenId;
    @Column(length = 50)
    private String nuevoClasificadorGasto;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Naturaleza nuevoTipo;
    @Column(length = 500)
    private String observacionAdmin;
    @Column(length = 255)
    private String nombreArchivo;
    @Column(columnDefinition = "BYTEA")
    private byte[] archivoAdjunto;
    private Long solicitanteId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public NotaModificatoria() {}
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public TipoNota getTipo() { return tipo; }
    public void setTipo(TipoNota tipo) { this.tipo = tipo; }
    public String getNuevoNombre() { return nuevoNombre; }
    public void setNuevoNombre(String nuevoNombre) { this.nuevoNombre = nuevoNombre; }
    public String getJustificacion() { return justificacion; }
    public void setJustificacion(String justificacion) { this.justificacion = justificacion; }
    public BigDecimal getCostoEstimadoReferencial() { return costoEstimadoReferencial; }
    public void setCostoEstimadoReferencial(BigDecimal costoEstimadoReferencial) { this.costoEstimadoReferencial = costoEstimadoReferencial; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public EstadoNota getEstado() { return estado; }
    public void setEstado(EstadoNota estado) { this.estado = estado; }
    public BigDecimal getMontoTransferir() { return montoTransferir; }
    public void setMontoTransferir(BigDecimal montoTransferir) { this.montoTransferir = montoTransferir; }
    public Long getActividadExistenteId() { return actividadExistenteId; }
    public void setActividadExistenteId(Long actividadExistenteId) { this.actividadExistenteId = actividadExistenteId; }
    public Long getActividadOrigenId() { return actividadOrigenId; }
    public void setActividadOrigenId(Long actividadOrigenId) { this.actividadOrigenId = actividadOrigenId; }
    public String getNuevoClasificadorGasto() { return nuevoClasificadorGasto; }
    public void setNuevoClasificadorGasto(String nuevoClasificadorGasto) { this.nuevoClasificadorGasto = nuevoClasificadorGasto; }
    public Naturaleza getNuevoTipo() { return nuevoTipo; }
    public void setNuevoTipo(Naturaleza nuevoTipo) { this.nuevoTipo = nuevoTipo; }
    public String getObservacionAdmin() { return observacionAdmin; }
    public void setObservacionAdmin(String observacionAdmin) { this.observacionAdmin = observacionAdmin; }
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public byte[] getArchivoAdjunto() { return archivoAdjunto; }
    public void setArchivoAdjunto(byte[] archivoAdjunto) { this.archivoAdjunto = archivoAdjunto; }
    public Long getSolicitanteId() { return solicitanteId; }
    public void setSolicitanteId(Long solicitanteId) { this.solicitanteId = solicitanteId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
