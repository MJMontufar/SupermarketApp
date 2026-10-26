package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.ResumenTurno;
import com.supermercado.pos.dominio.Retiro;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.servicio.RetiroService;
import com.supermercado.pos.util.Configuracion;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public final class CorteIntermedioDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private final transient Turno turno;
    private final transient Usuario actor;
    private final transient RetiroService servicio = new RetiroService();
    private final JTextArea resumen = new JTextArea("Consultando caja...");
    private final JTextField monto = new JTextField(12);
    private final JButton solicitar = new JButton("Solicitar autorizacion de retiro");
    private final JButton volver = new JButton("Volver");
    public CorteIntermedioDialog(Window padre, Turno turno, Usuario actor) {
        super(padre, "Supermercado - Corte intermedio", ModalityType.APPLICATION_MODAL);
        this.turno = turno; this.actor = actor;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        resumen.setEditable(false); resumen.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13)); resumen.setName("retiro.resumen");
        monto.setName("retiro.monto"); solicitar.setName("retiro.solicitar"); volver.setName("retiro.volver");
        JPanel entrada = new JPanel(new FlowLayout(FlowLayout.LEFT)); entrada.add(new JLabel("Monto a entregar a boveda (Q):")); entrada.add(monto);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT)); botones.add(volver); botones.add(solicitar);
        JPanel sur = new JPanel(new BorderLayout(8, 8)); sur.add(entrada, BorderLayout.NORTH); sur.add(botones, BorderLayout.SOUTH);
        JPanel contenido = new JPanel(new BorderLayout(12, 12)); contenido.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        contenido.add(new JScrollPane(resumen), BorderLayout.CENTER); contenido.add(sur, BorderLayout.SOUTH);
        setContentPane(contenido); setSize(650, 470); setLocationRelativeTo(padre);
        solicitar.addActionListener(e -> solicitar()); volver.addActionListener(e -> dispose()); cargar();
    }
    private void cargar() {
        solicitar.setEnabled(false);
        Tareas.ejecutar(this, () -> servicio.resumen(turno.id(), actor), this::mostrar, () -> { });
    }
    private void mostrar(ResumenTurno r) {
        resumen.setText(TicketDialog.resumen(r) + "\n\nMinimo que debe conservar la caja: Q " + Configuracion.cargar().efectivoMinimo());
        solicitar.setEnabled(true);
    }
    private void solicitar() {
        try {
            var importe = Campos.montoExacto(monto);
            var solicitud = servicio.solicitar(turno.id(), importe, actor);
            AutorizacionDialog dialogo = new AutorizacionDialog(this, actor,
                    "Entregar Q " + com.supermercado.pos.util.Dinero.normalizar(importe) + " a boveda. Turno " + turno.id(), solicitud);
            dialogo.setVisible(true);
            if (dialogo.getResultado() instanceof Retiro retiro) {
                monto.setText("");
                new TicketDialog(this, "Recibo " + retiro.referencia(), TicketDialog.retiro(retiro)).setVisible(true);
                cargar();
            }
        } catch (RuntimeException e) { Mensajes.error(this, e); }
    }
}
