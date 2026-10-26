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
        Tema.primario(ingresar);
        JPanel campos = new JPanel(new GridLayout(0, 1, 6, 6)); campos.setOpaque(false);
        campos.add(new JLabel("Usuario")); campos.add(usuario);
        campos.add(new JLabel("Contrasena")); campos.add(clave);
        campos.add(error); campos.add(ingresar);
        JPanel formulario = new JPanel(new BorderLayout(16, 24)); formulario.setBackground(Color.WHITE);
        formulario.setBorder(BorderFactory.createEmptyBorder(44, 36, 44, 36));
        formulario.add(Tema.cabecera("Bienvenido", "Ingresa con tu cuenta de trabajo."), BorderLayout.NORTH);
        formulario.add(campos, BorderLayout.CENTER);
        formulario.add(Tema.secundario("Punto de venta y administracion"), BorderLayout.SOUTH);
        JPanel marca = Tema.oscuro(); marca.setLayout(new BorderLayout(16, 30)); marca.setPreferredSize(new java.awt.Dimension(290, 430));
        marca.setBorder(BorderFactory.createEmptyBorder(40, 28, 40, 28));
        JLabel logo = new JLabel("SM / SUPERMERCADO"); logo.setForeground(Color.WHITE); logo.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 17));
        JLabel mensaje = new JLabel("<html>Todo listo<br>para una buena<br>jornada.</html>");
        mensaje.setVerticalAlignment(JLabel.TOP); mensaje.setForeground(Color.WHITE);
        mensaje.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 28));
        mensaje.setBorder(BorderFactory.createEmptyBorder(62, 0, 0, 0));
        JLabel pie = new JLabel("Ventas · Equipo · Inventario"); pie.setForeground(new Color(0xA7F3D0));
        marca.add(logo, BorderLayout.NORTH); marca.add(mensaje, BorderLayout.CENTER); marca.add(pie, BorderLayout.SOUTH);
        JPanel contenido = new JPanel(new BorderLayout()); contenido.add(marca, BorderLayout.WEST); contenido.add(formulario, BorderLayout.CENTER);
        setContentPane(contenido);
        getRootPane().setDefaultButton(ingresar);
        ingresar.addActionListener(evento -> autenticar());
        setSize(810, 500); Tema.acomodar(this); setResizable(false);
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
