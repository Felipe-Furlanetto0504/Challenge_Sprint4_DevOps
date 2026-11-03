package com.futurevet.ia;

import com.futurevet.service.RegraException;

public class AssistenteIndisponivelException extends RegraException {
    private static final long serialVersionUID = 1L;

    public AssistenteIndisponivelException(String codigo) {
        super(codigo);
    }
}
