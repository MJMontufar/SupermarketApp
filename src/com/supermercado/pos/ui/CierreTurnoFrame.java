package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.CierreTurno;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.servicio.TurnoService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public final class CierreTurnoFrame extends JDialog {
    private static final long serialVersionUID = 1L;
    private final transient Turno turno;
    private final transient Usuario actor;
    private final transient TurnoService servicio = new TurnoService();
    private transient CierreTurno cierre;
    private final JTextArea resumen = new JTextArea("Consultando resumen...");
    private final JTextField contado = new JTextField(12);
    private final JButton cerrar = new JButton("Confirmar cierre");
    private final JButton cancelar = new JButton("Volver");
    private boolean ocupado;

    public CierreTurnoFrame(Window padre, Turno turno, Usuario actor) {
        super(padre, "Supermercado - Cierre de turno", ModalityType.APPLICATION_MODAL);
        this.turno = turno; this.actor = actor;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { if (!ocupado) { dispose(); } }
        });
        resumen.setEditable(false); resumen.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13)); resumen.setName("cierre.resumen");
        contado.setName("cierre.contado"); cerrar.setName("cierre.confirmar");
        JPanel entrada = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entrada.add(new JLabel("Efectivo contado (Q):")); entrada.add(contado);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT)); botones.add(cancelar); botones.add(cerrar);
        JPanel sur = new JPanel(new BorderLayout()); sur.add(entrada, BorderLayout.NORTH); sur.add(botones, BorderLayout.SOUTH);
        JPanel contenido = new JPanel(new BorderLayout(12, 12)); contenido.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        contenido.add(new JScrollPane(resumen), BorderLayout.CENTER); contenido.add(sur, BorderLayout.SOUTH);
        setContentPane(contenido); setSize(590, 440); setLocationRelativeTo(padre);
        cancelar.addActionListener(e -> dispose()); cerrar.addActionListener(e -> cerrar());
        ocupado(true);
        Tareas.ejecutar(this, () -> servicio.resumen(turno.id(), actor), r -> resumen.setText(TicketDialog.resumen(r)), () -> ocupado(false));
    }
    public CierreTurno getCierre() { return cierre; }
    private void ocupado(boolean valor) {
        ocupado = valor; cerrar.setEnabled(!valor); cancelar.setEnabled(!valor); contado.setEnabled(!valor);
    }
    private void cerrar() {
        try {
            BigDecimal monto = Campos.monto(contado);
            if (JOptionPane.showConfirmDialog(this, "El turno quedara cerrado y no podra registrar mas ventas. ¿Continuar?",
                    "Cerrar turno", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) { return; }
            ocupado(true);
            Tareas.ejecutar(this, () -> servicio.cerrar(turno.id(), monto, actor), resultado -> {
                cierre = resultado; dispose();
            }, () -> ocupado(false));
        } catch (RuntimeException e) { Mensajes.error(this, e); }
    }
}
