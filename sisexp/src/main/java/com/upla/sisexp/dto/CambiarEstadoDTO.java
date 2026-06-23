package com.upla.sisexp.dto;

import com.upla.sisexp.enums.EstadoExpediente;
import jakarta.validation.constraints.NotNull;

public class CambiarEstadoDTO {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoExpediente nuevoEstado;

    private String observacion;

    public EstadoExpediente getNuevoEstado() { return nuevoEstado; }
    public void setNuevoEstado(EstadoExpediente nuevoEstado) { this.nuevoEstado = nuevoEstado; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
