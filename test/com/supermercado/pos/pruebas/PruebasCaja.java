package com.supermercado.pos.pruebas;

import com.supermercado.pos.dao.RolDAO;
import com.supermercado.pos.dominio.Carrito;
import com.supermercado.pos.dominio.CierreTurno;
import com.supermercado.pos.dominio.Pago;
import com.supermercado.pos.dominio.Producto;
import com.supermercado.pos.dominio.ResumenTurno;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.dominio.Venta;
import com.supermercado.pos.excepcion.AutorizacionException;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.servicio.AuthService;
import com.supermercado.pos.servicio.TurnoService;
import com.supermercado.pos.servicio.UsuarioService;
import com.supermercado.pos.servicio.VentaService;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Configuracion;
import java.io.Console;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Servicios reales y concurrencia JDBC. Crea sus propios usuarios, cajas y productos en POS_TEST. */
public final class PruebasCaja {
    private final TurnoService turnos = new TurnoService();
    private final VentaService ventas = new VentaService();
    final List<Long> usuarios = new ArrayList<>();
    final List<Long> cajas = new ArrayList<>();
    private final List<Long> productos = new ArrayList<>();
    Usuario ana;
    Usuario luis;
    long arroz;
    long leche;
    private int ejecutadas;
    private int fallos;

