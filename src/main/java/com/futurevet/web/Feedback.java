package com.futurevet.web;

import com.futurevet.internacionalizacao.Mensagens;
import com.futurevet.service.RegraException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Padroniza o retorno das ações das telas: executa a operação, guarda a mensagem de sucesso ou o
 * erro de regra (já traduzidos) e redireciona para a listagem.
 */
@Component
public class Feedback {
    private final Mensagens mensagens;

    public Feedback(Mensagens mensagens) {
        this.mensagens = mensagens;
    }

    public String executar(Runnable operacao, String codigoSucesso, String destino, RedirectAttributes r) {
        try {
            operacao.run();
            sucesso(r, codigoSucesso);
        } catch (RegraException e) {
            r.addFlashAttribute("erro", mensagens.texto(e));
        }
        return "redirect:" + destino;
    }

    public void sucesso(RedirectAttributes r, String codigo) {
        r.addFlashAttribute("sucesso", mensagens.texto(codigo));
    }
}
