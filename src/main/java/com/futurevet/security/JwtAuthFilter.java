package com.futurevet.security;

import com.futurevet.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UsuarioRepository usuarios;

    public JwtAuthFilter(JwtService jwt, UsuarioRepository usuarios) {
        this.jwt = jwt;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res,
            @NonNull FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String email = jwt.email(token);
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var usuario = usuarios.email(email).orElse(null);
                    if (usuario != null && jwt.valido(token, email)) {
                        var auth = new UsernamePasswordAuthenticationToken(usuario.email(), null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.role())));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (RuntimeException tokenInvalido) {
                // Token expirado ou adulterado: a requisição segue sem autenticação e recebe 401.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }
}