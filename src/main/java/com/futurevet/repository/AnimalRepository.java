package com.futurevet.repository;

import com.futurevet.model.Animal;
import com.futurevet.model.Usuario;
import com.futurevet.web.forms.AnimalForm;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class AnimalRepository {
    private final JdbcTemplate db;
    private final Ids ids;

    public AnimalRepository(JdbcTemplate db, Ids ids) {
        this.db = db;
        this.ids = ids;
    }

    private static final String SELECT = "SELECT A.*,U.NOME TUTOR FROM TB_ANIMAL A JOIN TB_USUARIO U ON U.ID_USUARIO=A.ID_USUARIO";
    private final RowMapper<Animal> mapper = (r, n) -> new Animal(r.getLong("ID_ANIMAL"),
            r.getLong("ID_USUARIO"), r.getString("NOME_ANIMAL"), r.getString("ESPEC_ANIMAL"),
            r.getString("RACA_ANIMAL"), r.getString("IDADE_ANIMAL"), r.getString("PORTE_ANIMAL"),
            r.getBigDecimal("PESO_ANIMAL"), r.getString("TUTOR"));

    public List<Animal> listar(Usuario u) {
        return u.admin() ? db.query(SELECT + " ORDER BY A.NOME_ANIMAL,A.ID_ANIMAL", mapper)
                : db.query(SELECT + " WHERE A.ID_USUARIO=? ORDER BY A.NOME_ANIMAL,A.ID_ANIMAL", mapper,
                        u.id());
    }

    public Optional<Animal> buscar(long id) {
        return db.query(SELECT + " WHERE A.ID_ANIMAL=?", mapper, id).stream().findFirst();
    }

    public long criar(AnimalForm f, long dono) {
        long id = ids.next("ANIMAL");
        db.update(
                "INSERT INTO TB_ANIMAL(ID_ANIMAL,ID_USUARIO,NOME_ANIMAL,ESPEC_ANIMAL,RACA_ANIMAL,IDADE_ANIMAL,PORTE_ANIMAL,PESO_ANIMAL) VALUES(?,?,?,?,?,?,?,?)",
                id, dono, f.getNome(), f.getEspecie(), f.getRaca(), f.getIdade(), f.getPorte(), f.getPeso());
        return id;
    }

    public void atualizar(long id, AnimalForm f) {
        db.update(
                "UPDATE TB_ANIMAL SET NOME_ANIMAL=?,ESPEC_ANIMAL=?,RACA_ANIMAL=?,IDADE_ANIMAL=?,PORTE_ANIMAL=?,PESO_ANIMAL=? WHERE ID_ANIMAL=?",
                f.getNome(), f.getEspecie(), f.getRaca(), f.getIdade(), f.getPorte(), f.getPeso(), id);
    }

    public boolean temHistorico(long id) {
        return db.queryForObject(
                "SELECT (SELECT COUNT(*) FROM TB_CONSULTA WHERE ID_ANIMAL=?)+(SELECT COUNT(*) FROM TB_VACINA WHERE ID_ANIMAL=?) FROM DUAL",
                Integer.class, id, id) > 0;
    }

    public void excluir(long id) {
        db.update("DELETE FROM TB_ANIMAL WHERE ID_ANIMAL=?", id);
    }
}
