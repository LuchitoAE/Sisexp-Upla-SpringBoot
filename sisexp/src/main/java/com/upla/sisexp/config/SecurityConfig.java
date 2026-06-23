package com.upla.sisexp.config;

import com.upla.sisexp.model.Usuario;
import com.upla.sisexp.repository.UsuarioRepository;
import com.upla.sisexp.security.HorarioLaboralFilter;
import com.upla.sisexp.service.BusinessValidationsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final HorarioLaboralFilter horarioLaboralFilter;
    private final UsuarioRepository usuarioRepository;
    private final BusinessValidationsService businessValidations;

    public SecurityConfig(HorarioLaboralFilter horarioLaboralFilter,
            UsuarioRepository usuarioRepository,
            BusinessValidationsService businessValidations) {
        this.horarioLaboralFilter = horarioLaboralFilter;
        this.usuarioRepository = usuarioRepository;
        this.businessValidations = businessValidations;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/rastreo/**")
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login", "/", "/index.html", "/favicon.ico",
                    "/rastreo/**", "/api/health", "/health",
                    "/api/auth/login",
                    "/static/**",
                    "/error", "/horario-cerrado"
                ).permitAll()
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/usuarios/**").hasRole("Administrador")
                .requestMatchers("/reportes/**").hasAnyRole("Administrador", "Coordinacion", "Director", "Decanato")
                .requestMatchers("/techos/**").hasAnyRole("Administrador", "Coordinacion", "Secretaria", "Director")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)
                .failureHandler(authenticationFailureHandler())
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("sisexp-upla-remember-me-key-2026")
                .tokenValiditySeconds(2592000)
                .rememberMeParameter("remember-me")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(10)
                .expiredUrl("/login?expired")
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((HttpServletRequest request,
                        HttpServletResponse response, AuthenticationException authException)
                        -> {
                    if (request.getServletPath().startsWith("/api/")) {
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write(
                                "{\"error\":\"No autorizado\"}");
                    } else {
                        response.sendRedirect("/login");
                    }
                })
            )
            .addFilterBefore(horarioLaboralFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public SimpleUrlAuthenticationFailureHandler authenticationFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler("/login?error") {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request,
                    HttpServletResponse response, AuthenticationException exception)
                    throws IOException, jakarta.servlet.ServletException {
                String email = request.getParameter("email");
                if (email != null) {
                    usuarioRepository.findByEmail(email).ifPresent(u -> {
                        businessValidations.registerFailedAttempt(u);
                    });
                }
                super.onAuthenticationFailure(request, response, exception);
            }
        };
    }
}
