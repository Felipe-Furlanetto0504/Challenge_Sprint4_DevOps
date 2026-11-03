package com.futurevet.web.api;

import com.futurevet.service.CarteiraService;
import com.futurevet.web.forms.ReforcoForm;
import com.futurevet.web.forms.VacinaForm;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/vacinas")
public class VacinaApiController {
    private final CarteiraService service;

    public VacinaApiController(CarteiraService service) {
        this.service = service;
    }

    @GetMapping
    public List<CarteiraService.Item> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public CarteiraService.Item buscar(@PathVariable long id) {
        return service.classificar(service.buscar(id));
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> criar(@Valid @RequestBody VacinaForm form) {
        long id = service.salvar(null, form);
        return ResponseEntity.status(201).body(Map.of("id", id));
    }

    @PutMapping("/{id}")
    public CarteiraService.Item atualizar(@PathVariable long id, @Valid @RequestBody VacinaForm form) {
        service.salvar(id, form);
        return service.classificar(service.buscar(id));
    }

    @PostMapping("/{id}/reforco")
    public ResponseEntity<Map<String, Long>> reforcar(@PathVariable long id, @Valid @RequestBody ReforcoForm form) {
        long novoId = service.reforcar(id, form);
        return ResponseEntity.status(201).body(Map.of("id", novoId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}