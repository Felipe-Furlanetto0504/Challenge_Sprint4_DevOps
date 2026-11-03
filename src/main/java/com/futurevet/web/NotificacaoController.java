package com.futurevet.web;

import com.futurevet.service.NotificacaoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notificacoes")
public class NotificacaoController {
    private final NotificacaoService service;
    private final Feedback feedback;

    public NotificacaoController(NotificacaoService service, Feedback feedback) {
        this.service = service;
        this.feedback = feedback;
    }

    @GetMapping
    public String listar(Model m) {
        m.addAttribute("notificacoes", service.listar());
        return "notificacoes";
    }

    @PostMapping("/lidas")
    public String marcarLidas(RedirectAttributes r) {
        return feedback.executar(service::marcarTodasLidas, "sucesso.notificacoes.lidas", "/notificacoes", r);
    }
}
