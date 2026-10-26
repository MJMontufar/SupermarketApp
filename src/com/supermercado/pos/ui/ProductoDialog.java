package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.DatosProducto;
import com.supermercado.pos.dominio.Producto;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.servicio.CatalogoService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

public final class ProductoDialog extends JDialog {
    private static final long serialVersionUID=1L;
    private final transient Producto original;
    private final transient Usuario actor;
    private final JTextField codigo=new JTextField(25),nombre=new JTextField(25),precio=new JTextField("0.00"),impuesto=new JTextField("12.00");
    private final JSpinner stock=new JSpinner(new SpinnerNumberModel(0,0,99999999,1));
    private final JCheckBox activo=new JCheckBox("Activo",true);
    private final JButton guardar=new JButton("Guardar"),cancelar=new JButton("Cancelar");
    private boolean ocupado;
    public ProductoDialog(Window padre,Producto original,Usuario actor){
        super(padre,original==null?"Nuevo producto":"Editar producto",ModalityType.APPLICATION_MODAL);this.original=original;this.actor=actor;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){if(!ocupado){dispose();}}});
        codigo.setName("producto.codigo");nombre.setName("producto.nombre");precio.setName("producto.precio");impuesto.setName("producto.impuesto");stock.setName("producto.stock");activo.setName("producto.activo");guardar.setName("producto.guardar");
        if(original!=null){codigo.setText(original.getCodigoBarras());nombre.setText(original.getNombre());precio.setText(original.getPrecio().toPlainString());impuesto.setText(original.getPorcentajeImpuesto().toPlainString());stock.setValue(original.getStock());activo.setSelected(original.isActivo());}
        JPanel campos=new JPanel(new GridLayout(0,2,10,10));campos.setBorder(javax.swing.BorderFactory.createEmptyBorder(16,16,16,16));
        campos.add(new JLabel("Codigo de barras:"));campos.add(codigo);campos.add(new JLabel("Nombre:"));campos.add(nombre);campos.add(new JLabel("Precio con impuesto (Q):"));campos.add(precio);campos.add(new JLabel("Porcentaje impuesto:"));campos.add(impuesto);campos.add(new JLabel("Existencias:"));campos.add(stock);campos.add(new JLabel("Estado:"));campos.add(activo);
        JPanel botones=new JPanel(new FlowLayout(FlowLayout.RIGHT));botones.add(cancelar);botones.add(guardar);add(campos,BorderLayout.CENTER);add(botones,BorderLayout.SOUTH);pack();setLocationRelativeTo(padre);
        guardar.addActionListener(e->guardar());cancelar.addActionListener(e->dispose());getRootPane().setDefaultButton(guardar);
    }
    private void controles(){for(var campo:new javax.swing.JComponent[]{codigo,nombre,precio,impuesto,stock,activo,guardar,cancelar}){campo.setEnabled(!ocupado);}}
    private void guardar(){
        try{
            stock.commitEdit();DatosProducto datos=new DatosProducto(codigo.getText(),nombre.getText(),Campos.montoExacto(precio),Campos.montoExacto(impuesto),((Number)stock.getValue()).intValue(),activo.isSelected());
            ocupado=true;controles();Tareas.ejecutar(this,()->new CatalogoService().guardarProducto(original,datos,actor),p->dispose(),()->{ocupado=false;controles();});
        }catch(java.text.ParseException e){Mensajes.error(this,new com.supermercado.pos.excepcion.ReglaNegocioException("Revisa las existencias"));}catch(RuntimeException e){Mensajes.error(this,e);}
    }
}
