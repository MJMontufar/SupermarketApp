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
        JPanel contenido = new JPanel(new BorderLayout());
        JPanel lateral = Tema.oscuro(); lateral.setLayout(new BorderLayout(12, 28));
        lateral.setPreferredSize(new java.awt.Dimension(240, 0)); lateral.setBorder(BorderFactory.createEmptyBorder(28, 24, 24, 24));
        JLabel marca = new JLabel("SM / SUPERMERCADO"); marca.setForeground(java.awt.Color.WHITE);
        marca.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 16)); lateral.add(marca, BorderLayout.NORTH);
        JLabel identidad = new JLabel("<html><div style='width:145px'>SESION ACTUAL<br><br><b>" + Tema.escapar(usuario.getUsername()) + "</b><br>"
                + Tema.escapar(usuario.getNombreCompleto() == null ? "" : usuario.getNombreCompleto()) + "<br><br>"
                + Tema.escapar(usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.joining(", "))) + "</div></html>");
        identidad.setVerticalAlignment(JLabel.TOP); identidad.setForeground(new java.awt.Color(0xCBD5E1));
        identidad.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 14)); lateral.add(identidad, BorderLayout.CENTER);
        JPanel principal = new JPanel(new BorderLayout(20, 24)); principal.setBorder(BorderFactory.createEmptyBorder(32, 28, 28, 28));
        principal.add(Tema.cabecera("Panel principal", "Selecciona un modulo para comenzar."), BorderLayout.NORTH);
        JPanel botones = new JPanel(new GridLayout(3, 2, 14, 14)); botones.setOpaque(false);
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
        Tema.tarjeta(caja, "01"); Tema.tarjeta(usuarios, "02"); Tema.tarjeta(roles, "03");
        Tema.tarjeta(asignacion, "04"); Tema.tarjeta(productos, "05"); Tema.tarjeta(cajas, "06");
        JButton salir = new JButton("Cerrar sesion"); Tema.peligro(salir);
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
        lateral.add(salir, BorderLayout.SOUTH);
        principal.add(botones, BorderLayout.CENTER);
        principal.add(Tema.secundario("Operaciones de caja, equipo y catalogos en un mismo lugar."), BorderLayout.SOUTH);
        contenido.add(lateral, BorderLayout.WEST); contenido.add(principal, BorderLayout.CENTER);
        setContentPane(contenido); setSize(1060, 660); Tema.acomodar(this);
    }
}
