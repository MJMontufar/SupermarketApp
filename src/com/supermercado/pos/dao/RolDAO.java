package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.Permiso;
import com.supermercado.pos.dominio.Rol;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RolDAO {
    public List<Rol> listar(Connection con) throws SQLException {
        return consultar(con, "SELECT id, nombre, nivel FROM rol ORDER BY nivel, nombre", null);
    }

    public Optional<Rol> buscarPorId(Connection con, long id) throws SQLException {
        return consultar(con, "SELECT id, nombre, nivel FROM rol WHERE id = ?", id).stream().findFirst();
    }

    public List<Rol> buscarPorUsuario(Connection con, long usuarioId) throws SQLException {
        return consultar(con, "SELECT r.id, r.nombre, r.nivel FROM rol r JOIN usuario_rol ur "
                + "ON ur.rol_id = r.id WHERE ur.usuario_id = ? ORDER BY r.nivel", usuarioId);
    }

    private List<Rol> consultar(Connection con, String sql, Long parametro) throws SQLException {
        List<Rol> roles = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (parametro != null) { ps.setLong(1, parametro); }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Rol rol = new Rol(rs.getLong("id"), rs.getString("nombre"), rs.getInt("nivel"));
                    cargarPermisos(con, rol);
                    roles.add(rol);
                }
            }
        }
        return roles;
    }

    private void cargarPermisos(Connection con, Rol rol) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT p.id, p.codigo, p.descripcion "
                + "FROM permiso p JOIN rol_permiso rp ON rp.permiso_id = p.id WHERE rp.rol_id = ?")) {
            ps.setLong(1, rol.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rol.agregarPermiso(new Permiso(rs.getLong(1), rs.getString(2), rs.getString(3)));
                }
            }
        }
    }
}
