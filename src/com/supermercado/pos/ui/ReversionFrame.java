package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Reversion;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.dominio.VentaParaReversion;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.servicio.ReversionService;
import com.supermercado.pos.util.Dinero;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.math.BigDecimal;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

public final class ReversionFrame extends JDialog {
    private static final long serialVersionUID = 1L;
    private final transient Turno turno;
    private final transient Usuario actor;
    private final transient ReversionService servicio = new ReversionService();
    private transient VentaParaReversion venta;
    private final JTextField folio = new JTextField(12);
    private final JTextField motivo = new JTextField(35);
    private final JSpinner cantidad = new JSpinner(new SpinnerNumberModel(1, 1, 999999, 1));
    private final JButton buscar = new JButton("Buscar venta");
    private final JButton solicitar = new JButton("Solicitar autorizacion de reversion");
    private final JLabel estado = new JLabel("Solo ventas de tu turno actual. El reembolso se entrega en efectivo.");
    private final DefaultTableModel modelo = new DefaultTableModel(new String[] {"Producto", "Vendido", "Reversado", "Disponible", "Precio historico Q"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    public ReversionFrame(Window padre, Turno turno, Usuario actor) {
        super(padre, "Supermercado - Reversion de producto", ModalityType.APPLICATION_MODAL);
        this.turno = turno; this.actor = actor; setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        Tema.primario(solicitar);
        folio.setName("reversion.folio"); motivo.setName("reversion.motivo"); cantidad.setName("reversion.cantidad");
        buscar.setName("reversion.buscar"); solicitar.setName("reversion.solicitar"); tabla.setName("reversion.tabla"); estado.setName("reversion.estado");
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel consulta = new JPanel(new FlowLayout(FlowLayout.LEFT)); consulta.add(new JLabel("Folio de venta:")); consulta.add(folio); consulta.add(buscar);
        JPanel norte = new JPanel(new BorderLayout(8, 8)); norte.add(consulta, BorderLayout.NORTH); norte.add(estado, BorderLayout.SOUTH);
        JPanel campos = new JPanel(new GridLayout(0, 2, 8, 8));
        campos.add(new JLabel("Cantidad a devolver:")); campos.add(cantidad); campos.add(new JLabel("Motivo obligatorio:")); campos.add(motivo);
        JButton volver = new JButton("Volver"); volver.setName("reversion.volver"); volver.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT)); botones.add(volver); botones.add(solicitar);
        JPanel sur = new JPanel(new BorderLayout(8, 8)); sur.add(campos, BorderLayout.NORTH); sur.add(botones, BorderLayout.SOUTH);
        JPanel contenido = new JPanel(new BorderLayout(12, 12)); contenido.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        contenido.add(norte, BorderLayout.NORTH); contenido.add(new JScrollPane(tabla), BorderLayout.CENTER); contenido.add(sur, BorderLayout.SOUTH);
        setContentPane(contenido); setSize(850, 460); setLocationRelativeTo(padre);
        solicitar.setEnabled(false); buscar.addActionListener(e -> buscar()); folio.addActionListener(e -> buscar());
        solicitar.addActionListener(e -> solicitar()); tabla.getSelectionModel().addListSelectionListener(e -> botones());
    }
    private void botones() {
        int fila = tabla.getSelectedRow();
        solicitar.setEnabled(buscar.isEnabled() && venta != null && fila >= 0 && venta.detalles().get(fila).disponible() > 0);
    }
    private void buscar() {
        try {
            long numero;
            try { numero = Long.parseLong(folio.getText().trim()); }
            catch (NumberFormatException e) { throw new ReglaNegocioException("Escribe el folio numerico de la venta"); }
            buscar.setEnabled(false); solicitar.setEnabled(false); venta = null; modelo.setRowCount(0);
            Tareas.ejecutar(this, () -> servicio.buscar(turno.id(), numero, actor), datos -> {
                venta = datos;
                for (var d : datos.detalles()) { modelo.addRow(new Object[] {d.producto(), d.cantidad(), d.reversada(), d.disponible(), d.precio()}); }
                estado.setText("Venta " + datos.id() + " | " + datos.estado() + " | Total Q " + datos.total() + " | Reembolso en efectivo");
            }, () -> { buscar.setEnabled(true); botones(); });
        } catch (RuntimeException e) { Mensajes.error(this, e); }
    }
    private void solicitar() {
        try {
            int fila = tabla.getSelectedRow(); if (fila < 0 || venta == null) { return; }
            cantidad.commitEdit(); int unidades = ((Number) cantidad.getValue()).intValue();
            var d = venta.detalles().get(fila);
            if (unidades > d.disponible()) { throw new ReglaNegocioException("Solo quedan " + d.disponible() + " unidades reversables"); }
            var solicitud = servicio.solicitar(turno.id(), d.id(), unidades, motivo.getText(), actor);
            BigDecimal monto = Dinero.normalizar(d.precio().multiply(BigDecimal.valueOf(unidades)));
            AutorizacionDialog autorizacion = new AutorizacionDialog(this, actor,
                    "Venta " + venta.id() + ": " + unidades + " x " + d.producto() + ". Reembolso efectivo Q " + monto
                    + ". Motivo: " + motivo.getText().strip(), solicitud);
            autorizacion.setVisible(true);
            if (autorizacion.getResultado() instanceof Reversion r) {
                motivo.setText(""); cantidad.setValue(1);
                new TicketDialog(this, "Reversion " + r.id(), TicketDialog.reversion(r)).setVisible(true);
                buscar();
            }
        } catch (java.text.ParseException e) { Mensajes.error(this, new ReglaNegocioException("Revisa la cantidad")); }
        catch (RuntimeException e) { Mensajes.error(this, e); }
    }
}
