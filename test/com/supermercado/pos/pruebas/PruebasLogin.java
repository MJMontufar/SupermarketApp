package com.supermercado.pos.pruebas;

import com.supermercado.pos.dao.RolDAO;
import com.supermercado.pos.dominio.Rol;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.AutorizacionException;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.servicio.AuthService;
import com.supermercado.pos.servicio.UsuarioService;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Configuracion;
import java.io.Console;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Pruebas funcionales de los servicios reales sobre usuarios temporales POS_TEST. */
public final class PruebasLogin {
    private static final String CLAVE_DE_PRUEBA = "ClavePrueba2026";
    private final AuthService auth = new AuthService();
    private final UsuarioService servicio = new UsuarioService();
    private final List<Long> temporales = new ArrayList<>();
    private int ejecutadas;
    private int fallos;
    private long rolCajero;
    private long rolAdmin;
    private Usuario administrador;

    @FunctionalInterface private interface Prueba { void ejecutar() throws SQLException; }

    private void caso(String nombre, Prueba prueba) {
        ejecutadas++;
        try { prueba.ejecutar(); System.out.println("OK    " + nombre); }
        catch (SQLException | RuntimeException | AssertionError e) {
            fallos++; System.out.println("FALLO " + nombre); e.printStackTrace(System.out);
        }
    }

    private static void verificar(boolean condicion) {
        if (!condicion) { throw new AssertionError("Resultado inesperado"); }
    }

    private static void espera(Class<? extends RuntimeException> tipo, Runnable accion) {
        try { accion.run(); }
        catch (RuntimeException e) { if (tipo.isInstance(e)) { return; } throw e; }
        throw new AssertionError("Se esperaba " + tipo.getSimpleName());
    }

    private Usuario autenticar(String username, String texto) {
        char[] clave = texto.toCharArray();
        try { return auth.autenticar(username, clave); }
        finally { Arrays.fill(clave, '\0'); }
    }

    private String nombreUnico() { return "p_login_" + UUID.randomUUID().toString().replace("-", ""); }

    private Usuario crear(String username, String texto, long rol, Usuario actor) {
        char[] clave = texto.toCharArray();
        try {
            Usuario usuario = servicio.crear(username, "Usuario de prueba", clave, rol, actor);
            temporales.add(usuario.getId());
            return usuario;
        } finally { Arrays.fill(clave, '\0'); }
    }

    private Usuario cajero() { return crear(nombreUnico(), CLAVE_DE_PRUEBA, rolCajero, administrador); }

