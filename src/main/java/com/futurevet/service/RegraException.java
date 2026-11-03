package com.futurevet.service;

import java.util.Arrays;

/**
 * Violação de uma regra de negócio. Carrega o código da mensagem (em mensagens*.properties) e seus
 * argumentos, para que o texto seja exibido no idioma escolhido pelo usuário.
 */
public class RegraException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final transient Object[] argumentos;

    public RegraException(String codigo, Object... argumentos) {
        super(codigo);
        this.argumentos = argumentos == null ? new Object[0] : argumentos.clone();
    }

    public String getCodigo() {
        return getMessage();
    }

    public Object[] getArgumentos() {
        return Arrays.copyOf(argumentos, argumentos.length);
    }
}
