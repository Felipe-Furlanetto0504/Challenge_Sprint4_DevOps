package com.futurevet.web.api;

import com.futurevet.model.Consulta;
import com.futurevet.service.AgendaService;
import com.futurevet.web.forms.AgendaForm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consultas")
public class AgendaApiController {
    private final AgendaService service;

    public AgendaApiController(AgendaService service) {
        this.service = service;
    }

    public record MotivoRequest(@NotBlank String motivo) {}
    public record ObservacaoRequest(@NotBlank String observacao) {}

    @GetMapping
    public List<Consulta> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Consulta buscar(@PathVariable long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> agendar(@Valid @RequestBody AgendaForm form) {
        long id = service.agendar(form);
        return ResponseEntity.status(201).body(Map.of("id", id));
    }

    @PutMapping("/{id}")
    public Consulta reagendar(@PathVariable long id, @Valid @RequestBody AgendaForm form) {
        service.reagendar(id, form);
        return service.buscar(id);
    }

    @PostMapping("/{id}/confirmar")
    public Consulta confirmar(@PathVariable long id) {
        service.confirmar(id);
        return service.buscar(id);
    }

    @PostMapping("/{id}/cancelar")
    public Consulta cancelar(@PathVariable long id, @Valid @RequestBody MotivoRequest body) {
        service.cancelar(id, body.motivo());
        return service.buscar(id);
    }

    @PostMapping("/{id}/concluir")
    public Consulta concluir(@PathVariable long id, @Valid @RequestBody ObservacaoRequest body) {
        service.concluir(id, body.observacao());
        return service.buscar(id);
    }
}