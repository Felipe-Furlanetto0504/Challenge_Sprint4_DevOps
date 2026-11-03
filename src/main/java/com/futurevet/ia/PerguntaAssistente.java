package com.futurevet.ia;

/**
 * Corpo enviado ao microsserviço de IA.
 *
 * @param contexto resumo dos dados do usuário (animais, vacinas, consultas) que embasa a resposta
 * @param idioma   código do idioma em que a resposta deve ser escrita (pt-BR ou en)
 */
public record PerguntaAssistente(String pergunta, String contexto, String idioma) {
}
