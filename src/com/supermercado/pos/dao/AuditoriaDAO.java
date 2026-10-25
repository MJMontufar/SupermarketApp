package com.supermercado.pos.dao;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class AuditoriaDAO {
    /** Solo identidad y resultado; nunca recibe contrasenas ni hashes. */
    public void registrar(Connection con, long solicitante, String username, Long supervisor,
            boolean exitoso, String motivo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                INSERT INTO auditoria_autorizacion
                (solicitado_por, username_ingresado, autorizado_por, exitoso, motivo)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            ps.setLong(1, solicitante); ps.setString(2, limitar(username));
            if (supervisor == null) { ps.setNull(3, java.sql.Types.NUMERIC); } else { ps.setLong(3, supervisor); }
            ps.setInt(4, exitoso ? 1 : 0); ps.setString(5, motivo); ps.executeUpdate();
        }
    }
    private String limitar(String texto) {
        if (texto == null) { return null; }
        texto = texto.strip();
        // VARCHAR2(50) puede medirse en bytes. No partir caracteres Unicode.
        int fin = 0;
        while (fin < texto.length()) {
            int siguiente = fin + Character.charCount(texto.codePointAt(fin));
            if (texto.substring(0, siguiente).getBytes(StandardCharsets.UTF_8).length > 50) { break; }
            fin = siguiente;
        }
        return texto.substring(0, fin);
    }
}
