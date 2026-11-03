package com.futurevet.service;

import com.futurevet.model.Perfil;
import com.futurevet.repository.UsuarioRepository;
import com.futurevet.web.forms.RegistroForm;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    /** O BCrypt considera apenas os primeiros 72 bytes da senha. */
    private static final int LIMITE_BYTES_BCRYPT = 72;

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    /** Todo cadastro público cria um TUTOR; o perfil ADMIN é atribuído apenas pela clínica. */
    @Transactional
    public long registrar(RegistroForm f) {
        if (!f.getSenha().equals(f.getConfirmacao()))
            throw new RegraException("erro.usuario.senhas");
        if (f.getSenha().getBytes(StandardCharsets.UTF_8).length > LIMITE_BYTES_BCRYPT)
            throw new RegraException("erro.usuario.senha-bytes", LIMITE_BYTES_BCRYPT);
        String cpf = somenteDigitos(f.getCpf());
        String telefone = somenteDigitos(f.getTelefone());
        if (cpf.length() != 11)
            throw new RegraException("erro.usuario.cpf");
        if (!telefone.isEmpty() && telefone.length() != 10 && telefone.length() != 11)
            throw new RegraException("erro.usuario.telefone");
        String email = normalizarEmail(f.getEmail());
        if (repo.duplicado(email, Long.parseLong(cpf)))
            throw new RegraException("erro.usuario.duplicado");
        return repo.criar(f.getNome().strip(), email, encoder.encode(f.getSenha()), Long.parseLong(cpf),
                telefone.isEmpty() ? null : Long.valueOf(telefone), Perfil.TUTOR);
    }

    public static String normalizarEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
