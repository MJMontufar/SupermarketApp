package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.AuditoriaDAO;
import com.supermercado.pos.dao.UsuarioDAO;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.AutorizacionException;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Seguridad;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

/** Autorizacion puntual: no altera Sesion ni conserva la clave del supervisor. */
public final class AutorizacionService {
    @FunctionalInterface interface Operacion<T> { T ejecutar(Connection con, Usuario supervisor) throws SQLException; }

    Solicitud solicitar(Usuario actor, String contexto) { return new Solicitud(actor, contexto); }

    /** Una solicitud ligada a una operacion, con tres fallos maximos y un solo uso exitoso. */
    static final class Solicitud {
        private final Usuario actor;
        private final String contexto;
        private int fallidos;
        private boolean consumida;
        private final UsuarioDAO usuarios = new UsuarioDAO();
        private final AuditoriaDAO auditoria = new AuditoriaDAO();

        private Solicitud(Usuario actor, String contexto) { this.actor = actor; this.contexto = contexto; }
        synchronized int restantes() { return Math.max(0, 3 - fallidos); }
        synchronized boolean terminada() { return consumida || fallidos >= 3; }

        /** Ejecuta autorizacion y negocio con la misma conexion y un solo commit. */
        synchronized <T> T ejecutar(String username, char[] clave, Operacion<T> operacion) {
            try {
                if (terminada()) { throw new AutorizacionException("La solicitud termino. Cancela y vuelve a solicitar autorizacion"); }
                try (Connection con = Conexion.obtener()) {
                    con.setAutoCommit(false);
                    try {
                        Usuario supervisor;
                        try {
                            supervisor = validar(con, username, clave);
                        } catch (AutorizacionException e) {
                            fallidos++;
                            registrar(username, null, false, "RECHAZADA");
                            throw e;
                        }
                        // Auditoria independiente: conserva el intento aunque falle el negocio.
                        registrar(username, supervisor.getId(), true, "CREDENCIALES_ACEPTADAS");
                        consumida = true;
                        T resultado = operacion.ejecutar(con, supervisor);
                        con.commit(); return resultado;
                    } catch (SQLException | RuntimeException e) { con.rollback(); throw e; }
                } catch (SQLException e) { throw new IllegalStateException("No se pudo completar la autorizacion", e); }
            } finally { if (clave != null) { Arrays.fill(clave, '\0'); } }
        }
        private Usuario validar(Connection con, String username, char[] clave) throws SQLException {
            if (username == null || username.isBlank() || username.length() > 50 || clave == null || clave.length == 0) {
                throw new AutorizacionException("Usuario o contrasena invalidos");
            }
            Usuario encontrado = usuarios.buscarPorUsername(con, username.trim())
                    .orElseThrow(() -> new AutorizacionException("Usuario o contrasena invalidos"));
            // Impide desactivar o cambiar el perfil durante esta autorizacion y apertura.
            Usuario supervisor = usuarios.buscarPorId(con, encontrado.getId(), true).orElseThrow();
            if (!supervisor.isActivo() || !Seguridad.verificar(clave, supervisor.getPasswordHash())) {
                throw new AutorizacionException("Usuario o contrasena invalidos");
            }
            if (supervisor.getId().equals(actor.getId()) || supervisor.nivelMaximo() < 2
                    || supervisor.nivelMaximo() <= actor.nivelMaximo() || !supervisor.tienePermiso("TURNO_ABRIR")) {
                throw new AutorizacionException("Debe autorizar otro usuario con nivel supervisor o superior y mayor nivel que el cajero");
            }
            return supervisor;
        }
        private void registrar(String username, Long supervisor, boolean exito, String resultado) throws SQLException {
            try (Connection registro = Conexion.obtener()) {
                auditoria.registrar(registro, actor.getId(), username, supervisor, exito, contexto + "; " + resultado);
            }
        }
    }
}
