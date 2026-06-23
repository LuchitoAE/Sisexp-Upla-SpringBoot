package com.upla.sisexp.security;

import com.upla.sisexp.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String nombre;
    private final String email;
    private final String password;
    private final String rol;
    private final boolean horarioRestringido;
    private final boolean activo;
    private final boolean bloqueado;

    public CustomUserDetails(Usuario usuario) {
        this.id = usuario.getId();
        this.nombre = usuario.getNombre();
        this.email = usuario.getEmail();
        this.password = usuario.getPassword();
        this.rol = usuario.getRol().name();
        this.horarioRestringido = usuario.getHorarioRestringido();
        this.activo = usuario.getActivo();
        this.bloqueado = usuario.getBloqueadoHasta() != null
                && usuario.getBloqueadoHasta().isAfter(java.time.LocalDateTime.now());
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
    public boolean isHorarioRestringido() { return horarioRestringido; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return !bloqueado; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return activo; }
}
