package com.supermercado.pos.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;

public final class Conexion {
    private static char[] claveTemporal;
    private Conexion() { }

    /** Solo la clave tecnica de Oracle, durante la ejecucion de la aplicacion. */
    public static synchronized void configurarClaveTemporal(char[] clave) {
        if (clave == null || clave.length == 0) {
            throw new IllegalArgumentException("La clave de Oracle es obligatoria");
        }
        limpiarClaveTemporal();
        claveTemporal = clave.clone();
    }

    public static synchronized void limpiarClaveTemporal() {
        if (claveTemporal != null) { Arrays.fill(claveTemporal, '\0'); }
        claveTemporal = null;
    }

    private static synchronized char[] copiarClaveTemporal() {
        return claveTemporal == null ? null : claveTemporal.clone();
    }

    /** Para procesos automatizados con una variable local ya configurada. */
    public static Connection obtener() throws SQLException {
        String valor = System.getenv("POS_DB_PASSWORD");
        char[] clave = valor == null || valor.isEmpty() ? copiarClaveTemporal() : valor.toCharArray();
        if (clave == null) {
            throw new IllegalStateException("Falta POS_DB_PASSWORD o usa obtener(char[] clave)");
        }
        try {
            return obtener(clave);
        } finally {
            Arrays.fill(clave, '\0');
        }
    }

    /** El llamador conserva la responsabilidad de limpiar su char[]. */
    public static Connection obtener(char[] clave) throws SQLException {
        if (clave == null || clave.length == 0) {
            throw new IllegalArgumentException("La clave de Oracle es obligatoria");
        }
        Configuracion configuracion = Configuracion.cargar();
        Properties propiedades = new Properties();
        propiedades.setProperty("user", configuracion.usuario());
        // JDBC necesita un String; no lo conservamos en atributos ni archivos.
        propiedades.setProperty("password", new String(clave));
        propiedades.setProperty("oracle.net.CONNECT_TIMEOUT", "5000");
        propiedades.setProperty("oracle.jdbc.ReadTimeout", "15000");
        try {
            return DriverManager.getConnection(configuracion.url(), propiedades);
        } finally {
            propiedades.clear();
        }
    }
}
