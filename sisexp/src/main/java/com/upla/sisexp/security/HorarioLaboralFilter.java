package com.upla.sisexp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

@Component
public class HorarioLaboralFilter extends OncePerRequestFilter {

    private static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");
    private static final int HORA_INICIO = 8;
    private static final int HORA_FIN = 20;

    private static final Set<String> RUTAS_EXENTAS = Set.of(
            "/login", "/rastreo", "/api", "/api/health",
            "/error", "/static", "/favicon.ico", "/index.html"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        for (String exenta : RUTAS_EXENTAS) {
            if (path.startsWith(exenta)) {
                chain.doFilter(request, response);
                return;
            }
        }

        ZonedDateTime ahora = ZonedDateTime.now(ZONA_PERU);
        int hora = ahora.getHour();

        if (hora >= HORA_INICIO && hora < HORA_FIN) {
            chain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails user) {
            if (!user.isHorarioRestringido()) {
                chain.doFilter(request, response);
                return;
            }
        }

        if (auth != null) {
            response.sendRedirect("/horario-cerrado");
        } else {
            response.sendRedirect("/login?horario");
        }
    }
}
