package com.supermercado.pos.pruebas;

import static com.supermercado.pos.pruebas.PruebasInterfaz.*;
import com.supermercado.pos.Main;
import com.supermercado.pos.dominio.Caja;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.ui.AperturaTurnoFrame;
import com.supermercado.pos.ui.AutorizacionDialog;
import com.supermercado.pos.ui.LoginFrame;
import com.supermercado.pos.ui.MenuPrincipalFrame;
import com.supermercado.pos.ui.PosFrame;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Configuracion;
import com.supermercado.pos.util.Sesion;
import java.awt.Window;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** RN-03 en ventanas reales, exclusivamente con datos propios de POS_TEST. */
public final class PruebasInterfazAperturaCero {
    private PruebasInterfazAperturaCero() { }
    private static void verificar(boolean condicion) { if (!condicion) { throw new AssertionError("Resultado inesperado"); } }
    private static AutorizacionDialog autorizar(AperturaTurnoFrame apertura) throws Exception {
        SwingUtilities.invokeLater(() -> campo(apertura, "apertura.abrir", JButton.class).doClick());
        return esperarVentana(AutorizacionDialog.class);
    }
    public static void main(String[] args) throws Exception {
        if (!"pos_test".equalsIgnoreCase(Configuracion.cargar().usuario())) { throw new IllegalStateException("Solo POS_TEST"); }
        try (Connection con = Conexion.obtener(); PreparedStatement ps = con.prepareStatement(
                "SELECT SYS_CONTEXT('USERENV','SESSION_USER'), SYS_CONTEXT('USERENV','CON_NAME') FROM dual"); ResultSet rs = ps.executeQuery()) {
            rs.next(); if (!"POS_TEST".equals(rs.getString(1)) || !"XEPDB1".equals(rs.getString(2))) { throw new IllegalStateException("Solo POS_TEST en XEPDB1"); }
        }
        PruebasAperturaCero datos = new PruebasAperturaCero();
        int correctas = 0;
        // Verifica el escritorio antes de crear datos si Xvfb no pudo arrancar.
        java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        try {
            datos.preparar();
            Main.main(new String[0]);
            ingresar(esperarVentana(LoginFrame.class), datos.datos.ana.getUsername(), "SoloPruebas2026");
            MenuPrincipalFrame menu = esperarVentana(MenuPrincipalFrame.class);
            SwingUtilities.invokeLater(() -> campo(menu, "menu.caja", JButton.class).doClick());
            AperturaTurnoFrame apertura = esperarVentana(AperturaTurnoFrame.class);
            esperar(() -> campo(apertura, "apertura.abrir", JButton.class).isEnabled());
            SwingUtilities.invokeAndWait(() -> {
                JComboBox<?> cajas = campo(apertura, "apertura.caja", JComboBox.class);
                for (int i = 0; i < cajas.getItemCount(); i++) {
                    if (((Caja) cajas.getItemAt(i)).id() == datos.datos.cajas.getFirst()) { cajas.setSelectedIndex(i); break; }
                }
                campo(apertura, "apertura.fondo", JTextField.class).setText("0.00");
            });
            AutorizacionDialog cancelado = autorizar(apertura);
            SwingUtilities.invokeAndWait(() -> campo(cancelado, "autorizacion.cancelar", JButton.class).doClick());
            verificar(datos.servicio.actual(datos.datos.ana).isEmpty());
            verificar(Sesion.getUsuario().getId().equals(datos.datos.ana.getId()));
            System.out.println("OK    Fondo cero solicita autorizacion; cancelar conserva sesion y no abre turno"); correctas++;

            AutorizacionDialog rechazado = autorizar(apertura);
            for (int i = 0; i < 3; i++) {
                SwingUtilities.invokeAndWait(() -> {
                    campo(rechazado, "autorizacion.usuario", JTextField.class).setText(datos.supervisora.getUsername());
                    campo(rechazado, "autorizacion.clave", JPasswordField.class).setText("Incorrecta2026");
                    campo(rechazado, "autorizacion.confirmar", JButton.class).doClick();
                });
                String texto = i == 2 ? "Solicitud terminada" : "Intentos restantes: " + (2 - i);
                esperar(() -> campo(rechazado, "autorizacion.estado", JLabel.class).getText().contains(texto));
                SwingUtilities.invokeAndWait(() -> verificar(campo(rechazado, "autorizacion.clave", JPasswordField.class).getPassword().length == 0));
            }
            SwingUtilities.invokeAndWait(() -> verificar(!campo(rechazado, "autorizacion.confirmar", JButton.class).isEnabled()));
            verificar(datos.servicio.actual(datos.datos.ana).isEmpty());
            verificar(PruebasCaja.numero("SELECT COUNT(*) FROM auditoria_autorizacion WHERE solicitado_por = ? AND exitoso = 0", datos.datos.ana.getId()).intValueExact() == 3);
            capturar(rechazado, "autorizacion-bloqueada.png");
            SwingUtilities.invokeAndWait(() -> campo(rechazado, "autorizacion.cancelar", JButton.class).doClick());
            System.out.println("OK    Tres fallos limpian la clave, bloquean confirmar y quedan auditados"); correctas++;

            AutorizacionDialog valido = autorizar(apertura);
            SwingUtilities.invokeAndWait(() -> {
                campo(valido, "autorizacion.usuario", JTextField.class).setText(datos.supervisora.getUsername());
                campo(valido, "autorizacion.clave", JPasswordField.class).setText("SoloPruebas2026");
            });
            capturar(valido, "autorizacion-apertura.png");
            SwingUtilities.invokeAndWait(() -> campo(valido, "autorizacion.confirmar", JButton.class).doClick());
            PosFrame pos = esperarVentana(PosFrame.class);
            esperar(() -> campo(pos, "pos.agregar", JButton.class).isEnabled());
            Turno turno = datos.servicio.actual(datos.datos.ana).orElseThrow();
            verificar(turno.fondoInicial().signum() == 0 && turno.aperturaAutorizadaPor().equals(datos.supervisora.getId()));
            verificar(Sesion.getUsuario().getId().equals(datos.datos.ana.getId()));
            verificar(PruebasCaja.numero("SELECT COUNT(*) FROM auditoria_autorizacion WHERE solicitado_por = ? AND exitoso = 1", datos.datos.ana.getId()).intValueExact() == 1);
            capturar(pos, "turno-sin-fondo.png");
            System.out.println("OK    Supervisora abre Q0 para el cajero, identifica autorizadora y conserva la sesion"); correctas++;
        } finally {
            try {
                SwingUtilities.invokeAndWait(() -> { for (Window w : Window.getWindows()) { w.dispose(); } });
            } finally {
                try { datos.datos.limpiar(); } finally { Sesion.cerrar(); Conexion.limpiarClaveTemporal(); }
                System.out.printf("%nPruebas interfaz apertura cero: 3; correctas: %d; fallos o pendientes: %d%n", correctas, 3 - correctas);
            }
        }
    }
}