    private long contarUsuario(String username) throws SQLException {
        try (Connection con = Conexion.obtener();
                PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM usuario WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    private void permisoExtra(long id, String codigo) throws SQLException {
        try (Connection con = Conexion.obtener(); PreparedStatement ps = con.prepareStatement(
                "INSERT INTO usuario_permiso (usuario_id, permiso_id) SELECT ?, id FROM permiso WHERE codigo = ?")) {
            ps.setLong(1, id); ps.setString(2, codigo);
            verificar(ps.executeUpdate() == 1);
        }
    }

    private void ejecutar() {
        caso("Admin autentica con permisos y nivel cargados desde Oracle", () -> {
            verificar(administrador.nivelMaximo() == 4);
            verificar(administrador.tienePermiso("USUARIO_ADMIN") && administrador.tienePermiso("ROL_ADMIN"));
        });
        caso("Contrasena incorrecta rechazada", () ->
                espera(AutorizacionException.class, () -> autenticar("admin", "OtraClave2026")));
        caso("Usuario inexistente rechazado", () ->
                espera(AutorizacionException.class, () -> autenticar(nombreUnico(), CLAVE_DE_PRUEBA)));
        caso("Una expresion SQL no autentica como usuario", () ->
                espera(AutorizacionException.class, () -> autenticar("' OR '1'='1", CLAVE_DE_PRUEBA)));
        caso("Alta de cajero guarda hash y rol en una transaccion", () -> {
            Usuario usuario = cajero();
            Usuario ingreso = autenticar(usuario.getUsername(), CLAVE_DE_PRUEBA);
            verificar(ingreso.getId().equals(usuario.getId()) && ingreso.nivelMaximo() == 1);
            verificar(ingreso.tienePermiso("VENTA_COBRAR") && !ingreso.tienePermiso("USUARIO_ADMIN"));
            verificar(!ingreso.getPasswordHash().equals(CLAVE_DE_PRUEBA));
        });
        caso("Un usuario inactivo no puede iniciar sesion", () -> {
            Usuario usuario = cajero();
            servicio.editar(usuario.getId(), usuario.getNombreCompleto(), new char[0], false, administrador);
            espera(AutorizacionException.class, () -> autenticar(usuario.getUsername(), CLAVE_DE_PRUEBA));
            verificar(contarUsuario(usuario.getUsername()) == 1);
        });
        caso("Clave de menos de ocho caracteres no crea usuario", () -> {
            String nombre = nombreUnico();
            espera(ReglaNegocioException.class, () -> crear(nombre, "corta", rolCajero, administrador));
            verificar(contarUsuario(nombre) == 0);
        });
        caso("Username con espacios no crea usuario", () ->
                espera(ReglaNegocioException.class, () -> crear("con espacios", CLAVE_DE_PRUEBA, rolCajero, administrador)));
        caso("Username duplicado conserva la cuenta original", () -> {
            Usuario usuario = cajero();
            espera(ReglaNegocioException.class, () -> crear(usuario.getUsername(), "OtraClave2026", rolCajero, administrador));
            verificar(contarUsuario(usuario.getUsername()) == 1);
            verificar(autenticar(usuario.getUsername(), CLAVE_DE_PRUEBA).getRoles().size() == 1);
        });
        caso("No se puede asignar un rol de nivel igual al propio", () -> {
            String nombre = nombreUnico();
            espera(AutorizacionException.class, () -> crear(nombre, CLAVE_DE_PRUEBA, rolAdmin, administrador));
            verificar(contarUsuario(nombre) == 0);
        });
        caso("Un rol inexistente no deja un usuario huerfano", () -> {
            String nombre = nombreUnico();
            espera(ReglaNegocioException.class, () -> crear(nombre, CLAVE_DE_PRUEBA, -1, administrador));
            verificar(contarUsuario(nombre) == 0);
        });
        caso("Un cajero no puede listar ni crear usuarios", () -> {
            Usuario usuario = cajero();
            espera(AutorizacionException.class, () -> servicio.listar(usuario));
            espera(AutorizacionException.class, () -> crear(nombreUnico(), CLAVE_DE_PRUEBA, rolCajero, usuario));
        });
        caso("Editar perfil conserva roles y contrasena si no se cambia", () -> {
            Usuario usuario = cajero();
            Usuario editado = servicio.editar(usuario.getId(), "Nombre actualizado", new char[0], true, administrador);
            verificar(editado.getNombreCompleto().equals("Nombre actualizado"));
            verificar(editado.getRoles().equals(usuario.getRoles()));
            verificar(autenticar(usuario.getUsername(), CLAVE_DE_PRUEBA).isActivo());
        });
        caso("Cambiar contrasena invalida la anterior", () -> {
            Usuario usuario = cajero();
            char[] nueva = "NuevaClave2026".toCharArray();
            try { servicio.editar(usuario.getId(), "Nombre", nueva, true, administrador); }
            finally { Arrays.fill(nueva, '\0'); }
            espera(AutorizacionException.class, () -> autenticar(usuario.getUsername(), CLAVE_DE_PRUEBA));
            verificar(autenticar(usuario.getUsername(), "NuevaClave2026").getId().equals(usuario.getId()));
        });
        caso("El administrador no puede desactivar su propia cuenta", () -> {
            espera(ReglaNegocioException.class, () -> servicio.editar(administrador.getId(), "Admin", new char[0], false, administrador));
            verificar(autenticar("admin", "Admin123!").isActivo());
        });
        caso("Permiso extra no permite editar un usuario de mayor nivel", () -> {
            Usuario usuario = cajero(); permisoExtra(usuario.getId(), "USUARIO_ADMIN");
            Usuario actor = autenticar(usuario.getUsername(), CLAVE_DE_PRUEBA);
            espera(AutorizacionException.class, () -> servicio.editar(administrador.getId(), "Cambio no permitido", new char[0], true, actor));
            verificar(Objects.equals(autenticar("admin", "Admin123!").getNombreCompleto(), administrador.getNombreCompleto()));
        });
        caso("Permisos extra aplican al siguiente login, sin elevar nivel", () -> {
            Usuario usuario = cajero();
            verificar(!usuario.tienePermiso("USUARIO_ADMIN"));
            permisoExtra(usuario.getId(), "USUARIO_ADMIN");
            verificar(!usuario.tienePermiso("USUARIO_ADMIN"));
            Usuario nuevo = autenticar(usuario.getUsername(), CLAVE_DE_PRUEBA);
            verificar(nuevo.tienePermiso("USUARIO_ADMIN") && nuevo.nivelMaximo() == 1);
        });
        caso("Un actor inactivo no puede administrar usuarios", () -> {
            Usuario actor = autenticar("admin", "Admin123!"); actor.setActivo(false);
            espera(AutorizacionException.class, () -> servicio.listar(actor));
        });
        caso("La lista de roles asignables excluye el nivel propio", () -> {
            List<Rol> roles = servicio.rolesAsignables(administrador);
            verificar(roles.size() == 3 && roles.stream().allMatch(rol -> rol.getNivel() < 4));
        });
    }

    private void limpiar() throws SQLException {
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                for (long id : temporales) {
                    for (String sql : List.of("DELETE FROM usuario_permiso WHERE usuario_id = ?",
                            "DELETE FROM usuario_rol WHERE usuario_id = ?", "DELETE FROM usuario WHERE id = ?")) {
                        try (PreparedStatement ps = con.prepareStatement(sql)) { ps.setLong(1, id); ps.executeUpdate(); }
                    }
                }
                con.commit();
            } catch (SQLException e) { con.rollback(); throw e; }
        }
    }

