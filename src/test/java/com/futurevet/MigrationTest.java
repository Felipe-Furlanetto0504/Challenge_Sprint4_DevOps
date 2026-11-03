package com.futurevet;

import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.assertj.core.api.Assertions.*;

class MigrationTest {
    @Test
    void horarioLegadoInvalidoBloqueiaAtualizacao() {
        String url = "jdbc:h2:mem:legado_invalido;MODE=Oracle;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").target("1").load().migrate();
        JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(url, "sa", ""));
        db.update(
                "INSERT INTO TB_USUARIO(ID_USUARIO,NOME,EMAIL,SENHA,CPF) VALUES(1,'Legado','x@example.com','senha123',11111111111)");
        db.update(
                "INSERT INTO TB_ANIMAL(ID_ANIMAL,ID_USUARIO,NOME_ANIMAL,ESPEC_ANIMAL) VALUES(1,1,'Pet','CAO')");
        db.update(
                "INSERT INTO TB_CONSULTA(ID_CONSULTA,ID_ANIMAL,TIPO_CONSULTA,DATA_CONSULTA,HORA_CONSULTA) VALUES(1,1,'Rotina',DATE '2026-01-01','25:99')");
        assertThatThrownBy(() -> Flyway.configure().dataSource(url, "sa", "").load().migrate())
                .isInstanceOf(org.flywaydb.core.api.FlywayException.class);
        assertThat(db.queryForObject("SELECT SENHA FROM TB_USUARIO WHERE ID_USUARIO=1", String.class))
                .isEqualTo("senha123");
    }

    @Test
    void bancoLegadoPreservaDadosGeraIdsAcimaDoMaximoEProtegeSenhas() {
        String url = "jdbc:h2:mem:legado;MODE=Oracle;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").target("1").load().migrate();
        JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(url, "sa", ""));
        db.update(
                "INSERT INTO TB_USUARIO(ID_USUARIO,NOME,EMAIL,SENHA,CPF) VALUES(77,'Legado','legado@example.com','senha123',12345678901)");
        db.update(
                "INSERT INTO TB_ANIMAL(ID_ANIMAL,ID_USUARIO,NOME_ANIMAL,ESPEC_ANIMAL,IDADE_ANIMAL) VALUES(90,77,'Pet legado','CAO','6 meses')");
        db.update(
                "INSERT INTO TB_CONSULTA(ID_CONSULTA,ID_ANIMAL,TIPO_CONSULTA,DATA_CONSULTA) VALUES(80,90,'Rotina',DATE '2026-01-01')");
        var flyway = Flyway.configure().dataSource(url, "sa", "").load();
        flyway.migrate();
        assertThat(db.queryForObject("SELECT NOME FROM TB_USUARIO WHERE ID_USUARIO=77", String.class))
                .isEqualTo("Legado");
        assertThat(db.queryForObject("SELECT FV_SEQ_USUARIO.NEXTVAL FROM DUAL", Long.class)).isEqualTo(78L);
        assertThat(db.queryForObject("SELECT FV_SEQ_ANIMAL.NEXTVAL FROM DUAL", Long.class)).isEqualTo(91L);
        assertThat(db.queryForObject("SELECT STATUS FROM TB_CONSULTA WHERE ID_CONSULTA=80", String.class))
                .isEqualTo("INDEFINIDA");
        assertThat(new BCryptPasswordEncoder().matches("senha123",
                db.queryForObject("SELECT SENHA FROM TB_USUARIO WHERE ID_USUARIO=77", String.class)))
                .isTrue();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
}
