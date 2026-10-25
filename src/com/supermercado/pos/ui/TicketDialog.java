package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.CierreTurno;
import com.supermercado.pos.dominio.Pago;
import com.supermercado.pos.dominio.ResumenTurno;
import com.supermercado.pos.dominio.Venta;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.print.PrinterException;
import java.time.format.DateTimeFormatter;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Vista previa textual; Imprimir usa el servicio de impresion estandar del JDK. */
public final class TicketDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    public TicketDialog(Window padre, String titulo, String texto) {
        super(padre, titulo, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JTextArea area = new JTextArea(texto); area.setEditable(false); area.setName("ticket.texto");
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13)); area.setCaretPosition(0);
        JButton imprimir = new JButton("Imprimir");
        imprimir.addActionListener(e -> {
            try { area.print(); }
            catch (PrinterException fallo) {
                JOptionPane.showMessageDialog(this, "No se pudo imprimir. El registro ya esta guardado en Oracle.", "Impresion", JOptionPane.WARNING_MESSAGE);
            }
        });
        JButton cerrar = new JButton("Cerrar"); cerrar.setName("ticket.cerrar"); cerrar.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT)); botones.add(imprimir); botones.add(cerrar);
        add(new JScrollPane(area), BorderLayout.CENTER); add(botones, BorderLayout.SOUTH);
        setSize(600, 530); setLocationRelativeTo(padre);
    }
    public static String venta(Venta venta) {
        StringBuilder t = new StringBuilder("     SUPERMERCADO - COMPROBANTE DE PRACTICA\n");
        t.append("Venta: ").append(venta.id()).append("    Turno: ").append(venta.turnoId()).append('\n');
        t.append("Fecha: ").append(venta.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n\n");
        for (Venta.Detalle d : venta.detalles()) {
            t.append(d.nombre()).append("\n  ").append(d.cantidad()).append(" x Q ").append(d.precioUnitario())
                    .append(" = Q ").append(d.subtotal()).append('\n');
        }
        t.append("\nBase sin impuesto: Q ").append(venta.subtotal());
        t.append("\nImpuesto incluido: Q ").append(venta.impuesto());
        t.append("\nTOTAL:             Q ").append(venta.total()).append("\n\n");
        for (Pago p : venta.pagos()) {
            t.append(p.metodo()).append(" aplicado: Q ").append(p.monto()).append('\n');
            if (p.metodo() == Pago.Metodo.EFECTIVO) {
                t.append("Recibido: Q ").append(p.recibido()).append("    Cambio: Q ").append(p.cambio()).append('\n');
            }
        }
        return t.toString();
    }
    public static String resumen(ResumenTurno r) {
        return "Turno: " + r.turnoId() + "\nVentas: " + r.ventas()
                + "\nFondo inicial:         Q " + r.fondo() + "\nTotal vendido:         Q " + r.vendido()
                + "\nEfectivo neto cobrado:  Q " + r.efectivo() + "\nTarjeta cobrada:        Q " + r.tarjeta()
                + "\nReversiones:           Q " + r.reversiones() + "\nReembolsos efectivo:   Q " + r.reembolsosEfectivo()
                + "\nRetiros a boveda:       Q " + r.retiros() + "\nEfectivo esperado:     Q " + r.esperado();
    }
    public static String cierre(CierreTurno c) {
        return "        CIERRE DE TURNO\n\n" + resumen(c.resumen()) + "\nEfectivo contado:      Q " + c.contado()
                + "\nDiferencia:            Q " + c.diferencia() + "\n(+ sobrante / - faltante)\n\nFecha: "
                + c.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                + "\nEstado: CERRADO\n";
    }
}