    public static void main(String[] args) throws SQLException {
        if (!"pos_test".equalsIgnoreCase(Configuracion.cargar().usuario())) {
            throw new IllegalStateException("Estas pruebas solo se ejecutan como POS_TEST");
        }
        String variable = System.getenv("POS_DB_PASSWORD");
        char[] clave;
        if (variable != null && !variable.isEmpty()) { clave = variable.toCharArray(); }
        else {
            Console consola = System.console();
            if (consola == null) { throw new IllegalStateException("Ejecuta desde una terminal"); }
            clave = consola.readPassword("Clave de Oracle para POS_TEST: ");
        }
        try { Conexion.configurarClaveTemporal(clave); }
        finally { if (clave != null) { Arrays.fill(clave, '\0'); } }
        PruebasLogin pruebas = new PruebasLogin();
        try (Connection con = Conexion.obtener()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT SYS_CONTEXT('USERENV','SESSION_USER'), "
                    + "SYS_CONTEXT('USERENV','CON_NAME') FROM dual"); ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (!"POS_TEST".equals(rs.getString(1)) || !"XEPDB1".equals(rs.getString(2))) {
                    throw new IllegalStateException("Se requiere POS_TEST en XEPDB1");
                }
            }
            pruebas.rolCajero = new RolDAO().listar(con).stream().filter(r -> r.getNombre().equals("CAJERO")).findFirst().orElseThrow().getId();
            pruebas.rolAdmin = new RolDAO().listar(con).stream().filter(r -> r.getNombre().equals("ADMIN")).findFirst().orElseThrow().getId();
            pruebas.administrador = pruebas.autenticar("admin", "Admin123!");
            try { pruebas.ejecutar(); }
            finally { pruebas.limpiar(); }
        } finally { Conexion.limpiarClaveTemporal(); }
        System.out.printf("%nPruebas login: %d; correctas: %d; fallos: %d%n",
                pruebas.ejecutadas, pruebas.ejecutadas - pruebas.fallos, pruebas.fallos);
        if (pruebas.fallos != 0) { throw new AssertionError("Hay pruebas de login fallidas"); }
    }
}
