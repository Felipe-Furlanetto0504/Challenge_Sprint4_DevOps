package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import java.sql.*;

public class V2__suporte_aplicacao extends BaseJavaMigration {
    @Override
    public Integer getChecksum() {
        return 2026090802;
    }

    @Override
    public void migrate(Context ctx) throws Exception {
        Connection c = ctx.getConnection();
        boolean oracle = c.getMetaData().getDatabaseProductName().contains("Oracle");
        verificarDadosLegados(c);
        try (Statement s = c.createStatement()) {
            s.execute(oracle ? "ALTER TABLE TB_USUARIO MODIFY (SENHA VARCHAR2(255))"
                    : "ALTER TABLE TB_USUARIO ALTER COLUMN SENHA VARCHAR2(255)");
            s.execute("ALTER TABLE TB_USUARIO ADD ROLE VARCHAR2(10) DEFAULT 'TUTOR' NOT NULL");
            s.execute("ALTER TABLE TB_USUARIO ADD CONSTRAINT CK_FV_ROLE CHECK (ROLE IN ('ADMIN','TUTOR'))");
            s.execute("ALTER TABLE TB_CONSULTA ADD STATUS VARCHAR2(15) DEFAULT 'INDEFINIDA' NOT NULL");
            s.execute("ALTER TABLE TB_CONSULTA ADD OBSERVACAO VARCHAR2(255)");
            s.execute(
                    "ALTER TABLE TB_CONSULTA ADD CONSTRAINT CK_FV_STATUS CHECK (STATUS IN ('INDEFINIDA','AGENDADA','CONFIRMADA','REALIZADA','CANCELADA'))");
            s.execute("ALTER TABLE TB_VACINA ADD ID_DOSE_ORIGEM NUMBER");
            s.execute(
                    "ALTER TABLE TB_VACINA ADD CONSTRAINT FK_FV_DOSE_ORIGEM FOREIGN KEY (ID_DOSE_ORIGEM) REFERENCES TB_VACINA(ID_VACINA)");
            s.execute("ALTER TABLE TB_VACINA ADD CONSTRAINT UQ_FV_DOSE_ORIGEM UNIQUE (ID_DOSE_ORIGEM)");
            s.execute("CREATE TABLE TB_FV_AGENDA_LOCK (ID NUMBER PRIMARY KEY)");
            s.execute("INSERT INTO TB_FV_AGENDA_LOCK (ID) VALUES (1)");
            s.execute("CREATE INDEX IX_FV_ANIMAL_USUARIO ON TB_ANIMAL(ID_USUARIO)");
            s.execute("CREATE INDEX IX_FV_CONSULTA_ANIMAL ON TB_CONSULTA(ID_ANIMAL)");
            s.execute("CREATE INDEX IX_FV_VACINA_ANIMAL ON TB_VACINA(ID_ANIMAL)");
        }
        for (String nome : new String[] { "USUARIO", "ANIMAL", "CONSULTA", "VACINA" }) {
            long inicio;
            try (Statement s = c.createStatement();
                    ResultSet r = s
                            .executeQuery("SELECT COALESCE(MAX(ID_" + nome + "),0)+1 FROM TB_" + nome)) {
                r.next();
                inicio = r.getLong(1);
            }
            try (Statement s = c.createStatement()) {
                s.execute("CREATE SEQUENCE FV_SEQ_" + nome + " START WITH " + inicio + " INCREMENT BY 1");
            }
        }
    }

    private void verificarDadosLegados(Connection c) throws SQLException {
        try (Statement s = c.createStatement();
                ResultSet r = s.executeQuery(
                        "SELECT LOWER(EMAIL) FROM TB_USUARIO GROUP BY LOWER(EMAIL) HAVING COUNT(*)>1")) {
            if (r.next())
                throw new SQLException(
                        "Há e-mails duplicados quando comparados sem diferenciar maiúsculas. Corrija antes de migrar.");
        }
        try (Statement s = c.createStatement();
                ResultSet r = s.executeQuery(
                        "SELECT HORA_CONSULTA FROM TB_CONSULTA WHERE HORA_CONSULTA IS NOT NULL")) {
            while (r.next()) {
                if (!r.getString(1).matches("([01][0-9]|2[0-3]):[0-5][0-9]"))
                    throw new SQLException(
                            "Há horário legado fora do formato HH:mm. Corrija antes de migrar.");
            }
        }
    }
}
