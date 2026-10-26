package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Producto;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.servicio.CatalogoService;
import com.supermercado.pos.servicio.FotoProductoService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public final class ProductosFrame extends JFrame {
    private static final long serialVersionUID=1L;
    private final transient Usuario actor;
    private final transient FotoProductoService fotos=new FotoProductoService();
    private transient List<Producto> productos=List.of();
    private final JButton nuevo=new JButton("Nuevo producto"),editar=new JButton("Editar seleccionado"),actualizar=new JButton("Actualizar");
    private final JButton cambiarFoto=new JButton("Elegir fotografia..."),quitarFoto=new JButton("Quitar fotografia");
    private final JLabel imagen=new JLabel(Tema.sinFoto());
    private final JLabel nombre=new JLabel("Selecciona un producto");
    private final JLabel detalle=new JLabel(" ");
    private final JLabel estadoFoto=new JLabel("JPG o PNG · hasta 10 MB");
    private final DefaultTableModel modelo=new DefaultTableModel(new String[]{"Codigo","Nombre","Precio Q","Impuesto %","Stock","Estado"},0){private static final long serialVersionUID=1L;@Override public boolean isCellEditable(int f,int c){return false;}};
    private final JTable tabla=new JTable(modelo);
    private boolean ocupado;
    private boolean guardandoFoto;
    private boolean hayFoto;
    private long versionFoto;
    private record VistaFoto(BufferedImage imagen,String mensaje){ }
    public ProductosFrame(Usuario actor){
        super("Supermercado - Productos");this.actor=actor;setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter(){
            @Override public void windowClosing(java.awt.event.WindowEvent e){if(!guardandoFoto){dispose();}}
        });
        tabla.setName("productos.tabla");nuevo.setName("productos.nuevo");editar.setName("productos.editar");Tema.primario(nuevo);
        cambiarFoto.setName("productos.foto.cambiar");quitarFoto.setName("productos.foto.quitar");imagen.setName("productos.foto.imagen");estadoFoto.setName("productos.foto.estado");
        imagen.setHorizontalAlignment(JLabel.CENTER);Tema.primario(cambiarFoto);Tema.peligro(quitarFoto);
        TableRowSorter<DefaultTableModel> orden=new TableRowSorter<>(modelo);tabla.setRowSorter(orden);tabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        JTextField filtro=new JTextField(25);filtro.setName("productos.buscar");filtro.getDocument().addDocumentListener(new DocumentListener(){
            private void filtrar(){String s=filtro.getText();orden.setRowFilter(s.isBlank()?null:RowFilter.regexFilter("(?i)"+Pattern.quote(s),0,1));}
            @Override public void insertUpdate(DocumentEvent e){filtrar();}@Override public void removeUpdate(DocumentEvent e){filtrar();}@Override public void changedUpdate(DocumentEvent e){filtrar();}
        });
        JPanel consulta=new JPanel(new FlowLayout(FlowLayout.LEFT));consulta.add(new JLabel("Buscar codigo o nombre:"));consulta.add(filtro);
        JPanel norte=new JPanel(new BorderLayout(0,14));norte.add(Tema.cabecera("Catalogo de productos","Precios, existencias y fotografia opcional."),BorderLayout.NORTH);norte.add(consulta,BorderLayout.SOUTH);
        JPanel botones=new JPanel(new FlowLayout(FlowLayout.LEFT));botones.add(nuevo);botones.add(editar);botones.add(actualizar);
        JPanel ficha=new JPanel(new BorderLayout(10,16));ficha.setPreferredSize(new java.awt.Dimension(260,0));ficha.setBackground(java.awt.Color.WHITE);
        ficha.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new java.awt.Color(0xDFE6EE)),BorderFactory.createEmptyBorder(16,16,16,16)));
        JPanel textos=new JPanel(new GridLayout(0,1,0,7));textos.setOpaque(false);nombre.setFont(nombre.getFont().deriveFont(java.awt.Font.BOLD,15));textos.add(nombre);textos.add(detalle);
        JPanel acciones=new JPanel(new GridLayout(0,1,0,8));acciones.setOpaque(false);acciones.add(estadoFoto);acciones.add(cambiarFoto);acciones.add(quitarFoto);
        ficha.add(textos,BorderLayout.NORTH);ficha.add(imagen,BorderLayout.CENTER);ficha.add(acciones,BorderLayout.SOUTH);
        JPanel contenido=new JPanel(new BorderLayout(18,18));contenido.setBorder(BorderFactory.createEmptyBorder(22,22,22,22));
        contenido.add(norte,BorderLayout.NORTH);contenido.add(new JScrollPane(tabla),BorderLayout.CENTER);contenido.add(ficha,BorderLayout.EAST);contenido.add(botones,BorderLayout.SOUTH);
        setContentPane(contenido);setSize(1180,690);Tema.acomodar(this);
        nuevo.addActionListener(e->formulario(null));editar.addActionListener(e->{Producto p=seleccionado();if(p!=null){formulario(p);}});actualizar.addActionListener(e->cargar());
        tabla.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting()){mostrarFoto();}});
        cambiarFoto.addActionListener(e->elegirFoto());quitarFoto.addActionListener(e->quitarFoto());cargar();
    }
    private Producto seleccionado(){int fila=tabla.getSelectedRow();return fila<0?null:productos.get(tabla.convertRowIndexToModel(fila));}
    private void controles(){
        boolean libre=!ocupado&&!guardandoFoto;Producto p=seleccionado();tabla.setEnabled(libre);
        nuevo.setEnabled(libre);actualizar.setEnabled(libre);editar.setEnabled(libre&&p!=null);cambiarFoto.setEnabled(libre&&p!=null);quitarFoto.setEnabled(libre&&p!=null&&hayFoto);
    }
    private void formulario(Producto producto){new ProductoDialog(this,producto,actor).setVisible(true);if(isDisplayable()){cargar();}}
    private void cargar(){ocupado=true;controles();Tareas.ejecutar(this,()->new CatalogoService().productos(actor),lista->{tabla.clearSelection();productos=lista;modelo.setRowCount(0);for(Producto p:lista){modelo.addRow(new Object[]{p.getCodigoBarras(),p.getNombre(),p.getPrecio(),p.getPorcentajeImpuesto(),p.getStock(),p.isActivo()?"Activo":"Inactivo"});}mostrarFoto();},()->{ocupado=false;controles();});}
    private void mostrarFoto(){
        long version=++versionFoto;Producto p=seleccionado();hayFoto=false;imagen.setIcon(Tema.sinFoto());
        nombre.setText(p==null?"Selecciona un producto":p.getNombre());nombre.setToolTipText(p==null?null:p.getNombre());detalle.setText(p==null?" ":"Q "+p.getPrecio()+" · Stock "+p.getStock());
        estadoFoto.setText(p==null?"JPG o PNG · hasta 10 MB":"Cargando fotografia...");controles();if(p==null){return;}
        Tareas.ejecutar(this,()->{
            try{return new VistaFoto(fotos.cargar(p.getId(),actor).orElse(null),"");}catch(RuntimeException e){return new VistaFoto(null,"Foto no disponible; puedes reemplazarla.");}
        },vista->{if(version==versionFoto&&seleccionado()!=null&&seleccionado().getId().equals(p.getId())){verFoto(vista.imagen());if(!vista.mensaje().isEmpty()){estadoFoto.setText(vista.mensaje());}controles();}},()->{ });
    }
    private void verFoto(BufferedImage foto){hayFoto=foto!=null;imagen.setIcon(foto==null?Tema.sinFoto():Tema.miniatura(foto,220,220));estadoFoto.setText(foto==null?"Sin fotografia · JPG o PNG":"Fotografia guardada");}
    private void elegirFoto(){
        Producto p=seleccionado();if(p==null){return;}
        JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Elegir fotografia de "+p.getNombre());chooser.setAcceptAllFileFilterUsed(false);chooser.setFileFilter(new FileNameExtensionFilter("Fotografias JPG o PNG","jpg","jpeg","png"));
        if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION){return;}guardarFoto(p,chooser.getSelectedFile().toPath());
    }
    private void guardarFoto(Producto p,Path archivo){
        guardandoFoto=true;versionFoto++;controles();estadoFoto.setText("Guardando fotografia...");
        boolean[] guardada={false};
        Tareas.ejecutar(this,()->fotos.guardar(p.getId(),archivo,actor),foto->{guardada[0]=true;verFoto(foto);},()->{
            guardandoFoto=false;if(!isDisplayable()){return;}if(guardada[0]){controles();}else{mostrarFoto();}
        });
    }
    private void quitarFoto(){
        Producto p=seleccionado();if(p==null||JOptionPane.showConfirmDialog(this,"¿Quitar la fotografia del catalogo?","Quitar fotografia",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION){return;}
        guardandoFoto=true;versionFoto++;controles();
        Tareas.ejecutar(this,()->{fotos.quitar(p.getId(),actor);return true;},ignorado->verFoto(null),()->{guardandoFoto=false;controles();});
    }
}
