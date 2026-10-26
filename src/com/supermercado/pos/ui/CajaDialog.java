package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.CajaCatalogo;
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

public final class CajaDialog extends JDialog {
    private static final long serialVersionUID=1L;
    private final transient CajaCatalogo original;
    private final transient Usuario actor;
    private final JSpinner numero=new JSpinner(new SpinnerNumberModel(1,1,9999,1));
    private final JTextField nombre=new JTextField(25);
    private final JCheckBox activa=new JCheckBox("Activa",true);
    private final JButton guardar=new JButton("Guardar"),cancelar=new JButton("Cancelar");
    private boolean ocupado;
    public CajaDialog(Window padre,CajaCatalogo original,Usuario actor){
        super(padre,original==null?"Nueva caja":"Editar caja",ModalityType.APPLICATION_MODAL);this.original=original;this.actor=actor;
        Tema.primario(guardar);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){if(!ocupado){dispose();}}});
        numero.setName("caja.numero");nombre.setName("caja.nombre");activa.setName("caja.activa");guardar.setName("caja.guardar");
        if(original!=null){numero.setValue(original.numero());nombre.setText(original.nombre());activa.setSelected(original.activa());}
        JPanel campos=new JPanel(new GridLayout(0,2,10,10));campos.setBorder(javax.swing.BorderFactory.createEmptyBorder(16,16,16,16));
        campos.add(new JLabel("Numero de caja:"));campos.add(numero);campos.add(new JLabel("Nombre:"));campos.add(nombre);campos.add(new JLabel("Estado:"));campos.add(activa);
        JPanel botones=new JPanel(new FlowLayout(FlowLayout.RIGHT));botones.add(cancelar);botones.add(guardar);add(campos,BorderLayout.CENTER);add(botones,BorderLayout.SOUTH);pack();setLocationRelativeTo(padre);
        guardar.addActionListener(e->guardar());cancelar.addActionListener(e->dispose());getRootPane().setDefaultButton(guardar);
    }
    private void controles(){for(var campo:new javax.swing.JComponent[]{numero,nombre,activa,guardar,cancelar}){campo.setEnabled(!ocupado);}}
    private void guardar(){
        try{
            numero.commitEdit();int valor=((Number)numero.getValue()).intValue();String texto=nombre.getText();boolean estado=activa.isSelected();
            ocupado=true;controles();Tareas.ejecutar(this,()->new CatalogoService().guardarCaja(original,valor,texto,estado,actor),c->dispose(),()->{ocupado=false;controles();});
        }catch(java.text.ParseException e){Mensajes.error(this,new com.supermercado.pos.excepcion.ReglaNegocioException("Revisa el numero de caja"));}
    }
}
