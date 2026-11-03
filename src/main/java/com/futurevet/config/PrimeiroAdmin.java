package com.futurevet.config;

import com.futurevet.model.Perfil;
import com.futurevet.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Em produção não há dados de demonstração: a primeira conta da clínica é promovida a ADMIN pelo
 * e-mail informado na variável FV_PRIMEIRO_ADMIN_EMAIL (só enquanto não existir nenhum ADMIN).
 */
@Component
@Profile("oracle")
public class PrimeiroAdmin implements CommandLineRunner {
    private final UsuarioRepository repo;
    private final Environment env;

    public PrimeiroAdmin(UsuarioRepository repo, Environment env) {
        this.repo = repo;
        this.env = env;
    }

    @Override
    public void run(String... args) {
        String email = env.getProperty("FV_PRIMEIRO_ADMIN_EMAIL");
        if (email == null || email.isBlank() || repo.admins() > 0)
            return;
        var u = repo.email(email).orElseThrow(() -> new IllegalStateException(
                "O e-mail indicado para primeiro administrador ainda não está cadastrado."));
        repo.promover(u.id(), Perfil.ADMIN);
    }
}
