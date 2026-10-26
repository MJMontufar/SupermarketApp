package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Producto;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.servicio.CatalogoService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public final class ProductosFrame extends JFrame {
    private static final long serialVersionUID=1L;
    private final transient Usuario actor;
    private transient List<Producto> productos=List.of();
    private final JButton nuevo=new JButton("Nuevo producto"),editar=new JButton("Editar seleccionado"),actualizar=new JButton("Actualizar");
    private final DefaultTableModel modelo=new DefaultTableModel(new String[]{"Codigo","Nombre","Precio Q","Impuesto %","Stock","Estado"},0){private static final long serialVersionUID=1L;@Override public boolean isCellEditable(int f,int c){return false;}};
    private final JTable tabla=new JTable(modelo);
    private boolean ocupado;
    public ProductosFrame(Usuario actor){
        super("Supermercado - Productos");this.actor=actor;setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        tabla.setName("productos.tabla");nuevo.setName("productos.nuevo");editar.setName("productos.editar");
        TableRowSorter<DefaultTableModel> orden=new TableRowSorter<>(modelo);tabla.setRowSorter(orden);tabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        JTextField filtro=new JTextField(25);filtro.setName("productos.buscar");filtro.getDocument().addDocumentListener(new DocumentListener(){
            private void filtrar(){String s=filtro.getText();orden.setRowFilter(s.isBlank()?null:RowFilter.regexFilter("(?i)"+Pattern.quote(s),0,1));}
            @Override public void insertUpdate(DocumentEvent e){filtrar();}@Override public void removeUpdate(DocumentEvent e){filtrar();}@Override public void changedUpdate(DocumentEvent e){filtrar();}
        });
        JPanel norte=new JPanel(new FlowLayout(FlowLayout.LEFT));norte.add(new JLabel("Buscar codigo o nombre:"));norte.add(filtro);
        JPanel botones=new JPanel(new FlowLayout(FlowLayout.LEFT));botones.add(nuevo);botones.add(editar);botones.add(actualizar);
        JPanel contenido=new JPanel(new BorderLayout(10,10));contenido.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));contenido.add(norte,BorderLayout.NORTH);contenido.add(new JScrollPane(tabla),BorderLayout.CENTER);contenido.add(botones,BorderLayout.SOUTH);setContentPane(contenido);setSize(940,460);setLocationRelativeTo(null);
        nuevo.addActionListener(e->formulario(null));editar.addActionListener(e->{int fila=tabla.getSelectedRow();if(fila>=0){formulario(productos.get(tabla.convertRowIndexToModel(fila)));}});actualizar.addActionListener(e->cargar());tabla.getSelectionModel().addListSelectionListener(e->controles());cargar();
    }
    private void controles(){nuevo.setEnabled(!ocupado);actualizar.setEnabled(!ocupado);editar.setEnabled(!ocupado&&tabla.getSelectedRow()>=0);}
    private void formulario(Producto producto){new ProductoDialog(this,producto,actor).setVisible(true);if(isDisplayable()){cargar();}}
    private void cargar(){ocupado=true;controles();Tareas.ejecutar(this,()->new CatalogoService().productos(actor),lista->{productos=lista;modelo.setRowCount(0);for(Producto p:lista){modelo.addRow(new Object[]{p.getCodigoBarras(),p.getNombre(),p.getPrecio(),p.getPorcentajeImpuesto(),p.getStock(),p.isActivo()?"Activo":"Inactivo"});}},()->{ocupado=false;controles();});}
}
