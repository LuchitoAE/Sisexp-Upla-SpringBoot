package com.upla.sisexp.common.dto;

import com.upla.sisexp.common.enums.Urgencia;
import com.upla.sisexp.common.enums.Naturaleza;

public class ExpedienteFormDTO {
    private Long actividadPoiId;
    private Long necesidadPapId;
    private Urgencia urgencia;
    private Naturaleza naturaleza;
    private String descripcion;
    private Integer cantidadSolicitada = 1;

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
}
