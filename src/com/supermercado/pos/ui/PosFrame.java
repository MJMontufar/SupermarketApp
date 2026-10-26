package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Carrito;
import com.supermercado.pos.dominio.CierreTurno;
import com.supermercado.pos.dominio.LineaCarrito;
import com.supermercado.pos.dominio.Producto;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.dominio.Venta;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.servicio.VentaService;
import com.supermercado.pos.util.Dinero;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

public final class PosFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private final transient Usuario actor;
    private final transient Turno turno;
    private final transient Carrito carrito = new Carrito();
    private transient List<Producto> catalogo = List.of();
    private final JTextField codigo = new JTextField(18);
    private final JSpinner cantidad = new JSpinner(new SpinnerNumberModel(1, 1, 999999, 1));
    private final JComboBox<Producto> productos = new JComboBox<>();
    private final JButton agregar = new JButton("Agregar seleccionado");
    private final JButton actualizar = new JButton("Actualizar catalogo");
    private final JButton cobrar = new JButton("Cobrar (F12)");
    private final JButton quitar = new JButton("Quitar seleccionado");
    private final JButton cancelar = new JButton("Cancelar venta");
    private final JButton cerrar = new JButton("Cerrar turno");
    private final JButton retiro = new JButton("Corte intermedio");
    private final JButton reversion = new JButton("Reversion");
    private final JLabel totales = new JLabel("Total: Q 0.00");
    private final JLabel estado = new JLabel("Consultando catalogo...");
    private boolean ocupado;
    private final DefaultTableModel modelo = new DefaultTableModel(new String[] {"Producto", "Cantidad", "Precio Q", "Subtotal Q"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int fila, int columna) { return columna == 1 && !ocupado; }
        @Override public void setValueAt(Object valor, int fila, int columna) {
            if (columna != 1) { super.setValueAt(valor, fila, columna); return; }
            try {
                int nueva = Integer.parseInt(String.valueOf(valor).trim());
                carrito.cambiarCantidad(carrito.lineas().get(fila).producto().getId(), nueva);
                refrescar();
            } catch (NumberFormatException e) {
                Mensajes.error(PosFrame.this, new ReglaNegocioException("La cantidad debe ser un numero entero positivo"));
            } catch (RuntimeException e) { Mensajes.error(PosFrame.this, e); }
        }
    };
    private final JTable tabla = new JTable(modelo);

    public PosFrame(Usuario actor, Turno turno) {
        super("Supermercado - Punto de venta - Caja " + turno.caja().numero());
        this.actor = actor; this.turno = turno;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (!ocupado && confirmarDescartar()) { dispose(); }
            }
        });
        Tema.primario(cobrar); Tema.peligro(cancelar);
        codigo.setName("pos.codigo"); cantidad.setName("pos.cantidad"); productos.setName("pos.producto");
        agregar.setName("pos.agregar"); cobrar.setName("pos.cobrar"); tabla.setName("pos.carrito");
        cerrar.setName("pos.cerrar"); estado.setName("pos.estado"); totales.setName("pos.totales");
        retiro.setName("pos.retiro"); reversion.setName("pos.reversion");
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productos.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice, boolean seleccionado, boolean foco) {
                super.getListCellRendererComponent(lista, valor, indice, seleccionado, foco);
                if (valor instanceof Producto p) { setText(p.getCodigoBarras() + " - " + p.getNombre() + " - Q " + p.getPrecio() + " - stock " + p.getStock()); }
                return this;
            }
        });
        JPanel norte = new JPanel(new GridLayout(0, 1, 8, 8));
        norte.add(new JLabel("Cajero: " + actor.getUsername() + " | Caja: " + turno.caja().numero() + " | Turno: " + turno.id() + " | Fondo: Q " + turno.fondoInicial()));
        JPanel lector = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lector.add(new JLabel("Codigo + Enter:")); lector.add(codigo); lector.add(new JLabel("Cantidad:")); lector.add(cantidad);
        norte.add(lector);
        JPanel seleccion = new JPanel(new FlowLayout(FlowLayout.LEFT)); seleccion.add(productos); seleccion.add(agregar); seleccion.add(actualizar);
        norte.add(seleccion); norte.add(estado);
        JPanel botones = new JPanel(new GridLayout(2, 3, 8, 8));
        botones.add(quitar); botones.add(cancelar); botones.add(cobrar); botones.add(retiro); botones.add(reversion); botones.add(cerrar);
        JPanel sur = new JPanel(new BorderLayout(8, 8)); sur.add(totales, BorderLayout.NORTH); sur.add(botones, BorderLayout.SOUTH);
        JPanel contenido = new JPanel(new BorderLayout(12, 12)); contenido.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        contenido.add(norte, BorderLayout.NORTH); contenido.add(new JScrollPane(tabla), BorderLayout.CENTER); contenido.add(sur, BorderLayout.SOUTH);
        setContentPane(contenido); setSize(1100, 680); Tema.acomodar(this);
        codigo.addActionListener(e -> agregarCodigo()); agregar.addActionListener(e -> agregar((Producto) productos.getSelectedItem()));
        actualizar.addActionListener(e -> cargar());
        quitar.addActionListener(e -> {
            if (!terminarEdicion()) { return; }
            int fila = tabla.getSelectedRow();
            if (fila >= 0) { carrito.quitar(carrito.lineas().get(fila).producto().getId()); refrescar(); }
            codigo.requestFocusInWindow();
        });
        cancelar.addActionListener(e -> { if (confirmarDescartar()) { carrito.vaciar(); refrescar(); codigo.requestFocusInWindow(); } });
        cobrar.addActionListener(e -> cobrar()); cerrar.addActionListener(e -> cerrarTurno());
        retiro.addActionListener(e -> {
            if (!terminarEdicion()) { return; }
            new CorteIntermedioDialog(this, turno, actor).setVisible(true); cargar();
        });
        reversion.addActionListener(e -> {
            if (!terminarEdicion()) { return; }
            new ReversionFrame(this, turno, actor).setVisible(true); cargar();
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F12"), "cobrar");
        getRootPane().getActionMap().put("cobrar", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { if (cobrar.isEnabled()) { cobrar(); } }
        });
        cargar();
    }
    private boolean terminarEdicion() { return !tabla.isEditing() || tabla.getCellEditor().stopCellEditing(); }
    private boolean confirmarDescartar() {
        return carrito.estaVacio() || JOptionPane.showConfirmDialog(this, "¿Descartar el carrito sin cobrar? El turno seguira abierto.",
                "Carrito pendiente", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
    private void ocupado(boolean valor) {
        ocupado = valor; codigo.setEnabled(!valor); cantidad.setEnabled(!valor); productos.setEnabled(!valor);
        agregar.setEnabled(!valor && !catalogo.isEmpty()); actualizar.setEnabled(!valor); tabla.setEnabled(!valor);
        cobrar.setEnabled(!valor && !carrito.estaVacio() && actor.tienePermiso("VENTA_COBRAR"));
        quitar.setEnabled(!valor && !carrito.estaVacio()); cancelar.setEnabled(!valor && !carrito.estaVacio());
        cerrar.setEnabled(!valor && actor.tienePermiso("TURNO_CERRAR"));
        retiro.setEnabled(!valor && actor.tienePermiso("CORTE_INTERMEDIO"));
        reversion.setEnabled(!valor && actor.tienePermiso("REVERSION_SOLICITAR"));
    }
    private void cargar() {
        if (ocupado) { return; }
        ocupado(true);
        Tareas.ejecutar(this, () -> new VentaService().productos(actor), lista -> {
            catalogo = lista; productos.removeAllItems(); lista.forEach(productos::addItem);
            estado.setText("Productos activos: " + lista.size() + ". Los precios ya incluyen impuesto.");
        }, () -> { ocupado(false); codigo.requestFocusInWindow(); });
    }
    private void agregarCodigo() {
        try {
            String texto = codigo.getText().trim();
            Producto producto = catalogo.stream().filter(p -> p.getCodigoBarras().equals(texto)).findFirst()
                    .orElseThrow(() -> new ReglaNegocioException("Codigo no encontrado en el catalogo activo"));
            agregar(producto);
        } catch (RuntimeException e) { Mensajes.error(this, e); }
    }
    private void agregar(Producto producto) {
        try {
            if (!terminarEdicion()) { return; }
            cantidad.commitEdit();
            carrito.agregar(producto, ((Number) cantidad.getValue()).intValue());
            codigo.setText(""); cantidad.setValue(1); refrescar(); codigo.requestFocusInWindow();
        } catch (java.text.ParseException e) { Mensajes.error(this, new ReglaNegocioException("Revisa la cantidad")); }
        catch (RuntimeException e) { Mensajes.error(this, e); }
    }
    private void refrescar() {
        modelo.setRowCount(0); BigDecimal base = Dinero.CERO;
        for (LineaCarrito l : carrito.lineas()) {
            modelo.addRow(new Object[] {l.producto().getNombre(), l.cantidad(), l.precioUnitario(), l.subtotal()});
            BigDecimal factor = BigDecimal.ONE.add(l.producto().getPorcentajeImpuesto().divide(new BigDecimal("100")));
            base = base.add(l.subtotal().divide(factor, 2, RoundingMode.HALF_UP));
        }
        totales.setText("Base: Q " + base + " | Impuesto incluido: Q " + carrito.total().subtract(base) + " | TOTAL: Q " + carrito.total());
        ocupado(ocupado);
    }
    private void cobrar() {
        if (ocupado || !terminarEdicion() || carrito.estaVacio()) { return; }
        PagoDialog dialogo = new PagoDialog(this, turno.id(), carrito, actor); dialogo.setVisible(true);
        Venta venta = dialogo.getVenta();
        if (venta != null) {
            // Se limpia solo despues del commit, antes de mostrar o imprimir el comprobante.
            carrito.vaciar(); refrescar();
            estado.setText("Venta " + venta.id() + " guardada en Oracle.");
            new TicketDialog(this, "Venta " + venta.id(), TicketDialog.venta(venta)).setVisible(true);
            cargar();
        }
        codigo.requestFocusInWindow();
    }
    private void cerrarTurno() {
        if (!terminarEdicion() || !confirmarDescartar()) { return; }
        CierreTurnoFrame dialogo = new CierreTurnoFrame(this, turno, actor); dialogo.setVisible(true);
        CierreTurno cierre = dialogo.getCierre();
        if (cierre != null) {
            carrito.vaciar(); refrescar();
            new TicketDialog(this, "Reporte de cierre", TicketDialog.cierre(cierre)).setVisible(true);
            dispose();
        }
    }
}
