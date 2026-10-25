package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.RolDAO;
import com.supermercado.pos.dao.UsuarioDAO;
import com.supermercado.pos.dominio.Rol;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.AutorizacionException;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Seguridad;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class UsuarioService {
    private final UsuarioDAO usuarioDao = new UsuarioDAO();
    private final RolDAO rolDao = new RolDAO();

    private void requerir(Usuario actor, String permiso) {
        if (actor == null || !actor.isActivo() || !actor.tienePermiso(permiso)) {
            throw new AutorizacionException("No tienes permiso para esta operacion");
        }
    }

    public List<Usuario> listar(Usuario actor) {
        requerir(actor, "USUARIO_ADMIN");
        try (Connection con = Conexion.obtener()) { return usuarioDao.listar(con); }
        catch (SQLException e) { throw new IllegalStateException("No se pudieron listar los usuarios", e); }
    }

    public List<Rol> rolesAsignables(Usuario actor) {
        requerir(actor, "ROL_ADMIN");
        try (Connection con = Conexion.obtener()) {
            return rolDao.listar(con).stream().filter(rol -> rol.getNivel() < actor.nivelMaximo()).toList();
        } catch (SQLException e) { throw new IllegalStateException("No se pudieron consultar los roles", e); }
    }

    public Usuario crear(String username, String nombre, char[] clave, long rolId, Usuario actor) {
        requerir(actor, "USUARIO_ADMIN");
        requerir(actor, "ROL_ADMIN");
        if (username == null || username.isBlank() || username.length() > 50
                || username.chars().anyMatch(Character::isWhitespace)) {
            throw new ReglaNegocioException("El usuario es obligatorio, sin espacios y de hasta 50 caracteres");
        }
        validarNombre(nombre);
        validarClave(clave);
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                Rol rol = rolDao.buscarPorId(con, rolId)
                        .orElseThrow(() -> new ReglaNegocioException("El rol no existe"));
                if (rol.getNivel() >= actor.nivelMaximo()) {
                    throw new AutorizacionException("Solo puedes asignar roles de nivel inferior al tuyo");
                }
                long id = usuarioDao.insertar(con, username, nombre, Seguridad.hash(clave));
                usuarioDao.asignarRolInicial(con, id, rolId);
                Usuario usuario = usuarioDao.buscarPorId(con, id, false).orElseThrow();
                con.commit();
                return usuario;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                if (e instanceof SQLException sql && sql.getErrorCode() == 1) {
                    throw new ReglaNegocioException("Ese nombre de usuario ya existe");
                }
                throw e;
            }
        } catch (SQLException e) { throw new IllegalStateException("No se pudo crear el usuario", e); }
    }

    /** Conserva los roles y permisos del usuario; no los cambia al editar su perfil. */
    public Usuario editar(long id, String nombre, char[] nuevaClave, boolean activo, Usuario actor) {
        requerir(actor, "USUARIO_ADMIN");
        validarNombre(nombre);
        boolean cambiarClave = nuevaClave != null && nuevaClave.length > 0;
        if (cambiarClave) { validarClave(nuevaClave); }
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                Usuario usuario = usuarioDao.buscarPorId(con, id, true)
                        .orElseThrow(() -> new ReglaNegocioException("El usuario no existe"));
                boolean propio = usuario.getId().equals(actor.getId());
                if (!propio && usuario.nivelMaximo() >= actor.nivelMaximo()) {
                    throw new AutorizacionException("No puedes editar usuarios de nivel igual o superior");
                }
                if (propio && !activo) {
                    throw new ReglaNegocioException("No puedes desactivar tu propia cuenta");
                }
                usuarioDao.actualizar(con, id, nombre, activo, cambiarClave ? Seguridad.hash(nuevaClave) : null);
                Usuario actualizado = usuarioDao.buscarPorId(con, id, false).orElseThrow();
                con.commit();
                return actualizado;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) { throw new IllegalStateException("No se pudo editar el usuario", e); }
    }

    private void validarNombre(String nombre) {
        if (nombre != null && nombre.length() > 120) {
            throw new ReglaNegocioException("El nombre admite hasta 120 caracteres");
        }
    }

    private void validarClave(char[] clave) {
        if (clave == null || clave.length < 8) {
            throw new ReglaNegocioException("La contrasena requiere al menos 8 caracteres");
        }
    }
}
