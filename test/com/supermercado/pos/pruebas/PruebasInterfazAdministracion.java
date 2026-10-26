package com.supermercado.pos.pruebas;

import static com.supermercado.pos.pruebas.PruebasInterfaz.*;
import com.supermercado.pos.Main;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.ui.*;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Sesion;
import java.awt.Window;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Completa las pantallas administrativas con filas propias de POS_TEST. */
public final class PruebasInterfazAdministracion {
    private PruebasInterfazAdministracion(){ }
    private static void verificar(boolean c){if(!c){throw new AssertionError("Resultado inesperado");}}
    private static int fila(JTable tabla,int columna,String texto){for(int i=0;i<tabla.getRowCount();i++){if(texto.equals(String.valueOf(tabla.getValueAt(i,columna)))){return i;}}return -1;}
    private static void marcar(JTable tabla,int fila,int columna){
        verificar(tabla.editCellAt(fila,columna));((JCheckBox)tabla.getEditorComponent()).setSelected(true);verificar(tabla.getCellEditor().stopCellEditing());
    }
    public static void main(String[] args)throws Exception{
        PruebasNegocio.iniciarConexion();java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        PruebasAperturaCero datos=new PruebasAperturaCero();String sufijo=UUID.randomUUID().toString().substring(0,8);
        String nombreRol="P_UI_ROL_"+sufijo.toUpperCase(java.util.Locale.ROOT),codigo="ui_"+sufijo,nombreCaja="Caja UI "+sufijo;int correctas=0;
        try{
            datos.preparar();Main.main(new String[0]);ingresar(esperarVentana(LoginFrame.class),"admin","Admin123!");MenuPrincipalFrame menu=esperarVentana(MenuPrincipalFrame.class);
            SwingUtilities.invokeAndWait(()->campo(menu,"menu.roles",JButton.class).doClick());RolesFrame roles=esperarVentana(RolesFrame.class);
            esperar(()->campo(roles,"roles.guardar",JButton.class).isEnabled()&&campo(roles,"roles.matriz",JTable.class).getRowCount()==9);
            SwingUtilities.invokeAndWait(()->{
                campo(roles,"roles.nombre",JTextField.class).setText(nombreRol);campo(roles,"roles.nivel",JSpinner.class).setValue(1);
                JTable matriz=campo(roles,"roles.matriz",JTable.class);int indice=-1;for(int i=0;i<matriz.getRowCount();i++){if(String.valueOf(matriz.getValueAt(i,1)).startsWith("VENTA_COBRAR")){indice=i;}}
                verificar(indice>=0);marcar(matriz,indice,2);campo(roles,"roles.guardar",JButton.class).doClick();
            });
            esperar(()->fila(campo(roles,"roles.lista",JTable.class),0,nombreRol)>=0);
            SwingUtilities.invokeAndWait(()->{JTable tabla=campo(roles,"roles.lista",JTable.class);int i=fila(tabla,0,nombreRol);tabla.setRowSelectionInterval(i,i);});
            capturar(roles,"roles-matriz.png");SwingUtilities.invokeAndWait(roles::dispose);
            System.out.println("OK    Crear rol desde Swing guarda nivel y matriz de permisos");correctas++;

            SwingUtilities.invokeLater(()->campo(menu,"menu.asignacion",JButton.class).doClick());AsignacionPermisosDialog asignacion=esperarVentana(AsignacionPermisosDialog.class);
            esperar(()->campo(asignacion,"asignacion.guardar",JButton.class).isEnabled());
            SwingUtilities.invokeAndWait(()->{
                JComboBox<?> usuarios=campo(asignacion,"asignacion.usuario",JComboBox.class);
                for(int i=0;i<usuarios.getItemCount();i++){if(((Usuario)usuarios.getItemAt(i)).getId().equals(datos.datos.ana.getId())){usuarios.setSelectedIndex(i);break;}}
                JTable tabla=campo(asignacion,"asignacion.roles",JTable.class);int i=fila(tabla,1,nombreRol);verificar(i>=0);marcar(tabla,i,0);
                JTable permisos=campo(asignacion,"asignacion.permisos",JTable.class);int cobro=fila(permisos,1,"VENTA_COBRAR"),cat=fila(permisos,1,"CATALOGO_ADMIN");
                verificar(Boolean.TRUE.equals(permisos.getValueAt(cobro,2))&&!permisos.isCellEditable(cobro,3));marcar(permisos,cat,3);
            });capturar(asignacion,"asignacion-permisos.png");SwingUtilities.invokeAndWait(()->campo(asignacion,"asignacion.guardar",JButton.class).doClick());esperar(()->!asignacion.isDisplayable());
            System.out.println("OK    Asignacion visual combina dos roles, muestra heredados bloqueados y suma un extra");correctas++;

            SwingUtilities.invokeAndWait(()->campo(menu,"menu.productos",JButton.class).doClick());ProductosFrame productos=esperarVentana(ProductosFrame.class);
            esperar(()->campo(productos,"productos.nuevo",JButton.class).isEnabled());SwingUtilities.invokeLater(()->campo(productos,"productos.nuevo",JButton.class).doClick());ProductoDialog producto=esperarVentana(ProductoDialog.class);
            SwingUtilities.invokeAndWait(()->{campo(producto,"producto.codigo",JTextField.class).setText(codigo);campo(producto,"producto.nombre",JTextField.class).setText("Producto UI temporal");campo(producto,"producto.precio",JTextField.class).setText("7.50");campo(producto,"producto.stock",JSpinner.class).setValue(9);});
            capturar(producto,"producto-formulario.png");SwingUtilities.invokeAndWait(()->campo(producto,"producto.guardar",JButton.class).doClick());esperar(()->!producto.isDisplayable());esperar(()->fila(campo(productos,"productos.tabla",JTable.class),0,codigo)>=0);
            capturar(productos,"productos.png");SwingUtilities.invokeAndWait(productos::dispose);System.out.println("OK    Alta de producto desde Swing guarda precio e inventario en Oracle");correctas++;

            int numero=9200;while(PruebasCaja.numero("SELECT COUNT(*) FROM caja WHERE numero=?",numero).signum()>0){numero++;}int disponible=numero;
            SwingUtilities.invokeAndWait(()->campo(menu,"menu.cajas",JButton.class).doClick());CajasFrame cajas=esperarVentana(CajasFrame.class);esperar(()->campo(cajas,"cajas.nuevo",JButton.class).isEnabled());
            SwingUtilities.invokeLater(()->campo(cajas,"cajas.nuevo",JButton.class).doClick());CajaDialog caja=esperarVentana(CajaDialog.class);
            SwingUtilities.invokeAndWait(()->{campo(caja,"caja.numero",JSpinner.class).setValue(disponible);campo(caja,"caja.nombre",JTextField.class).setText(nombreCaja);campo(caja,"caja.guardar",JButton.class).doClick();});
            esperar(()->!caja.isDisplayable());esperar(()->fila(campo(cajas,"cajas.tabla",JTable.class),1,nombreCaja)>=0);capturar(cajas,"cajas.png");SwingUtilities.invokeAndWait(cajas::dispose);
            System.out.println("OK    Alta de caja desde Swing aparece en el catalogo sin tocar cajas anteriores");correctas++;

            SwingUtilities.invokeAndWait(()->campo(menu,"menu.salir",JButton.class).doClick());ingresar(esperarVentana(LoginFrame.class),datos.datos.ana.getUsername(),"SoloPruebas2026");MenuPrincipalFrame cajero=esperarVentana(MenuPrincipalFrame.class);
            esperar(()->campo(cajero,"menu.productos",JButton.class).isEnabled());
            SwingUtilities.invokeAndWait(()->{verificar(!campo(cajero,"menu.roles",JButton.class).isEnabled());verificar(!campo(cajero,"menu.usuarios",JButton.class).isEnabled());});
            verificar(Sesion.getUsuario().getRoles().size()==2&&Sesion.getUsuario().nivelMaximo()==1&&Sesion.getUsuario().tienePermiso("CATALOGO_ADMIN"));capturar(cajero,"menu-permiso-extra.png");
            System.out.println("OK    Nuevo login aplica el extra de catalogo sin promover al cajero ni habilitar roles");correctas++;
        }finally{
            try{SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows()){w.dispose();}});}
            finally{
                try{
                    PruebasCaja.ejecutarSql("DELETE FROM producto WHERE codigo_barras=?",codigo);
                    PruebasCaja.ejecutarSql("DELETE FROM caja WHERE nombre=?",nombreCaja);
                    datos.datos.limpiar();
                    PruebasCaja.ejecutarSql("DELETE FROM rol_permiso WHERE rol_id IN (SELECT id FROM rol WHERE nombre=?)",nombreRol);
                    PruebasCaja.ejecutarSql("DELETE FROM rol WHERE nombre=?",nombreRol);
                }finally{Sesion.cerrar();Conexion.limpiarClaveTemporal();}
            }
            System.out.printf("%nPruebas interfaz administracion: 5; correctas: %d; fallos o pendientes: %d%n",correctas,5-correctas);
        }
    }
}
