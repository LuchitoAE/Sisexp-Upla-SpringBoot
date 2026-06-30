package com.upla.sisexp.auth.service;

import com.upla.sisexp.auth.model.Usuario;
import com.upla.sisexp.auth.repository.UsuarioRepository;
import com.upla.sisexp.auth.security.JwtTokenProvider;
import com.upla.sisexp.common.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public Map<String, Object> login(String email, String password) {
        Usuario usuario = usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Credenciales invalidas"));

        if (!usuario.getActivo()) {
            throw new BusinessException("Cuenta desactivada");
        }

        if (usuario.getBloqueadoHasta() != null) {
            if (usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
                throw new BusinessException("Cuenta bloqueada hasta: " + usuario.getBloqueadoHasta());
            }
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
        }

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
            if (usuario.getIntentosFallidos() >= 5) {
                usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(30));
            }
            usuarioRepo.save(usuario);
            throw new BusinessException("Credenciales invalidas");
        }

        usuario.setIntentosFallidos(0);
        usuarioRepo.save(usuario);

        String token = jwtTokenProvider.generateToken(usuario.getEmail(), usuario.getRol().name());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("usuario", toMap(usuario));
        return result;
    }

    public boolean validateToken(String token) {
        return jwtTokenProvider.isTokenValid(token);
    }

    private Map<String, Object> toMap(Usuario u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("nombre", u.getNombre());
        map.put("email", u.getEmail());
        map.put("rol", u.getRol().name());
        map.put("activo", u.getActivo());
        map.put("horarioRestringido", u.getHorarioRestringido());
        return map;
    }
}
