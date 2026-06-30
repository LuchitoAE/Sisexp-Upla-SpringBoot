package com.upla.sisexp.auth.service;

import com.upla.sisexp.auth.model.Usuario;
import com.upla.sisexp.auth.repository.UsuarioRepository;
import com.upla.sisexp.common.enums.RolUsuario;
import com.upla.sisexp.common.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listar() {
        return usuarioRepo.findAll();
    }

    public Usuario obtener(Long id) {
        return usuarioRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
    }

    @Transactional
    public Usuario crear(String nombre, String email, String password, RolUsuario rol, boolean horarioRestringido) {
        if (usuarioRepo.existsByEmail(email)) {
            throw new BusinessException("El email ya esta registrado");
        }
        if (password == null || password.length() < 6) {
            throw new BusinessException("La contrasena debe tener al menos 6 caracteres");
        }
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(password));
        u.setRol(rol);
        u.setHorarioRestringido(horarioRestringido);
        u.setActivo(true);
        return usuarioRepo.save(u);
    }

    @Transactional
    public Usuario editar(Long id, String nombre, String email, RolUsuario rol, boolean horarioRestringido) {
        Usuario u = obtener(id);
        if (email != null && !email.equals(u.getEmail()) && usuarioRepo.existsByEmail(email)) {
            throw new BusinessException("El email ya esta registrado");
        }
        if (nombre != null) u.setNombre(nombre);
        if (email != null) u.setEmail(email);
        if (rol != null) u.setRol(rol);
        u.setHorarioRestringido(horarioRestringido);
        return usuarioRepo.save(u);
    }

    @Transactional
    public void toggleActivo(Long id) {
        Usuario u = obtener(id);
        u.setActivo(!u.getActivo());
        usuarioRepo.save(u);
    }

    @Transactional
    public void cambiarPassword(Long id, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("La contrasena debe tener al menos 6 caracteres");
        }
        Usuario u = obtener(id);
        u.setPassword(passwordEncoder.encode(newPassword));
        usuarioRepo.save(u);
    }
}
