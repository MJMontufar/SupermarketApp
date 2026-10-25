package com.supermercado.pos.pruebas;

import static com.supermercado.pos.pruebas.PruebasInterfaz.*;
import com.supermercado.pos.Main;
import com.supermercado.pos.dominio.Caja;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.servicio.TurnoService;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Configuracion;
import com.supermercado.pos.ui.AperturaTurnoFrame;
import com.supermercado.pos.ui.CierreTurnoFrame;
import com.supermercado.pos.ui.LoginFrame;
import com.supermercado.pos.ui.MenuPrincipalFrame;
import com.supermercado.pos.ui.PagoDialog;
import com.supermercado.pos.ui.PosFrame;
import com.supermercado.pos.ui.TicketDialog;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Recorrido real de Swing: apertura, carrito, cobro, comprobante, reanudacion y cierre. */
public final class PruebasInterfazCaja {
    private PruebasInterfazCaja() { }
    private static int correctas;
    private static void verificar(boolean condicion) { if (!condicion) { throw new AssertionError("Resultado inesperado"); } }
    private static void ok(String nombre) { correctas++; System.out.println("OK    " + nombre); }
    private static void agregar(PosFrame pos, String codigo, int cantidad) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            campo(pos, "pos.cantidad", JSpinner.class).setValue(cantidad);
            JTextField lector = campo(pos, "pos.codigo", JTextField.class);
            lector.setText(codigo); lector.postActionEvent();
        });
    }
    private static PagoDialog pago(PosFrame pos) throws Exception {
        SwingUtilities.invokeLater(() -> campo(pos, "pos.cobrar", JButton.class).doClick());
        return esperarVentana(PagoDialog.class);
    }
    private static TicketDialog confirmar(PagoDialog pago) throws Exception {
        SwingUtilities.invokeAndWait(() -> campo(pago, "pago.confirmar", JButton.class).doClick());
        return esperarVentana(TicketDialog.class);
    }
    private static void cerrarTicket(TicketDialog ticket) throws Exception {
        SwingUtilities.invokeAndWait(() -> campo(ticket, "ticket.cerrar", JButton.class).doClick());
    }
    private static JButton boton(Component raiz, String texto) {
        if (raiz instanceof JButton b && texto.equals(b.getText())) { return b; }
        if (raiz instanceof Container c) {
            for (Component hijo : c.getComponents()) { JButton b = boton(hijo, texto); if (b != null) { return b; } }
        }
        return null;
    }
    private static JDialog confirmacion() {
        for (Window w : Window.getWindows()) {
            if (w.isShowing() && w instanceof JDialog d && "Cerrar turno".equals(d.getTitle())) { return d; }
        }
        return null;
    }
    public static void main(String[] args) throws Exception {
        if (!"pos_test".equalsIgnoreCase(Configuracion.cargar().usuario())) { throw new IllegalStateException("Solo POS_TEST"); }
        try (Connection con = Conexion.obtener(); PreparedStatement ps = con.prepareStatement(
                "SELECT SYS_CONTEXT('USERENV','SESSION_USER'), SYS_CONTEXT('USERENV','CON_NAME') FROM dual"); ResultSet rs = ps.executeQuery()) {
            rs.next();
            if (!"POS_TEST".equals(rs.getString(1)) || !"XEPDB1".equals(rs.getString(2))) { throw new IllegalStateException("Solo POS_TEST en XEPDB1"); }
        }
        PruebasCaja datos = new PruebasCaja();
        try {
            datos.preparar();
            String codigo;
            String codigoLeche;
            try (Connection con = Conexion.obtener(); PreparedStatement ps = con.prepareStatement("SELECT codigo_barras FROM producto WHERE id = ?")) {
                ps.setLong(1, datos.arroz);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); codigo = rs.getString(1); }
                ps.setLong(1, datos.leche);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); codigoLeche = rs.getString(1); }
            }
            Main.main(new String[0]);
            ingresar(esperarVentana(LoginFrame.class), datos.ana.getUsername(), "SoloPruebas2026");
            MenuPrincipalFrame menu = esperarVentana(MenuPrincipalFrame.class);
            SwingUtilities.invokeLater(() -> campo(menu, "menu.caja", JButton.class).doClick());
            AperturaTurnoFrame apertura = esperarVentana(AperturaTurnoFrame.class);
            esperar(() -> campo(apertura, "apertura.abrir", JButton.class).isEnabled());
            SwingUtilities.invokeAndWait(() -> {
                JComboBox<?> cajas = campo(apertura, "apertura.caja", JComboBox.class);
                for (int i = 0; i < cajas.getItemCount(); i++) {
                    if (((Caja) cajas.getItemAt(i)).id() == datos.cajas.getFirst()) { cajas.setSelectedIndex(i); break; }
                }
                campo(apertura, "apertura.fondo", JTextField.class).setText("1000.00");
            });
            capturar(apertura, "apertura.png");
            SwingUtilities.invokeAndWait(() -> campo(apertura, "apertura.abrir", JButton.class).doClick());
            PosFrame pos = esperarVentana(PosFrame.class);
            esperar(() -> campo(pos, "pos.agregar", JButton.class).isEnabled());
            Turno turno = new TurnoService().actual(datos.ana).orElseThrow();
            verificar(turno.caja().id() == datos.cajas.getFirst());
            ok("Apertura desde el formulario crea turno y abre punto de venta");

            agregar(pos, codigo, 5);
            SwingUtilities.invokeAndWait(() -> {
                JTable tabla = campo(pos, "pos.carrito", JTable.class);
                verificar(tabla.getRowCount() == 1);
                verificar(tabla.editCellAt(0, 1));
                ((JTextField) tabla.getEditorComponent()).setText("4");
                verificar(tabla.getCellEditor().stopCellEditing());
                verificar(campo(pos, "pos.totales", JLabel.class).getText().contains("TOTAL: Q 120.00"));
                verificar(tabla.editCellAt(0, 1));
                ((JTextField) tabla.getEditorComponent()).setText("5");
                verificar(tabla.getCellEditor().stopCellEditing());
                verificar(campo(pos, "pos.totales", JLabel.class).getText().contains("TOTAL: Q 150.00"));
            });
            capturar(pos, "carrito.png");
            ok("Codigo de barras y edicion de cantidad recalculan carrito e impuesto incluido");

            PagoDialog efectivo = pago(pos);
            SwingUtilities.invokeAndWait(() -> {
                campo(efectivo, "pago.efectivo", JTextField.class).setText("149");
                verificar(!campo(efectivo, "pago.confirmar", JButton.class).isEnabled());
                campo(efectivo, "pago.efectivo", JTextField.class).setText("200");
                verificar(campo(efectivo, "pago.cambio", JLabel.class).getText().equals("Cambio: Q 50.00"));
            });
            capturar(efectivo, "pago-efectivo.png");
            TicketDialog ticket = confirmar(efectivo);
            SwingUtilities.invokeAndWait(() -> verificar(campo(ticket, "ticket.texto", JTextArea.class).getText().contains("Cambio: Q 50.00")));
            capturar(ticket, "ticket.png"); cerrarTicket(ticket);
            esperar(() -> campo(pos, "pos.agregar", JButton.class).isEnabled());
            verificar(new TurnoService().resumen(turno.id(), datos.ana).esperado().toPlainString().equals("1150.00"));
            ok("Pago insuficiente bloqueado; efectivo valido guarda venta y muestra comprobante con cambio");

            agregar(pos, codigo, 4);
            PagoDialog mixto = pago(pos);
            SwingUtilities.invokeAndWait(() -> {
                campo(mixto, "pago.metodo", JComboBox.class).setSelectedItem("MIXTO");
                campo(mixto, "pago.tarjeta", JTextField.class).setText("50");
                campo(mixto, "pago.efectivo", JTextField.class).setText("100");
                verificar(campo(mixto, "pago.cambio", JLabel.class).getText().equals("Cambio: Q 30.00"));
            });
            capturar(mixto, "pago-mixto.png"); cerrarTicket(confirmar(mixto));
            esperar(() -> campo(pos, "pos.agregar", JButton.class).isEnabled());
            verificar(new TurnoService().resumen(turno.id(), datos.ana).esperado().toPlainString().equals("1220.00"));
            ok("Pago mixto muestra cambio solo del efectivo y conserva el saldo de caja");

            agregar(pos, codigoLeche, 4);
            PagoDialog tarjeta = pago(pos);
            SwingUtilities.invokeAndWait(() -> {
                campo(tarjeta, "pago.metodo", JComboBox.class).setSelectedItem("TARJETA");
                verificar(!campo(tarjeta, "pago.efectivo", JTextField.class).isEnabled());
                verificar(campo(tarjeta, "pago.tarjeta", JTextField.class).getText().equals("80.00"));
                verificar(campo(tarjeta, "pago.cambio", JLabel.class).getText().equals("Cambio: Q 0.00"));
            });
            cerrarTicket(confirmar(tarjeta));
            esperar(() -> campo(pos, "pos.agregar", JButton.class).isEnabled());
            verificar(new TurnoService().resumen(turno.id(), datos.ana).esperado().toPlainString().equals("1220.00"));
            ok("Tarjeta completa el total automaticamente sin aumentar el efectivo de caja");

            SwingUtilities.invokeAndWait(() -> pos.dispatchEvent(new WindowEvent(pos, WindowEvent.WINDOW_CLOSING)));
            esperar(() -> menu.isShowing());
            SwingUtilities.invokeLater(() -> campo(menu, "menu.caja", JButton.class).doClick());
            PosFrame continuado = esperarVentana(PosFrame.class);
            esperar(() -> campo(continuado, "pos.agregar", JButton.class).isEnabled());
            verificar(new TurnoService().actual(datos.ana).orElseThrow().id() == turno.id());
            ok("Volver al menu y continuar recupera el mismo turno sin otra apertura");

            SwingUtilities.invokeLater(() -> campo(continuado, "pos.cerrar", JButton.class).doClick());
            CierreTurnoFrame cierre = esperarVentana(CierreTurnoFrame.class);
            esperar(() -> campo(cierre, "cierre.confirmar", JButton.class).isEnabled());
            SwingUtilities.invokeAndWait(() -> campo(cierre, "cierre.contado", JTextField.class).setText("1200"));
            capturar(cierre, "cierre.png");
            SwingUtilities.invokeLater(() -> campo(cierre, "cierre.confirmar", JButton.class).doClick());
            esperar(() -> confirmacion() != null);
            SwingUtilities.invokeAndWait(() -> boton(confirmacion(), UIManager.getString("OptionPane.yesButtonText")).doClick());
            TicketDialog reporte = esperarVentana(TicketDialog.class);
            SwingUtilities.invokeAndWait(() -> {
                String texto = campo(reporte, "ticket.texto", JTextArea.class).getText();
                verificar(texto.contains("-20.00") && texto.contains("CERRADO"));
            });
            capturar(reporte, "reporte-cierre.png"); cerrarTicket(reporte);
            esperar(() -> menu.isShowing());
            verificar(new TurnoService().actual(datos.ana).isEmpty());
            ok("Cierre grafico guarda diferencia -20, muestra reporte y vuelve al menu");
        } finally {
            SwingUtilities.invokeAndWait(() -> { for (Window w : Window.getWindows()) { w.dispose(); } });
            try { datos.limpiar(); } finally { Conexion.limpiarClaveTemporal(); }
            System.out.printf("%nPruebas interfaz caja: 7; correctas: %d; fallos o pendientes: %d%n", correctas, 7 - correctas);
        }
    }
}
