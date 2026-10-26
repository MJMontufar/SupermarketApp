package com.supermercado.pos.util;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;

/** Ejecuta el mismo bloque PL/SQL de sql/04 sin depender de rutas de SQL*Plus. */
public final class ActualizarEsquema {
    private ActualizarEsquema() { }
    public static void aplicar(Connection con) throws Exception {
        Path config = Path.of(System.getProperty("pos.config", "config.properties")).toAbsolutePath();
        Path archivo = config.getParent().resolve("sql/04_apertura_cero_autorizada.sql");
        String contenido = Files.readString(archivo, StandardCharsets.UTF_8);
        String inicio = "-- INICIO MIGRACION";
        String fin = "-- FIN MIGRACION";
        int desde = contenido.indexOf(inicio);
        int hasta = contenido.indexOf(fin);
        if (desde < 0 || hasta <= desde) { throw new IllegalStateException("Archivo de actualizacion incompleto"); }
        String bloque = contenido.substring(desde + inicio.length(), hasta).trim();
        try (Statement ps = con.createStatement()) { ps.execute(bloque); }
    }
    public static void main(String[] args) throws Exception {
        Configuracion config = Configuracion.cargar();
        String variable = System.getenv("POS_DB_PASSWORD");
        char[] clave;
        if (variable != null && !variable.isEmpty()) { clave = variable.toCharArray(); }
        else {
            Console consola = System.console();
            if (consola == null) { throw new IllegalStateException("Ejecuta scripts/actualizar-bd.ps1 desde una terminal"); }
            clave = consola.readPassword("Clave de Oracle para %s: ", config.usuario());
        }
        try (Connection con = Conexion.obtener(clave)) {
            aplicar(con);
            System.out.println("Actualizacion lista para " + config.usuario() + ": fondo cero con autorizacion. Datos conservados.");
        } finally { if (clave != null) { Arrays.fill(clave, '\0'); } }
    }
}
