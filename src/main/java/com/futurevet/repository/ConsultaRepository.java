package com.futurevet.repository;

import com.futurevet.model.Consulta;
import com.futurevet.model.StatusConsulta;
import com.futurevet.model.Usuario;
import com.futurevet.web.forms.AgendaForm;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ConsultaRepository {
    private static final String SELECT = "SELECT C.*,A.NOME_ANIMAL,A.ID_USUARIO,U.NOME TUTOR FROM TB_CONSULTA C JOIN TB_ANIMAL A ON A.ID_ANIMAL=C.ID_ANIMAL JOIN TB_USUARIO U ON U.ID_USUARIO=A.ID_USUARIO";
    private static final String ORDEM = " ORDER BY C.DATA_CONSULTA DESC,C.HORA_CONSULTA,C.ID_CONSULTA";
    /** Lista SQL com os status que ainda ocupam a agenda, derivada do próprio enum. */
    private static final String STATUS_ATIVOS = StatusConsulta.ATIVOS.stream()
            .map(s -> "'" + s.name() + "'").collect(Collectors.joining(",", "(", ")"));

    private final JdbcTemplate db;
    private final Ids ids;

    public ConsultaRepository(JdbcTemplate db, Ids ids) {
        this.db = db;
        this.ids = ids;
    }

    private final RowMapper<Consulta> mapper = (r, n) -> new Consulta(r.getLong("ID_CONSULTA"),
            r.getLong("ID_ANIMAL"), r.getString("NOME_ANIMAL"), r.getLong("ID_USUARIO"), r.getString("TUTOR"),
            r.getString("TIPO_CONSULTA"), r.getDate("DATA_CONSULTA").toLocalDate(), r.getString("HORA_CONSULTA"),
            r.getString("LOCAL_CONSULTA"), StatusConsulta.valueOf(r.getString("STATUS")),
            r.getString("OBSERVACAO"));

    public void bloquearAgenda() {
        db.queryForObject("SELECT ID FROM TB_FV_AGENDA_LOCK WHERE ID=1 FOR UPDATE", Long.class);
    }

    public List<Consulta> listar(Usuario u) {
        return u.admin() ? db.query(SELECT + ORDEM, mapper)
                : db.query(SELECT + " WHERE A.ID_USUARIO=?" + ORDEM, mapper, u.id());
    }

    public Optional<Consulta> buscar(long id) {
        return db.query(SELECT + " WHERE C.ID_CONSULTA=?", mapper, id).stream().findFirst();
    }

    /** Há conflito quando o mesmo animal ou o mesmo local já está ocupado na data e hora. */
    public boolean conflito(long animal, LocalDate data, String hora, String local, long ignorar) {
        return db.queryForObject("SELECT COUNT(*) FROM TB_CONSULTA WHERE ID_CONSULTA<>? AND DATA_CONSULTA=? "
                + "AND HORA_CONSULTA=? AND STATUS IN " + STATUS_ATIVOS
                + " AND (ID_ANIMAL=? OR LOWER(LOCAL_CONSULTA)=LOWER(?))", Integer.class, ignorar, data, hora,
                animal, local) > 0;
    }

    public long criar(AgendaForm f) {
        long id = ids.next("CONSULTA");
        db.update(
                "INSERT INTO TB_CONSULTA(ID_CONSULTA,ID_ANIMAL,TIPO_CONSULTA,DATA_CONSULTA,HORA_CONSULTA,LOCAL_CONSULTA,STATUS,OBSERVACAO) VALUES(?,?,?,?,?,?,?,?)",
                id, f.getAnimalId(), f.getTipo(), f.getData(), f.getHora().toString(), f.getLocal(),
                StatusConsulta.AGENDADA.name(), f.getObservacao());
        return id;
    }

    /** Reagendar devolve a consulta para "aguardando confirmação" da clínica. */
    public void reagendar(long id, AgendaForm f) {
        db.update(
                "UPDATE TB_CONSULTA SET DATA_CONSULTA=?,HORA_CONSULTA=?,LOCAL_CONSULTA=?,TIPO_CONSULTA=?,OBSERVACAO=?,STATUS=? WHERE ID_CONSULTA=?",
                f.getData(), f.getHora().toString(), f.getLocal(), f.getTipo(), f.getObservacao(),
                StatusConsulta.AGENDADA.name(), id);
    }

    public void status(long id, StatusConsulta status, String observacao) {
        db.update("UPDATE TB_CONSULTA SET STATUS=?,OBSERVACAO=? WHERE ID_CONSULTA=?", status.name(), observacao,
                id);
    }
}
