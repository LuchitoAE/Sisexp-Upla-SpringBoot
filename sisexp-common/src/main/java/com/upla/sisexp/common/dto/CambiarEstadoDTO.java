package com.upla.sisexp.common.dto;

import com.upla.sisexp.common.enums.EstadoExpediente;

public class CambiarEstadoDTO {
    private EstadoExpediente nuevoEstado;
    private String observacion;

    public EstadoExpediente getNuevoEstado() { return nuevoEstado; }
    public void setNuevoEstado(EstadoExpediente nuevoEstado) { this.nuevoEstado = nuevoEstado; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
