package com.futurevet.model;

public enum SituacaoVacina {
    ATRASADA("Atrasada"),
    PROXIMA("Próxima dose"),
    EM_DIA("Em dia"),
    SEM_PREVISAO("Sem previsão"),
    HISTORICO("Histórico");

    /** Rótulo em português enviado pela API ao app mobile; as telas web usam mensagens*.properties. */
    private final String descricao;

    SituacaoVacina(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }

    public boolean precisaAtencao() {
        return this == ATRASADA || this == PROXIMA;
    }
}
