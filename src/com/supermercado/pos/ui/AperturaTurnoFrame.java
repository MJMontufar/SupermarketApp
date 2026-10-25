package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Caja;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.servicio.TurnoService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class AperturaTurnoFrame extends JDialog {
    private static final long serialVersionUID = 1L;
    private final transient Usuario actor;
    private final transient TurnoService servicio = new TurnoService();
    private transient Turno creado;
    private final JComboBox<Caja> cajas = new JComboBox<>();
    private final JTextField fondo = new JTextField(16);
    private final JLabel estado = new JLabel("Consultando cajas...");
    private final JButton abrir = new JButton("Abrir turno");
    private final JButton actualizar = new JButton("Actualizar cajas");
    private final JButton cancelar = new JButton("Cancelar");
    private boolean ocupado;

    public AperturaTurnoFrame(Window padre, Usuario actor) {
        super(padre, "Supermercado - Apertura de turno", ModalityType.APPLICATION_MODAL);
        this.actor = actor;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { if (!ocupado) { dispose(); } }
        });
        cajas.setName("apertura.caja"); fondo.setName("apertura.fondo"); abrir.setName("apertura.abrir");
        estado.setName("apertura.estado");
        JPanel campos = new JPanel(new GridLayout(0, 2, 8, 12));
        campos.add(new JLabel("Cajero:")); campos.add(new JLabel(actor.getUsername()));
        campos.add(new JLabel("Fecha y hora:")); campos.add(new JLabel(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        campos.add(new JLabel("Caja disponible:")); campos.add(cajas);
        campos.add(new JLabel("Fondo inicial (Q):")); campos.add(fondo);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(actualizar); botones.add(cancelar); botones.add(abrir);
        JPanel centro = new JPanel(new BorderLayout(12, 12));
        centro.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        centro.add(campos, BorderLayout.NORTH); centro.add(estado, BorderLayout.CENTER); centro.add(botones, BorderLayout.SOUTH);
        setContentPane(centro); pack(); setMinimumSize(new java.awt.Dimension(520, 260)); setLocationRelativeTo(padre);
        actualizar.addActionListener(e -> cargar()); cancelar.addActionListener(e -> dispose());
        abrir.addActionListener(e -> abrir()); getRootPane().setDefaultButton(abrir);
        cargar();
    }
    public Turno getTurnoCreado() { return creado; }
    private void ocupado(boolean valor) {
        ocupado = valor; abrir.setEnabled(!valor && cajas.getItemCount() > 0);
        cajas.setEnabled(!valor); fondo.setEnabled(!valor); actualizar.setEnabled(!valor); cancelar.setEnabled(!valor);
    }
    private void cargar() {
        ocupado(true);
        Tareas.ejecutar(this, () -> servicio.cajasLibres(actor), this::mostrarCajas, () -> ocupado(false));
    }
    private void mostrarCajas(List<Caja> lista) {
        cajas.removeAllItems(); lista.forEach(cajas::addItem);
        estado.setText(lista.isEmpty() ? "No hay cajas libres. Puedes actualizar la lista." : "Declara el efectivo disponible para dar cambio.");
    }
    private void abrir() {
        try {
            Caja caja = (Caja) cajas.getSelectedItem();
            if (caja == null) { throw new ReglaNegocioException("Selecciona una caja disponible"); }
            BigDecimal monto = Campos.monto(fondo);
            ocupado(true);
            Tareas.ejecutar(this, () -> servicio.abrir(actor, caja.id(), monto), turno -> {
                creado = turno; dispose();
            }, () -> ocupado(false));
        } catch (RuntimeException e) { Mensajes.error(this, e); }
    }
}
