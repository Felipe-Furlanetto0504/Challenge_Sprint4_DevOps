package com.futurevet.web;

import com.futurevet.model.Consulta;
import com.futurevet.repository.UsuarioRepository;
import com.futurevet.security.Acesso;
import com.futurevet.service.AgendaService;
import com.futurevet.service.AnimalService;
import com.futurevet.service.CarteiraService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PainelController {
    private static final int CONSULTAS_NO_PAINEL = 5;
    private static final int ANIMAIS_NO_PAINEL = 4;
    private static final String SEM_HORARIO = "99:99";

    private final AnimalService animais;
    private final AgendaService agenda;
    private final CarteiraService carteira;
    private final UsuarioRepository users;
    private final Acesso acesso;
    private final Clock clock;

    public PainelController(AnimalService animais, AgendaService agenda, CarteiraService carteira,
            UsuarioRepository users, Acesso acesso, Clock clock) {
        this.animais = animais;
        this.agenda = agenda;
        this.carteira = carteira;
        this.users = users;
        this.acesso = acesso;
        this.clock = clock;
    }

    @GetMapping("/")
    public String painel(Model m) {
        LocalDate hoje = LocalDate.now(clock);
        var pets = animais.listar();
        var consultas = agenda.listar().stream()
                .filter(c -> c.ativa() && !c.data().isBefore(hoje))
                .sorted(Comparator.comparing(Consulta::data)
                        .thenComparing(c -> c.hora() == null ? SEM_HORARIO : c.hora()))
                .toList();
        m.addAttribute("totalAnimais", pets.size());
        m.addAttribute("pendencias", carteira.listar().stream().filter(i -> i.situacao().precisaAtencao()).count());
        m.addAttribute("totalConsultas", consultas.size());
        m.addAttribute("aguardandoConfirmacao", consultas.stream().filter(Consulta::aguardandoConfirmacao).count());
        m.addAttribute("consultas", consultas.stream().limit(CONSULTAS_NO_PAINEL).toList());
        m.addAttribute("animais", pets.stream().limit(ANIMAIS_NO_PAINEL).toList());
        return "painel";
    }

    @GetMapping("/admin/usuarios")
    public String usuarios(Model m) {
        acesso.admin();
        m.addAttribute("usuarios", users.todos());
        return "usuarios";
    }
}
