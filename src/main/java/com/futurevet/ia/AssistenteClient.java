package com.futurevet.ia;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente declarativo (OpenFeign) do microsserviço futurevet-ia. Com {@code futurevet.ia.url} vazio,
 * o endereço é descoberto pelo nome no Eureka; com a URL preenchida (ex.: no deploy), a chamada vai
 * direto para ela.
 */
@FeignClient(name = "futurevet-ia", url = "${futurevet.ia.url:}")
public interface AssistenteClient {

    @PostMapping("/ia/assistente")
    RespostaAssistente perguntar(@RequestBody PerguntaAssistente pergunta);
}
