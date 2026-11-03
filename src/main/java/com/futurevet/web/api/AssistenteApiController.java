package com.futurevet.web.api;

import com.futurevet.ia.AssistenteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistente")
public class AssistenteApiController {
    private final AssistenteService service;

    public AssistenteApiController(AssistenteService service) {
        this.service = service;
    }

    public record PerguntaRequest(@NotBlank @Size(max = AssistenteService.LIMITE_PERGUNTA) String pergunta) {
    }

    public record RespostaResponse(String resposta) {
    }

    @PostMapping
    public RespostaResponse perguntar(@Valid @RequestBody PerguntaRequest body) {
        return new RespostaResponse(service.perguntar(body.pergunta()));
    }
}
