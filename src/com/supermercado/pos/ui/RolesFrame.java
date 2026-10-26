package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Permiso;
import com.supermercado.pos.dominio.Rol;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.servicio.RolService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

public final class RolesFrame extends JFrame {
    private static final long serialVersionUID=1L;
    private final transient Usuario actor;
    private final transient RolService servicio=new RolService();
    private transient List<Rol> roles=List.of();
    private transient List<Permiso> permisos=List.of();
    private transient Long editando;
    private boolean ocupado;
    private boolean editable=true;
    private final JTextField nombre=new JTextField(20);
    private final JSpinner nivel=new JSpinner(new SpinnerNumberModel(1,1,99,1));
    private final JButton guardar=new JButton("Guardar rol y permisos");
    private final JButton nuevo=new JButton("Nuevo rol");
    private final JButton actualizar=new JButton("Actualizar");
    private final DefaultTableModel lista=new DefaultTableModel(new String[]{"Rol","Nivel"},0){
        private static final long serialVersionUID=1L;
        @Override public boolean isCellEditable(int f,int c){return false;}
    };
    private final JTable tabla=new JTable(lista);
    private final DefaultTableModel matriz=new DefaultTableModel(new String[]{"Modulo","Permiso","Conceder"},0){
        private static final long serialVersionUID=1L;
        @Override public Class<?> getColumnClass(int c){return c==2?Boolean.class:String.class;}
        @Override public boolean isCellEditable(int f,int c){return c==2&&!ocupado&&editable&&actor.tienePermiso(permisos.get(f).getCodigo());}
    };
    private final JTable permisosTabla=new JTable(matriz);
    private record Carga(List<Rol> roles,List<Permiso> permisos){ }
    public RolesFrame(Usuario actor){
        super("Supermercado - Roles y permisos");this.actor=actor;setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        tabla.setName("roles.lista");permisosTabla.setName("roles.matriz");nombre.setName("roles.nombre");nivel.setName("roles.nivel");guardar.setName("roles.guardar");nuevo.setName("roles.nuevo");
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel datos=new JPanel(new FlowLayout(FlowLayout.LEFT));datos.add(new JLabel("Nombre:"));datos.add(nombre);datos.add(new JLabel("Nivel:"));datos.add(nivel);
        JPanel botones=new JPanel(new FlowLayout(FlowLayout.LEFT));botones.add(nuevo);botones.add(guardar);botones.add(actualizar);
        JPanel contenido=new JPanel(new BorderLayout(12,12));contenido.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JSplitPane division=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,new JScrollPane(tabla),new JScrollPane(permisosTabla));
        division.setDividerLocation(240);division.setResizeWeight(0.25);
        permisosTabla.getColumnModel().getColumn(0).setPreferredWidth(100);
        permisosTabla.getColumnModel().getColumn(1).setPreferredWidth(400);
        permisosTabla.getColumnModel().getColumn(2).setPreferredWidth(75);
        contenido.add(datos,BorderLayout.NORTH);contenido.add(division,BorderLayout.CENTER);contenido.add(botones,BorderLayout.SOUTH);
        setContentPane(contenido);setSize(940,480);setLocationRelativeTo(null);
        tabla.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting()&&!ocupado){seleccionar();}});
        nuevo.addActionListener(e->limpiar());guardar.addActionListener(e->guardar());actualizar.addActionListener(e->cargar());cargar();
    }
    private void controles(){
        tabla.setEnabled(!ocupado);nombre.setEnabled(!ocupado&&editable);nivel.setEnabled(!ocupado&&editable);
        guardar.setEnabled(!ocupado&&editable);nuevo.setEnabled(!ocupado);actualizar.setEnabled(!ocupado);permisosTabla.setEnabled(!ocupado&&editable);
    }
    private void cargar(){
        ocupado=true;controles();
        Tareas.ejecutar(this,()->new Carga(servicio.listar(actor),servicio.permisos(actor)),c->{roles=c.roles();permisos=c.permisos();lista.setRowCount(0);for(Rol r:roles){lista.addRow(new Object[]{r.getNombre(),r.getNivel()});}limpiar();},()->{ocupado=false;controles();});
    }
    private void limpiar(){
        tabla.clearSelection();editando=null;editable=true;nombre.setText("");nivel.setValue(1);mostrarPermisos(Set.of());controles();
    }
    private void seleccionar(){
        int fila=tabla.getSelectedRow();if(fila<0){return;}Rol r=roles.get(fila);editando=r.getId();nombre.setText(r.getNombre());nivel.setValue(r.getNivel());
        editable=r.getNivel()<actor.nivelMaximo()&&actor.getRoles().stream().noneMatch(x->x.getId().equals(r.getId()));
        mostrarPermisos(r.getPermisos().stream().map(Permiso::getId).collect(java.util.stream.Collectors.toSet()));controles();
    }
    private void mostrarPermisos(Set<Long> ids){matriz.setRowCount(0);for(Permiso p:permisos){matriz.addRow(new Object[]{PermisosUI.modulo(p.getCodigo()),p.getCodigo()+" - "+p.getDescripcion(),ids.contains(p.getId())});}}
    private void guardar(){
        try{
            if(permisosTabla.isEditing()){permisosTabla.getCellEditor().stopCellEditing();}nivel.commitEdit();
            Set<Long> ids=new HashSet<>();for(int i=0;i<permisos.size();i++){if(Boolean.TRUE.equals(matriz.getValueAt(i,2))){ids.add(permisos.get(i).getId());}}
            Long id=editando;String texto=nombre.getText();int jerarquia=((Number)nivel.getValue()).intValue();ocupado=true;controles();
            Tareas.ejecutar(this,()->servicio.guardar(id,texto,jerarquia,ids,actor),r->{
                var copia=new java.util.ArrayList<>(roles);copia.removeIf(x->x.getId().equals(r.getId()));copia.add(r);
                copia.sort(java.util.Comparator.comparingInt(Rol::getNivel).thenComparing(Rol::getNombre));
                roles=List.copyOf(copia);lista.setRowCount(0);for(Rol rol:roles){lista.addRow(new Object[]{rol.getNombre(),rol.getNivel()});}limpiar();
            },()->{ocupado=false;controles();});
        }catch(java.text.ParseException e){Mensajes.error(this,new com.supermercado.pos.excepcion.ReglaNegocioException("Revisa el nivel del rol"));}
    }
}
