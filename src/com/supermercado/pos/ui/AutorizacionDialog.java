package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Caja;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.servicio.TurnoService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/** La autorizacion abre la caja para el cajero; no inicia sesion como supervisor. */
public final class AutorizacionDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private final transient TurnoService.AperturaCero solicitud;
    private transient Turno creado;
    private final JTextField username = new JTextField(22);
    private final JPasswordField clave = new JPasswordField(22);
    private final JLabel estado = new JLabel("Dispones de 3 intentos de autorizacion.");
    private final JButton confirmar = new JButton("Autorizar y abrir sin fondo");
    private final JButton cancelar = new JButton("Cancelar");
    private boolean ocupado;

    public AutorizacionDialog(Window padre, Usuario cajero, Caja caja, TurnoService.AperturaCero solicitud) {
        super(padre, "Supermercado - Autorizacion de supervisor", ModalityType.APPLICATION_MODAL);
        this.solicitud = solicitud;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { if (!ocupado) { dispose(); } }
        });
        username.setName("autorizacion.usuario"); clave.setName("autorizacion.clave");
        confirmar.setName("autorizacion.confirmar"); cancelar.setName("autorizacion.cancelar"); estado.setName("autorizacion.estado");
        JPanel campos = new JPanel(new GridLayout(0, 2, 10, 12));
        campos.add(new JLabel("Operacion:")); campos.add(new JLabel("Abrir Caja " + caja.numero() + " con Q0.00"));
        campos.add(new JLabel("Cajero que conserva su sesion:")); campos.add(new JLabel(cajero.getUsername()));
        campos.add(new JLabel("Usuario del supervisor:")); campos.add(username);
        campos.add(new JLabel("Contrasena del supervisor:")); campos.add(clave);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT)); botones.add(cancelar); botones.add(confirmar);
        JPanel sur = new JPanel(new BorderLayout(8, 8)); sur.add(estado, BorderLayout.CENTER); sur.add(botones, BorderLayout.SOUTH);
        JPanel contenido = new JPanel(new BorderLayout(16, 16)); contenido.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contenido.add(campos, BorderLayout.CENTER); contenido.add(sur, BorderLayout.SOUTH);
        setContentPane(contenido); setSize(800, 310); setLocationRelativeTo(padre);
        confirmar.addActionListener(e -> autorizar()); cancelar.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(confirmar);
    }
    public Turno getTurnoCreado() { return creado; }
    private void ocupado(boolean valor) {
        ocupado = valor; username.setEnabled(!valor); clave.setEnabled(!valor); cancelar.setEnabled(!valor);
        confirmar.setEnabled(!valor && !solicitud.terminada());
    }
    private void autorizar() {
        String usuario = username.getText();
        char[] temporal = clave.getPassword(); clave.setText("");
        ocupado(true); estado.setText("Validando autorizacion y apertura...");
        new SwingWorker<Turno, Void>() {
            @Override protected Turno doInBackground() {
                try { return solicitud.confirmar(usuario, temporal); }
                finally { Arrays.fill(temporal, '\0'); }
            }
            @Override protected void done() {
                try { creado = get(); dispose(); }
                catch (Exception e) {
                    String mensaje = Mensajes.texto(e);
                    String resto = solicitud.terminada() ? "Solicitud terminada; pulsa Cancelar para volver."
                            : "Intentos restantes: " + solicitud.intentosRestantes();
                    estado.setText("<html>" + escapar(mensaje) + "<br>" + resto + "</html>");
                } finally { ocupado(false); clave.requestFocusInWindow(); }
            }
        }.execute();
    }
    private String escapar(String texto) { return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    @Override public void dispose() { clave.setText(""); super.dispose(); }
}
