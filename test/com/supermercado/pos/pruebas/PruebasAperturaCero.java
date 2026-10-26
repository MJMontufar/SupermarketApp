package com.supermercado.pos.pruebas;

import static com.supermercado.pos.pruebas.PruebasCaja.*;
import com.supermercado.pos.dao.RolDAO;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.AutorizacionException;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.servicio.AuthService;
import com.supermercado.pos.servicio.TurnoService;
import com.supermercado.pos.servicio.UsuarioService;
import com.supermercado.pos.util.ActualizarEsquema;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Configuracion;
import com.supermercado.pos.util.Sesion;
import java.io.Console;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

/** RN-03: excepcion autorizada, limites de intentos, auditoria y restricciones reales. */
public final class PruebasAperturaCero {
    final PruebasCaja datos = new PruebasCaja();
    final TurnoService servicio = new TurnoService();
    Usuario supervisora;
    Usuario gerente;
    private int ejecutadas;
    private int fallos;
    @FunctionalInterface private interface Prueba { void ejecutar() throws Exception; }
    private void caso(String nombre, Prueba prueba) {
        ejecutadas++;
        try { datos.limpiarOperaciones(); Sesion.setUsuario(datos.ana); prueba.ejecutar(); System.out.println("OK    " + nombre); }
        catch (Exception | AssertionError e) { fallos++; System.out.println("FALLO " + nombre); e.printStackTrace(System.out); }
    }
    private TurnoService.AperturaCero solicitud(Usuario actor, int caja) { return servicio.solicitarAperturaCero(actor, datos.cajas.get(caja)); }
    private Turno confirmar(TurnoService.AperturaCero solicitud, Usuario supervisor) {
        char[] clave = "SoloPruebas2026".toCharArray();
        try { return solicitud.confirmar(supervisor.getUsername(), clave); }
        finally { for (char caracter : clave) { verificar(caracter == '\0'); } }
    }
    private long auditorias(Usuario actor, boolean exitoso) throws SQLException {
        return numero("SELECT COUNT(*) FROM auditoria_autorizacion WHERE solicitado_por = ? AND exitoso = ?", actor.getId(), exitoso ? 1 : 0).longValueExact();
    }
    private static void rechazaSql(int codigo, String sql, Object... parametros) throws SQLException {
        try { ejecutarSql(sql, parametros); }
        catch (SQLException e) { if (e.getErrorCode() == codigo) { return; } throw e; }
        throw new AssertionError("Se esperaba ORA-" + codigo);
    }
    void preparar() throws Exception {
        datos.preparar();
        char[] clave = "Admin123!".toCharArray(); Usuario admin;
        try { admin = new AuthService().autenticar("admin", clave); } finally { Arrays.fill(clave, '\0'); }
        long rolSupervisor; long rolGerente;
        try (Connection con = Conexion.obtener()) {
            var roles = new RolDAO().listar(con);
            rolSupervisor = roles.stream().filter(r -> r.getNombre().equals("SUPERVISOR")).findFirst().orElseThrow().getId();
            rolGerente = roles.stream().filter(r -> r.getNombre().equals("GERENTE")).findFirst().orElseThrow().getId();
        }
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        clave = "SoloPruebas2026".toCharArray();
        try {
            supervisora = new UsuarioService().crear("p_super_" + sufijo, "Supervisora de prueba", clave, rolSupervisor, admin);
            datos.usuarios.add(supervisora.getId());
            gerente = new UsuarioService().crear("p_gerente_" + sufijo, "Gerente de prueba", clave, rolGerente, admin);
            datos.usuarios.add(gerente.getId());
        } finally { Arrays.fill(clave, '\0'); }
    }
    private void ejecutar() {
        caso("Sin autorizacion, el fondo cero sigue rechazado", () -> {
            rechaza(ReglaNegocioException.class, () -> servicio.abrir(datos.ana, datos.cajas.get(0), q("0")));
            verificar(servicio.actual(datos.ana).isEmpty() && auditorias(datos.ana, true) == 0);
        });
        caso("Supervisora autoriza Q0 y queda identificada en turno y fondo", () -> {
            Turno t = confirmar(solicitud(datos.ana, 0), supervisora);
            igual(t.fondoInicial(), "0"); verificar(t.aperturaAutorizadaPor().equals(supervisora.getId()));
            verificar(numero("SELECT autorizado_por FROM movimiento_caja WHERE turno_id = ?", t.id()).longValueExact() == supervisora.getId());
            igual(numero("SELECT monto FROM movimiento_caja WHERE turno_id = ?", t.id()), "0");
            verificar(auditorias(datos.ana, true) == 1 && Sesion.getUsuario() == datos.ana);
        });
        caso("Clave incorrecta no crea turno y registra rechazo", () -> {
            var s = solicitud(datos.ana, 0); char[] clave = "Incorrecta2026".toCharArray();
            rechaza(AutorizacionException.class, () -> s.confirmar(supervisora.getUsername(), clave));
            verificar(s.intentosRestantes() == 2 && auditorias(datos.ana, false) == 1 && servicio.actual(datos.ana).isEmpty());
            for (char caracter : clave) { verificar(caracter == '\0'); }
        });
        caso("Usuario inexistente no autoriza y el intento queda auditado", () -> {
            rechaza(AutorizacionException.class, () -> solicitud(datos.ana, 0).confirmar("inexistente_" + UUID.randomUUID(), "Clave2026".toCharArray()));
            verificar(auditorias(datos.ana, false) == 1 && servicio.actual(datos.ana).isEmpty());
        });
        caso("Otro cajero no puede autorizar aunque su clave sea correcta", () -> {
            rechaza(AutorizacionException.class, () -> confirmar(solicitud(datos.ana, 0), datos.luis));
            verificar(auditorias(datos.ana, false) == 1);
        });
        caso("El cajero no puede autorizarse a si mismo", () -> {
            rechaza(AutorizacionException.class, () -> confirmar(solicitud(datos.ana, 0), datos.ana));
            verificar(servicio.actual(datos.ana).isEmpty() && Sesion.getUsuario() == datos.ana);
        });
        caso("Una supervisora tampoco puede autorizarse a si misma", () -> {
            rechaza(AutorizacionException.class, () -> confirmar(solicitud(supervisora, 0), supervisora));
            verificar(servicio.actual(supervisora).isEmpty());
        });
        caso("Se exige nivel estrictamente superior al solicitante", () -> {
            rechaza(AutorizacionException.class, () -> confirmar(solicitud(gerente, 0), supervisora));
            verificar(servicio.actual(gerente).isEmpty());
        });
        caso("Gerente puede autorizar apertura de una supervisora", () -> {
            Turno t = confirmar(solicitud(supervisora, 0), gerente);
            verificar(t.cajeroId() == supervisora.getId() && t.aperturaAutorizadaPor().equals(gerente.getId()));
        });
        caso("Supervisora inactiva no autoriza", () -> {
            ejecutarSql("UPDATE usuario SET activo = 0 WHERE id = ?", supervisora.getId());
            try { rechaza(AutorizacionException.class, () -> confirmar(solicitud(datos.ana, 0), supervisora)); }
            finally { ejecutarSql("UPDATE usuario SET activo = 1 WHERE id = ?", supervisora.getId()); }
            verificar(auditorias(datos.ana, false) == 1);
        });
        caso("Permiso extra no convierte un cajero en supervisor", () -> {
            ejecutarSql("INSERT INTO usuario_permiso (usuario_id, permiso_id) SELECT ?, id FROM permiso WHERE codigo = 'REVERSION_AUTORIZAR'", datos.luis.getId());
            try { rechaza(AutorizacionException.class, () -> confirmar(solicitud(datos.ana, 0), datos.luis)); }
            finally { ejecutarSql("DELETE FROM usuario_permiso WHERE usuario_id = ?", datos.luis.getId()); }
        });
        caso("Tres fallos agotan solicitud; cuarto intento correcto no abre", () -> {
            var s = solicitud(datos.ana, 0);
            for (int i = 0; i < 3; i++) {
                rechaza(AutorizacionException.class, () -> s.confirmar(supervisora.getUsername(), "Incorrecta2026".toCharArray()));
            }
            verificar(s.terminada() && s.intentosRestantes() == 0);
            rechaza(AutorizacionException.class, () -> confirmar(s, supervisora));
            verificar(auditorias(datos.ana, false) == 3 && auditorias(datos.ana, true) == 0 && servicio.actual(datos.ana).isEmpty());
        });
        caso("Una autorizacion exitosa es de un solo uso", () -> {
            var s = solicitud(datos.ana, 0); Turno t = confirmar(s, supervisora);
            rechaza(AutorizacionException.class, () -> confirmar(s, supervisora));
            verificar(s.terminada() && servicio.actual(datos.ana).orElseThrow().id() == t.id() && auditorias(datos.ana, true) == 1);
        });
        caso("Caja ocupada revierte apertura; conserva auditoria de credenciales aceptadas", () -> {
            servicio.abrir(datos.ana, datos.cajas.get(0), q("1000"));
            rechaza(ReglaNegocioException.class, () -> confirmar(solicitud(datos.luis, 0), supervisora));
            verificar(servicio.actual(datos.luis).isEmpty() && auditorias(datos.luis, true) == 1);
            igual(servicio.actual(datos.ana).orElseThrow().fondoInicial(), "1000");
        });
        caso("Caja inexistente no crea turno ni movimiento tras autorizar", () -> {
            var s = servicio.solicitarAperturaCero(datos.ana, -1);
            rechaza(ReglaNegocioException.class, () -> confirmar(s, supervisora));
            verificar(servicio.actual(datos.ana).isEmpty() && auditorias(datos.ana, true) == 1);
        });
        caso("La solicitud queda ligada a la caja seleccionada", () -> {
            Turno t = confirmar(solicitud(datos.ana, 1), supervisora);
            verificar(t.caja().id() == datos.cajas.get(1));
        });
        caso("Un solicitante sin permiso no puede iniciar la excepcion", () -> {
            Usuario sinPermisos = new Usuario(datos.ana.getId(), datos.ana.getUsername(), "Sin roles", datos.ana.getPasswordHash());
            rechaza(AutorizacionException.class, () -> solicitud(sinPermisos, 0));
            verificar(auditorias(datos.ana, false) == 0 && auditorias(datos.ana, true) == 0);
        });
        caso("Oracle rechaza Q0 sin autorizador y autoautorizacion por SQL", () -> {
            rechazaSql(2290, "INSERT INTO turno (caja_id, cajero_id, fondo_inicial) VALUES (?, ?, 0)", datos.cajas.get(0), datos.ana.getId());
            rechazaSql(2290, "INSERT INTO turno (caja_id, cajero_id, fondo_inicial, apertura_autorizada_por) VALUES (?, ?, 0, ?)", datos.cajas.get(0), datos.ana.getId(), datos.ana.getId());
            verificar(servicio.actual(datos.ana).isEmpty());
        });
        caso("Oracle rechaza fondo negativo incluso con autorizador", () -> {
            rechazaSql(2290, "INSERT INTO turno (caja_id, cajero_id, fondo_inicial, apertura_autorizada_por) VALUES (?, ?, -1, ?)", datos.cajas.get(0), datos.ana.getId(), supervisora.getId());
        });
        caso("Oracle exige que el autorizador exista", () -> {
            rechazaSql(2291, "INSERT INTO turno (caja_id, cajero_id, fondo_inicial, apertura_autorizada_por) VALUES (?, ?, 0, -1)", datos.cajas.get(0), datos.ana.getId());
        });
        caso("Fondo positivo minimo sigue abriendo sin supervisor", () -> {
            Turno t = servicio.abrir(datos.ana, datos.cajas.get(0), q("0.01"));
            igual(t.fondoInicial(), "0.01"); verificar(t.aperturaAutorizadaPor() == null && auditorias(datos.ana, true) == 0);
        });
        caso("Migracion repetida conserva turnos, usuarios y auditoria existentes", () -> {
            Turno t = confirmar(solicitud(datos.ana, 0), supervisora);
            long usuarios = numero("SELECT COUNT(*) FROM usuario").longValueExact();
            try (Connection con = Conexion.obtener()) { ActualizarEsquema.aplicar(con); ActualizarEsquema.aplicar(con); }
            verificar(servicio.actual(datos.ana).orElseThrow().id() == t.id());
            verificar(numero("SELECT COUNT(*) FROM usuario").longValueExact() == usuarios && auditorias(datos.ana, true) == 1);
        });
        caso("Entradas largas o SQL como username se rechazan sin romper auditoria", () -> {
            var s = solicitud(datos.ana, 0);
            rechaza(AutorizacionException.class, () -> s.confirmar("😀".repeat(40), "Clave2026".toCharArray()));
            rechaza(AutorizacionException.class, () -> s.confirmar("' OR '1'='1", "Clave2026".toCharArray()));
            verificar(auditorias(datos.ana, false) == 2);
            verificar(numero("SELECT MAX(LENGTHB(username_ingresado)) FROM auditoria_autorizacion WHERE solicitado_por = ?", datos.ana.getId()).intValueExact() <= 50);
        });
        caso("Abrir y cerrar con Q0 autorizado calcula esperado y diferencia cero", () -> {
            Turno t = confirmar(solicitud(datos.ana, 0), supervisora);
            var cierre = servicio.cerrar(t.id(), q("0"), datos.ana);
            igual(cierre.resumen().esperado(), "0"); igual(cierre.diferencia(), "0");
            verificar(servicio.actual(datos.ana).isEmpty());
        });
    }
    public static void main(String[] args) throws Exception {
        if (!"pos_test".equalsIgnoreCase(Configuracion.cargar().usuario())) { throw new IllegalStateException("Solo POS_TEST"); }
        if (System.getenv("POS_DB_PASSWORD") == null || System.getenv("POS_DB_PASSWORD").isEmpty()) {
            Console consola = System.console();
            if (consola == null) { throw new IllegalStateException("Ejecuta en consola para ingresar la clave de POS_TEST"); }
            char[] clave = consola.readPassword("Clave de Oracle para POS_TEST: ");
            try { Conexion.configurarClaveTemporal(clave); } finally { if (clave != null) { Arrays.fill(clave, '\0'); } }
        }
        try (Connection con = Conexion.obtener(); PreparedStatement ps = con.prepareStatement(
                "SELECT SYS_CONTEXT('USERENV','SESSION_USER'), SYS_CONTEXT('USERENV','CON_NAME') FROM dual"); ResultSet rs = ps.executeQuery()) {
            rs.next(); if (!"POS_TEST".equals(rs.getString(1)) || !"XEPDB1".equals(rs.getString(2))) { throw new IllegalStateException("Solo POS_TEST en XEPDB1"); }
        }
        PruebasAperturaCero suite = new PruebasAperturaCero();
        try { suite.preparar(); suite.ejecutar(); }
        finally { try { suite.datos.limpiar(); } finally { Sesion.cerrar(); Conexion.limpiarClaveTemporal(); } }
        System.out.printf("%nPruebas apertura cero: %d; correctas: %d; fallos: %d%n", suite.ejecutadas, suite.ejecutadas-suite.fallos, suite.fallos);
        if (suite.fallos > 0) { throw new AssertionError("Fallaron pruebas de apertura cero"); }
    }
}
