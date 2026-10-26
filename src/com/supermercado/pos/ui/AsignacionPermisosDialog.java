package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Permiso;
import com.supermercado.pos.dominio.Rol;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.servicio.RolService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public final class AsignacionPermisosDialog extends JDialog {
    private static final long serialVersionUID=1L;
    private final transient Usuario actor;
    private final transient RolService servicio=new RolService();
    private transient List<Rol> roles=List.of();
    private transient List<Permiso> permisos=List.of();
    private final JComboBox<Usuario> usuarios=new JComboBox<>();
    private final JButton guardar=new JButton("Guardar asignacion");
    private final JButton cancelar=new JButton("Cerrar");
    private boolean ocupado=true;
    private boolean construyendo;
    private final DefaultTableModel modeloRoles=new DefaultTableModel(new String[]{"Asignar","Rol","Nivel"},0){
        private static final long serialVersionUID=1L;
        @Override public Class<?> getColumnClass(int c){return c==0?Boolean.class:String.class;}
        @Override public boolean isCellEditable(int f,int c){return c==0&&!ocupado;}
    };
    private final DefaultTableModel modeloPermisos=new DefaultTableModel(new String[]{"Modulo","Permiso","Heredado","Extra","Efectivo"},0){
        private static final long serialVersionUID=1L;
        @Override public Class<?> getColumnClass(int c){return c>=2?Boolean.class:String.class;}
        @Override public boolean isCellEditable(int f,int c){return c==3&&!ocupado&&!Boolean.TRUE.equals(getValueAt(f,2))&&actor.tienePermiso(permisos.get(f).getCodigo());}
    };
    private final JTable tablaRoles=new JTable(modeloRoles);
    private final JTable tablaPermisos=new JTable(modeloPermisos){
        private static final long serialVersionUID=1L;
        @Override public java.awt.Component prepareRenderer(javax.swing.table.TableCellRenderer renderer,int fila,int columna){
            java.awt.Component c=super.prepareRenderer(renderer,fila,columna);
            c.setEnabled(!ocupado&&(columna==3?modeloPermisos.isCellEditable(fila,columna):!Boolean.TRUE.equals(modeloPermisos.getValueAt(fila,2))));return c;
        }
    };
    private record Carga(List<Usuario> usuarios,List<Rol> roles,List<Permiso> permisos){ }
    public AsignacionPermisosDialog(Window padre,Usuario actor){
        super(padre,"Supermercado - Asignacion de roles y permisos",ModalityType.APPLICATION_MODAL);this.actor=actor;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){if(!ocupado){dispose();}}});
        usuarios.setName("asignacion.usuario");tablaRoles.setName("asignacion.roles");tablaPermisos.setName("asignacion.permisos");guardar.setName("asignacion.guardar");cancelar.setName("asignacion.cerrar");
        tablaRoles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel arriba=new JPanel(new FlowLayout(FlowLayout.LEFT));arriba.add(new JLabel("Usuario de nivel inferior:"));arriba.add(usuarios);
        JPanel botones=new JPanel(new FlowLayout(FlowLayout.RIGHT));botones.add(new JLabel("Los cambios aplican al siguiente inicio de sesion."));botones.add(cancelar);botones.add(guardar);
        JSplitPane division=new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(tablaRoles),new JScrollPane(tablaPermisos));division.setResizeWeight(0.35);
        JPanel contenido=new JPanel(new BorderLayout(12,12));contenido.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));contenido.add(arriba,BorderLayout.NORTH);contenido.add(division,BorderLayout.CENTER);contenido.add(botones,BorderLayout.SOUTH);
        setContentPane(contenido);setSize(1060,570);setLocationRelativeTo(padre);
        usuarios.addActionListener(e->{if(!construyendo){seleccionar();}});
        modeloRoles.addTableModelListener(e->{if(!construyendo){actualizarEfectivos();}});
        modeloPermisos.addTableModelListener(e->{if(!construyendo&&e.getColumn()==3){actualizarEfectivos();}});
        guardar.addActionListener(e->guardar());cancelar.addActionListener(e->dispose());controles();
        Tareas.ejecutar(this,()->new Carga(servicio.usuariosAsignables(actor),servicio.listar(actor).stream().filter(r->r.getNivel()<actor.nivelMaximo()).toList(),servicio.permisos(actor)),c->{
            construyendo=true;roles=c.roles();permisos=c.permisos();c.usuarios().forEach(usuarios::addItem);construyendo=false;seleccionar();
        },()->{ocupado=false;controles();});
    }
    private void controles(){usuarios.setEnabled(!ocupado);guardar.setEnabled(!ocupado&&usuarios.getSelectedItem()!=null);cancelar.setEnabled(!ocupado);tablaRoles.setEnabled(!ocupado);tablaPermisos.repaint();}
    private Set<Long> elegidos(){Set<Long> ids=new HashSet<>();for(int i=0;i<roles.size();i++){if(Boolean.TRUE.equals(modeloRoles.getValueAt(i,0))){ids.add(roles.get(i).getId());}}return ids;}
    private void seleccionar(){
        Usuario u=(Usuario)usuarios.getSelectedItem();construyendo=true;modeloRoles.setRowCount(0);modeloPermisos.setRowCount(0);
        if(u!=null){
            Set<Long> ids=u.getRoles().stream().map(Rol::getId).collect(java.util.stream.Collectors.toSet());
            Set<Long> extras=u.getPermisosExtra().stream().map(Permiso::getId).collect(java.util.stream.Collectors.toSet());
            for(Rol r:roles){modeloRoles.addRow(new Object[]{ids.contains(r.getId()),r.getNombre(),r.getNivel()});}
            for(Permiso p:permisos){modeloPermisos.addRow(new Object[]{PermisosUI.modulo(p.getCodigo()),p.getCodigo(),false,extras.contains(p.getId()),false});}
        }
        construyendo=false;actualizarEfectivos();controles();
    }
    private void actualizarEfectivos(){
        if(modeloPermisos.getRowCount()!=permisos.size()){return;}
        Set<Long> ids=elegidos();Set<Long> heredados=roles.stream().filter(r->ids.contains(r.getId())).flatMap(r->r.getPermisos().stream()).map(Permiso::getId).collect(java.util.stream.Collectors.toSet());
        construyendo=true;for(int i=0;i<permisos.size();i++){boolean h=heredados.contains(permisos.get(i).getId());modeloPermisos.setValueAt(h,i,2);modeloPermisos.setValueAt(h||Boolean.TRUE.equals(modeloPermisos.getValueAt(i,3)),i,4);}construyendo=false;tablaPermisos.repaint();
    }
    private void guardar(){
        if(tablaRoles.isEditing()){tablaRoles.getCellEditor().stopCellEditing();}if(tablaPermisos.isEditing()){tablaPermisos.getCellEditor().stopCellEditing();}
        Usuario destino=(Usuario)usuarios.getSelectedItem();if(destino==null){return;}
        Set<Long> ids=elegidos();Set<Long> extras=new HashSet<>();for(int i=0;i<permisos.size();i++){if(Boolean.TRUE.equals(modeloPermisos.getValueAt(i,3))){extras.add(permisos.get(i).getId());}}
        ocupado=true;controles();Tareas.ejecutar(this,()->servicio.asignar(destino.getId(),ids,extras,actor),u->dispose(),()->{ocupado=false;controles();});
    }
}
