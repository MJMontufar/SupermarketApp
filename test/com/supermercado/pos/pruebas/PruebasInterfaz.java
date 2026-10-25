package com.supermercado.pos.pruebas;

import com.supermercado.pos.Main;
import com.supermercado.pos.dominio.Rol;
import com.supermercado.pos.ui.LoginFrame;
import com.supermercado.pos.ui.MenuPrincipalFrame;
import com.supermercado.pos.ui.UsuarioDialog;
import com.supermercado.pos.ui.UsuariosFrame;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Configuracion;
import java.awt.Component;
import java.awt.Container;
import java.awt.Robot;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Prueba ventanas y eventos Swing reales; necesita escritorio o Xvfb y POS_TEST. */
public final class PruebasInterfaz {
    private PruebasInterfaz() { }

    private static <T extends Window> T ventana(Class<T> tipo) {
        for (Window ventana : Window.getWindows()) {
            if (ventana.isShowing() && tipo.isInstance(ventana)) { return tipo.cast(ventana); }
        }
        return null;
    }

    private static Component buscar(Component componente, String nombre) {
        if (nombre.equals(componente.getName())) { return componente; }
        if (componente instanceof Container contenedor) {
            for (Component hijo : contenedor.getComponents()) {
                Component encontrado = buscar(hijo, nombre);
                if (encontrado != null) { return encontrado; }
            }
        }
        return null;
    }

    private static <T extends Component> T campo(Window ventana, String nombre, Class<T> tipo) {
        Component componente = buscar(ventana, nombre);
        if (componente == null) { throw new AssertionError("No existe el control " + nombre); }
        return tipo.cast(componente);
    }

