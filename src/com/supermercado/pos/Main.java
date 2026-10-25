package com.supermercado.pos;

import com.supermercado.pos.ui.LoginFrame;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Configuracion;
import java.io.Console;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() { }

    public static void main(String[] args) throws Exception {
        Configuracion configuracion = Configuracion.cargar();
        String variable = System.getenv("POS_DB_PASSWORD");
        char[] clave;
        if (variable != null && !variable.isEmpty()) { clave = variable.toCharArray(); }
        else {
            Console consola = System.console();
            if (consola == null) { throw new IllegalStateException("Ejecuta scripts/iniciar.ps1 desde una terminal"); }
            clave = consola.readPassword("Clave de Oracle para %s: ", configuracion.usuario());
        }
        try { Conexion.configurarClaveTemporal(clave); }
        finally { if (clave != null) { Arrays.fill(clave, '\0'); } }
        Runtime.getRuntime().addShutdownHook(new Thread(Conexion::limpiarClaveTemporal));
        try (Connection con = Conexion.obtener();
                PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM usuario WHERE username = ?")) {
            ps.setString(1, "admin");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0) { throw new IllegalStateException("Faltan los datos iniciales del proyecto"); }
            }
        }
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
