package com.futurevet.security;

import com.futurevet.model.Usuario;
import com.futurevet.repository.UsuarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class Acesso {
    private final UsuarioRepository usuarios;

    public Acesso(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Usuario atual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            throw new AccessDeniedException("Entre na sua conta.");
        return usuarios.email(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Conta não encontrada."));
    }

    public void dono(long dono) {
        Usuario u = atual();
        if (!u.admin() && u.id() != dono)
            throw new AccessDeniedException("Você não pode acessar este registro.");
    }

    public void admin() {
        if (!atual().admin())
            throw new AccessDeniedException("Apenas a clínica pode realizar esta operação.");
    }
}
