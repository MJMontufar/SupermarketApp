package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Carrito;
import com.supermercado.pos.dominio.Pago;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.dominio.Venta;
import com.supermercado.pos.servicio.PlanPago;
import com.supermercado.pos.servicio.VentaService;
import com.supermercado.pos.util.Dinero;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public final class PagoDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private final transient Carrito carrito;
    private final transient Usuario actor;
    private final long turno;
    private transient Venta venta;
    private final JComboBox<String> metodo = new JComboBox<>(new String[] {"EFECTIVO", "TARJETA", "MIXTO"});
    private final JTextField efectivo = new JTextField("0.00", 14);
    private final JTextField tarjeta = new JTextField("0.00", 14);
    private final JLabel cambio = new JLabel("Cambio: Q 0.00");
    private final JLabel aviso = new JLabel(" ");
    private final JButton confirmar = new JButton("Confirmar cobro");
    private final JButton cancelar = new JButton("Cancelar");
    private boolean ocupado;

    public PagoDialog(Window padre, long turno, Carrito carrito, Usuario actor) {
        super(padre, "Supermercado - Pago", ModalityType.APPLICATION_MODAL);
        this.turno = turno; this.carrito = carrito; this.actor = actor;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { if (!ocupado) { dispose(); } }
        });
        metodo.setName("pago.metodo"); efectivo.setName("pago.efectivo"); tarjeta.setName("pago.tarjeta");
        confirmar.setName("pago.confirmar"); aviso.setName("pago.aviso"); cambio.setName("pago.cambio");
        JPanel campos = new JPanel(new GridLayout(0, 2, 8, 12));
        campos.add(new JLabel("Total a pagar:")); campos.add(new JLabel("Q " + carrito.total()));
        campos.add(new JLabel("Metodo:")); campos.add(metodo);
        campos.add(new JLabel("Efectivo recibido (Q):")); campos.add(efectivo);
        campos.add(new JLabel("Monto en tarjeta (Q):")); campos.add(tarjeta);
        campos.add(cambio); campos.add(aviso);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT)); botones.add(cancelar); botones.add(confirmar);
        JPanel contenido = new JPanel(new BorderLayout(12, 12)); contenido.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contenido.add(campos, BorderLayout.CENTER); contenido.add(botones, BorderLayout.SOUTH);
        setContentPane(contenido); setSize(650, 285); setLocationRelativeTo(padre);
        DocumentListener escucha = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { calcular(); }
            @Override public void removeUpdate(DocumentEvent e) { calcular(); }
            @Override public void changedUpdate(DocumentEvent e) { calcular(); }
        };
        efectivo.getDocument().addDocumentListener(escucha); tarjeta.getDocument().addDocumentListener(escucha);
        metodo.addActionListener(e -> cambiarMetodo());
        cancelar.addActionListener(e -> dispose()); confirmar.addActionListener(e -> cobrar());
        getRootPane().setDefaultButton(confirmar); cambiarMetodo();
    }
    public Venta getVenta() { return venta; }
    private void cambiarMetodo() {
        String seleccionado = (String) metodo.getSelectedItem();
        boolean soloTarjeta = "TARJETA".equals(seleccionado);
        efectivo.setEnabled(!soloTarjeta); tarjeta.setEnabled("MIXTO".equals(seleccionado));
        if (soloTarjeta) { efectivo.setText("0.00"); tarjeta.setText(carrito.total().toPlainString()); }
        else if ("EFECTIVO".equals(seleccionado)) { tarjeta.setText("0.00"); }
        else { tarjeta.setText("0.00"); }
        calcular();
    }
    private void calcular() {
        if (ocupado) { return; }
        try {
            List<Pago> pagos = PlanPago.calcular(carrito.total(), Campos.monto(efectivo), Campos.monto(tarjeta));
            BigDecimal devuelto = pagos.stream().map(Pago::cambio).reduce(Dinero.CERO, BigDecimal::add);
            cambio.setText("Cambio: Q " + devuelto); aviso.setText("Pago completo"); confirmar.setEnabled(true);
        } catch (RuntimeException e) {
            cambio.setText("Cambio: pendiente"); aviso.setText(e.getMessage()); confirmar.setEnabled(false);
        }
    }
    private void cobrar() {
        try {
            BigDecimal recibido = Campos.monto(efectivo); BigDecimal montoTarjeta = Campos.monto(tarjeta);
            PlanPago.calcular(carrito.total(), recibido, montoTarjeta);
            ocupado = true; confirmar.setEnabled(false); cancelar.setEnabled(false);
            metodo.setEnabled(false); efectivo.setEnabled(false); tarjeta.setEnabled(false);
            Tareas.ejecutar(this, () -> new VentaService().cobrar(turno, carrito, recibido, montoTarjeta, actor), resultado -> {
                venta = resultado; dispose();
            }, () -> {
                ocupado = false; cancelar.setEnabled(true); metodo.setEnabled(true);
                efectivo.setEnabled(!"TARJETA".equals(metodo.getSelectedItem()));
                tarjeta.setEnabled("MIXTO".equals(metodo.getSelectedItem())); calcular();
            });
        } catch (RuntimeException e) { Mensajes.error(this, e); }
    }
}
