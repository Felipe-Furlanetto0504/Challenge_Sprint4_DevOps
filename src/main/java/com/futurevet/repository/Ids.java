package com.futurevet.repository;

import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class Ids {
    private static final Set<String> SEQUENCES = Set.of("USUARIO", "ANIMAL", "CONSULTA", "VACINA", "NOTIFICACAO");

    private final JdbcTemplate db;

    public Ids(JdbcTemplate db) {
        this.db = db;
    }

    public long next(String tabela) {
        if (!SEQUENCES.contains(tabela))
            throw new IllegalArgumentException("Sequence desconhecida: " + tabela);
        return db.queryForObject("SELECT FV_SEQ_" + tabela + ".NEXTVAL FROM DUAL", Long.class);
    }
}
