package com.futurevet.repository;

import com.futurevet.model.Usuario;
import com.futurevet.model.Vacina;
import com.futurevet.web.forms.VacinaForm;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class VacinaRepository {
    private final JdbcTemplate db;
    private final Ids ids;

    public VacinaRepository(JdbcTemplate db, Ids ids) {
        this.db = db;
        this.ids = ids;
    }

    private static final String SELECT = "SELECT V.*,A.NOME_ANIMAL,A.ID_USUARIO,(SELECT COUNT(*) FROM TB_VACINA F WHERE F.ID_DOSE_ORIGEM=V.ID_VACINA) REFORCADA FROM TB_VACINA V JOIN TB_ANIMAL A ON A.ID_ANIMAL=V.ID_ANIMAL";
    private final RowMapper<Vacina> mapper = (r, n) -> new Vacina(r.getLong("ID_VACINA"),
            r.getLong("ID_ANIMAL"), r.getString("NOME_ANIMAL"), r.getLong("ID_USUARIO"),
            r.getString("NOME_VACINA"), r.getDate("DATA_VACINA").toLocalDate(),
            r.getDate("PROX_DOSE") == null ? null : r.getDate("PROX_DOSE").toLocalDate(),
            r.getString("LOC_VACINACAO"), r.getObject("ID_DOSE_ORIGEM", Long.class),
            r.getInt("REFORCADA") > 0);

    public List<Vacina> listar(Usuario u) {
        return u.admin() ? db.query(SELECT + " ORDER BY V.DATA_VACINA DESC,V.ID_VACINA", mapper)
                : db.query(SELECT + " WHERE A.ID_USUARIO=? ORDER BY V.DATA_VACINA DESC,V.ID_VACINA", mapper,
                        u.id());
    }

    public Optional<Vacina> buscar(long id) {
        return db.query(SELECT + " WHERE V.ID_VACINA=?", mapper, id).stream().findFirst();
    }

    public void bloquear(long id) {
        db.queryForObject("SELECT ID_VACINA FROM TB_VACINA WHERE ID_VACINA=? FOR UPDATE", Long.class, id);
    }

    public long criar(VacinaForm f, Long origem) {
        long id = ids.next("VACINA");
        db.update(
                "INSERT INTO TB_VACINA(ID_VACINA,ID_ANIMAL,NOME_VACINA,DATA_VACINA,PROX_DOSE,LOC_VACINACAO,ID_DOSE_ORIGEM) VALUES(?,?,?,?,?,?,?)",
                id, f.getAnimalId(), f.getNome(), f.getData(), f.getProximaDose(), f.getLocal(), origem);
        return id;
    }

    public boolean duplicada(VacinaForm f, long ignorar) {
        return db.queryForObject(
                "SELECT COUNT(*) FROM TB_VACINA WHERE ID_ANIMAL=? AND LOWER(NOME_VACINA)=LOWER(?) AND DATA_VACINA=? AND ID_VACINA<>?",
                Integer.class, f.getAnimalId(), f.getNome(), f.getData(), ignorar) > 0;
    }

    public void atualizar(long id, VacinaForm f) {
        db.update(
                "UPDATE TB_VACINA SET NOME_VACINA=?,DATA_VACINA=?,PROX_DOSE=?,LOC_VACINACAO=? WHERE ID_VACINA=?",
                f.getNome(), f.getData(), f.getProximaDose(), f.getLocal(), id);
    }

    public void excluir(long id) {
        db.update("DELETE FROM TB_VACINA WHERE ID_VACINA=?", id);
    }
}
