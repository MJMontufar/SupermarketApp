package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.CatalogoDAO;
import com.supermercado.pos.dao.ProductoDAO;
import com.supermercado.pos.dominio.CajaCatalogo;
import com.supermercado.pos.dominio.DatosProducto;
import com.supermercado.pos.dominio.Producto;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Conexion;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public final class CatalogoService {
    private final CatalogoDAO dao=new CatalogoDAO();
    private final ProductoDAO productos=new ProductoDAO();
    public List<Producto> productos(Usuario actor){
        Reglas.requerir(actor,"CATALOGO_ADMIN");
        try(Connection con=Conexion.obtener()){return dao.productos(con);}catch(SQLException e){throw new IllegalStateException("No se pudieron listar productos",e);}
    }
    public List<CajaCatalogo> cajas(Usuario actor){
        Reglas.requerir(actor,"CATALOGO_ADMIN");
        try(Connection con=Conexion.obtener()){return dao.cajas(con);}catch(SQLException e){throw new IllegalStateException("No se pudieron listar cajas",e);}
    }
    public Producto guardarProducto(Producto original,DatosProducto datos,Usuario actor){
        Reglas.requerir(actor,"CATALOGO_ADMIN");
        if(datos==null){throw new ReglaNegocioException("Faltan datos del producto");}
        String codigo=Reglas.texto(datos.codigo(),"Codigo",30);
        if(codigo.chars().anyMatch(Character::isWhitespace)){throw new ReglaNegocioException("El codigo no admite espacios");}
        String nombre=Reglas.texto(datos.nombre(),"Nombre",120);
        var precio=Reglas.monto(datos.precio(),"Precio",false);var impuesto=Reglas.monto(datos.impuesto(),"Impuesto",false);
        if(impuesto.compareTo(new BigDecimal("999.99"))>0||datos.stock()<0||datos.stock()>99999999){throw new ReglaNegocioException("Revisa el impuesto o el stock: estan fuera del rango permitido");}
        DatosProducto limpio=new DatosProducto(codigo,nombre,precio,impuesto,datos.stock(),datos.activo());
        try(Connection con=Conexion.obtener()){
            con.setAutoCommit(false);
            try{
                long id;
                if(original==null){id=dao.insertarProducto(con,limpio);}
                else{
                    Producto actual=productos.bloquear(con,original.getId()).orElseThrow(()->new ReglaNegocioException("El producto no existe"));
                    if(!mismo(actual,original)){throw new ReglaNegocioException("El producto cambio desde que abriste el formulario; actualiza la lista y vuelve a editar");}
                    id=actual.getId();dao.actualizarProducto(con,id,limpio);
                }
                Producto salida=productos.bloquear(con,id).orElseThrow();con.commit();return salida;
            }catch(SQLException|RuntimeException e){con.rollback();if(e instanceof SQLException sql&&sql.getErrorCode()==1){throw new ReglaNegocioException("Ese codigo de producto ya existe");}throw e;}
        }catch(SQLException e){throw new IllegalStateException("No se pudo guardar el producto",e);}
    }
    public CajaCatalogo guardarCaja(CajaCatalogo original,int numero,String nombre,boolean activa,Usuario actor){
        Reglas.requerir(actor,"CATALOGO_ADMIN");String texto=Reglas.texto(nombre,"Nombre",60);
        if(numero<1||numero>9999){throw new ReglaNegocioException("El numero de caja debe estar entre 1 y 9999");}
        try(Connection con=Conexion.obtener()){
            con.setAutoCommit(false);
            try{
                long id;
                if(original==null){id=dao.insertarCaja(con,numero,texto,activa);}
                else{
                    CajaCatalogo actual=dao.bloquearCaja(con,original.id()).orElseThrow(()->new ReglaNegocioException("La caja no existe"));
                    if(!actual.equals(original)){throw new ReglaNegocioException("La caja cambio; actualiza la lista antes de guardar");}
                    if((!activa||numero!=actual.numero())&&dao.cajaOcupada(con,actual.id())){throw new ReglaNegocioException("Cierra el turno antes de desactivar la caja o cambiar su numero");}
                    id=actual.id();dao.actualizarCaja(con,id,numero,texto,activa);
                }
                CajaCatalogo salida=dao.bloquearCaja(con,id).orElseThrow();con.commit();return salida;
            }catch(SQLException|RuntimeException e){con.rollback();if(e instanceof SQLException sql&&sql.getErrorCode()==1){throw new ReglaNegocioException("Ese numero de caja ya existe");}throw e;}
        }catch(SQLException e){throw new IllegalStateException("No se pudo guardar la caja",e);}
    }
    private boolean mismo(Producto a,Producto b){
        return Objects.equals(a.getId(),b.getId())&&a.getCodigoBarras().equals(b.getCodigoBarras())&&a.getNombre().equals(b.getNombre())
                &&a.getPrecio().compareTo(b.getPrecio())==0&&a.getPorcentajeImpuesto().compareTo(b.getPorcentajeImpuesto())==0&&a.getStock()==b.getStock()&&a.isActivo()==b.isActivo();
    }
}
