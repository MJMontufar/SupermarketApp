package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.CajaCatalogo;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.servicio.CatalogoService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public final class CajasFrame extends JFrame {
    private static final long serialVersionUID=1L;
    private final transient Usuario actor;
    private transient List<CajaCatalogo> cajas=List.of();
    private final JButton nuevo=new JButton("Nueva caja"),editar=new JButton("Editar seleccionada"),actualizar=new JButton("Actualizar");
    private final DefaultTableModel modelo=new DefaultTableModel(new String[]{"Numero","Nombre","Estado"},0){private static final long serialVersionUID=1L;@Override public boolean isCellEditable(int f,int c){return false;}};
    private final JTable tabla=new JTable(modelo);
    private boolean ocupado;
    public CajasFrame(Usuario actor){
        super("Supermercado - Cajas");this.actor=actor;setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        tabla.setName("cajas.tabla");nuevo.setName("cajas.nuevo");editar.setName("cajas.editar");tabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        JPanel botones=new JPanel(new FlowLayout(FlowLayout.LEFT));botones.add(nuevo);botones.add(editar);botones.add(actualizar);
        JPanel contenido=new JPanel(new BorderLayout(10,10));contenido.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));contenido.add(new JScrollPane(tabla),BorderLayout.CENTER);contenido.add(botones,BorderLayout.SOUTH);setContentPane(contenido);setSize(640,420);setLocationRelativeTo(null);
        nuevo.addActionListener(e->formulario(null));editar.addActionListener(e->{int fila=tabla.getSelectedRow();if(fila>=0){formulario(cajas.get(fila));}});actualizar.addActionListener(e->cargar());tabla.getSelectionModel().addListSelectionListener(e->controles());cargar();
    }
    private void controles(){nuevo.setEnabled(!ocupado);actualizar.setEnabled(!ocupado);editar.setEnabled(!ocupado&&tabla.getSelectedRow()>=0);}
    private void formulario(CajaCatalogo caja){new CajaDialog(this,caja,actor).setVisible(true);if(isDisplayable()){cargar();}}
    private void cargar(){ocupado=true;controles();Tareas.ejecutar(this,()->new CatalogoService().cajas(actor),lista->{cajas=lista;modelo.setRowCount(0);for(CajaCatalogo c:lista){modelo.addRow(new Object[]{c.numero(),c.nombre(),c.activa()?"Activa":"Inactiva"});}},()->{ocupado=false;controles();});}
}
