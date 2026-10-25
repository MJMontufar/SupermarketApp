package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Rol;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.servicio.UsuarioService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public final class UsuariosFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private final transient Usuario actor;
    private final transient UsuarioService servicio = new UsuarioService();
    private transient List<Usuario> usuarios = List.of();
    private transient List<Rol> roles = List.of();
    private boolean cargando;
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[] {"Usuario", "Nombre", "Estado", "Roles"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final JButton nuevo = new JButton("Nuevo usuario");
    private final JButton editar = new JButton("Editar seleccionado");
    private final JButton actualizar = new JButton("Actualizar");

    private record Carga(List<Usuario> usuarios, List<Rol> roles) { }

    public UsuariosFrame(Usuario actor) {
        super("Supermercado - Usuarios");
        this.actor = actor;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        tabla.setName("usuarios.tabla");
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        nuevo.setName("usuarios.nuevo"); editar.setName("usuarios.editar");
        actualizar.setName("usuarios.actualizar");
        JTextField busqueda = new JTextField(28);
        TableRowSorter<DefaultTableModel> ordenador = new TableRowSorter<>(modelo);
        tabla.setRowSorter(ordenador);
        busqueda.getDocument().addDocumentListener(new DocumentListener() {
            private void filtrar() {
                String texto = busqueda.getText().trim();
                ordenador.setRowFilter(texto.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(texto), 0, 1));
            }
            @Override public void insertUpdate(DocumentEvent e) { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(new JLabel("Buscar usuario o nombre:")); norte.add(busqueda);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(nuevo); botones.add(editar); botones.add(actualizar);
        JPanel contenido = new JPanel(new BorderLayout(10, 10));
        contenido.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        contenido.add(norte, BorderLayout.NORTH);
        contenido.add(new JScrollPane(tabla), BorderLayout.CENTER);
        contenido.add(botones, BorderLayout.SOUTH);
        setContentPane(contenido); setSize(820, 450); setLocationRelativeTo(null);
        nuevo.addActionListener(e -> abrirFormulario(null));
        editar.addActionListener(e -> abrirFormulario(seleccionado()));
        actualizar.addActionListener(e -> cargar());
        tabla.getSelectionModel().addListSelectionListener(e -> actualizarBotones());
        cargar();
    }

    private Usuario seleccionado() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : usuarios.get(tabla.convertRowIndexToModel(fila));
    }

    private void actualizarBotones() {
        nuevo.setEnabled(!cargando && actor.tienePermiso("ROL_ADMIN") && !roles.isEmpty());
        Usuario seleccionado = seleccionado();
        editar.setEnabled(!cargando && seleccionado != null && (seleccionado.getId().equals(actor.getId())
                || seleccionado.nivelMaximo() < actor.nivelMaximo()));
    }

    private void abrirFormulario(Usuario usuario) {
        new UsuarioDialog(this, actor, usuario, roles).setVisible(true);
        if (isDisplayable()) { cargar(); }
    }

    private void cargar() {
        if (cargando) { return; }
        cargando = true;
        nuevo.setEnabled(false); editar.setEnabled(false); actualizar.setEnabled(false);
        new SwingWorker<Carga, Void>() {
            @Override protected Carga doInBackground() {
                return new Carga(servicio.listar(actor), actor.tienePermiso("ROL_ADMIN")
                        ? servicio.rolesAsignables(actor) : List.of());
            }
            @Override protected void done() {
                if (!isDisplayable()) { return; }
                try {
                    Carga datos = get(); usuarios = datos.usuarios(); roles = datos.roles();
                    tabla.clearSelection(); modelo.setRowCount(0);
                    for (Usuario usuario : usuarios) {
                        modelo.addRow(new Object[] {usuario.getUsername(), usuario.getNombreCompleto(),
                            usuario.isActivo() ? "Activo" : "Inactivo",
                            usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.joining(", "))});
                    }
                } catch (Exception e) { Mensajes.error(UsuariosFrame.this, e); }
                finally { cargando = false; actualizar.setEnabled(true); actualizarBotones(); }
            }
        }.execute();
    }
}
