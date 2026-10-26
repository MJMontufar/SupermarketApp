package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.Permiso;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class PermisoDAO {
    public List<Permiso> listar(Connection con) throws SQLException {
        try (PreparedStatement ps=con.prepareStatement("SELECT id,codigo,descripcion FROM permiso ORDER BY codigo"); ResultSet rs=ps.executeQuery()) {
            List<Permiso> salida=new ArrayList<>();
            while(rs.next()){salida.add(new Permiso(rs.getLong(1),rs.getString(2),rs.getString(3)));}
            return List.copyOf(salida);
        }
    }
}
