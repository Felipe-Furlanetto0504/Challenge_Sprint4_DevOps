package com.futurevet.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida de uma consulta: o tutor solicita (AGENDADA), a clínica confirma (CONFIRMADA) e,
 * após o horário, registra o atendimento (REALIZADA). Qualquer etapa ativa pode ser CANCELADA.
 * INDEFINIDA identifica registros anteriores à aplicação web.
 */
public enum StatusConsulta {
    INDEFINIDA, AGENDADA, CONFIRMADA, REALIZADA, CANCELADA;

    public static final Set<StatusConsulta> ATIVOS = EnumSet.of(INDEFINIDA, AGENDADA, CONFIRMADA);

    public boolean ativa() {
        return ATIVOS.contains(this);
    }
}
