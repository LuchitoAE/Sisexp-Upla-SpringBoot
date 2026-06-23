package com.upla.sisexp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.upla.sisexp.enums.EstadoActividad;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "actividades_poi")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ActividadPOI {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "techo_presupuestal_id", nullable = false)
    private TechoPresupuestal techoPresupuestal;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal presupuestoAsignado = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoComprometido = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoEjecutado = BigDecimal.ZERO;

    private LocalDate fechaLimite;

    @Enumerated(EnumType.STRING)
    private EstadoActividad estado = EstadoActividad.Pendiente;

    @Column(nullable = false)
    private Boolean planificado = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ActividadPOI() {}

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TechoPresupuestal getTechoPresupuestal() { return techoPresupuestal; }
    public void setTechoPresupuestal(TechoPresupuestal techoPresupuestal) { this.techoPresupuestal = techoPresupuestal; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public BigDecimal getPresupuestoAsignado() { return presupuestoAsignado; }
    public void setPresupuestoAsignado(BigDecimal presupuestoAsignado) { this.presupuestoAsignado = presupuestoAsignado; }

    public BigDecimal getSaldoComprometido() { return saldoComprometido; }
    public void setSaldoComprometido(BigDecimal saldoComprometido) { this.saldoComprometido = saldoComprometido; }

    public BigDecimal getSaldoEjecutado() { return saldoEjecutado; }
    public void setSaldoEjecutado(BigDecimal saldoEjecutado) { this.saldoEjecutado = saldoEjecutado; }

    public LocalDate getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDate fechaLimite) { this.fechaLimite = fechaLimite; }

    public EstadoActividad getEstado() { return estado; }
    public void setEstado(EstadoActividad estado) { this.estado = estado; }

    public Boolean getPlanificado() { return planificado; }
    public void setPlanificado(Boolean planificado) { this.planificado = planificado; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public BigDecimal getPorcentajeEjecucion() {
        if (presupuestoAsignado == null || presupuestoAsignado.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return saldoEjecutado.multiply(new BigDecimal("100"))
                .divide(presupuestoAsignado, 1, java.math.RoundingMode.HALF_UP);
    }
}
