package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.VentaParaReversion;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ReversionDAO {
    public Optional<VentaParaReversion> buscarVenta(Connection con, long turno, long venta) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id, turno_id, fecha, estado, total FROM venta WHERE id = ? AND turno_id = ?")) {
            ps.setLong(1, venta); ps.setLong(2, turno);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { return Optional.empty(); }
                List<VentaParaReversion.Detalle> detalles = new ArrayList<>();
                try (PreparedStatement lineas = con.prepareStatement("""
                        SELECT d.id, d.producto_id, p.nombre, d.cantidad, d.cantidad_reversada, d.precio_unitario
                        FROM venta_detalle d JOIN producto p ON p.id = d.producto_id WHERE d.venta_id = ? ORDER BY d.id
                        """)) {
                    lineas.setLong(1, venta);
                    try (ResultSet filas = lineas.executeQuery()) { while (filas.next()) { detalles.add(mapear(filas)); } }
                }
                return Optional.of(new VentaParaReversion(rs.getLong(1), rs.getLong(2), rs.getTimestamp(3).toLocalDateTime(),
                        rs.getString(4), rs.getBigDecimal(5), detalles));
            }
        }
    }
    public record DetalleBloqueado(long ventaId, VentaParaReversion.Detalle detalle) { }
    public Optional<DetalleBloqueado> bloquearDetalle(Connection con, long turno, long detalle) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                SELECT d.id, d.producto_id, p.nombre, d.cantidad, d.cantidad_reversada, d.precio_unitario, d.venta_id
                FROM venta_detalle d JOIN venta v ON v.id = d.venta_id JOIN producto p ON p.id = d.producto_id
                WHERE d.id = ? AND v.turno_id = ? FOR UPDATE OF d.cantidad_reversada
                """)) {
            ps.setLong(1, detalle); ps.setLong(2, turno);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new DetalleBloqueado(rs.getLong(7), mapear(rs))) : Optional.empty();
            }
        }
    }
    public boolean sumarReversada(Connection con, long detalle, int cantidad) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                UPDATE venta_detalle SET cantidad_reversada = cantidad_reversada + ?
                WHERE id = ? AND cantidad_reversada + ? <= cantidad
                """)) {
            ps.setInt(1, cantidad); ps.setLong(2, detalle); ps.setInt(3, cantidad); return ps.executeUpdate() == 1;
        }
    }
    public String actualizarEstado(Connection con, long venta) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                UPDATE venta v SET estado = CASE WHEN NOT EXISTS (
                    SELECT 1 FROM venta_detalle d WHERE d.venta_id = v.id AND d.cantidad_reversada < d.cantidad)
                    THEN 'ANULADA' ELSE 'PARCIALMENTE_REVERSADA' END WHERE v.id = ?
                """)) { ps.setLong(1, venta); ps.executeUpdate(); }
        try (PreparedStatement ps = con.prepareStatement("SELECT estado FROM venta WHERE id = ?")) {
            ps.setLong(1, venta);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getString(1); }
        }
    }
    public long insertar(Connection con, long detalle, int cantidad, BigDecimal monto, String motivo,
            long solicitante, long supervisor) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                INSERT INTO reversion (detalle_id, cantidad, monto, motivo, solicitado_por, autorizado_por)
                VALUES (?, ?, ?, ?, ?, ?)
                """, new String[] {"ID"})) {
            ps.setLong(1, detalle); ps.setInt(2, cantidad); ps.setBigDecimal(3, monto); ps.setString(4, motivo);
            ps.setLong(5, solicitante); ps.setLong(6, supervisor); ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getLong(1); }
        }
    }
    private VentaParaReversion.Detalle mapear(ResultSet rs) throws SQLException {
        return new VentaParaReversion.Detalle(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getInt(4), rs.getInt(5), rs.getBigDecimal(6));
    }
}
