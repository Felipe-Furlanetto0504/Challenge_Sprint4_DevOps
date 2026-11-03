package com.futurevet.model;

import java.math.BigDecimal;

public record Animal(long id, long usuarioId, String nome, String especie, String raca, String idade,
        String porte, BigDecimal peso, String tutor) {
}
