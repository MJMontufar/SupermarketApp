package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Rol;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.servicio.UsuarioService;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

public final class UsuarioDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private final JTextField username = new JTextField(25);
    private final JTextField nombre = new JTextField(25);
    private final JPasswordField clave = new JPasswordField(25);
    private final JCheckBox activo = new JCheckBox("Usuario activo", true);
    private final JButton guardar = new JButton("Guardar");
    private final JButton cancelar = new JButton("Cancelar");

    public UsuarioDialog(JFrame padre, Usuario actor, Usuario existente, List<Rol> roles) {
        super(padre, existente == null ? "Nuevo usuario" : "Editar usuario", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent evento) { clave.setText(""); }
        });
        username.setName("usuario.username"); nombre.setName("usuario.nombre");
        clave.setName("usuario.clave"); activo.setName("usuario.activo");
        guardar.setName("usuario.guardar"); cancelar.setName("usuario.cancelar");
        JComboBox<Rol> rol = new JComboBox<>(roles.toArray(Rol[]::new));
        rol.setName("usuario.rol");
        JPanel campos = new JPanel(new GridLayout(0, 1, 5, 5));
        campos.add(new JLabel("Usuario (sin espacios)")); campos.add(username);
        campos.add(new JLabel("Nombre completo")); campos.add(nombre);
        campos.add(new JLabel(existente == null ? "Contrasena (minimo 8 caracteres)"
                : "Nueva contrasena (vacia conserva la actual)")); campos.add(clave);
        campos.add(new JLabel("Rol"));
        if (existente == null) { campos.add(rol); }
        else {
            username.setText(existente.getUsername()); username.setEditable(false);
            nombre.setText(existente.getNombreCompleto()); activo.setSelected(existente.isActivo());
            campos.add(new JLabel(existente.getRoles().stream().map(Rol::getNombre).collect(Collectors.joining(", "))));
        }
        activo.setEnabled(existente != null && !existente.getId().equals(actor.getId()));
        campos.add(activo);
        JPanel botones = new JPanel(); botones.add(guardar); botones.add(cancelar);
        JPanel contenido = new JPanel(new BorderLayout(12, 12));
        contenido.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        contenido.add(campos, BorderLayout.CENTER); contenido.add(botones, BorderLayout.SOUTH);
        setContentPane(contenido); getRootPane().setDefaultButton(guardar);
        cancelar.addActionListener(e -> dispose());
        guardar.addActionListener(e -> {
            String textoUsuario = username.getText();
            String textoNombre = nombre.getText().trim();
            boolean estado = activo.isSelected();
            Rol seleccionado = (Rol) rol.getSelectedItem();
            char[] contrasena = clave.getPassword(); clave.setText("");
            guardar.setEnabled(false); cancelar.setEnabled(false);
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            new SwingWorker<Usuario, Void>() {
                @Override protected Usuario doInBackground() {
                    try {
                        UsuarioService servicio = new UsuarioService();
                        if (existente == null) {
                            return servicio.crear(textoUsuario, textoNombre, contrasena,
                                    seleccionado == null ? -1 : seleccionado.getId(), actor);
                        }
                        return servicio.editar(existente.getId(), textoNombre, contrasena, estado, actor);
                    } finally { Arrays.fill(contrasena, '\0'); }
                }
                @Override protected void done() {
                    try { get(); dispose(); }
                    catch (Exception error) { Mensajes.error(UsuarioDialog.this, error); }
                    finally {
                        guardar.setEnabled(true); cancelar.setEnabled(true);
                        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                    }
                }
            }.execute();
        });
        pack(); setResizable(false); setLocationRelativeTo(padre);
    }
}
