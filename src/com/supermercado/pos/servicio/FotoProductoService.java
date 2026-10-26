package com.supermercado.pos.servicio;

import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.FotosProducto;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class FotoProductoService {
    private void verificar(long producto,Usuario actor){
        Reglas.requerir(actor,"CATALOGO_ADMIN");
        try(Connection con=Conexion.obtener();PreparedStatement ps=con.prepareStatement("SELECT id FROM producto WHERE id=?")){
            ps.setLong(1,producto);try(ResultSet rs=ps.executeQuery()){if(!rs.next()){throw new ReglaNegocioException("El producto no existe");}}
        }catch(SQLException e){throw new IllegalStateException("No se pudo consultar el producto",e);}
    }
    public BufferedImage guardar(long producto,Path archivo,Usuario actor){
        verificar(producto,actor);
        try{BufferedImage imagen=FotosProducto.preparar(archivo);FotosProducto.guardar(producto,imagen);return imagen;}
        catch(IOException e){throw new ReglaNegocioException("No se pudo guardar la foto. Elige un JPG o PNG valido de hasta 10 MB y revisa el acceso a la carpeta de fotos.");}
    }
    public Optional<BufferedImage> cargar(long producto,Usuario actor){
        verificar(producto,actor);
        try{return FotosProducto.cargar(producto);}
        catch(IOException e){throw new ReglaNegocioException("La foto guardada no se pudo leer; puedes reemplazarla o quitarla");}
    }
    public void quitar(long producto,Usuario actor){
        verificar(producto,actor);
        try{FotosProducto.quitar(producto);}
        catch(IOException e){throw new ReglaNegocioException("No se pudo quitar la foto; revisa el acceso a la carpeta de fotos");}
    }
}