    private static void esperar(BooleanSupplier condicion) throws Exception {
        long limite = System.nanoTime() + 20_000_000_000L;
        while (System.nanoTime() < limite) {
            AtomicBoolean cumple = new AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> cumple.set(condicion.getAsBoolean()));
            if (cumple.get()) { return; }
            Thread.sleep(50);
        }
        throw new AssertionError("La interfaz no alcanzo el estado esperado en veinte segundos");
    }

    private static <T extends Window> T esperarVentana(Class<T> tipo) throws Exception {
        esperar(() -> ventana(tipo) != null);
        AtomicReference<T> referencia = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> referencia.set(ventana(tipo)));
        return referencia.get();
    }

    private static void ingresar(LoginFrame login, String usuario, String clave) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            campo(login, "login.usuario", JTextField.class).setText(usuario);
            campo(login, "login.clave", JPasswordField.class).setText(clave);
            campo(login, "login.ingresar", JButton.class).doClick();
        });
    }

    private static void capturar(Window ventana, String nombre) throws Exception {
        Path carpeta = Path.of(System.getProperty("pos.capturas", "out/capturas"));
        Files.createDirectories(carpeta);
        new Robot().waitForIdle();
        ImageIO.write(new Robot().createScreenCapture(ventana.getBounds()), "png", carpeta.resolve(nombre).toFile());
    }

    private static void limpiarUsuario(String username) throws Exception {
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                for (String sql : new String[] {
                    "DELETE FROM usuario_rol WHERE usuario_id IN (SELECT id FROM usuario WHERE username = ?)",
                    "DELETE FROM usuario WHERE username = ?"}) {
                    try (PreparedStatement ps = con.prepareStatement(sql)) { ps.setString(1, username); ps.executeUpdate(); }
                }
                con.commit();
            } catch (Exception e) { con.rollback(); throw e; }
        }
    }

    public static void main(String[] args) throws Exception {
        if (!"pos_test".equalsIgnoreCase(Configuracion.cargar().usuario())) {
            throw new IllegalStateException("La prueba grafica solo se ejecuta en POS_TEST");
        }
        // La automatizacion recibe una clave de BD ya configurada localmente.
        try (Connection con = Conexion.obtener(); PreparedStatement ps = con.prepareStatement(
                "SELECT SYS_CONTEXT('USERENV','SESSION_USER'), SYS_CONTEXT('USERENV','CON_NAME') FROM dual");
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            if (!"POS_TEST".equals(rs.getString(1)) || !"XEPDB1".equals(rs.getString(2))) {
                throw new IllegalStateException("Se requiere POS_TEST en XEPDB1");
            }
        }
        String username = "p_ui_" + UUID.randomUUID().toString().substring(0, 8);
        int correctas = 0;
        try {
            Main.main(new String[0]);
            LoginFrame login = esperarVentana(LoginFrame.class);
            capturar(login, "login.png");
            ingresar(login, "admin", "ClaveIncorrecta2026");
            esperar(() -> campo(login, "login.error", JLabel.class).getText().contains("invalidos"));
            System.out.println("OK    Login grafico rechaza una clave incorrecta"); correctas++;
            ingresar(login, "admin", "Admin123!");
            MenuPrincipalFrame menu = esperarVentana(MenuPrincipalFrame.class);
            esperar(() -> campo(menu, "menu.usuarios", JButton.class).isEnabled());
            SwingUtilities.invokeAndWait(() -> campo(menu, "menu.usuarios", JButton.class).doClick());
            UsuariosFrame usuarios = esperarVentana(UsuariosFrame.class);
            esperar(() -> campo(usuarios, "usuarios.nuevo", JButton.class).isEnabled());
            System.out.println("OK    Admin accede a la lista de usuarios mediante la interfaz"); correctas++;
            SwingUtilities.invokeLater(() -> campo(usuarios, "usuarios.nuevo", JButton.class).doClick());
            UsuarioDialog dialogo = esperarVentana(UsuarioDialog.class);
            capturar(dialogo, "alta-usuario.png");
            SwingUtilities.invokeAndWait(() -> {
                campo(dialogo, "usuario.username", JTextField.class).setText(username);
                campo(dialogo, "usuario.nombre", JTextField.class).setText("Prueba de interfaz");
                campo(dialogo, "usuario.clave", JPasswordField.class).setText("ClavePrueba2026");
                JComboBox<?> roles = campo(dialogo, "usuario.rol", JComboBox.class);
                boolean encontrado = false;
                for (int i = 0; i < roles.getItemCount(); i++) {
                    if (((Rol) roles.getItemAt(i)).getNombre().equals("CAJERO")) {
                        roles.setSelectedIndex(i); encontrado = true; break;
                    }
                }
                if (!encontrado) { throw new AssertionError("No se ofrece el rol CAJERO"); }
                campo(dialogo, "usuario.guardar", JButton.class).doClick();
            });
            esperar(() -> !dialogo.isDisplayable());
            esperar(() -> {
                JTable tabla = campo(usuarios, "usuarios.tabla", JTable.class);
                for (int i = 0; i < tabla.getRowCount(); i++) {
                    if (username.equals(tabla.getValueAt(i, 0))) { return true; }
                }
                return false;
            });
            capturar(usuarios, "usuarios.png");
            System.out.println("OK    Alta desde el dialogo guarda en Oracle y actualiza la tabla"); correctas++;
            SwingUtilities.invokeAndWait(() -> campo(menu, "menu.salir", JButton.class).doClick());
            LoginFrame segundo = esperarVentana(LoginFrame.class);
            ingresar(segundo, username, "ClavePrueba2026");
            MenuPrincipalFrame cajero = esperarVentana(MenuPrincipalFrame.class);
            esperar(() -> !campo(cajero, "menu.usuarios", JButton.class).isEnabled());
            capturar(cajero, "menu-cajero.png");
            System.out.println("OK    Nuevo cajero autentica y tiene administracion deshabilitada"); correctas++;
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                for (Window ventana : Window.getWindows()) { ventana.dispose(); }
            });
            limpiarUsuario(username);
            Conexion.limpiarClaveTemporal();
            System.out.printf("%nPruebas interfaz: 4; correctas: %d; fallos o pendientes: %d%n", correctas, 4 - correctas);
        }
    }
}
