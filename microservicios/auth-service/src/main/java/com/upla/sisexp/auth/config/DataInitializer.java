package com.upla.sisexp.auth.config;

import com.upla.sisexp.auth.model.Usuario;
import com.upla.sisexp.auth.repository.UsuarioRepository;
import com.upla.sisexp.common.enums.RolUsuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepo.count() > 0) {
            log.info("Auth DB ya tiene datos, omitiendo seed");
            return;
        }

        log.info("Sembrando usuarios iniciales en auth-service...");
        usuarioRepo.saveAll(java.util.List.of(
            crear("Jefe Administrativo", "jefe@upla.edu.pe", "jefe123", RolUsuario.Administrador, false),
            crear("Coordinador Admin", "coord@upla.edu.pe", "coord123", RolUsuario.Coordinacion, true),
            crear("Secretaria General", "secretaria@upla.edu.pe", "secretaria123", RolUsuario.Secretaria, true),
            crear("Director de Escuela", "director@upla.edu.pe", "director123", RolUsuario.Director, true),
            crear("Resp. Laboratorio", "lab@upla.edu.pe", "lab123", RolUsuario.Laboratorio, true),
            crear("Decano", "decanato@upla.edu.pe", "decanato123", RolUsuario.Decanato, true)
        ));
        log.info("6 usuarios creados en auth-service");
    }

    private Usuario crear(String nombre, String email, String password, RolUsuario rol, boolean horario) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(password));
        u.setRol(rol);
        u.setActivo(true);
        u.setHorarioRestringido(horario);
        return u;
    }
}
