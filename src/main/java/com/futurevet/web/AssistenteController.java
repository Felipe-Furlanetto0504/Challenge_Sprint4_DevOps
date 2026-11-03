package com.futurevet.web;

import com.futurevet.ia.AssistenteService;
import com.futurevet.ia.ConversaAssistente;
import com.futurevet.internacionalizacao.Mensagens;
import com.futurevet.service.RegraException;
import com.futurevet.web.forms.PerguntaForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/assistente")
public class AssistenteController {
    private static final String TELA = "assistente";
    private static final String REDIRECIONAR = "redirect:/assistente";

    private final AssistenteService service;
    private final ConversaAssistente conversa;
    private final Mensagens mensagens;

    public AssistenteController(AssistenteService service, ConversaAssistente conversa, Mensagens mensagens) {
        this.service = service;
        this.conversa = conversa;
        this.mensagens = mensagens;
    }

    @GetMapping
    public String tela(Model m) {
        if (!m.containsAttribute("form"))
            m.addAttribute("form", new PerguntaForm());
        m.addAttribute("trocas", conversa.trocas());
        return TELA;
    }

    @PostMapping
    public String perguntar(@Valid @ModelAttribute("form") PerguntaForm form, BindingResult b, Model m,
            RedirectAttributes r) {
        if (b.hasErrors()) {
            m.addAttribute("trocas", conversa.trocas());
            return TELA;
        }
        try {
            conversa.registrar(form.getPergunta(), service.perguntar(form.getPergunta(), conversa.trocas()));
        } catch (RegraException e) {
            r.addFlashAttribute("erro", mensagens.texto(e));
            r.addFlashAttribute("form", form);
        }
        return REDIRECIONAR;
    }

    @PostMapping("/limpar")
    public String limpar() {
        conversa.limpar();
        return REDIRECIONAR;
    }
}
