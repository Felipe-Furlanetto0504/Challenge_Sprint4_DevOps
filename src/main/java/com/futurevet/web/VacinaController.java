package com.futurevet.web;

import com.futurevet.model.SituacaoVacina;
import com.futurevet.service.AnimalService;
import com.futurevet.service.CarteiraService;
import com.futurevet.service.RegraException;
import com.futurevet.web.forms.ReforcoForm;
import com.futurevet.web.forms.VacinaForm;
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
@RequestMapping("/vacinas")
public class VacinaController {
    private static final String FORM = "vacina-form";
    private static final String FORM_REFORCO = "reforco-form";
    private static final String LISTA = "/vacinas";

    private final CarteiraService service;
    private final AnimalService animais;
    private final Feedback feedback;

    public VacinaController(CarteiraService service, AnimalService animais, Feedback feedback) {
        this.service = service;
        this.animais = animais;
        this.feedback = feedback;
    }

    @GetMapping
    public String listar(Model m) {
        var itens = service.listar();
        m.addAttribute("itens", itens);
        m.addAttribute("atrasadas", itens.stream().filter(i -> i.situacao() == SituacaoVacina.ATRASADA).count());
        m.addAttribute("proximas", itens.stream().filter(i -> i.situacao() == SituacaoVacina.PROXIMA).count());
        return "vacinas";
    }

    @GetMapping("/nova")
    public String nova(Model m) {
        return formulario(new VacinaForm(), null, m);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable long id, Model m) {
        return formulario(VacinaForm.de(service.buscar(id)), id, m);
    }

    @PostMapping("/salvar")
    public String salvar(@RequestParam(required = false) Long id, @Valid @ModelAttribute("form") VacinaForm f,
            BindingResult b, Model m, RedirectAttributes r) {
        if (id != null)
            service.buscar(id);
        formulario(f, id, m);
        if (b.hasErrors())
            return FORM;
        try {
            service.salvar(id, f);
        } catch (RegraException e) {
            b.reject(e.getCodigo(), e.getArgumentos(), e.getCodigo());
            return FORM;
        }
        feedback.sucesso(r, "sucesso.vacina.salva");
        return "redirect:" + LISTA;
    }

    @GetMapping("/{id}/reforco")
    public String reforco(@PathVariable long id, Model m) {
        m.addAttribute("dose", service.buscar(id));
        m.addAttribute("form", new ReforcoForm());
        return FORM_REFORCO;
    }

    @PostMapping("/{id}/reforco")
    public String reforcar(@PathVariable long id, @Valid @ModelAttribute("form") ReforcoForm f, BindingResult b,
            Model m, RedirectAttributes r) {
        m.addAttribute("dose", service.buscar(id));
        if (b.hasErrors())
            return FORM_REFORCO;
        try {
            service.reforcar(id, f);
        } catch (RegraException e) {
            b.reject(e.getCodigo(), e.getArgumentos(), e.getCodigo());
            return FORM_REFORCO;
        }
        feedback.sucesso(r, "sucesso.vacina.reforco");
        return "redirect:" + LISTA;
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable long id, RedirectAttributes r) {
        return feedback.executar(() -> service.excluir(id), "sucesso.vacina.excluida", LISTA, r);
    }

    private String formulario(VacinaForm form, Long id, Model m) {
        m.addAttribute("form", form);
        m.addAttribute("id", id);
        m.addAttribute("animais", animais.listar());
        return FORM;
    }
}
