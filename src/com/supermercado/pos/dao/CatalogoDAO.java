package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.CajaCatalogo;
import com.supermercado.pos.dominio.DatosProducto;
import com.supermercado.pos.dominio.Producto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CatalogoDAO {
    public List<Producto> productos(Connection con)throws SQLException {
        try(PreparedStatement ps=con.prepareStatement("SELECT * FROM producto ORDER BY nombre");ResultSet rs=ps.executeQuery()){
            List<Producto> salida=new ArrayList<>(); ProductoDAO mapa=new ProductoDAO();
            while(rs.next()){salida.add(mapa.mapear(rs));} return List.copyOf(salida);
        }
    }
    public long insertarProducto(Connection con,DatosProducto p)throws SQLException {
        try(PreparedStatement ps=con.prepareStatement("INSERT INTO producto (codigo_barras,nombre,precio,porcentaje_impuesto,stock,activo) VALUES (?,?,?,?,?,?)",new String[]{"ID"})){
            datos(ps,p);ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){rs.next();return rs.getLong(1);}
        }
    }
    public void actualizarProducto(Connection con,long id,DatosProducto p)throws SQLException {
        try(PreparedStatement ps=con.prepareStatement("UPDATE producto SET codigo_barras=?,nombre=?,precio=?,porcentaje_impuesto=?,stock=?,activo=? WHERE id=?")){
            datos(ps,p);ps.setLong(7,id);ps.executeUpdate();
        }
    }
    private void datos(PreparedStatement ps,DatosProducto p)throws SQLException {
        ps.setString(1,p.codigo());ps.setString(2,p.nombre());ps.setBigDecimal(3,p.precio());ps.setBigDecimal(4,p.impuesto());ps.setInt(5,p.stock());ps.setInt(6,p.activo()?1:0);
    }
    public List<CajaCatalogo> cajas(Connection con)throws SQLException {
        try(PreparedStatement ps=con.prepareStatement("SELECT id,numero,nombre,activa FROM caja ORDER BY numero");ResultSet rs=ps.executeQuery()){
            List<CajaCatalogo> salida=new ArrayList<>();while(rs.next()){salida.add(mapearCaja(rs));}return List.copyOf(salida);
        }
    }
    public Optional<CajaCatalogo> bloquearCaja(Connection con,long id)throws SQLException {
        try(PreparedStatement ps=con.prepareStatement("SELECT id,numero,nombre,activa FROM caja WHERE id=? FOR UPDATE")){
            ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){return rs.next()?Optional.of(mapearCaja(rs)):Optional.empty();}
        }
    }
    public boolean cajaOcupada(Connection con,long id)throws SQLException {
        try(PreparedStatement ps=con.prepareStatement("SELECT COUNT(*) FROM turno WHERE caja_id=? AND estado='ABIERTO'")){
            ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){rs.next();return rs.getInt(1)>0;}
        }
    }
    public long insertarCaja(Connection con,int numero,String nombre,boolean activa)throws SQLException {
        try(PreparedStatement ps=con.prepareStatement("INSERT INTO caja (numero,nombre,activa) VALUES (?,?,?)",new String[]{"ID"})){
            ps.setInt(1,numero);ps.setString(2,nombre);ps.setInt(3,activa?1:0);ps.executeUpdate();
            try(ResultSet rs=ps.getGeneratedKeys()){rs.next();return rs.getLong(1);}
        }
    }
    public void actualizarCaja(Connection con,long id,int numero,String nombre,boolean activa)throws SQLException {
        try(PreparedStatement ps=con.prepareStatement("UPDATE caja SET numero=?,nombre=?,activa=? WHERE id=?")){
            ps.setInt(1,numero);ps.setString(2,nombre);ps.setInt(3,activa?1:0);ps.setLong(4,id);ps.executeUpdate();
        }
    }
    private CajaCatalogo mapearCaja(ResultSet rs)throws SQLException {return new CajaCatalogo(rs.getLong(1),rs.getInt(2),rs.getString(3),rs.getInt(4)==1);}
}
