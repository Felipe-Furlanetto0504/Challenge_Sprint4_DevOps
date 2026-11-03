package com.futurevet.web;

import com.futurevet.monitoramento.MonitoramentoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MonitoramentoController {
    private final MonitoramentoService service;

    public MonitoramentoController(MonitoramentoService service) {
        this.service = service;
    }

    @GetMapping("/admin/monitoramento")
    public String painel(Model m) {
        m.addAttribute("painel", service.painel());
        return "monitoramento";
    }
}
