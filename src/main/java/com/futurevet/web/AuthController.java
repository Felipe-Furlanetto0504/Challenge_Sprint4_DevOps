package com.futurevet.web;

import com.futurevet.internacionalizacao.Mensagens;
import com.futurevet.service.RegraException;
import com.futurevet.service.UsuarioService;
import com.futurevet.web.forms.RegistroForm;
import jakarta.validation.Valid;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class AuthController {
    private static final String TELA_REGISTRO = "registro";

    private final UsuarioService users;
    private final Environment env;
    private final Mensagens mensagens;

    public AuthController(UsuarioService users, Environment env, Mensagens mensagens) {
        this.users = users;
        this.env = env;
        this.mensagens = mensagens;
    }

    @GetMapping("/login")
    public String login(Model m) {
        m.addAttribute("demo", env.acceptsProfiles(Profiles.of("demo")));
        return "login";
    }

    @GetMapping("/registro")
    public String registro(Model m) {
        m.addAttribute("form", new RegistroForm());
        return TELA_REGISTRO;
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("form") RegistroForm f, BindingResult b) {
        if (b.hasErrors())
            return TELA_REGISTRO;
        try {
            users.registrar(f);
        } catch (RegraException e) {
            b.reject(e.getCodigo(), e.getArgumentos(), e.getCodigo());
            return TELA_REGISTRO;
        }
        return "redirect:/login?cadastro";
    }

    @GetMapping("/acesso-negado")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String negado(Model m) {
        m.addAttribute("mensagem", mensagens.texto("erro.acesso.clinica"));
        return "erro";
    }
}
