package com.upla.sisexp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.upla.sisexp.enums.TipoDocumento;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_adjuntos")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DocumentoAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDocumento tipo;

    @Column(nullable = false, length = 255)
    private String nombreOriginal;

    @Column(nullable = false, length = 255)
    private String nombreArchivo;

    @Column(length = 100)
    private String mimeType;

    private Integer tamaño;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DocumentoAdjunto() {}

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Expediente getExpediente() { return expediente; }
    public void setExpediente(Expediente expediente) { this.expediente = expediente; }

    public TipoDocumento getTipo() { return tipo; }
    public void setTipo(TipoDocumento tipo) { this.tipo = tipo; }

    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Integer getTamaño() { return tamaño; }
    public void setTamaño(Integer tamaño) { this.tamaño = tamaño; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
