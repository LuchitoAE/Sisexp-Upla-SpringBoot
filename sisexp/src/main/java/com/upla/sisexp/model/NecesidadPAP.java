package com.upla.sisexp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.upla.sisexp.enums.Naturaleza;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "necesidades_pap")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NecesidadPAP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_poi_id", nullable = false)
    private ActividadPOI actividadPOI;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(nullable = false)
    private Integer cantidad = 1;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioEstimado = BigDecimal.ZERO;

    @Column(length = 50)
    private String unidad;

    @Column(length = 150)
    private String oficinaLaboratorio;

    @Enumerated(EnumType.STRING)
    private Naturaleza tipo = Naturaleza.Bien;

    @Column(length = 30)
    private String clasificadorGasto;

    @Column(nullable = false)
    private Integer cantidadDisponible = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoDisponible = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer cantidadEjecutada = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoEjecutado = BigDecimal.ZERO;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public NecesidadPAP() {}

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ActividadPOI getActividadPOI() { return actividadPOI; }
    public void setActividadPOI(ActividadPOI actividadPOI) { this.actividadPOI = actividadPOI; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioEstimado() { return precioEstimado; }
    public void setPrecioEstimado(BigDecimal precioEstimado) { this.precioEstimado = precioEstimado; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public String getOficinaLaboratorio() { return oficinaLaboratorio; }
    public void setOficinaLaboratorio(String oficinaLaboratorio) { this.oficinaLaboratorio = oficinaLaboratorio; }

    public Naturaleza getTipo() { return tipo; }
    public void setTipo(Naturaleza tipo) { this.tipo = tipo; }

    public String getClasificadorGasto() { return clasificadorGasto; }
    public void setClasificadorGasto(String clasificadorGasto) { this.clasificadorGasto = clasificadorGasto; }

    public Integer getCantidadDisponible() { return cantidadDisponible; }
    public void setCantidadDisponible(Integer cantidadDisponible) { this.cantidadDisponible = cantidadDisponible; }

    public BigDecimal getMontoDisponible() { return montoDisponible; }
    public void setMontoDisponible(BigDecimal montoDisponible) { this.montoDisponible = montoDisponible; }

    public Integer getCantidadEjecutada() { return cantidadEjecutada; }
    public void setCantidadEjecutada(Integer cantidadEjecutada) { this.cantidadEjecutada = cantidadEjecutada; }

    public BigDecimal getMontoEjecutado() { return montoEjecutado; }
    public void setMontoEjecutado(BigDecimal montoEjecutado) { this.montoEjecutado = montoEjecutado; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
