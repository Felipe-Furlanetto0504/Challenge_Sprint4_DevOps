package com.futurevet.ia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Pergunta enviada pela aplicação principal.
 *
 * @param contexto resumo dos dados do usuário (animais, vacinas, consultas) montado pelo FutureVet
 * @param idioma   idioma da resposta (ex.: pt-BR, en)
 */
public record PerguntaAssistente(@NotBlank @Size(max = 500) String pergunta, @Size(max = 12000) String contexto,
        @Size(max = 20) String idioma) {
}
