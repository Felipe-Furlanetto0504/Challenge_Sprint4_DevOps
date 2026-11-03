package com.futurevet.web;

import com.futurevet.service.AgendaService;
import com.futurevet.service.AnimalService;
import com.futurevet.service.RegraException;
import com.futurevet.web.forms.AgendaForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/consultas")
public class AgendaController {
    private static final String FORM = "consulta-form";
    private static final String LISTA = "/consultas";

    private final AgendaService service;
    private final AnimalService animais;
    private final Feedback feedback;

    public AgendaController(AgendaService service, AnimalService animais, Feedback feedback) {
        this.service = service;
        this.animais = animais;
        this.feedback = feedback;
    }

    @GetMapping
    public String listar(Model m) {
        m.addAttribute("consultas", service.listar());
        return "consultas";
    }

    @GetMapping("/nova")
    public String nova(Model m) {
        return formulario(new AgendaForm(), null, m);
    }

    @GetMapping("/{id}/reagendar")
    public String editar(@PathVariable long id, Model m) {
        return formulario(AgendaForm.de(service.buscar(id)), id, m);
    }

    @PostMapping("/salvar")
    public String salvar(@RequestParam(required = false) Long id, @Valid @ModelAttribute("form") AgendaForm f,
            BindingResult b, Model m, RedirectAttributes r) {
        if (id != null)
            service.buscar(id);
        formulario(f, id, m);
        if (b.hasErrors())
            return FORM;
        try {
            if (id == null)
                service.agendar(f);
            else
                service.reagendar(id, f);
        } catch (RegraException e) {
            b.reject(e.getCodigo(), e.getArgumentos(), e.getCodigo());
            return FORM;
        }
        feedback.sucesso(r, id == null ? "sucesso.consulta.solicitada" : "sucesso.consulta.reagendada");
        return "redirect:" + LISTA;
    }

    @PostMapping("/{id}/confirmar")
    public String confirmar(@PathVariable long id, RedirectAttributes r) {
        return feedback.executar(() -> service.confirmar(id), "sucesso.consulta.confirmada", LISTA, r);
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable long id, @RequestParam String motivo, RedirectAttributes r) {
        return feedback.executar(() -> service.cancelar(id, motivo), "sucesso.consulta.cancelada", LISTA, r);
    }

    @PostMapping("/{id}/concluir")
    public String concluir(@PathVariable long id, @RequestParam String observacao, RedirectAttributes r) {
        return feedback.executar(() -> service.concluir(id, observacao), "sucesso.consulta.concluida", LISTA, r);
    }

    private String formulario(AgendaForm form, Long id, Model m) {
        m.addAttribute("form", form);
        m.addAttribute("id", id);
        m.addAttribute("animais", animais.listar());
        if (id != null)
            m.addAttribute("animalAtual", service.buscar(id).animal());
        return FORM;
    }
}
