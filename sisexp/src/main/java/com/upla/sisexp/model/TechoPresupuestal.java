package com.upla.sisexp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "techos_presupuestales")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TechoPresupuestal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "año", nullable = false, unique = true)
    private Integer año;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoUtilizado = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por")
    private Usuario creadoPor;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false)
    private Boolean planificado = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TechoPresupuestal() {}

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getAño() { return año; }
    public void setAño(Integer año) { this.año = año; }

    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }

    public BigDecimal getMontoUtilizado() { return montoUtilizado; }
    public void setMontoUtilizado(BigDecimal montoUtilizado) { this.montoUtilizado = montoUtilizado; }

    public Usuario getCreadoPor() { return creadoPor; }
    public void setCreadoPor(Usuario creadoPor) { this.creadoPor = creadoPor; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public Boolean getPlanificado() { return planificado; }
    public void setPlanificado(Boolean planificado) { this.planificado = planificado; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public BigDecimal getPorcentajeUtilizado() {
        if (montoTotal == null || montoTotal.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return montoUtilizado.multiply(new BigDecimal("100"))
                .divide(montoTotal, 1, java.math.RoundingMode.HALF_UP);
    }
}
