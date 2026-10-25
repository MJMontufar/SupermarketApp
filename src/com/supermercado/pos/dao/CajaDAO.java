package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.Caja;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class CajaDAO {
    public List<Caja> libres(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                SELECT c.id, c.numero, c.nombre FROM caja c WHERE c.activa = 1
                AND NOT EXISTS (SELECT 1 FROM turno t WHERE t.caja_id = c.id AND t.estado = 'ABIERTO')
                ORDER BY c.numero
                """); ResultSet rs = ps.executeQuery()) {
            List<Caja> cajas = new ArrayList<>();
            while (rs.next()) { cajas.add(new Caja(rs.getLong(1), rs.getInt(2), rs.getString(3))); }
            return List.copyOf(cajas);
        }
    }
    /** La fila se bloquea antes de abrir para serializar aperturas de la misma caja. */
    public boolean bloquearActiva(Connection con, long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT activa FROM caja WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) == 1; }
        }
    }
}
