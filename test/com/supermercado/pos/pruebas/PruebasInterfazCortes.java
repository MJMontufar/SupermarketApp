package com.supermercado.pos.pruebas;

import static com.supermercado.pos.pruebas.PruebasInterfaz.*;
import static com.supermercado.pos.pruebas.PruebasInterfazCaja.cerrarTicket;
import com.supermercado.pos.Main;
import com.supermercado.pos.dominio.Carrito;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.dominio.Venta;
import com.supermercado.pos.servicio.VentaService;
import com.supermercado.pos.ui.*;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Sesion;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Escenario completo con retiro, autorizacion denegada/aceptada, reversion y cierre desde Swing. */
public final class PruebasInterfazCortes {
    private PruebasInterfazCortes() { }
    private static void verificar(boolean condicion){if(!condicion){throw new AssertionError("Resultado inesperado");}}
    private static void credenciales(AutorizacionDialog dialogo,String username) throws Exception {
        SwingUtilities.invokeAndWait(()->{
            campo(dialogo,"autorizacion.usuario",JTextField.class).setText(username);
            campo(dialogo,"autorizacion.clave",JPasswordField.class).setText("SoloPruebas2026");
            campo(dialogo,"autorizacion.confirmar",JButton.class).doClick();
        });
    }
    public static void main(String[] args) throws Exception {
        PruebasNegocio.iniciarConexion();
        java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        PruebasAperturaCero datos=new PruebasAperturaCero(); int correctas=0;
        try{
            datos.preparar(); var ana=datos.datos.ana; VentaService ventas=new VentaService();
            Turno turno=datos.servicio.abrir(ana,datos.datos.cajas.get(0),PruebasCaja.q("1000"));
            var arroz=ventas.productos(ana).stream().filter(p->p.getId()==datos.datos.arroz).findFirst().orElseThrow();
            var leche=ventas.productos(ana).stream().filter(p->p.getId()==datos.datos.leche).findFirst().orElseThrow();
            Carrito c=new Carrito(); c.agregar(arroz,5); Venta primera=ventas.cobrar(turno.id(),c,PruebasCaja.q("200"),PruebasCaja.q("0"),ana);
            c.vaciar(); c.agregar(leche,4); ventas.cobrar(turno.id(),c,PruebasCaja.q("0"),PruebasCaja.q("80"),ana);
            c.vaciar(); c.agregar(arroz,4); ventas.cobrar(turno.id(),c,PruebasCaja.q("70"),PruebasCaja.q("50"),ana);
            Main.main(new String[0]); ingresar(esperarVentana(LoginFrame.class),ana.getUsername(),"SoloPruebas2026");
            MenuPrincipalFrame menu=esperarVentana(MenuPrincipalFrame.class);
            SwingUtilities.invokeLater(()->campo(menu,"menu.caja",JButton.class).doClick());
            PosFrame pos=esperarVentana(PosFrame.class); esperar(()->campo(pos,"pos.retiro",JButton.class).isEnabled());
            SwingUtilities.invokeLater(()->campo(pos,"pos.retiro",JButton.class).doClick());
            CorteIntermedioDialog corte=esperarVentana(CorteIntermedioDialog.class);
            esperar(()->campo(corte,"retiro.solicitar",JButton.class).isEnabled());
            SwingUtilities.invokeAndWait(()->campo(corte,"retiro.monto",JTextField.class).setText("500")); capturar(corte,"corte-intermedio.png");
            SwingUtilities.invokeLater(()->campo(corte,"retiro.solicitar",JButton.class).doClick());
            credenciales(esperarVentana(AutorizacionDialog.class),datos.supervisora.getUsername());
            TicketDialog recibo=esperarVentana(TicketDialog.class);
            SwingUtilities.invokeAndWait(()->verificar(campo(recibo,"ticket.texto",JTextArea.class).getText().contains("720.00")));
            capturar(recibo,"recibo-retiro.png"); cerrarTicket(recibo);
            esperar(()->campo(corte,"retiro.solicitar",JButton.class).isEnabled());
            SwingUtilities.invokeAndWait(()->campo(corte,"retiro.volver",JButton.class).doClick());
            esperar(()->campo(pos,"pos.reversion",JButton.class).isEnabled());
            verificar(datos.servicio.resumen(turno.id(),ana).esperado().toPlainString().equals("720.00"));
            System.out.println("OK    Corte intermedio solicita supervisor, guarda retiro Q500 y emite recibo con Q720 restantes"); correctas++;

            SwingUtilities.invokeLater(()->campo(pos,"pos.reversion",JButton.class).doClick());
            ReversionFrame reversion=esperarVentana(ReversionFrame.class);
            SwingUtilities.invokeAndWait(()->{
                campo(reversion,"reversion.folio",JTextField.class).setText(Long.toString(primera.id()));
                campo(reversion,"reversion.buscar",JButton.class).doClick();
            });
            esperar(()->campo(reversion,"reversion.tabla",JTable.class).getRowCount()==1);
            SwingUtilities.invokeAndWait(()->{
                campo(reversion,"reversion.tabla",JTable.class).setRowSelectionInterval(0,0);
                campo(reversion,"reversion.cantidad",JSpinner.class).setValue(1);
                campo(reversion,"reversion.motivo",JTextField.class).setText("Devolucion del escenario");
            });
            capturar(reversion,"reversion-solicitud.png");
            SwingUtilities.invokeLater(()->campo(reversion,"reversion.solicitar",JButton.class).doClick());
            AutorizacionDialog autorizacion=esperarVentana(AutorizacionDialog.class);
            credenciales(autorizacion,datos.datos.luis.getUsername());
            esperar(()->campo(autorizacion,"autorizacion.estado",JLabel.class).getText().contains("Intentos restantes: 2"));
            verificar(Sesion.getUsuario().getId().equals(ana.getId()));
            capturar(autorizacion,"reversion-cajero-rechazado.png");
            System.out.println("OK    Credenciales del cajero Luis se rechazan sin cambiar sesion ni devolver dinero"); correctas++;

            SwingUtilities.invokeAndWait(()->{
                campo(autorizacion,"autorizacion.usuario",JTextField.class).setText(datos.supervisora.getUsername());
                campo(autorizacion,"autorizacion.clave",JPasswordField.class).setText("SoloPruebas2026");
            }); capturar(autorizacion,"reversion-autorizacion.png");
            SwingUtilities.invokeAndWait(()->campo(autorizacion,"autorizacion.confirmar",JButton.class).doClick());
            TicketDialog devolucion=esperarVentana(TicketDialog.class);
            SwingUtilities.invokeAndWait(()->{
                String texto=campo(devolucion,"ticket.texto",JTextArea.class).getText();
                verificar(texto.contains("30.00")&&texto.contains("690.00")&&texto.contains("PARCIALMENTE_REVERSADA"));
            }); capturar(devolucion,"comprobante-reversion.png"); cerrarTicket(devolucion);
            esperar(()->campo(reversion,"reversion.buscar",JButton.class).isEnabled()&&campo(reversion,"reversion.tabla",JTable.class).getRowCount()==1);
            SwingUtilities.invokeAndWait(()->verificar(campo(reversion,"reversion.tabla",JTable.class).getValueAt(0,3).equals(4)));
            verificar(Sesion.getUsuario().getId().equals(ana.getId()));
            PruebasCaja.igual(PruebasCaja.numero("SELECT stock FROM producto WHERE id=?",datos.datos.arroz),"22");
            SwingUtilities.invokeAndWait(()->campo(reversion,"reversion.volver",JButton.class).doClick());
            System.out.println("OK    Supervisora autoriza Q30, vuelve el stock y quedan Q690 sin sustituir a Ana"); correctas++;

            esperar(()->campo(pos,"pos.cerrar",JButton.class).isEnabled());
            SwingUtilities.invokeLater(()->campo(pos,"pos.cerrar",JButton.class).doClick());
            CierreTurnoFrame cierre=esperarVentana(CierreTurnoFrame.class);
            esperar(()->campo(cierre,"cierre.confirmar",JButton.class).isEnabled());
            SwingUtilities.invokeAndWait(()->campo(cierre,"cierre.contado",JTextField.class).setText("680")); capturar(cierre,"cierre-escenario.png");
            SwingUtilities.invokeLater(()->campo(cierre,"cierre.confirmar",JButton.class).doClick());
            esperar(()->PruebasInterfazCaja.confirmacion()!=null);
            SwingUtilities.invokeAndWait(()->PruebasInterfazCaja.boton(PruebasInterfazCaja.confirmacion(),UIManager.getString("OptionPane.yesButtonText")).doClick());
            TicketDialog finalizado=esperarVentana(TicketDialog.class);
            SwingUtilities.invokeAndWait(()->{
                String texto=campo(finalizado,"ticket.texto",JTextArea.class).getText(); verificar(texto.contains("-10.00")&&texto.contains("690.00")&&texto.contains("CERRADO"));
            }); capturar(finalizado,"reporte-escenario.png"); cerrarTicket(finalizado);
            esperar(menu::isShowing); verificar(datos.servicio.actual(ana).isEmpty());
            System.out.println("OK    Cierre del escenario completo muestra esperado Q690, contado Q680 y diferencia -Q10"); correctas++;
        }finally{
            try{SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows()){w.dispose();}});}
            finally{try{datos.datos.limpiar();}finally{Sesion.cerrar(); Conexion.limpiarClaveTemporal();}}
            System.out.printf("%nPruebas interfaz cortes: 4; correctas: %d; fallos o pendientes: %d%n",correctas,4-correctas);
        }
    }
}
