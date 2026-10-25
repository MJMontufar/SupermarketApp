package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Rol;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.util.Sesion;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public final class MenuPrincipalFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private UsuariosFrame usuariosAbiertos;

    public MenuPrincipalFrame(Usuario usuario) {
        super("Supermercado - Inicio");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel contenido = new JPanel(new BorderLayout(16, 16));
        contenido.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JPanel datos = new JPanel(new GridLayout(0, 1, 8, 8));
        datos.add(new JLabel("Usuario: " + usuario.getUsername()));
        datos.add(new JLabel("Nombre: " + usuario.getNombreCompleto()));
        datos.add(new JLabel("Roles: " + usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.joining(", "))));
        contenido.add(datos, BorderLayout.NORTH);
        JPanel botones = new JPanel(new GridLayout(0, 1, 8, 8));
        JButton usuarios = new JButton("Administrar usuarios");
        usuarios.setName("menu.usuarios");
        usuarios.setEnabled(usuario.tienePermiso("USUARIO_ADMIN"));
        usuarios.addActionListener(evento -> {
            if (usuariosAbiertos == null || !usuariosAbiertos.isDisplayable()) {
                usuariosAbiertos = new UsuariosFrame(usuario);
            }
            usuariosAbiertos.setVisible(true); usuariosAbiertos.toFront();
        });
        botones.add(usuarios);
        JButton salir = new JButton("Cerrar sesion");
        salir.setName("menu.salir");
        salir.addActionListener(evento -> {
            Sesion.cerrar();
            for (Window ventana : Window.getWindows()) { ventana.dispose(); }
            new LoginFrame().setVisible(true);
        });
        botones.add(salir);
        contenido.add(botones, BorderLayout.CENTER);
        setContentPane(contenido); setSize(490, 270); setLocationRelativeTo(null);
    }
}
