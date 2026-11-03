package com.futurevet.ia;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/ia")
public class AssistenteController {
    private static final Logger log = LoggerFactory.getLogger(AssistenteController.class);

    private final AssistenteService service;

    public AssistenteController(AssistenteService service) {
        this.service = service;
    }

    @PostMapping("/assistente")
    public RespostaAssistente perguntar(@Valid @RequestBody PerguntaAssistente pergunta) {
        return service.responder(pergunta);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail status(ResponseStatusException e) {
        return ProblemDetail.forStatusAndDetail(e.getStatusCode(), e.getReason());
    }

    /** Falhas do provedor de IA (chave inválida, limite de uso, rede) viram 502 para o chamador. */
    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail falhaModelo(RuntimeException e) {
        log.warn("Falha ao consultar o modelo de IA: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Falha ao consultar o modelo de IA.");
    }
}
