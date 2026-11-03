package com.futurevet.web;

import com.futurevet.internacionalizacao.Mensagens;
import com.futurevet.security.Acesso;
import com.futurevet.service.NotificacaoService;
import com.futurevet.service.RegraException;
import jakarta.servlet.http.HttpServletRequest;
import java.beans.PropertyEditorSupport;
import java.security.Principal;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalAdvice {
    private static final String TELA_ERRO = "erro";
    private static final String[] CAMPOS_SEM_TRIM = { "senha", "confirmacao" };
    private static final String[] CAMPOS_PROIBIDOS = { "role", "senhaHash", "usuarioId" };

    private final Acesso acesso;
    private final NotificacaoService notificacoes;
    private final Mensagens mensagens;

    public GlobalAdvice(Acesso acesso, NotificacaoService notificacoes, Mensagens mensagens) {
        this.acesso = acesso;
        this.notificacoes = notificacoes;
        this.mensagens = mensagens;
    }

    @InitBinder
    public void binder(WebDataBinder b) {
        b.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        for (String campo : CAMPOS_SEM_TRIM) {
            b.registerCustomEditor(String.class, campo, new PropertyEditorSupport() {
                @Override
                public void setAsText(String text) {
                    setValue(text);
                }
            });
        }
        b.setDisallowedFields(CAMPOS_PROIBIDOS);
    }

    /**
     * Usuário logado, contador de avisos e endereço para a troca de idioma — usados pelo layout. Também é
     * chamado pelos tratadores de exceção, que recebem um Model novo.
     */
    @ModelAttribute
    public void layout(Model m, Principal p, HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/"))
            return;
        m.addAttribute("paginaAtual", request.getRequestURI());
        m.addAttribute("uriIdioma", "GET".equals(request.getMethod()) ? request.getRequestURI() : "/");
        if (p != null) {
            m.addAttribute("usuario", acesso.atual());
            m.addAttribute("notificacoesNaoLidas", notificacoes.naoLidas());
        }
    }

    @ExceptionHandler(RegraException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String regra(RegraException e, Model m, Principal p, HttpServletRequest request) {
        layout(m, p, request);
        m.addAttribute("mensagem", mensagens.texto(e));
        return TELA_ERRO;
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String negado(Model m, Principal p, HttpServletRequest request) {
        layout(m, p, request);
        m.addAttribute("mensagem", mensagens.texto("erro.acesso.registro"));
        return TELA_ERRO;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflito(Model m, Principal p, HttpServletRequest request) {
        layout(m, p, request);
        m.addAttribute("mensagem", mensagens.texto("erro.integridade"));
        return TELA_ERRO;
    }
}
