package com.supermercado.pos.pruebas;

import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Configuracion;
import com.supermercado.pos.util.Seguridad;
import java.io.Console;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Arrays;
import java.util.UUID;

/** Prueba restricciones y transacciones JDBC reales, exclusivamente en POS_TEST. */
public final class PruebasOracle {
    private final char[] clave;
    private int ejecutadas;
    private int fallos;

    @FunctionalInterface
    private interface PruebaSQL {
        void ejecutar(Connection con) throws SQLException;
    }

    private PruebasOracle(char[] clave) { this.clave = clave; }

    private static void verificar(boolean condicion) {
        if (!condicion) { throw new AssertionError("Resultado inesperado"); }
    }

    private void caso(String nombre, PruebaSQL prueba) {
        ejecutadas++;
        try (Connection con = Conexion.obtener(clave)) {
            con.setAutoCommit(false);
            try {
                prueba.ejecutar(con);
            } finally {
                con.rollback();
            }
            System.out.println("OK    " + nombre);
        } catch (SQLException | RuntimeException | AssertionError e) {
            fallos++;
            System.out.println("FALLO " + nombre);
            e.printStackTrace(System.out);
        }
    }

    private static PreparedStatement preparar(Connection con, String sql, Object... parametros)
            throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        try {
            for (int i = 0; i < parametros.length; i++) { ps.setObject(i + 1, parametros[i]); }
            return ps;
        } catch (SQLException | RuntimeException e) {
            ps.close();
            throw e;
        }
    }

    private static long numero(Connection con, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = preparar(con, sql, parametros); ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) { throw new SQLException("La consulta no devolvio filas"); }
            return rs.getLong(1);
        }
    }

    private static int actualizar(Connection con, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = preparar(con, sql, parametros)) { return ps.executeUpdate(); }
    }

    private static long insertar(Connection con, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql, new String[] {"ID"})) {
            for (int i = 0; i < parametros.length; i++) { ps.setObject(i + 1, parametros[i]); }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) { throw new SQLException("No se recupero el ID"); }
                return rs.getLong(1);
            }
        }
    }

    private static void esperaOracle(int codigo, PruebaSQL accion, Connection con) throws SQLException {
        try { accion.ejecutar(con); }
        catch (SQLException e) {
            if (e.getErrorCode() == codigo) { return; }
            throw e;
        }
        throw new AssertionError("Se esperaba el codigo Oracle " + codigo);
    }

    private static long usuarioTemporal(Connection con) throws SQLException {
        String nombre = "prueba_" + UUID.randomUUID().toString().replace("-", "");
        String hash;
        try (PreparedStatement ps = preparar(con,
                "SELECT password_hash FROM usuario WHERE username = ?", "admin");
                ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) { throw new SQLException("Faltan los datos iniciales de admin"); }
            hash = rs.getString(1);
        }
        return insertar(con, "INSERT INTO usuario (username, password_hash) VALUES (?, ?)", nombre, hash);
    }

    private static long caja(Connection con, int numero) throws SQLException {
        return numero(con, "SELECT id FROM caja WHERE numero = ?", numero);
    }

    private static long turno(Connection con, long usuario, long caja) throws SQLException {
        return insertar(con, "INSERT INTO turno (caja_id, cajero_id, fondo_inicial) VALUES (?, ?, ?)",
                caja, usuario, 500);
    }

    private static long venta(Connection con) throws SQLException {
        long turno = turno(con, usuarioTemporal(con), caja(con, 1));
        return insertar(con, "INSERT INTO venta (turno_id, total) VALUES (?, ?)", turno, 30);
    }

    private void ejecutar() {
        caso("Esquema, datos iniciales y hash del administrador", con -> {
            verificar(numero(con, "SELECT COUNT(*) FROM user_tables") == 15);
            verificar(numero(con, "SELECT COUNT(*) FROM permiso") == 9);
            verificar(numero(con, "SELECT COUNT(*) FROM rol") == 4);
            verificar(numero(con, "SELECT COUNT(*) FROM caja") == 3);
            verificar(numero(con, "SELECT COUNT(*) FROM producto") == 5);
            char[] demo = "Admin123!".toCharArray();
            try (PreparedStatement ps = preparar(con,
                    "SELECT password_hash FROM usuario WHERE username = ?", "admin");
                    ResultSet rs = ps.executeQuery()) {
                verificar(rs.next() && Seguridad.verificar(demo, rs.getString(1)));
            } finally { Arrays.fill(demo, '\0'); }
        });
        caso("RN-01: una caja no admite dos turnos abiertos", con -> {
            long caja = caja(con, 1);
            turno(con, usuarioTemporal(con), caja);
            long otro = usuarioTemporal(con);
            esperaOracle(1, c -> turno(c, otro, caja), con);
        });
        caso("RN-01: un cajero no admite dos turnos abiertos", con -> {
            long usuario = usuarioTemporal(con);
            turno(con, usuario, caja(con, 1));
            esperaOracle(1, c -> turno(c, usuario, caja(c, 2)), con);
        });
        caso("Turnos cerrados permiten abrir de nuevo la misma caja y cajero", con -> {
            long usuario = usuarioTemporal(con);
            long caja = caja(con, 1);
            long primero = turno(con, usuario, caja);
            actualizar(con, "UPDATE turno SET estado = 'CERRADO', cierre = SYSTIMESTAMP WHERE id = ?", primero);
            long segundo = turno(con, usuario, caja);
            verificar(segundo != primero);
            verificar(numero(con, "SELECT COUNT(*) FROM turno WHERE cajero_id = ?", usuario) == 2);
        });
        caso("RN-03: fondo cero rechazado por la base", con -> {
            long usuario = usuarioTemporal(con);
            long caja = caja(con, 1);
            esperaOracle(2290, c -> insertar(c,
                    "INSERT INTO turno (caja_id, cajero_id, fondo_inicial) VALUES (?, ?, ?)", caja, usuario, 0), con);
        });
        caso("El descuento atomico no permite stock negativo", con -> {
            long producto = numero(con, "SELECT id FROM producto WHERE codigo_barras = ?", "750100000001");
            long stock = numero(con, "SELECT stock FROM producto WHERE id = ?", producto);
            int filas = actualizar(con,
                    "UPDATE producto SET stock = stock - ? WHERE id = ? AND stock >= ?", stock + 1, producto, stock + 1);
            verificar(filas == 0 && numero(con, "SELECT stock FROM producto WHERE id = ?", producto) == stock);
        });
        caso("Rollback revierte venta, detalle, pago y stock juntos", con -> {
            long turno = turno(con, usuarioTemporal(con), caja(con, 1));
            long producto = numero(con, "SELECT id FROM producto WHERE codigo_barras = ?", "750100000001");
            long stock = numero(con, "SELECT stock FROM producto WHERE id = ?", producto);
            Savepoint antes = con.setSavepoint();
            long venta = insertar(con, "INSERT INTO venta (turno_id, total) VALUES (?, ?)", turno, 30);
            insertar(con, "INSERT INTO venta_detalle (venta_id, producto_id, cantidad, precio_unitario, subtotal) "
                    + "VALUES (?, ?, ?, ?, ?)", venta, producto, 1, 30, 30);
            insertar(con, "INSERT INTO pago (venta_id, metodo, monto, recibido, cambio) VALUES (?, ?, ?, ?, ?)",
                    venta, "EFECTIVO", 30, 50, 20);
            verificar(actualizar(con, "UPDATE producto SET stock = stock - 1 WHERE id = ?", producto) == 1);
            verificar(numero(con, "SELECT COUNT(*) FROM venta WHERE id = ?", venta) == 1);
            con.rollback(antes);
            verificar(numero(con, "SELECT stock FROM producto WHERE id = ?", producto) == stock);
            verificar(numero(con, "SELECT COUNT(*) FROM venta WHERE id = ?", venta) == 0);
            verificar(numero(con, "SELECT COUNT(*) FROM venta_detalle WHERE venta_id = ?", venta) == 0);
            verificar(numero(con, "SELECT COUNT(*) FROM pago WHERE venta_id = ?", venta) == 0);
        });
        caso("La tarjeta no admite cambio en efectivo", con -> {
            long venta = venta(con);
            esperaOracle(2290, c -> insertar(c,
                    "INSERT INTO pago (venta_id, metodo, monto, recibido, cambio) VALUES (?, ?, ?, ?, ?)",
                    venta, "TARJETA", 30, 40, 10), con);
        });
        caso("El detalle no admite mas cantidad reversada que vendida", con -> {
            long venta = venta(con);
            long producto = numero(con, "SELECT id FROM producto WHERE codigo_barras = ?", "750100000001");
            long detalle = insertar(con, "INSERT INTO venta_detalle "
                    + "(venta_id, producto_id, cantidad, precio_unitario, subtotal) VALUES (?, ?, ?, ?, ?)",
                    venta, producto, 1, 30, 30);
            esperaOracle(2290, c -> actualizar(c,
                    "UPDATE venta_detalle SET cantidad_reversada = ? WHERE id = ?", 2, detalle), con);
        });
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
        try {
            try (Connection con = Conexion.obtener(clave);
                    PreparedStatement ps = con.prepareStatement(
                            "SELECT SYS_CONTEXT('USERENV','SESSION_USER'), SYS_CONTEXT('USERENV','CON_NAME') FROM dual");
                    ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (!"POS_TEST".equals(rs.getString(1)) || !"XEPDB1".equals(rs.getString(2))) {
                    throw new IllegalStateException("Se requiere el esquema POS_TEST en XEPDB1");
                }
            }
            PruebasOracle pruebas = new PruebasOracle(clave);
            pruebas.ejecutar();
            System.out.printf("%nPruebas Oracle: %d; correctas: %d; fallos: %d%n",
                    pruebas.ejecutadas, pruebas.ejecutadas - pruebas.fallos, pruebas.fallos);
            if (pruebas.fallos != 0) { throw new AssertionError("Hay pruebas Oracle fallidas"); }
        } finally {
            if (clave != null) { Arrays.fill(clave, '\0'); }
        }
    }
}
