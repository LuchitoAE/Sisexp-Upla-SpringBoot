package com.upla.sisexp.service;

import com.upla.sisexp.enums.RolUsuario;
import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.model.Usuario;
import com.upla.sisexp.repository.UsuarioRepository;
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

    @Transactional
    public Usuario crear(String nombre, String email, String password, RolUsuario rol,
            boolean horarioRestringido) {
        if (usuarioRepo.findByEmail(email).isPresent()) {
            throw new BusinessException("El email " + email + " ya esta registrado");
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
    public Usuario editar(Long id, String nombre, String email, RolUsuario rol,
            boolean horarioRestringido) {
        Usuario u = usuarioRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var existente = usuarioRepo.findByEmail(email);
        if (existente.isPresent() && !existente.get().getId().equals(id)) {
            throw new BusinessException("El email " + email + " ya esta en uso");
        }
        u.setNombre(nombre);
        u.setEmail(email);
        u.setRol(rol);
        u.setHorarioRestringido(horarioRestringido);
        return usuarioRepo.save(u);
    }

    @Transactional
    public void cambiarPassword(Long id, String nuevaPassword) {
        Usuario u = usuarioRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        u.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepo.save(u);
    }

    @Transactional
    public void toggleActivo(Long id) {
        Usuario u = usuarioRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        u.setActivo(!u.getActivo());
        usuarioRepo.save(u);
    }
}
