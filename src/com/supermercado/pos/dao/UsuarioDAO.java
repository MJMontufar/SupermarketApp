package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.Permiso;
import com.supermercado.pos.dominio.Rol;
import com.supermercado.pos.dominio.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class UsuarioDAO {
    private final RolDAO rolDao = new RolDAO();

    public Optional<Usuario> buscarPorUsername(Connection con, String username) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id, username, password_hash, "
                + "nombre_completo, activo FROM usuario WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(con, rs)) : Optional.empty();
            }
        }
    }

    public Optional<Usuario> buscarPorId(Connection con, long id, boolean bloquear) throws SQLException {
        String sql = "SELECT id, username, password_hash, nombre_completo, activo FROM usuario WHERE id = ?";
        if (bloquear) { sql += " FOR UPDATE"; }
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(con, rs)) : Optional.empty();
            }
        }
    }

    public List<Usuario> listar(Connection con) throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT id, username, password_hash, "
                + "nombre_completo, activo FROM usuario ORDER BY username");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { usuarios.add(mapear(con, rs)); }
        }
        return usuarios;
    }

    public long insertar(Connection con, String username, String nombre, String hash) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO usuario "
                + "(username, nombre_completo, password_hash) VALUES (?, ?, ?)", new String[] {"ID"})) {
            ps.setString(1, username);
            ps.setString(2, nombre);
            ps.setString(3, hash);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) { throw new SQLException("No se recibio el ID del usuario"); }
                return rs.getLong(1);
            }
        }
    }

    public void asignarRolInicial(Connection con, long usuarioId, long rolId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO usuario_rol (usuario_id, rol_id) VALUES (?, ?)")) {
            ps.setLong(1, usuarioId);
            ps.setLong(2, rolId);
            ps.executeUpdate();
        }
    }

    public void actualizar(Connection con, long id, String nombre, boolean activo, String hash) throws SQLException {
        String sql = hash == null
                ? "UPDATE usuario SET nombre_completo = ?, activo = ? WHERE id = ?"
                : "UPDATE usuario SET nombre_completo = ?, activo = ?, password_hash = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, activo ? 1 : 0);
            int indice = 3;
            if (hash != null) { ps.setString(indice++, hash); }
            ps.setLong(indice, id);
            if (ps.executeUpdate() != 1) { throw new SQLException("El usuario ya no existe"); }
        }
    }

    private Usuario mapear(Connection con, ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario(rs.getLong("id"), rs.getString("username"),
                rs.getString("nombre_completo"), rs.getString("password_hash"));
        usuario.setActivo(rs.getInt("activo") == 1);
        for (Rol rol : rolDao.buscarPorUsuario(con, usuario.getId())) { usuario.agregarRol(rol); }
        try (PreparedStatement ps = con.prepareStatement("SELECT p.id, p.codigo, p.descripcion "
                + "FROM permiso p JOIN usuario_permiso up ON up.permiso_id = p.id WHERE up.usuario_id = ?")) {
            ps.setLong(1, usuario.getId());
            try (ResultSet extras = ps.executeQuery()) {
                while (extras.next()) {
                    usuario.agregarPermisoExtra(new Permiso(extras.getLong(1), extras.getString(2), extras.getString(3)));
                }
            }
        }
        return usuario;
    }
}
