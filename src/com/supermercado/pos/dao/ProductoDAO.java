package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.Producto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ProductoDAO {
    public List<Producto> activos(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM producto WHERE activo = 1 ORDER BY nombre");
                ResultSet rs = ps.executeQuery()) {
            List<Producto> productos = new ArrayList<>();
            while (rs.next()) { productos.add(mapear(rs)); }
            return List.copyOf(productos);
        }
    }
    public Optional<Producto> bloquear(Connection con, long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM producto WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(mapear(rs)) : Optional.empty(); }
        }
    }
    /** Predicado atomico: dos cajas nunca pueden vender la misma ultima unidad. */
    public boolean descontar(Connection con, long id, int cantidad) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE producto SET stock = stock - ? WHERE id = ? AND activo = 1 AND stock >= ?")) {
            ps.setInt(1, cantidad); ps.setLong(2, id); ps.setInt(3, cantidad);
            return ps.executeUpdate() == 1;
        }
    }
    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto(rs.getLong("id"), rs.getString("codigo_barras"), rs.getString("nombre"),
                rs.getBigDecimal("precio"), rs.getInt("stock"));
        p.setActivo(rs.getInt("activo") == 1); p.setPorcentajeImpuesto(rs.getBigDecimal("porcentaje_impuesto"));
        return p;
    }
}
