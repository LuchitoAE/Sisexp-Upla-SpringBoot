package com.upla.sisexp.auth.security;

import com.upla.sisexp.auth.model.Usuario;
import com.upla.sisexp.auth.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UsuarioRepository usuarioRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token == null || !jwtTokenProvider.isTokenValid(token)) {
            chain.doFilter(request, response);
            return;
        }
        Claims claims = jwtTokenProvider.validateToken(token);
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(claims.getSubject());
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            String rol = "ROLE_" + usuario.getRol().name();
            var auth = new UsernamePasswordAuthenticationToken(
                    usuario, null, List.of(new SimpleGrantedAuthority(rol)));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
