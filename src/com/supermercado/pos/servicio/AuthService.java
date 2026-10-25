package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.UsuarioDAO;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.AutorizacionException;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Seguridad;
import java.sql.Connection;
import java.sql.SQLException;

public final class AuthService {
    private final UsuarioDAO usuarioDao = new UsuarioDAO();

    public Usuario autenticar(String username, char[] clave) {
        if (username == null || username.isBlank() || clave == null || clave.length == 0) {
            throw new AutorizacionException("Usuario o contrasena invalidos");
        }
        try (Connection con = Conexion.obtener()) {
            return usuarioDao.buscarPorUsername(con, username.trim())
                    .filter(Usuario::isActivo)
                    .filter(usuario -> Seguridad.verificar(clave, usuario.getPasswordHash()))
                    .orElseThrow(() -> new AutorizacionException("Usuario o contrasena invalidos"));
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar Oracle", e);
        }
    }
}
