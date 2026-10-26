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
    public Optional<Rol> bloquear(Connection con, long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id FROM rol WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { return Optional.empty(); }
            }
        }
        return buscarPorId(con, id);
    }
    public long insertar(Connection con, String nombre, int nivel) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO rol (nombre,nivel) VALUES (?,?)", new String[] {"ID"})) {
            ps.setString(1, nombre); ps.setInt(2, nivel); ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getLong(1); }
        }
    }
    public void actualizar(Connection con, long id, String nombre, int nivel) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE rol SET nombre=?,nivel=? WHERE id=?")) {
            ps.setString(1, nombre); ps.setInt(2, nivel); ps.setLong(3, id); ps.executeUpdate();
        }
    }
    public void reemplazarPermisos(Connection con, long rol, java.util.Set<Long> permisos) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM rol_permiso WHERE rol_id=?")) {
            ps.setLong(1, rol); ps.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO rol_permiso (rol_id,permiso_id) VALUES (?,?)")) {
            for (long permiso : permisos) { ps.setLong(1, rol); ps.setLong(2, permiso); ps.addBatch(); }
            ps.executeBatch();
        }
    }
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
