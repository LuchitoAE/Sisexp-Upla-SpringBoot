package com.upla.sisexp.dto;

import com.upla.sisexp.enums.TipoDocumento;
import org.springframework.web.multipart.MultipartFile;

public class DocumentoDTO {

    private TipoDocumento tipo;
    private MultipartFile archivo;

    public TipoDocumento getTipo() { return tipo; }
    public void setTipo(TipoDocumento tipo) { this.tipo = tipo; }

    public MultipartFile getArchivo() { return archivo; }
    public void setArchivo(MultipartFile archivo) { this.archivo = archivo; }
}
