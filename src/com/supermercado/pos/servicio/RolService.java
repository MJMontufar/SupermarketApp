package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.PermisoDAO;
import com.supermercado.pos.dao.RolDAO;
import com.supermercado.pos.dao.UsuarioDAO;
import com.supermercado.pos.dominio.Permiso;
import com.supermercado.pos.dominio.Rol;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.AutorizacionException;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Conexion;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RolService {
    private final RolDAO roles=new RolDAO();
    private final PermisoDAO permisos=new PermisoDAO();
    private final UsuarioDAO usuarios=new UsuarioDAO();
    public List<Rol> listar(Usuario actor) {
        Reglas.requerir(actor,"ROL_ADMIN");
        try(Connection con=Conexion.obtener()){return roles.listar(con);}
        catch(SQLException e){throw new IllegalStateException("No se pudieron listar roles",e);}
    }
    public List<Permiso> permisos(Usuario actor) {
        Reglas.requerir(actor,"ROL_ADMIN");
        try(Connection con=Conexion.obtener()){return permisos.listar(con);}
        catch(SQLException e){throw new IllegalStateException("No se pudieron listar permisos",e);}
    }
    public List<Usuario> usuariosAsignables(Usuario actor) {
        Reglas.requerir(actor,"ROL_ADMIN");
        try(Connection con=Conexion.obtener()){
            return usuarios.listar(con).stream().filter(u->!u.getId().equals(actor.getId())&&u.nivelMaximo()<actor.nivelMaximo()).toList();
        }catch(SQLException e){throw new IllegalStateException("No se pudieron listar usuarios asignables",e);}
    }
    public Rol guardar(Long id,String nombre,int nivel,Set<Long> seleccion,Usuario actor) {
        Reglas.requerir(actor,"ROL_ADMIN");
        String texto=Reglas.texto(nombre == null ? null : nombre.toUpperCase(Locale.ROOT),"Nombre de rol",50);
        if(nivel<1||nivel>=actor.nivelMaximo()){throw new AutorizacionException("El nivel del rol debe ser positivo e inferior al tuyo");}
        Set<Long> ids=ids(seleccion);
        try(Connection con=Conexion.obtener()){
            con.setAutoCommit(false);
            try{
                if(id!=null){
                    Rol actual=roles.bloquear(con,id).orElseThrow(()->new ReglaNegocioException("El rol no existe"));
                    if(actual.getNivel()>=actor.nivelMaximo()||actor.getRoles().stream().anyMatch(r->r.getId().equals(id))){
                        throw new AutorizacionException("No puedes editar un rol propio ni de nivel igual o superior");
                    }
                }
                validarPermisos(con,ids,actor);
                long destino=id==null?roles.insertar(con,texto,nivel):id;
                if(id!=null){roles.actualizar(con,id,texto,nivel);}
                roles.reemplazarPermisos(con,destino,ids);
                Rol salida=roles.buscarPorId(con,destino).orElseThrow(); con.commit(); return salida;
            }catch(SQLException|RuntimeException e){
                con.rollback(); if(e instanceof SQLException sql&&sql.getErrorCode()==1){throw new ReglaNegocioException("Ese nombre de rol ya existe");} throw e;
            }
        }catch(SQLException e){throw new IllegalStateException("No se pudo guardar el rol",e);}
    }
    public Usuario asignar(long id,Set<Long> rolesElegidos,Set<Long> extras,Usuario actor) {
        Reglas.requerir(actor,"ROL_ADMIN");
        if(actor.getId()==id){throw new ReglaNegocioException("No puedes modificar tus propios roles ni permisos extra");}
        Set<Long> r=ids(rolesElegidos); Set<Long> p=ids(extras);
        if(r.isEmpty()){throw new ReglaNegocioException("Selecciona al menos un rol");}
        try(Connection con=Conexion.obtener()){
            con.setAutoCommit(false);
            try{
                Usuario destino=usuarios.buscarPorId(con,id,true).orElseThrow(()->new ReglaNegocioException("El usuario no existe"));
                if(destino.nivelMaximo()>=actor.nivelMaximo()){throw new AutorizacionException("No puedes cambiar usuarios de nivel igual o superior");}
                for(long rolId:r.stream().sorted().toList()){
                    Rol rol=roles.bloquear(con,rolId).orElseThrow(()->new ReglaNegocioException("Uno de los roles no existe"));
                    if(rol.getNivel()>=actor.nivelMaximo()){throw new AutorizacionException("Solo puedes asignar roles inferiores al tuyo");}
                    if(rol.getPermisos().stream().anyMatch(permiso->!actor.tienePermiso(permiso.getCodigo()))){
                        throw new AutorizacionException("Ese rol contiene permisos que no puedes conceder");
                    }
                }
                validarPermisos(con,p,actor);
                usuarios.reemplazarAsignacion(con,id,r,p);
                Usuario salida=usuarios.buscarPorId(con,id,false).orElseThrow(); con.commit(); return salida;
            }catch(SQLException|RuntimeException e){con.rollback();throw e;}
        }catch(SQLException e){throw new IllegalStateException("No se pudo guardar la asignacion",e);}
    }
    private Set<Long> ids(Set<Long> conjunto) {
        if(conjunto==null||conjunto.stream().anyMatch(java.util.Objects::isNull)){throw new ReglaNegocioException("La seleccion contiene valores invalidos");}
        return Set.copyOf(conjunto);
    }
    private void validarPermisos(Connection con,Set<Long> ids,Usuario actor)throws SQLException {
        List<Permiso> disponibles=permisos.listar(con);
        for(long id:ids){
            Permiso p=disponibles.stream().filter(x->x.getId()==id).findFirst().orElseThrow(()->new ReglaNegocioException("Uno de los permisos no existe"));
            if(!actor.tienePermiso(p.getCodigo())){throw new AutorizacionException("No puedes conceder permisos que no tienes");}
        }
    }
}
