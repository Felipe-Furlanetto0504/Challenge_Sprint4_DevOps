package com.futurevet.repository;

import com.futurevet.mensageria.EventoFuturevet;
import com.futurevet.model.Notificacao;
import com.futurevet.model.TipoEvento;
import com.futurevet.model.Usuario;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class NotificacaoRepository {
    private static final int LIMITE = 50;
    /** Avisos da clínica não têm usuário; avisos de tutor pertencem ao dono do animal. */
    private static final String DESTINO = " WHERE ((? = 1 AND ID_USUARIO IS NULL) OR (? = 0 AND ID_USUARIO = ?))";

    private final JdbcTemplate db;
    private final Ids ids;

    public NotificacaoRepository(JdbcTemplate db, Ids ids) {
        this.db = db;
        this.ids = ids;
    }

    private final RowMapper<Notificacao> mapper = (r, n) -> new Notificacao(r.getLong("ID_NOTIFICACAO"),
            r.getObject("ID_USUARIO", Long.class), TipoEvento.valueOf(r.getString("TIPO")),
            r.getString("NOME_ANIMAL"), r.getString("DETALHE"),
            r.getDate("DATA_EVENTO") == null ? null : r.getDate("DATA_EVENTO").toLocalDate(),
            r.getTimestamp("CRIADA_EM").toLocalDateTime(), r.getInt("LIDA") == 1);

    public long criar(EventoFuturevet e) {
        long id = ids.next("NOTIFICACAO");
        db.update(
                "INSERT INTO TB_FV_NOTIFICACAO(ID_NOTIFICACAO,ID_USUARIO,TIPO,NOME_ANIMAL,DETALHE,DATA_EVENTO,CRIADA_EM,LIDA) VALUES(?,?,?,?,?,?,?,0)",
                id, e.destinatarioId(), e.tipo().name(), e.animal(), e.detalhe(),
                e.dataEvento() == null ? null : Date.valueOf(e.dataEvento()), Timestamp.valueOf(e.ocorridoEm()));
        return id;
    }

    public List<Notificacao> listar(Usuario u) {
        return db.query("SELECT * FROM TB_FV_NOTIFICACAO" + DESTINO
                + " ORDER BY CRIADA_EM DESC, ID_NOTIFICACAO DESC FETCH FIRST " + LIMITE + " ROWS ONLY", mapper,
                destino(u));
    }

    public long naoLidas(Usuario u) {
        return db.queryForObject("SELECT COUNT(*) FROM TB_FV_NOTIFICACAO" + DESTINO + " AND LIDA = 0",
                Long.class, destino(u));
    }

    public void marcarTodasLidas(Usuario u) {
        db.update("UPDATE TB_FV_NOTIFICACAO SET LIDA = 1" + DESTINO + " AND LIDA = 0", destino(u));
    }

    private static Object[] destino(Usuario u) {
        int clinica = u.admin() ? 1 : 0;
        return new Object[] { clinica, clinica, u.id() };
    }
}
