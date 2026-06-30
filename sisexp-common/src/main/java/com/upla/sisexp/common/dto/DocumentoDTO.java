package com.upla.sisexp.common.dto;

import com.upla.sisexp.common.enums.TipoDocumento;

public class DocumentoDTO {
    private TipoDocumento tipo;
    private byte[] contenido;
    private String nombreOriginal;

    public TipoDocumento getTipo() { return tipo; }
    public void setTipo(TipoDocumento tipo) { this.tipo = tipo; }
    public byte[] getContenido() { return contenido; }
    public void setContenido(byte[] contenido) { this.contenido = contenido; }
    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }
}
