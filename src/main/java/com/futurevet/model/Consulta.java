package com.futurevet.model;

import java.time.LocalDate;

public record Consulta(long id, long animalId, String animal, long usuarioId, String tutor, String tipo,
        LocalDate data, String hora, String local, StatusConsulta status, String observacao) {

    public boolean ativa() {
        return status.ativa();
    }

    public boolean aguardandoConfirmacao() {
        return status == StatusConsulta.AGENDADA;
    }

    public boolean confirmada() {
        return status == StatusConsulta.CONFIRMADA;
    }
}
