package com.futurevet.ia;

import com.futurevet.security.Acesso;
import java.util.List;
import com.futurevet.service.RegraException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AssistenteService {
    public static final int LIMITE_PERGUNTA = 500;
    private static final Logger log = LoggerFactory.getLogger(AssistenteService.class);

    private final AssistenteClient cliente;
    private final ContextoAssistente contexto;
    private final Acesso acesso;

    public AssistenteService(AssistenteClient cliente, ContextoAssistente contexto, Acesso acesso) {
        this.cliente = cliente;
        this.contexto = contexto;
        this.acesso = acesso;
    }

    public String perguntar(String pergunta) {
        return perguntar(pergunta, List.of());
    }

    /** Pergunta com as últimas trocas da conversa, para o assistente entender perguntas de continuação. */
    public String perguntar(String pergunta, List<ConversaAssistente.Troca> historico) {
        if (pergunta == null || pergunta.isBlank() || pergunta.length() > LIMITE_PERGUNTA)
            throw new RegraException("erro.ia.pergunta", LIMITE_PERGUNTA);
        var corpo = new PerguntaAssistente(pergunta.strip(), contexto.montar(acesso.atual(), historico),
                LocaleContextHolder.getLocale().toLanguageTag());
        try {
            RespostaAssistente resposta = cliente.perguntar(corpo);
            if (resposta == null || resposta.resposta() == null || resposta.resposta().isBlank())
                throw new AssistenteIndisponivelException("erro.ia.indisponivel");
            return resposta.resposta();
        } catch (RegraException e) {
            throw e;
        } catch (FeignException e) {
            log.warn("Microsserviço de IA respondeu com erro (status {}): {}", e.status(), e.getMessage());
            throw new AssistenteIndisponivelException(e.status() == 503 ? "erro.ia.sem-chave" : "erro.ia.indisponivel");
        } catch (RuntimeException e) {
            log.warn("Microsserviço de IA inacessível: {}", e.getMessage());
            throw new AssistenteIndisponivelException("erro.ia.indisponivel");
        }
    }
}
