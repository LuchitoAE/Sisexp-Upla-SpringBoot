package com.upla.sisexp.security;

import com.upla.sisexp.model.Usuario;
import com.upla.sisexp.repository.UsuarioRepository;
import com.upla.sisexp.service.BusinessValidationsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final BusinessValidationsService businessValidations;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository,
            BusinessValidationsService businessValidations) {
        this.usuarioRepository = usuarioRepository;
        this.businessValidations = businessValidations;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        if (!usuario.getActivo()) {
            throw new UsernameNotFoundException("Cuenta desactivada: " + email);
        }

        businessValidations.checkLoginAttempts(usuario);

        return new CustomUserDetails(usuario);
    }
}
