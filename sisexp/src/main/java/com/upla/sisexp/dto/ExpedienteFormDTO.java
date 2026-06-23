package com.upla.sisexp.dto;

import com.upla.sisexp.enums.Naturaleza;
import com.upla.sisexp.enums.Urgencia;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpedienteFormDTO {

    @NotNull(message = "La actividad POI es obligatoria")
    private Long actividadPoiId;

    @NotNull(message = "La necesidad PAP es obligatoria")
    private Long necesidadPapId;

    @NotNull(message = "La urgencia es obligatoria")
    private Urgencia urgencia;

    private Naturaleza naturaleza;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(min = 10, max = 2000, message = "La descripcion debe tener entre 10 y 2000 caracteres")
    private String descripcion;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Max(value = 9999, message = "La cantidad no puede exceder 9999")
    private Integer cantidadSolicitada = 1;

    private LocalDate fechaLimite;

    private BigDecimal costoEstimado = BigDecimal.ZERO;

    public Long getActividadPoiId() { return actividadPoiId; }
    public void setActividadPoiId(Long actividadPoiId) { this.actividadPoiId = actividadPoiId; }

    public Long getNecesidadPapId() { return necesidadPapId; }
    public void setNecesidadPapId(Long necesidadPapId) { this.necesidadPapId = necesidadPapId; }

    public Urgencia getUrgencia() { return urgencia; }
    public void setUrgencia(Urgencia urgencia) { this.urgencia = urgencia; }

    public Naturaleza getNaturaleza() { return naturaleza; }
    public void setNaturaleza(Naturaleza naturaleza) { this.naturaleza = naturaleza; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getCantidadSolicitada() { return cantidadSolicitada; }
    public void setCantidadSolicitada(Integer cantidadSolicitada) { this.cantidadSolicitada = cantidadSolicitada; }

    public LocalDate getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDate fechaLimite) { this.fechaLimite = fechaLimite; }

    public BigDecimal getCostoEstimado() { return costoEstimado; }
    public void setCostoEstimado(BigDecimal costoEstimado) { this.costoEstimado = costoEstimado; }
}
