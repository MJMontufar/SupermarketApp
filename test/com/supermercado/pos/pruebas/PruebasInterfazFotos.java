package com.supermercado.pos.pruebas;

import static com.supermercado.pos.pruebas.PruebasInterfaz.*;
import com.supermercado.pos.Main;
import com.supermercado.pos.ui.LoginFrame;
import com.supermercado.pos.ui.MenuPrincipalFrame;
import com.supermercado.pos.ui.ProductosFrame;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.FotosProducto;
import com.supermercado.pos.util.Sesion;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Seleccion y persistencia de imagen con JFileChooser y ventanas reales, solo POS_TEST. */
public final class PruebasInterfazFotos {
    private PruebasInterfazFotos(){ }
    private static void verificar(boolean c){if(!c){throw new AssertionError("Resultado inesperado");}}
    private static JFileChooser encontrar(Component c){
        if(c instanceof JFileChooser selector){return selector;}
        if(c instanceof Container contenedor){for(Component hijo:contenedor.getComponents()){JFileChooser selector=encontrar(hijo);if(selector!=null){return selector;}}}return null;
    }
    private static JFileChooser selector(){for(Window w:Window.getWindows()){if(w.isShowing()){JFileChooser s=encontrar(w);if(s!=null){return s;}}}return null;}
    private static JDialog confirmacion(){for(Window w:Window.getWindows()){if(w.isShowing()&&w instanceof JDialog d&&"Quitar fotografia".equals(d.getTitle())){return d;}}return null;}
    private static void seleccionar(ProductosFrame ventana,String codigo)throws Exception{
        esperar(()->campo(ventana,"productos.nuevo",JButton.class).isEnabled());
        SwingUtilities.invokeAndWait(()->{
            JTable tabla=campo(ventana,"productos.tabla",JTable.class);boolean existe=false;
            for(int i=0;i<tabla.getRowCount();i++){if(codigo.equals(tabla.getValueAt(i,0))){tabla.setRowSelectionInterval(i,i);existe=true;break;}}verificar(existe);
        });
    }
    public static void main(String[] args)throws Exception{
        PruebasNegocio.iniciarConexion();java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        PruebasCaja datos=new PruebasCaja();Path carpeta=Files.createTempDirectory("pos-fotos-ui-");String anterior=System.getProperty("pos.fotos");System.setProperty("pos.fotos",carpeta.resolve("catalogo").toString());int correctas=0;
        try{
            datos.preparar();Path original=PruebasFotos.imagen(carpeta.resolve("foto de prueba.png"),"png",new java.awt.Color(0x10B981));
            String codigo;try(var con=Conexion.obtener();var ps=con.prepareStatement("SELECT codigo_barras FROM producto WHERE id=?")){ps.setLong(1,datos.arroz);try(var rs=ps.executeQuery()){rs.next();codigo=rs.getString(1);}}
            Main.main(new String[0]);LoginFrame login=esperarVentana(LoginFrame.class);capturar(login,"login-moderno.png");ingresar(login,"admin","Admin123!");
            MenuPrincipalFrame menu=esperarVentana(MenuPrincipalFrame.class);capturar(menu,"menu-moderno.png");SwingUtilities.invokeAndWait(()->campo(menu,"menu.productos",JButton.class).doClick());
            ProductosFrame productos=esperarVentana(ProductosFrame.class);seleccionar(productos,codigo);esperar(()->campo(productos,"productos.foto.estado",JLabel.class).getText().startsWith("Sin fotografia"));
            SwingUtilities.invokeAndWait(()->{verificar(campo(productos,"productos.foto.cambiar",JButton.class).isEnabled());verificar(!campo(productos,"productos.foto.quitar",JButton.class).isEnabled());});
            capturar(productos,"catalogo-sin-foto.png");System.out.println("OK    Catalogo muestra marcador y permite elegir una foto opcional");correctas++;

            SwingUtilities.invokeLater(()->campo(productos,"productos.foto.cambiar",JButton.class).doClick());esperar(()->selector()!=null);
            SwingUtilities.invokeAndWait(()->{JFileChooser dialogo=selector();dialogo.setSelectedFile(original.toFile());dialogo.approveSelection();});
            esperar(()->campo(productos,"productos.foto.estado",JLabel.class).getText().equals("Fotografia guardada")&&campo(productos,"productos.foto.quitar",JButton.class).isEnabled());
            SwingUtilities.invokeAndWait(()->verificar(campo(productos,"productos.foto.imagen",JLabel.class).getIcon() instanceof ImageIcon));
            verificar(Files.exists(FotosProducto.archivo(datos.arroz)));PruebasCaja.igual(PruebasCaja.numero("SELECT stock FROM producto WHERE id=?",datos.arroz),"30");
            capturar(productos,"catalogo-con-foto.png");System.out.println("OK    JFileChooser guarda y muestra foto sin cambiar precio ni existencias");correctas++;

            SwingUtilities.invokeAndWait(productos::dispose);SwingUtilities.invokeAndWait(()->campo(menu,"menu.productos",JButton.class).doClick());ProductosFrame reabierto=esperarVentana(ProductosFrame.class);seleccionar(reabierto,codigo);
            esperar(()->campo(reabierto,"productos.foto.estado",JLabel.class).getText().equals("Fotografia guardada"));
            SwingUtilities.invokeLater(()->campo(reabierto,"productos.foto.quitar",JButton.class).doClick());esperar(()->confirmacion()!=null);
            SwingUtilities.invokeAndWait(()->PruebasInterfazCaja.boton(confirmacion(),UIManager.getString("OptionPane.yesButtonText")).doClick());
            esperar(()->campo(reabierto,"productos.foto.estado",JLabel.class).getText().startsWith("Sin fotografia"));
            verificar(!Files.exists(FotosProducto.archivo(datos.arroz))&&Files.exists(original));System.out.println("OK    Foto persiste al reabrir; quitar restaura marcador y conserva el original");correctas++;
        }finally{
            try{SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows()){w.dispose();}});}
            finally{
                try{datos.limpiar();try(var archivos=Files.walk(carpeta)){for(Path p:archivos.sorted(Comparator.reverseOrder()).toList()){Files.deleteIfExists(p);}}}
                finally{if(anterior==null){System.clearProperty("pos.fotos");}else{System.setProperty("pos.fotos",anterior);}Sesion.cerrar();Conexion.limpiarClaveTemporal();}
            }
            System.out.printf("%nPruebas interfaz fotos: 3; correctas: %d; fallos o pendientes: %d%n",correctas,3-correctas);
        }
    }
}
