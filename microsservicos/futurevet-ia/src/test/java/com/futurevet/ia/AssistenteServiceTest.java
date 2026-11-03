package com.futurevet.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

class AssistenteServiceTest {

    private ChatModel modeloQueResponde(String texto) {
        ChatModel modelo = mock(ChatModel.class);
        when(modelo.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(texto)))));
        return modelo;
    }

    @Test
    void enviaInstrucoesContextoEIdiomaEDevolveResposta() {
        ChatModel modelo = modeloQueResponde("A V10 do Bento está atrasada.");
        var service = new AssistenteService(ChatClient.builder(modelo), "chave-teste", "modelo-teste");

        var resposta = service.responder(new PerguntaAssistente("Alguma vacina atrasada? {x}",
                "- Bento: V10, situação ATRASADA", "pt-BR"));

        assertThat(resposta.resposta()).isEqualTo("A V10 do Bento está atrasada.");
        assertThat(resposta.modelo()).isEqualTo("modelo-teste");
        ArgumentCaptor<Prompt> enviado = ArgumentCaptor.forClass(Prompt.class);
        Mockito.verify(modelo).call(enviado.capture());
        List<Message> mensagens = enviado.getValue().getInstructions();
        assertThat(mensagens).extracting(Message::getMessageType).containsExactly(MessageType.SYSTEM,
                MessageType.USER);
        assertThat(mensagens.get(0).getText()).contains("Bento: V10, situação ATRASADA").contains("pt-BR")
                .contains("não faz diagnóstico");
        assertThat(mensagens.get(1).getText()).isEqualTo("Alguma vacina atrasada? {x}");
    }

    @Test
    void semChaveConfiguradaRespondeServicoIndisponivel() {
        var service = new AssistenteService(ChatClient.builder(modeloQueResponde("x")), AssistenteService.SEM_CHAVE,
                "modelo");
        assertThatThrownBy(() -> service.responder(new PerguntaAssistente("Oi", "", "pt-BR")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("503");
    }
}
