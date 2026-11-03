package com.futurevet.web;

import com.futurevet.service.AnimalService;
import com.futurevet.web.forms.AnimalForm;
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
@RequestMapping("/animais")
public class AnimalController {
    private static final String FORM = "animal-form";
    private static final String LISTA = "/animais";

    private final AnimalService service;
    private final Feedback feedback;

    public AnimalController(AnimalService service, Feedback feedback) {
        this.service = service;
        this.feedback = feedback;
    }

    @GetMapping
    public String listar(Model m) {
        m.addAttribute("animais", service.listar());
        return "animais";
    }

    @GetMapping("/novo")
    public String novo(Model m) {
        m.addAttribute("form", new AnimalForm());
        return FORM;
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable long id, Model m) {
        m.addAttribute("form", AnimalForm.de(service.buscar(id)));
        m.addAttribute("id", id);
        return FORM;
    }

    @PostMapping("/salvar")
    public String salvar(@RequestParam(required = false) Long id, @Valid @ModelAttribute("form") AnimalForm f,
            BindingResult b, Model m, RedirectAttributes r) {
        if (id != null)
            service.buscar(id);
        m.addAttribute("id", id);
        if (b.hasErrors())
            return FORM;
        return feedback.executar(() -> service.salvar(id, f), "sucesso.animal.salvo", LISTA, r);
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable long id, RedirectAttributes r) {
        return feedback.executar(() -> service.excluir(id), "sucesso.animal.excluido", LISTA, r);
    }
}
