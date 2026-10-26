package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Rol;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.servicio.TurnoService;
import com.supermercado.pos.util.Sesion;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public final class MenuPrincipalFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private UsuariosFrame usuariosAbiertos;
    private RolesFrame rolesAbiertos;
    private ProductosFrame productosAbiertos;
    private CajasFrame cajasAbiertas;

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
        JButton caja = new JButton("Abrir / continuar turno de caja");
        caja.setName("menu.caja");
        caja.setEnabled(usuario.tienePermiso("TURNO_ABRIR") && usuario.tienePermiso("VENTA_COBRAR"));
        botones.add(caja);
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
        JButton roles = new JButton("Roles y permisos");
        roles.setName("menu.roles"); roles.setEnabled(usuario.tienePermiso("ROL_ADMIN"));
        roles.addActionListener(e -> {
            if (rolesAbiertos == null || !rolesAbiertos.isDisplayable()) { rolesAbiertos = new RolesFrame(usuario); }
            rolesAbiertos.setVisible(true); rolesAbiertos.toFront();
        }); botones.add(roles);
        JButton asignacion = new JButton("Asignar roles y permisos a usuarios");
        asignacion.setName("menu.asignacion"); asignacion.setEnabled(usuario.tienePermiso("ROL_ADMIN"));
        asignacion.addActionListener(e -> new AsignacionPermisosDialog(this, usuario).setVisible(true)); botones.add(asignacion);
        JButton productos = new JButton("Catalogo de productos");
        productos.setName("menu.productos"); productos.setEnabled(usuario.tienePermiso("CATALOGO_ADMIN"));
        productos.addActionListener(e -> {
            if (productosAbiertos == null || !productosAbiertos.isDisplayable()) { productosAbiertos = new ProductosFrame(usuario); }
            productosAbiertos.setVisible(true); productosAbiertos.toFront();
        }); botones.add(productos);
        JButton cajas = new JButton("Catalogo de cajas");
        cajas.setName("menu.cajas"); cajas.setEnabled(usuario.tienePermiso("CATALOGO_ADMIN"));
        cajas.addActionListener(e -> {
            if (cajasAbiertas == null || !cajasAbiertas.isDisplayable()) { cajasAbiertas = new CajasFrame(usuario); }
            cajasAbiertas.setVisible(true); cajasAbiertas.toFront();
        }); botones.add(cajas);
        JButton salir = new JButton("Cerrar sesion");
        salir.setName("menu.salir");
        caja.addActionListener(evento -> {
            caja.setEnabled(false); salir.setEnabled(false);
            Tareas.ejecutar(this, () -> new TurnoService().actual(usuario), actual -> {
                Turno turno = actual.orElse(null);
                if (turno == null) {
                    AperturaTurnoFrame apertura = new AperturaTurnoFrame(this, usuario);
                    apertura.setVisible(true); turno = apertura.getTurnoCreado();
                }
                if (turno != null) {
                    PosFrame puntoVenta = new PosFrame(usuario, turno);
                    puntoVenta.addWindowListener(new WindowAdapter() {
                        @Override public void windowClosed(WindowEvent e) {
                            if (isDisplayable()) { setVisible(true); }
                        }
                    });
                    // El menu permanece oculto mientras la caja esta abierta en pantalla.
                    // Cerrar esta ventana de caja permite salir de sesion sin cerrar el turno.
                    setVisible(false); puntoVenta.setVisible(true);
                }
            }, () -> { caja.setEnabled(true); salir.setEnabled(true); });
        });
        salir.addActionListener(evento -> {
            Sesion.cerrar();
            for (Window ventana : Window.getWindows()) { ventana.dispose(); }
            new LoginFrame().setVisible(true);
        });
        botones.add(salir);
        contenido.add(botones, BorderLayout.CENTER);
        setContentPane(contenido); setSize(560, 560); setLocationRelativeTo(null);
    }
}
