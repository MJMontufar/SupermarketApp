package com.supermercado.pos.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class MovimientoCajaDAO {
    public long insertar(Connection con, long turno, String tipo, BigDecimal monto, long supervisor, String referencia) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                INSERT INTO movimiento_caja (turno_id, tipo, monto, autorizado_por, referencia)
                VALUES (?, ?, ?, ?, ?)
                """, new String[] {"ID"})) {
            ps.setLong(1, turno); ps.setString(2, tipo); ps.setBigDecimal(3, monto);
            ps.setLong(4, supervisor); ps.setString(5, referencia); ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getLong(1); }
        }
    }
    public void referencia(Connection con, long id, String referencia) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE movimiento_caja SET referencia = ? WHERE id = ?")) {
            ps.setString(1, referencia); ps.setLong(2, id); ps.executeUpdate();
        }
    }
}
