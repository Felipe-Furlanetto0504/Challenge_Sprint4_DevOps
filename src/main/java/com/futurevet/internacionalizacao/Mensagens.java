package com.futurevet.internacionalizacao;

import com.futurevet.service.RegraException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class Mensagens {
    private final MessageSource messageSource;

    public Mensagens(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String texto(String codigo, Object... argumentos) {
        return messageSource.getMessage(codigo, argumentos, codigo, LocaleContextHolder.getLocale());
    }

    public String texto(RegraException e) {
        return texto(e.getCodigo(), e.getArgumentos());
    }
}
