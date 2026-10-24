package com.supermercado.pos.util;

import java.io.Console;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;

public final class PruebaConexion {
    private PruebaConexion() { }

    public static void main(String[] args) throws Exception {
        Configuracion configuracion = Configuracion.cargar();
        String variable = System.getenv("POS_DB_PASSWORD");
        char[] clave;
        if (variable != null && !variable.isEmpty()) {
            clave = variable.toCharArray();
        } else {
            Console consola = System.console();
            if (consola == null) {
                throw new IllegalStateException("Ejecuta desde una terminal para ingresar la clave de Oracle");
            }
            clave = consola.readPassword("Clave de Oracle para %s: ", configuracion.usuario());
        }
        try (Connection con = Conexion.obtener(clave)) {
            System.out.println("Oracle: " + con.getMetaData().getDatabaseProductVersion());
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT SYS_CONTEXT('USERENV','SESSION_USER'), "
                    + "SYS_CONTEXT('USERENV','CON_NAME') FROM dual");
                    ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (!"XEPDB1".equals(rs.getString(2))) {
                    throw new IllegalStateException("La conexion debe apuntar a XEPDB1");
                }
                System.out.println("Usuario: " + rs.getString(1) + "; base: " + rs.getString(2));
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM producto WHERE activo = ?")) {
                ps.setInt(1, 1);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    System.out.println("Productos activos: " + rs.getInt(1));
                }
            }
            System.out.println("Conexion JDBC y consulta de productos: OK");
        } finally {
            if (clave != null) {
                Arrays.fill(clave, '\0');
            }
        }
    }
}
