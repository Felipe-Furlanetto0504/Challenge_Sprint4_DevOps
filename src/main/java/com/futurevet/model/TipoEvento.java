package com.futurevet.model;

/**
 * Acontecimentos do domínio que geram avisos. Quem recebe o aviso é sempre o outro lado da
 * relação: se o tutor age, a clínica é avisada; se a clínica age, o tutor do animal é avisado.
 */
public enum TipoEvento {
    CONSULTA_SOLICITADA,
    CONSULTA_REAGENDADA,
    CONSULTA_CONFIRMADA,
    CONSULTA_CANCELADA,
    CONSULTA_REALIZADA,
    VACINA_REGISTRADA,
    REFORCO_REGISTRADO
}
