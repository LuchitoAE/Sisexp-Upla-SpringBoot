package com.upla.sisexp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.upla.sisexp.enums.RolUsuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rol;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false)
    private Boolean horarioRestringido = false;

    @Column(nullable = false)
    private Integer intentosFallidos = 0;

    private LocalDateTime bloqueadoHasta;

    public Usuario() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public RolUsuario getRol() { return rol; }
    public void setRol(RolUsuario rol) { this.rol = rol; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public Boolean getHorarioRestringido() { return horarioRestringido; }
    public void setHorarioRestringido(Boolean horarioRestringido) { this.horarioRestringido = horarioRestringido; }

    public Integer getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(Integer intentosFallidos) { this.intentosFallidos = intentosFallidos; }

    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
}
