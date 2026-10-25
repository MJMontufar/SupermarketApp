package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.servicio.AuthService;
import com.supermercado.pos.util.Sesion;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

public final class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private final JTextField usuario = new JTextField(22);
    private final JPasswordField clave = new JPasswordField(22);
    private final JButton ingresar = new JButton("Ingresar");
    private final JLabel error = new JLabel(" ");
    private final transient AuthService servicio = new AuthService();

    public LoginFrame() {
        super("Supermercado - Ingreso");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        usuario.setName("login.usuario");
        clave.setName("login.clave");
        ingresar.setName("login.ingresar");
        error.setName("login.error");
        error.setForeground(new Color(170, 30, 30));
        JPanel campos = new JPanel(new GridLayout(0, 1, 6, 6));
        campos.add(new JLabel("Usuario")); campos.add(usuario);
        campos.add(new JLabel("Contrasena")); campos.add(clave);
        campos.add(error); campos.add(ingresar);
        JPanel contenido = new JPanel(new BorderLayout(12, 12));
        contenido.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        contenido.add(new JLabel("Sistema de supermercado"), BorderLayout.NORTH);
        contenido.add(campos, BorderLayout.CENTER);
        setContentPane(contenido);
        getRootPane().setDefaultButton(ingresar);
        ingresar.addActionListener(evento -> autenticar());
        pack(); setResizable(false); setLocationRelativeTo(null);
    }

    private void autenticar() {
        String username = usuario.getText();
        char[] contrasena = clave.getPassword();
        clave.setText("");
        ingresar.setEnabled(false);
        error.setText("Validando...");
        new SwingWorker<Usuario, Void>() {
            @Override protected Usuario doInBackground() {
                try { return servicio.autenticar(username, contrasena); }
                finally { Arrays.fill(contrasena, '\0'); }
            }
            @Override protected void done() {
                if (!isDisplayable()) { return; }
                try {
                    Usuario autenticado = get();
                    Sesion.setUsuario(autenticado);
                    dispose();
                    new MenuPrincipalFrame(autenticado).setVisible(true);
                } catch (Exception e) {
                    error.setText(Mensajes.texto(e));
                    ingresar.setEnabled(true);
                    clave.requestFocusInWindow();
                }
            }
        }.execute();
    }
}