    @FunctionalInterface private interface Prueba { void ejecutar() throws Exception; }
    private void caso(String nombre, Prueba prueba) {
        ejecutadas++;
        try { limpiarOperaciones(); reiniciarProductos(); prueba.ejecutar(); System.out.println("OK    " + nombre); }
        catch (Exception | AssertionError e) { fallos++; System.out.println("FALLO " + nombre); e.printStackTrace(System.out); }
    }
    static BigDecimal q(String texto) { return new BigDecimal(texto).setScale(2); }
    static void verificar(boolean condicion) { if (!condicion) { throw new AssertionError("Resultado inesperado"); } }
    static void igual(BigDecimal obtenido, String esperado) { verificar(obtenido.compareTo(q(esperado)) == 0); }
    static void rechaza(Class<? extends RuntimeException> tipo, Runnable accion) {
        try { accion.run(); }
        catch (RuntimeException e) { if (tipo.isInstance(e)) { return; } throw e; }
        throw new AssertionError("Se esperaba " + tipo.getSimpleName());
    }
    static void ejecutarSql(String sql, Object... parametros) throws SQLException {
        try (Connection con = Conexion.obtener(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) { ps.setObject(i + 1, parametros[i]); }
            ps.executeUpdate();
        }
    }
    static BigDecimal numero(String sql, Object... parametros) throws SQLException {
        try (Connection con = Conexion.obtener(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) { ps.setObject(i + 1, parametros[i]); }
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        }
    }
    static long insertar(String sql, Object... parametros) throws SQLException {
        try (Connection con = Conexion.obtener(); PreparedStatement ps = con.prepareStatement(sql, new String[] {"ID"})) {
            for (int i = 0; i < parametros.length; i++) { ps.setObject(i + 1, parametros[i]); }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getLong(1); }
        }
    }
    private Turno abrir(Usuario cajero, int caja) { return turnos.abrir(cajero, cajas.get(caja), q("1000")); }
    private Carrito carrito(long id, int cantidad, Usuario cajero) {
        Producto producto = ventas.productos(cajero).stream().filter(p -> p.getId() == id).findFirst().orElseThrow();
        Carrito c = new Carrito(); c.agregar(producto, cantidad); return c;
    }
    private void sinVenta(Turno turno) throws SQLException {
        igual(numero("SELECT COUNT(*) FROM venta WHERE turno_id = ?", turno.id()), "0");
        igual(numero("SELECT stock FROM producto WHERE id = ?", arroz), "30");
    }
    private static boolean intento(Runnable operacion) {
        try { operacion.run(); return true; }
        catch (ReglaNegocioException e) { return false; }
    }
    private static List<Boolean> simultaneo(Callable<Boolean> primera, Callable<Boolean> segunda) throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            Future<Boolean> a = hilos.submit(() -> { salida.await(); return primera.call(); });
            Future<Boolean> b = hilos.submit(() -> { salida.await(); return segunda.call(); });
            salida.countDown();
            return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        } finally { hilos.shutdownNow(); hilos.awaitTermination(30, TimeUnit.SECONDS); }
    }
    private void ejecutar() {
        caso("Apertura atomica guarda turno y un unico movimiento de fondo", () -> {
            Turno t = abrir(ana, 0); verificar(t.abierto() && t.cajeroId() == ana.getId());
            verificar(t.fondoInicial().scale() == 2 && turnos.resumen(t.id(), ana).esperado().scale() == 2);
            igual(numero("SELECT COUNT(*) FROM movimiento_caja WHERE turno_id = ? AND tipo = 'FONDO_INICIAL'", t.id()), "1");
            igual(numero("SELECT monto FROM movimiento_caja WHERE turno_id = ?", t.id()), "1000");
        });
        caso("Caja ocupada rechazada y excluida de la lista disponible", () -> {
            abrir(ana, 0); rechaza(ReglaNegocioException.class, () -> abrir(luis, 0));
            verificar(turnos.actual(luis).isEmpty());
            verificar(turnos.cajasLibres(luis).stream().noneMatch(c -> c.id() == cajas.get(0)));
        });
        caso("El mismo cajero no puede abrir otra caja", () -> {
            Turno t = abrir(ana, 0); rechaza(ReglaNegocioException.class, () -> abrir(ana, 1));
            verificar(turnos.actual(ana).orElseThrow().id() == t.id());
        });
        caso("Fondo cero, negativo, nulo o redondeado a cero rechazado", () -> {
            for (BigDecimal fondo : new BigDecimal[] {q("0"), q("-1"), null, new BigDecimal("0.001")}) {
                rechaza(ReglaNegocioException.class, () -> turnos.abrir(ana, cajas.get(0), fondo));
            }
            verificar(turnos.actual(ana).isEmpty());
        });
        caso("Caja inactiva no puede abrir turno", () -> {
            ejecutarSql("UPDATE caja SET activa = 0 WHERE id = ?", cajas.get(0));
            try { rechaza(ReglaNegocioException.class, () -> abrir(ana, 0)); }
            finally { ejecutarSql("UPDATE caja SET activa = 1 WHERE id = ?", cajas.get(0)); }
        });
        caso("Servicios verifican permisos aunque se omita la interfaz", () -> {
            Usuario sinPermisos = new Usuario(ana.getId(), ana.getUsername(), "Sin roles", ana.getPasswordHash());
            rechaza(AutorizacionException.class, () -> turnos.abrir(sinPermisos, cajas.get(0), q("1000")));
            rechaza(AutorizacionException.class, () -> ventas.productos(sinPermisos));
        });
        caso("Consultar turno recupera la misma apertura sin duplicarla", () -> {
            Turno t = abrir(ana, 0); verificar(turnos.actual(ana).orElseThrow().id() == t.id());
            igual(numero("SELECT COUNT(*) FROM turno WHERE cajero_id = ?", ana.getId()), "1");
        });
        caso("No se puede vender sin turno abierto", () -> {
            Carrito c = carrito(arroz, 1, ana);
            rechaza(ReglaNegocioException.class, () -> ventas.cobrar(-1, c, q("30"), q("0"), ana));
            igual(numero("SELECT stock FROM producto WHERE id = ?", arroz), "30");
        });
        caso("Un cajero no puede cobrar dentro del turno de otro", () -> {
            Turno t = abrir(ana, 0); Carrito c = carrito(arroz, 1, luis);
            rechaza(ReglaNegocioException.class, () -> ventas.cobrar(t.id(), c, q("30"), q("0"), luis)); sinVenta(t);
        });
        caso("Venta efectivo 150, recibido 200, cambio 50: esperado 1150", () -> {
            Turno t = abrir(ana, 0);
            Venta v = ventas.cobrar(t.id(), carrito(arroz, 5, ana), q("200"), q("0"), ana);
            igual(v.total(), "150"); igual(v.pagos().getFirst().monto(), "150"); igual(v.pagos().getFirst().cambio(), "50");
            igual(turnos.resumen(t.id(), ana).esperado(), "1150");
            igual(numero("SELECT stock FROM producto WHERE id = ?", arroz), "25");
            igual(numero("SELECT SUM(monto) FROM pago WHERE venta_id = ?", v.id()), "150");
            igual(v.subtotal().add(v.impuesto()), "150");
        });
        caso("Venta tarjeta 80 no aumenta el efectivo esperado", () -> {
            Turno t = abrir(ana, 0);
            Venta v = ventas.cobrar(t.id(), carrito(leche, 4, ana), q("0"), q("80"), ana);
            verificar(v.pagos().getFirst().metodo() == Pago.Metodo.TARJETA); igual(v.pagos().getFirst().cambio(), "0");
            igual(turnos.resumen(t.id(), ana).esperado(), "1000");
        });
        caso("Mixto 120: tarjeta 50, recibido 100, aplicado 70 y cambio efectivo 30", () -> {
            Turno t = abrir(ana, 0);
            Venta v = ventas.cobrar(t.id(), carrito(arroz, 4, ana), q("100"), q("50"), ana);
            verificar(v.pagos().size() == 2); igual(v.pagos().get(0).monto(), "70"); igual(v.pagos().get(0).cambio(), "30");
            igual(v.pagos().get(1).cambio(), "0"); igual(turnos.resumen(t.id(), ana).esperado(), "1070");
            igual(numero("SELECT SUM(monto) FROM pago WHERE venta_id = ?", v.id()), "120");
        });
        caso("Pago insuficiente no deja venta ni descuenta stock", () -> {
            Turno t = abrir(ana, 0); Carrito c = carrito(arroz, 5, ana);
            rechaza(ReglaNegocioException.class, () -> ventas.cobrar(t.id(), c, q("149"), q("0"), ana)); sinVenta(t);
        });
        caso("Tarjeta que supera el total no puede producir cambio", () -> {
            Turno t = abrir(ana, 0); Carrito c = carrito(arroz, 1, ana);
            rechaza(ReglaNegocioException.class, () -> ventas.cobrar(t.id(), c, q("0"), q("40"), ana)); sinVenta(t);
        });
        caso("Fallo en segunda linea revierte primera linea, detalle y cabecera", () -> {
            Turno t = abrir(ana, 0); Carrito c = carrito(arroz, 1, ana);
            Producto p = ventas.productos(ana).stream().filter(x -> x.getId() == leche).findFirst().orElseThrow(); c.agregar(p, 1);
            ejecutarSql("UPDATE producto SET stock = 0 WHERE id = ?", leche);
            rechaza(ReglaNegocioException.class, () -> ventas.cobrar(t.id(), c, q("50"), q("0"), ana)); sinVenta(t);
            igual(numero("SELECT COUNT(*) FROM venta_detalle WHERE producto_id IN (?, ?)", arroz, leche), "0");
        });
        caso("Producto desactivado despues de armar el carrito no se vende", () -> {
            Turno t = abrir(ana, 0); Carrito c = carrito(arroz, 1, ana);
            ejecutarSql("UPDATE producto SET activo = 0 WHERE id = ?", arroz);
            rechaza(ReglaNegocioException.class, () -> ventas.cobrar(t.id(), c, q("30"), q("0"), ana)); sinVenta(t);
        });
        caso("Precio cambiado antes del cobro exige actualizar carrito", () -> {
            Turno t = abrir(ana, 0); Carrito c = carrito(arroz, 1, ana);
            ejecutarSql("UPDATE producto SET precio = 40 WHERE id = ?", arroz);
            rechaza(ReglaNegocioException.class, () -> ventas.cobrar(t.id(), c, q("30"), q("0"), ana)); sinVenta(t);
        });
        caso("Una venta conserva su precio historico al modificar el catalogo", () -> {
            Turno t = abrir(ana, 0);
            Venta v = ventas.cobrar(t.id(), carrito(arroz, 1, ana), q("30"), q("0"), ana);
            ejecutarSql("UPDATE producto SET precio = 40 WHERE id = ?", arroz);
            igual(numero("SELECT precio_unitario FROM venta_detalle WHERE venta_id = ?", v.id()), "30");
            igual(turnos.resumen(t.id(), ana).esperado(), "1030");
        });
        caso("Carrito vacio no genera una venta", () -> {
            Turno t = abrir(ana, 0);
            rechaza(ReglaNegocioException.class, () -> ventas.cobrar(t.id(), new Carrito(), q("0"), q("0"), ana)); sinVenta(t);
        });
        caso("Cierre con faltante de 20 guarda diferencia -20", () -> {
            Turno t = abrir(ana, 0); ventas.cobrar(t.id(), carrito(arroz, 5, ana), q("200"), q("0"), ana);
            CierreTurno c = turnos.cerrar(t.id(), q("1130"), ana); igual(c.diferencia(), "-20");
            igual(numero("SELECT efectivo_esperado FROM turno WHERE id = ?", t.id()), "1150");
            igual(numero("SELECT diferencia FROM turno WHERE id = ?", t.id()), "-20"); verificar(turnos.actual(ana).isEmpty());
        });
        caso("Turno cerrado rechaza ventas y un segundo cierre", () -> {
            Turno t = abrir(ana, 0); turnos.cerrar(t.id(), q("1000"), ana); Carrito c = carrito(arroz, 1, ana);
            rechaza(ReglaNegocioException.class, () -> ventas.cobrar(t.id(), c, q("30"), q("0"), ana));
            rechaza(ReglaNegocioException.class, () -> turnos.cerrar(t.id(), q("1000"), ana)); sinVenta(t);
        });
        caso("Un cajero no puede cerrar el turno de otro", () -> {
            Turno t = abrir(ana, 0); rechaza(ReglaNegocioException.class, () -> turnos.cerrar(t.id(), q("1000"), luis));
            verificar(turnos.actual(ana).isPresent());
        });
        caso("Despues del cierre se puede reabrir la misma caja con un nuevo turno", () -> {
            Turno t = abrir(ana, 0); turnos.cerrar(t.id(), q("1000"), ana);
            Turno nuevo = abrir(ana, 0); verificar(nuevo.id() != t.id()); igual(turnos.resumen(nuevo.id(), ana).esperado(), "1000");
        });
        caso("Dos aperturas simultaneas en una caja: exactamente una confirma", () -> {
            List<Boolean> resultado = simultaneo(() -> intento(() -> abrir(ana, 0)), () -> intento(() -> abrir(luis, 0)));
            verificar(resultado.stream().filter(Boolean::booleanValue).count() == 1);
            igual(numero("SELECT COUNT(*) FROM turno WHERE caja_id = ? AND estado = 'ABIERTO'", cajas.get(0)), "1");
        });
        caso("Mismo cajero abre dos cajas simultaneas: exactamente una confirma", () -> {
            List<Boolean> resultado = simultaneo(() -> intento(() -> abrir(ana, 0)), () -> intento(() -> abrir(ana, 1)));
            verificar(resultado.stream().filter(Boolean::booleanValue).count() == 1);
            igual(numero("SELECT COUNT(*) FROM turno WHERE cajero_id = ? AND estado = 'ABIERTO'", ana.getId()), "1");
        });
        caso("Dos cajas venden la ultima unidad: una venta y stock cero", () -> {
            Turno a = abrir(ana, 0); Turno b = abrir(luis, 1);
            ejecutarSql("UPDATE producto SET stock = 1 WHERE id = ?", arroz);
            Carrito ca = carrito(arroz, 1, ana); Carrito cb = carrito(arroz, 1, luis);
            List<Boolean> resultado = simultaneo(() -> intento(() -> ventas.cobrar(a.id(), ca, q("30"), q("0"), ana)),
                    () -> intento(() -> ventas.cobrar(b.id(), cb, q("30"), q("0"), luis)));
            verificar(resultado.stream().filter(Boolean::booleanValue).count() == 1);
            igual(numero("SELECT stock FROM producto WHERE id = ?", arroz), "0");
            igual(numero("SELECT COUNT(*) FROM venta WHERE turno_id IN (?, ?)", a.id(), b.id()), "1");
        });
        caso("Cobro y cierre simultaneos mantienen el efectivo esperado consistente", () -> {
            Turno t = abrir(ana, 0); Carrito c = carrito(arroz, 1, ana);
            simultaneo(() -> intento(() -> ventas.cobrar(t.id(), c, q("30"), q("0"), ana)),
                    () -> { turnos.cerrar(t.id(), q("1000"), ana); return true; });
            BigDecimal cantidad = numero("SELECT COUNT(*) FROM venta WHERE turno_id = ?", t.id());
            BigDecimal esperado = q("1000").add(q("30").multiply(cantidad));
            verificar(numero("SELECT efectivo_esperado FROM turno WHERE id = ?", t.id()).compareTo(esperado) == 0);
            rechaza(ReglaNegocioException.class, () -> ventas.cobrar(t.id(), c, q("30"), q("0"), ana));
        });
        caso("Conteo negativo no cierra el turno", () -> {
            Turno t = abrir(ana, 0); rechaza(ReglaNegocioException.class, () -> turnos.cerrar(t.id(), q("-1"), ana));
            verificar(turnos.actual(ana).isPresent());
        });
        caso("Tres ventas del escenario: 350 vendido, 220 efectivo y 130 tarjeta", () -> {
            Turno t = abrir(ana, 0);
            ventas.cobrar(t.id(), carrito(arroz, 5, ana), q("200"), q("0"), ana);
            ventas.cobrar(t.id(), carrito(leche, 4, ana), q("0"), q("80"), ana);
            ventas.cobrar(t.id(), carrito(arroz, 4, ana), q("70"), q("50"), ana);
            ResumenTurno r = turnos.resumen(t.id(), ana);
            verificar(r.ventas() == 3); igual(r.vendido(), "350"); igual(r.efectivo(), "220"); igual(r.tarjeta(), "130"); igual(r.esperado(), "1220");
        });
    }
    void preparar() throws SQLException {
        char[] clave = "Admin123!".toCharArray(); Usuario admin;
        try { admin = new AuthService().autenticar("admin", clave); }
        finally { Arrays.fill(clave, '\0'); }
        long rol;
        try (Connection con = Conexion.obtener()) {
            rol = new RolDAO().listar(con).stream().filter(r -> r.getNombre().equals("CAJERO")).findFirst().orElseThrow().getId();
        }
        String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        clave = "SoloPruebas2026".toCharArray();
        try {
            ana = new UsuarioService().crear("p_caja_a_" + sufijo, "Prueba caja A", clave, rol, admin); usuarios.add(ana.getId());
            luis = new UsuarioService().crear("p_caja_b_" + sufijo, "Prueba caja B", clave, rol, admin); usuarios.add(luis.getId());
        } finally { Arrays.fill(clave, '\0'); }
        for (int i = 9000; cajas.size() < 2 && i <= 9999; i++) {
            if (numero("SELECT COUNT(*) FROM caja WHERE numero = ?", i).signum() == 0) {
                cajas.add(insertar("INSERT INTO caja (numero, nombre) VALUES (?, ?)", i, "Prueba " + sufijo));
            }
        }
        verificar(cajas.size() == 2);
        arroz = insertar("INSERT INTO producto (codigo_barras, nombre, precio, stock) VALUES (?, ?, 30, 30)", "p_a_" + sufijo, "Arroz prueba"); productos.add(arroz);
        leche = insertar("INSERT INTO producto (codigo_barras, nombre, precio, stock) VALUES (?, ?, 20, 30)", "p_l_" + sufijo, "Leche prueba"); productos.add(leche);
    }
    private void reiniciarProductos() throws SQLException {
        ejecutarSql("UPDATE producto SET stock = 30, activo = 1, precio = 30 WHERE id = ?", arroz);
        ejecutarSql("UPDATE producto SET stock = 30, activo = 1, precio = 20 WHERE id = ?", leche);
    }
    void limpiarOperaciones() throws SQLException {
        for (long id : usuarios) {
            String turnosPropios = "SELECT id FROM turno WHERE cajero_id = ?";
            String ventasPropias = "SELECT id FROM venta WHERE turno_id IN (" + turnosPropios + ")";
            for (String sql : List.of(
                    "DELETE FROM pago WHERE venta_id IN (" + ventasPropias + ")",
                    "DELETE FROM venta_detalle WHERE venta_id IN (" + ventasPropias + ")",
                    "DELETE FROM venta WHERE turno_id IN (" + turnosPropios + ")",
                    "DELETE FROM movimiento_caja WHERE turno_id IN (" + turnosPropios + ")",
                    "DELETE FROM turno WHERE cajero_id = ?",
                    "DELETE FROM auditoria_autorizacion WHERE solicitado_por = ?")) { ejecutarSql(sql, id); }
        }
    }
    void limpiar() throws SQLException {
        limpiarOperaciones();
        for (long id : productos) { ejecutarSql("DELETE FROM producto WHERE id = ?", id); }
        for (long id : cajas) { ejecutarSql("DELETE FROM caja WHERE id = ?", id); }
        for (long id : usuarios) {
            ejecutarSql("DELETE FROM usuario_permiso WHERE usuario_id = ?", id);
            ejecutarSql("DELETE FROM usuario_rol WHERE usuario_id = ?", id);
            ejecutarSql("DELETE FROM usuario WHERE id = ?", id);
        }
    }
    public static void main(String[] args) throws Exception {
        if (!"pos_test".equalsIgnoreCase(Configuracion.cargar().usuario())) { throw new IllegalStateException("Solo POS_TEST"); }
        if (System.getenv("POS_DB_PASSWORD") == null || System.getenv("POS_DB_PASSWORD").isEmpty()) {
            Console consola = System.console();
            if (consola == null) { throw new IllegalStateException("Ejecuta en una consola para ingresar la clave de POS_TEST"); }
            char[] clave = consola.readPassword("Clave de Oracle para POS_TEST: ");
            try { Conexion.configurarClaveTemporal(clave); } finally { if (clave != null) { Arrays.fill(clave, '\0'); } }
        }
        try (Connection con = Conexion.obtener(); PreparedStatement ps = con.prepareStatement(
                "SELECT SYS_CONTEXT('USERENV','SESSION_USER'), SYS_CONTEXT('USERENV','CON_NAME') FROM dual"); ResultSet rs = ps.executeQuery()) {
            rs.next();
            if (!"POS_TEST".equals(rs.getString(1)) || !"XEPDB1".equals(rs.getString(2))) { throw new IllegalStateException("Solo POS_TEST en XEPDB1"); }
        }
        PruebasCaja suite = new PruebasCaja();
        try { suite.preparar(); suite.ejecutar(); }
        finally { try { suite.limpiar(); } finally { Conexion.limpiarClaveTemporal(); } }
        System.out.printf("%nPruebas caja: %d; correctas: %d; fallos: %d%n", suite.ejecutadas, suite.ejecutadas - suite.fallos, suite.fallos);
        if (suite.fallos > 0) { throw new AssertionError("Fallaron pruebas de caja"); }
    }
}
