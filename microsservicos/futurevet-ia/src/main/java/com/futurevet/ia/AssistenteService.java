package com.futurevet.ia;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Monta o prompt do Assistente Vet e consulta o modelo pelo ChatClient do Spring AI. As mensagens
 * são montadas como objetos (e não como template), para que chaves e símbolos digitados pelo usuário
 * não sejam interpretados como variáveis.
 */
@Service
public class AssistenteService {
    static final String SEM_CHAVE = "nao-configurada";

    static final String INSTRUCOES = """
            Você é o Assistente Vet, assistente virtual da plataforma FutureVet, criada em parceria com a \
            clínica Clyvo Vet para acompanhar a saúde contínua de pets.
            Regras:
            1. Responda de forma acolhedora, clara e objetiva, em no máximo 3 parágrafos curtos.
            2. Use os DADOS DO SISTEMA abaixo para responder sobre os animais, vacinas e consultas do \
            usuário. Nunca invente animais, datas ou registros que não estejam nesses dados.
            3. Você não faz diagnóstico nem prescreve medicamentos ou doses. Para sintomas, oriente a \
            agendar uma consulta pelo FutureVet.
            4. Em sinais de emergência (dificuldade para respirar, convulsão, sangramento, \
            envenenamento, trauma), oriente a procurar atendimento veterinário imediatamente.
            5. Se a pergunta não tiver relação com cuidados de animais ou com o uso do FutureVet, \
            explique gentilmente que só pode ajudar com esses assuntos.
            6. Escreva a resposta no idioma: %s.

            DADOS DO SISTEMA:
            %s
            """;

    private final ChatClient chat;
    private final String chave;
    private final String modelo;

    public AssistenteService(ChatClient.Builder builder, @Value("${spring.ai.openai.api-key}") String chave,
            @Value("${spring.ai.openai.chat.options.model}") String modelo) {
        this.chat = builder.build();
        this.chave = chave;
        this.modelo = modelo;
    }

    public RespostaAssistente responder(PerguntaAssistente pergunta) {
        if (chave == null || chave.isBlank() || SEM_CHAVE.equals(chave))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chave da API de IA não configurada.");
        String idioma = pergunta.idioma() == null || pergunta.idioma().isBlank() ? "pt-BR" : pergunta.idioma();
        String contexto = pergunta.contexto() == null || pergunta.contexto().isBlank() ? "(sem dados)"
                : pergunta.contexto();
        Prompt prompt = new Prompt(List.of(new SystemMessage(INSTRUCOES.formatted(idioma, contexto)),
                new UserMessage(pergunta.pergunta())));
        String texto = chat.prompt(prompt).call().content();
        return new RespostaAssistente(texto, modelo);
    }
}
