package com.supermercado.pos.pruebas;

import static com.supermercado.pos.pruebas.PruebasCaja.*;
import com.supermercado.pos.dao.RolDAO;
import com.supermercado.pos.dominio.*;
import com.supermercado.pos.excepcion.AutorizacionException;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.servicio.*;
import com.supermercado.pos.util.Conexion;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Administracion real en POS_TEST: jerarquia, permisos efectivos, catalogos y escrituras concurrentes. */
public final class PruebasAdministracion {
    private final PruebasAperturaCero datos=new PruebasAperturaCero();
    private final RolService roles=new RolService();
    private final CatalogoService catalogo=new CatalogoService();
    private final List<Long> propios=new ArrayList<>();
    private Usuario admin;
    private long cajero,supervisor,rolAdmin,venta,cat,rolPermiso;
    private int ejecutadas,fallos;
    @FunctionalInterface private interface Prueba{void ejecutar()throws Exception;}
    private void caso(String nombre,Prueba prueba){
        ejecutadas++;
        try{
            datos.datos.limpiarOperaciones();
            roles.asignar(datos.datos.ana.getId(),Set.of(cajero),Set.of(),admin);
            roles.asignar(datos.datos.luis.getId(),Set.of(cajero),Set.of(),admin);
            roles.asignar(datos.supervisora.getId(),Set.of(supervisor),Set.of(),admin);
            ejecutarSql("UPDATE producto SET stock=30,precio=30,activo=1 WHERE id=?",datos.datos.arroz);
            ejecutarSql("UPDATE caja SET activa=1 WHERE id=?",datos.datos.cajas.get(0));
            prueba.ejecutar();System.out.println("OK    "+nombre);
        }catch(Exception|AssertionError e){fallos++;System.out.println("FALLO "+nombre);e.printStackTrace(System.out);}
    }
    private Usuario entrar(Usuario usuario){
        char[] clave="SoloPruebas2026".toCharArray();try{return new AuthService().autenticar(usuario.getUsername(),clave);}finally{Arrays.fill(clave,'\0');}
    }
    private Rol crear(int nivel,Set<Long> permisos){
        Rol r=roles.guardar(null,"p_rol_"+UUID.randomUUID().toString().substring(0,8),nivel,permisos,admin);propios.add(r.getId());return r;
    }
    private Producto producto(){return catalogo.productos(admin).stream().filter(p->p.getId()==datos.datos.arroz).findFirst().orElseThrow();}
    private DatosProducto datos(Producto p,boolean activo){return new DatosProducto(p.getCodigoBarras(),p.getNombre(),p.getPrecio(),p.getPorcentajeImpuesto(),p.getStock(),activo);}
    private CajaCatalogo caja(){return catalogo.cajas(admin).stream().filter(c->c.id()==datos.datos.cajas.get(0)).findFirst().orElseThrow();}
    private void ejecutar(){
        caso("Crear rol normaliza nombre y guarda matriz completa",()->{
            Rol r=crear(1,Set.of(venta,cat));verificar(r.getNombre().equals(r.getNombre().toUpperCase(java.util.Locale.ROOT))&&r.getPermisos().size()==2);
        });
        caso("Crear rol de nivel propio, superior o cero rechazado",()->{
            for(int nivel:new int[]{0,4,5}){rechaza(AutorizacionException.class,()->roles.guardar(null,"No permitido",nivel,Set.of(),admin));}
        });
        caso("No se puede editar el rol ADMIN propio",()->{
            rechaza(AutorizacionException.class,()->roles.guardar(rolAdmin,"ADMIN CAMBIADO",1,Set.of(),admin));
            verificar(roles.listar(admin).stream().filter(r->r.getId()==rolAdmin).findFirst().orElseThrow().getNombre().equals("ADMIN"));
        });
        caso("Nombre de rol duplicado no modifica el original",()->{
            Rol r=crear(1,Set.of(venta));rechaza(ReglaNegocioException.class,()->roles.guardar(null,r.getNombre(),2,Set.of(cat),admin));
            verificar(roles.listar(admin).stream().filter(x->x.getId().equals(r.getId())).findFirst().orElseThrow().getPermisos().size()==1);
        });
        caso("No puedes cambiar tus propios roles ni extras",()->rechaza(ReglaNegocioException.class,()->roles.asignar(admin.getId(),Set.of(cajero),Set.of(),admin)));
        caso("Asignacion admite varios roles y hereda sus permisos",()->{
            Rol r=crear(2,Set.of(cat));Usuario u=roles.asignar(datos.datos.ana.getId(),Set.of(cajero,r.getId()),Set.of(),admin);
            verificar(u.getRoles().size()==2&&u.nivelMaximo()==2&&u.tienePermiso("CATALOGO_ADMIN")&&u.tienePermiso("VENTA_COBRAR"));
        });
        caso("Extra aplica al siguiente login sin elevar nivel y puede quitarse",()->{
            Usuario anterior=entrar(datos.datos.ana);roles.asignar(anterior.getId(),Set.of(cajero),Set.of(cat),admin);
            verificar(!anterior.tienePermiso("CATALOGO_ADMIN"));Usuario nuevo=entrar(anterior);verificar(nuevo.tienePermiso("CATALOGO_ADMIN")&&nuevo.nivelMaximo()==1);
            roles.asignar(nuevo.getId(),Set.of(cajero),Set.of(),admin);verificar(!entrar(nuevo).tienePermiso("CATALOGO_ADMIN"));
        });
        caso("Asignar rol igual al del actor se rechaza sin cambiar usuario",()->{
            rechaza(AutorizacionException.class,()->roles.asignar(datos.datos.ana.getId(),Set.of(rolAdmin),Set.of(),admin));verificar(entrar(datos.datos.ana).nivelMaximo()==1);
        });
        caso("No se modifica a un usuario de nivel superior",()->{
            ejecutarSql("INSERT INTO usuario_permiso (usuario_id,permiso_id) VALUES (?,?)",datos.supervisora.getId(),rolPermiso);
            Usuario actor=entrar(datos.supervisora);rechaza(AutorizacionException.class,()->roles.asignar(datos.gerente.getId(),Set.of(cajero),Set.of(),actor));
        });
        caso("Rol inexistente hace rollback y conserva asignacion original",()->{
            rechaza(ReglaNegocioException.class,()->roles.asignar(datos.datos.ana.getId(),Set.of(cajero,-1L),Set.of(),admin));verificar(entrar(datos.datos.ana).getRoles().size()==1);
        });
        caso("Permiso inexistente no deja asignacion parcial",()->{
            Rol r=crear(1,Set.of());rechaza(ReglaNegocioException.class,()->roles.asignar(datos.datos.ana.getId(),Set.of(r.getId()),Set.of(-1L),admin));
            verificar(entrar(datos.datos.ana).getRoles().stream().anyMatch(x->x.getId()==cajero));
        });
        caso("No se permite dejar una cuenta sin roles",()->rechaza(ReglaNegocioException.class,()->roles.asignar(datos.datos.ana.getId(),Set.of(),Set.of(),admin)));
        caso("Cajero no administra roles ni catalogos sin permiso",()->{
            rechaza(AutorizacionException.class,()->roles.listar(datos.datos.ana));rechaza(AutorizacionException.class,()->catalogo.productos(datos.datos.ana));
            rechaza(AutorizacionException.class,()->catalogo.guardarCaja(null,123,"No permitida",true,datos.datos.ana));
        });
        caso("ROL_ADMIN extra no permite conceder permisos ajenos mediante roles o extras",()->{
            ejecutarSql("INSERT INTO usuario_permiso (usuario_id,permiso_id) VALUES (?,?)",datos.supervisora.getId(),rolPermiso);
            Usuario actor=entrar(datos.supervisora);Rol r=crear(1,Set.of(cat));
            rechaza(AutorizacionException.class,()->roles.asignar(datos.datos.ana.getId(),Set.of(r.getId()),Set.of(),actor));
            rechaza(AutorizacionException.class,()->roles.asignar(datos.datos.ana.getId(),Set.of(cajero),Set.of(cat),actor));
            rechaza(AutorizacionException.class,()->roles.guardar(null,"No permitido",1,Set.of(cat),actor));
        });
        caso("No se edita un rol propio aunque sea inferior al nivel maximo",()->{
            Rol r=crear(1,Set.of());roles.asignar(datos.supervisora.getId(),Set.of(supervisor,r.getId()),Set.of(rolPermiso),admin);Usuario actor=entrar(datos.supervisora);
            rechaza(AutorizacionException.class,()->roles.guardar(r.getId(),"Cambio propio",1,Set.of(),actor));
        });
        caso("Cambios de matriz se ven al siguiente login",()->{
            Rol r=crear(1,Set.of(venta));roles.asignar(datos.datos.ana.getId(),Set.of(r.getId()),Set.of(),admin);Usuario anterior=entrar(datos.datos.ana);
            roles.guardar(r.getId(),r.getNombre(),1,Set.of(venta,cat),admin);
            verificar(!anterior.tienePermiso("CATALOGO_ADMIN")&&entrar(datos.datos.ana).tienePermiso("CATALOGO_ADMIN"));
        });
        caso("Producto nuevo usa BigDecimal y redondeo HALF_UP",()->{
            Producto p=catalogo.guardarProducto(null,new DatosProducto("p_n_"+UUID.randomUUID().toString().substring(0,8),"Temporal",new java.math.BigDecimal("12.345"),q("12"),10,true),admin);
            datos.datos.productos.add(p.getId());igual(p.getPrecio(),"12.35");verificar(p.getStock()==10&&p.isActivo());
        });
        caso("Codigo duplicado no altera el producto existente",()->{
            Producto p=producto();rechaza(ReglaNegocioException.class,()->catalogo.guardarProducto(null,datos(p,true),admin));igual(producto().getPrecio(),"30");
        });
        caso("Validaciones de precio, impuesto, stock, codigo y nombre",()->{
            for(DatosProducto d:new DatosProducto[]{
                new DatosProducto("x","Nombre",q("-1"),q("12"),1,true),new DatosProducto("x","Nombre",q("1"),q("-1"),1,true),
                new DatosProducto("x","Nombre",q("1"),q("1000"),1,true),new DatosProducto("x","Nombre",q("1"),q("12"),-1,true),
                new DatosProducto("con espacios","Nombre",q("1"),q("12"),1,true),new DatosProducto("x"," ",q("1"),q("12"),1,true)}){
                rechaza(ReglaNegocioException.class,()->catalogo.guardarProducto(null,d,admin));
            }
        });
        caso("Editar producto desactualizado no sobreescribe una venta concurrente",()->{
            Producto original=producto();Turno t=datos.servicio.abrir(datos.datos.ana,datos.datos.cajas.get(0),q("1000"));Carrito c=new Carrito();c.agregar(original,1);
            new VentaService().cobrar(t.id(),c,q("30"),q("0"),datos.datos.ana);
            rechaza(ReglaNegocioException.class,()->catalogo.guardarProducto(original,datos(original,true),admin));verificar(producto().getStock()==29);
        });
        caso("Desactivar producto conserva historial y lo oculta del POS",()->{
            Producto p=producto();Producto cambiado=catalogo.guardarProducto(p,datos(p,false),admin);verificar(!cambiado.isActivo());
            verificar(new VentaService().productos(datos.datos.ana).stream().noneMatch(x->x.getId().equals(p.getId())));
        });
        caso("Caja nueva inactiva no aparece disponible",()->{
            int n=9100;while(numero("SELECT COUNT(*) FROM caja WHERE numero=?",n).signum()>0){n++;}
            CajaCatalogo c=catalogo.guardarCaja(null,n,"Caja temporal",false,admin);datos.datos.cajas.add(c.id());
            verificar(datos.servicio.cajasLibres(datos.datos.ana).stream().noneMatch(x->x.id()==c.id()));
        });
        caso("Numero de caja duplicado rechazado",()->{CajaCatalogo c=caja();rechaza(ReglaNegocioException.class,()->catalogo.guardarCaja(null,c.numero(),"Duplicada",true,admin));});
        caso("Caja con turno no puede desactivarse ni cambiar de numero",()->{
            CajaCatalogo c=caja();datos.servicio.abrir(datos.datos.ana,c.id(),q("1000"));
            rechaza(ReglaNegocioException.class,()->catalogo.guardarCaja(c,c.numero(),c.nombre(),false,admin));
            rechaza(ReglaNegocioException.class,()->catalogo.guardarCaja(c,8999,c.nombre(),true,admin));
        });
        caso("Caja desactualizada no sobreescribe cambios de otro administrador",()->{
            CajaCatalogo c=caja();catalogo.guardarCaja(c,c.numero(),"Nombre actualizado",true,admin);
            rechaza(ReglaNegocioException.class,()->catalogo.guardarCaja(c,c.numero(),"Nombre perdido",true,admin));verificar(caja().nombre().equals("Nombre actualizado"));
        });
        caso("Despues de cerrar el turno puede desactivarse la caja",()->{
            CajaCatalogo c=caja();Turno t=datos.servicio.abrir(datos.datos.ana,c.id(),q("1000"));datos.servicio.cerrar(t.id(),q("1000"),datos.datos.ana);
            verificar(!catalogo.guardarCaja(c,c.numero(),c.nombre(),false,admin).activa());
        });
        caso("No se crean cajas con numero cero o fuera de rango",()->{
            for(int n:new int[]{0,-1,10000}){rechaza(ReglaNegocioException.class,()->catalogo.guardarCaja(null,n,"Temporal",true,admin));}
        });
        caso("Producto gratuito se vende y revierte sin inventar cobros ni cambiar efectivo",()->{
            Producto p=catalogo.guardarProducto(null,new DatosProducto("gratis_"+UUID.randomUUID().toString().substring(0,8),"Promocion",q("0"),q("12"),2,true),admin);
            datos.datos.productos.add(p.getId());Turno t=datos.servicio.abrir(datos.datos.ana,datos.datos.cajas.get(0),q("1000"));
            Carrito c=new Carrito();c.agregar(p,1);Venta v=new VentaService().cobrar(t.id(),c,q("0"),q("0"),datos.datos.ana);
            igual(v.total(),"0");verificar(v.pagos().isEmpty());ReversionService reversiones=new ReversionService();
            long detalle=reversiones.buscar(t.id(),v.id(),datos.datos.ana).detalles().getFirst().id();
            Reversion r=reversiones.solicitar(t.id(),detalle,1,"Devolucion de promocion",datos.datos.ana).confirmar(datos.supervisora.getUsername(),"SoloPruebas2026".toCharArray());
            igual(r.monto(),"0");igual(r.efectivoPosterior(),"1000");igual(numero("SELECT stock FROM producto WHERE id=?",p.getId()),"2");
        });
    }
    private void preparar()throws Exception{
        datos.preparar();char[] clave="Admin123!".toCharArray();try{admin=new AuthService().autenticar("admin",clave);}finally{Arrays.fill(clave,'\0');}
        try(Connection con=Conexion.obtener()){
            var lista=new RolDAO().listar(con);cajero=lista.stream().filter(r->r.getNombre().equals("CAJERO")).findFirst().orElseThrow().getId();
            supervisor=lista.stream().filter(r->r.getNombre().equals("SUPERVISOR")).findFirst().orElseThrow().getId();rolAdmin=lista.stream().filter(r->r.getNombre().equals("ADMIN")).findFirst().orElseThrow().getId();
        }
        var permisos=roles.permisos(admin);venta=permisos.stream().filter(p->p.getCodigo().equals("VENTA_COBRAR")).findFirst().orElseThrow().getId();cat=permisos.stream().filter(p->p.getCodigo().equals("CATALOGO_ADMIN")).findFirst().orElseThrow().getId();rolPermiso=permisos.stream().filter(p->p.getCodigo().equals("ROL_ADMIN")).findFirst().orElseThrow().getId();
    }
    public static void main(String[] args)throws Exception{
        PruebasNegocio.iniciarConexion();PruebasAdministracion suite=new PruebasAdministracion();
        try{suite.preparar();suite.ejecutar();}
        finally{
            try{
                suite.datos.datos.limpiar();for(long id:suite.propios){ejecutarSql("DELETE FROM rol_permiso WHERE rol_id=?",id);ejecutarSql("DELETE FROM rol WHERE id=?",id);}
            }finally{Conexion.limpiarClaveTemporal();}
        }
        System.out.printf("%nPruebas administracion: %d; correctas: %d; fallos: %d%n",suite.ejecutadas,suite.ejecutadas-suite.fallos,suite.fallos);
        if(suite.fallos>0){throw new AssertionError("Fallaron pruebas de administracion");}
    }
}
