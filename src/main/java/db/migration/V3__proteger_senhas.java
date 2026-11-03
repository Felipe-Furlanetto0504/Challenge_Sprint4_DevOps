package db.migration;

import org.flywaydb.core.api.migration.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.sql.*;
import java.util.*;

public class V3__proteger_senhas extends BaseJavaMigration {
    @Override
    public Integer getChecksum() {
        return 2026090803;
    }

    @Override
    public void migrate(Context ctx) throws Exception {
        var hashes = new LinkedHashMap<Long, String>();
        var encoder = new BCryptPasswordEncoder();
        try (var s = ctx.getConnection().createStatement();
                var r = s.executeQuery("SELECT ID_USUARIO,SENHA FROM TB_USUARIO")) {
            while (r.next()) {
                String senha = r.getString(2);
                if (!senha.matches("^\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}$")) {
                    if (senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
                        throw new SQLException("Senha legada excede limite BCrypt; revisão necessária.");
                    hashes.put(r.getLong(1), encoder.encode(senha));
                }
            }
        }
        try (var s = ctx.getConnection()
                .prepareStatement("UPDATE TB_USUARIO SET SENHA=? WHERE ID_USUARIO=?")) {
            for (var entry : hashes.entrySet()) {
                s.setString(1, entry.getValue());
                s.setLong(2, entry.getKey());
                s.addBatch();
            }
            s.executeBatch();
        }
    }
}
