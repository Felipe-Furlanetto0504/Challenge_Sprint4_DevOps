package com.futurevet.web.api;

import com.futurevet.model.Notificacao;
import com.futurevet.service.NotificacaoService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoApiController {
    private final NotificacaoService service;

    public NotificacaoApiController(NotificacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Notificacao> listar() {
        return service.listar();
    }

    @GetMapping("/nao-lidas")
    public Map<String, Long> naoLidas() {
        return Map.of("total", service.naoLidas());
    }

    @PostMapping("/lidas")
    public ResponseEntity<Void> marcarLidas() {
        service.marcarTodasLidas();
        return ResponseEntity.noContent().build();
    }
}
