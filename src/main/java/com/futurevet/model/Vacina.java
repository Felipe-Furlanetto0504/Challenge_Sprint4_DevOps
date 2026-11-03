package com.futurevet.model;

import java.time.LocalDate;

public record Vacina(long id, long animalId, String animal, long usuarioId, String nome, LocalDate data,
        LocalDate proximaDose, String local, Long origemId, boolean substituida) {

    /** Doses que já ganharam reforço, ou que são reforço de outra, ficam travadas para preservar o histórico. */
    public boolean vinculadaAReforco() {
        return substituida || origemId != null;
    }
}
