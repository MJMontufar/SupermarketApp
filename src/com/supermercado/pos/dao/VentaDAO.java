package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.LineaCarrito;
import com.supermercado.pos.dominio.Pago;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class VentaDAO {
    public long insertar(Connection con, long turno, BigDecimal total) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO venta (turno_id, total) VALUES (?, ?)", new String[] {"ID"})) {
            ps.setLong(1, turno); ps.setBigDecimal(2, total); ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getLong(1); }
        }
    }
    public void detalle(Connection con, long venta, LineaCarrito linea) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                INSERT INTO venta_detalle (venta_id, producto_id, cantidad, precio_unitario, subtotal)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            ps.setLong(1, venta); ps.setLong(2, linea.producto().getId()); ps.setInt(3, linea.cantidad());
            ps.setBigDecimal(4, linea.precioUnitario()); ps.setBigDecimal(5, linea.subtotal()); ps.executeUpdate();
        }
    }
    public void pago(Connection con, long venta, Pago pago) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO pago (venta_id, metodo, monto, recibido, cambio) VALUES (?, ?, ?, ?, ?)")) {
            ps.setLong(1, venta); ps.setString(2, pago.metodo().name()); ps.setBigDecimal(3, pago.monto());
            ps.setBigDecimal(4, pago.recibido()); ps.setBigDecimal(5, pago.cambio()); ps.executeUpdate();
        }
    }
}
