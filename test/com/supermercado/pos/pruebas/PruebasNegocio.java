package com.supermercado.pos.pruebas;

import static com.supermercado.pos.pruebas.PruebasCaja.*;
import com.supermercado.pos.dominio.Carrito;
import com.supermercado.pos.dominio.Producto;
import com.supermercado.pos.dominio.Reversion;
import com.supermercado.pos.dominio.Retiro;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.dominio.Venta;
import com.supermercado.pos.excepcion.AutorizacionException;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.servicio.ReversionService;
import com.supermercado.pos.servicio.RetiroService;
import com.supermercado.pos.servicio.SolicitudAutorizada;
import com.supermercado.pos.servicio.TurnoService;
import com.supermercado.pos.servicio.VentaService;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Configuracion;
import com.supermercado.pos.util.Sesion;
import java.io.Console;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.UUID;

/** Nueve casos obligatorios de fase 10, casos de concurrencia y escenario completo de fase 11. */
public final class PruebasNegocio {
    private final PruebasAperturaCero datos = new PruebasAperturaCero();
    private final TurnoService turnos = new TurnoService();
    private final VentaService ventas = new VentaService();
    private final RetiroService retiros = new RetiroService();
    private final ReversionService reversiones = new ReversionService();
    private int ejecutadas;
    private int fallos;
    @FunctionalInterface private interface Prueba { void ejecutar() throws Exception; }
    private void caso(String nombre, Prueba prueba) {
        ejecutadas++;
        try {
            datos.datos.limpiarOperaciones();
            ejecutarSql("UPDATE producto SET stock = 30, activo = 1, precio = 30 WHERE id = ?", datos.datos.arroz);
            ejecutarSql("UPDATE producto SET stock = 30, activo = 1, precio = 20 WHERE id = ?", datos.datos.leche);
            Sesion.setUsuario(ana()); prueba.ejecutar(); System.out.println("OK    " + nombre);
        } catch (Exception | AssertionError e) { fallos++; System.out.println("FALLO " + nombre); e.printStackTrace(System.out); }
    }
    private Usuario ana() { return datos.datos.ana; }
    private Usuario luis() { return datos.datos.luis; }
    private Turno abrir() { return turnos.abrir(ana(), datos.datos.cajas.get(0), q("1000")); }
    private Carrito carrito(long producto, int cantidad, Usuario actor) {
        Producto p = ventas.productos(actor).stream().filter(x -> x.getId() == producto).findFirst().orElseThrow();
        Carrito c = new Carrito(); c.agregar(p, cantidad); return c;
    }
    private Venta vender(Turno t, int cantidad, String efectivo, String tarjeta) {
        return ventas.cobrar(t.id(), carrito(datos.datos.arroz, cantidad, ana()), q(efectivo), q(tarjeta), ana());
    }
    private long detalle(Turno t, Venta venta) { return reversiones.buscar(t.id(), venta.id(), ana()).detalles().getFirst().id(); }
    private <T> T confirmar(SolicitudAutorizada<T> solicitud, Usuario supervisor) {
        char[] clave = "SoloPruebas2026".toCharArray();
        try { return solicitud.confirmar(supervisor.getUsername(), clave); }
        finally { for (char c : clave) { verificar(c == '\0'); } }
    }
    private Reversion revertir(Turno t, long detalle, int cantidad) {
        return confirmar(reversiones.solicitar(t.id(), detalle, cantidad, "Devolucion de prueba", ana()), datos.supervisora);
    }
    private void sinReversion(long detalle) throws Exception {
        igual(numero("SELECT cantidad_reversada FROM venta_detalle WHERE id = ?", detalle), "0");
        igual(numero("SELECT COUNT(*) FROM reversion WHERE detalle_id = ?", detalle), "0");
    }
    private void ejecutar() {
        caso("OBLIGATORIA 1: caja ocupada rechazada", () -> {
            abrir(); rechaza(ReglaNegocioException.class, () -> turnos.abrir(luis(), datos.datos.cajas.get(0), q("800")));
            verificar(turnos.actual(luis()).isEmpty());
        });
        caso("OBLIGATORIA 2: fondo cero sin autorizacion rechazado", () ->
                rechaza(ReglaNegocioException.class, () -> turnos.abrir(ana(), datos.datos.cajas.get(0), q("0"))));
        caso("OBLIGATORIA 3: pago insuficiente no guarda ni descuenta", () -> {
            Turno t = abrir(); Carrito c = carrito(datos.datos.arroz, 5, ana());
            rechaza(ReglaNegocioException.class, () -> ventas.cobrar(t.id(), c, q("149"), q("0"), ana()));
            igual(numero("SELECT COUNT(*) FROM venta WHERE turno_id = ?", t.id()), "0");
            igual(numero("SELECT stock FROM producto WHERE id = ?", datos.datos.arroz), "30");
        });
        caso("OBLIGATORIA 4: pago mixto calcula cambio solo en efectivo", () -> {
            Turno t = abrir(); Venta v = vender(t, 4, "100", "50");
            igual(v.pagos().get(0).monto(), "70"); igual(v.pagos().get(0).cambio(), "30"); igual(v.pagos().get(1).cambio(), "0");
            igual(turnos.resumen(t.id(), ana()).esperado(), "1070");
        });
        caso("OBLIGATORIA 5: retiro bajo minimo rechazado sin movimiento", () -> {
            Turno t = abrir();
            rechaza(ReglaNegocioException.class, () -> confirmar(retiros.solicitar(t.id(), q("501"), ana()), datos.supervisora));
            igual(turnos.resumen(t.id(), ana()).esperado(), "1000");
            igual(numero("SELECT COUNT(*) FROM movimiento_caja WHERE turno_id = ? AND tipo = 'RETIRO_BOVEDA'", t.id()), "0");
        });
        caso("OBLIGATORIA 6: cajero con permiso extra no autoriza reversion", () -> {
            Turno t = abrir(); Venta v = vender(t, 1, "30", "0"); long d = detalle(t, v);
            ejecutarSql("INSERT INTO usuario_permiso (usuario_id, permiso_id) SELECT ?, id FROM permiso WHERE codigo='REVERSION_AUTORIZAR'", luis().getId());
            try { rechaza(AutorizacionException.class, () -> confirmar(reversiones.solicitar(t.id(), d, 1, "Prueba", ana()), luis())); }
            finally { ejecutarSql("DELETE FROM usuario_permiso WHERE usuario_id = ?", luis().getId()); }
            sinReversion(d); verificar(Sesion.getUsuario() == ana());
        });
        caso("OBLIGATORIA 7: autoautorizacion de reversion rechazada", () -> {
            Turno t = abrir(); Venta v = vender(t, 1, "30", "0"); long d = detalle(t, v);
            rechaza(AutorizacionException.class, () -> confirmar(reversiones.solicitar(t.id(), d, 1, "Prueba", ana()), ana())); sinReversion(d);
        });
        caso("OBLIGATORIA 8: segunda reversion que excede vendido rechazada", () -> {
            Turno t = abrir(); Venta v = vender(t, 2, "60", "0"); long d = detalle(t, v);
            Reversion r = revertir(t, d, 1); verificar(r.estadoVenta().equals("PARCIALMENTE_REVERSADA"));
            rechaza(ReglaNegocioException.class, () -> revertir(t, d, 2));
            igual(numero("SELECT cantidad_reversada FROM venta_detalle WHERE id = ?", d), "1");
            igual(numero("SELECT stock FROM producto WHERE id = ?", datos.datos.arroz), "29");
            igual(turnos.resumen(t.id(), ana()).esperado(), "1030");
        });
        caso("OBLIGATORIA 9: cierre con faltante Q20 guarda -20", () -> {
            Turno t = abrir(); vender(t, 5, "200", "0");
            igual(turnos.cerrar(t.id(), q("1130"), ana()).diferencia(), "-20");
        });
        caso("Retiro exacto al minimo guarda receptor, folio y efectivo", () -> {
            Turno t = abrir(); Retiro r = confirmar(retiros.solicitar(t.id(), q("500"), ana()), datos.supervisora);
            igual(r.posterior(), "500"); verificar(r.referencia().equals("RET-" + r.id()) && r.receptor().equals(datos.supervisora.getUsername()));
            verificar(numero("SELECT autorizado_por FROM movimiento_caja WHERE id = ?", r.id()).longValueExact() == datos.supervisora.getId());
            verificar(Sesion.getUsuario() == ana());
        });
        caso("Retiro cero, negativo, nulo o redondeado a cero rechazado", () -> {
            Turno t = abrir();
            for (BigDecimal q : new BigDecimal[] {q("0"), q("-1"), null, new BigDecimal("0.001")}) {
                rechaza(ReglaNegocioException.class, () -> retiros.solicitar(t.id(), q, ana()));
            }
        });
        caso("No se puede retirar del turno de otro cajero", () -> {
            Turno t = abrir();
            rechaza(ReglaNegocioException.class, () -> confirmar(retiros.solicitar(t.id(), q("100"), luis()), datos.supervisora));
            igual(turnos.resumen(t.id(), ana()).esperado(), "1000");
        });
        caso("Turno cerrado rechaza retiro, busqueda y reversion", () -> {
            Turno t = abrir(); Venta v = vender(t, 1, "30", "0"); long d = detalle(t, v);
            turnos.cerrar(t.id(), q("1030"), ana());
            rechaza(ReglaNegocioException.class, () -> confirmar(retiros.solicitar(t.id(), q("100"), ana()), datos.supervisora));
            rechaza(ReglaNegocioException.class, () -> reversiones.buscar(t.id(), v.id(), ana()));
            rechaza(ReglaNegocioException.class, () -> revertir(t, d, 1)); sinReversion(d);
        });
        caso("Supervisor sin REVERSION_AUTORIZAR no autoriza", () -> {
            Turno t = abrir(); Venta v = vender(t, 1, "30", "0"); long d = detalle(t, v);
            long anterior = numero("SELECT rol_id FROM usuario_rol WHERE usuario_id = ?", datos.supervisora.getId()).longValueExact();
            long rol = insertar("INSERT INTO rol (nombre,nivel) VALUES (?,2)", "p_sinperm_" + UUID.randomUUID().toString().substring(0,8));
            ejecutarSql("INSERT INTO rol_permiso (rol_id,permiso_id) SELECT ?,id FROM permiso WHERE codigo='TURNO_ABRIR'", rol);
            try {
                ejecutarSql("UPDATE usuario_rol SET rol_id=? WHERE usuario_id=?", rol, datos.supervisora.getId());
                rechaza(AutorizacionException.class, () -> revertir(t,d,1)); sinReversion(d);
            } finally {
                ejecutarSql("UPDATE usuario_rol SET rol_id=? WHERE usuario_id=?", anterior, datos.supervisora.getId());
                ejecutarSql("DELETE FROM rol_permiso WHERE rol_id=?",rol); ejecutarSql("DELETE FROM rol WHERE id=?",rol);
            }
        });
        caso("Supervisora inactiva no puede autorizar retiro", () -> {
            Turno t = abrir(); ejecutarSql("UPDATE usuario SET activo=0 WHERE id=?",datos.supervisora.getId());
            try { rechaza(AutorizacionException.class, () -> confirmar(retiros.solicitar(t.id(),q("100"),ana()),datos.supervisora)); }
            finally { ejecutarSql("UPDATE usuario SET activo=1 WHERE id=?",datos.supervisora.getId()); }
        });
        caso("Reembolso de venta mixta sale completo del efectivo", () -> {
            Turno t = abrir(); Venta v=vender(t,4,"100","50"); Reversion r=revertir(t,detalle(t,v),1);
            igual(r.monto(),"30"); igual(r.efectivoPosterior(),"1040"); igual(turnos.resumen(t.id(),ana()).tarjeta(),"50");
        });
        caso("Venta con tarjeta se reembolsa en efectivo segun el PDF", () -> {
            Turno t = abrir(); Venta v=vender(t,2,"0","60"); Reversion r=revertir(t,detalle(t,v),1);
            igual(r.efectivoPosterior(),"970"); igual(turnos.resumen(t.id(),ana()).reembolsosEfectivo(),"30");
        });
        caso("Reversion total anula venta y restaura existencias", () -> {
            Turno t=abrir(); Venta v=vender(t,2,"60","0"); Reversion r=revertir(t,detalle(t,v),2);
            verificar(r.estadoVenta().equals("ANULADA")); igual(r.efectivoPosterior(),"1000");
            igual(numero("SELECT stock FROM producto WHERE id=?",datos.datos.arroz),"30");
        });
        caso("Venta de dos productos se anula solo al devolver ambos", () -> {
            Turno t=abrir(); Carrito c=carrito(datos.datos.arroz,1,ana());
            c.agregar(ventas.productos(ana()).stream().filter(p->p.getId()==datos.datos.leche).findFirst().orElseThrow(),1);
            Venta v=ventas.cobrar(t.id(),c,q("50"),q("0"),ana()); var lineas=reversiones.buscar(t.id(),v.id(),ana()).detalles();
            verificar(revertir(t,lineas.get(0).id(),1).estadoVenta().equals("PARCIALMENTE_REVERSADA"));
            verificar(revertir(t,lineas.get(1).id(),1).estadoVenta().equals("ANULADA"));
        });
        caso("Reversion usa precio historico aunque cambie el catalogo", () -> {
            Turno t=abrir(); Venta v=vender(t,1,"30","0"); long d=detalle(t,v);
            ejecutarSql("UPDATE producto SET precio=99 WHERE id=?",datos.datos.arroz); igual(revertir(t,d,1).monto(),"30");
        });
        caso("Se devuelve stock de producto desactivado sin reactivarlo", () -> {
            Turno t=abrir(); Venta v=vender(t,1,"30","0"); long d=detalle(t,v);
            ejecutarSql("UPDATE producto SET activo=0 WHERE id=?",datos.datos.arroz); revertir(t,d,1);
            igual(numero("SELECT stock FROM producto WHERE id=?",datos.datos.arroz),"30");
            igual(numero("SELECT activo FROM producto WHERE id=?",datos.datos.arroz),"0");
        });
        caso("No se devuelve efectivo inexistente en caja abierta con Q0", () -> {
            Turno t=confirmar(turnos.solicitarAperturaCero(ana(),datos.datos.cajas.get(0)),datos.supervisora);
            Venta v=vender(t,1,"0","30"); long d=detalle(t,v);
            rechaza(ReglaNegocioException.class,()->revertir(t,d,1)); sinReversion(d);
            igual(numero("SELECT stock FROM producto WHERE id=?",datos.datos.arroz),"29");
        });
        caso("Minimo operativo limita retiros, no reembolsos con efectivo suficiente", () -> {
            Turno t=turnos.abrir(ana(),datos.datos.cajas.get(0),q("10")); Venta v=vender(t,1,"30","0");
            igual(revertir(t,detalle(t,v),1).efectivoPosterior(),"10");
        });
        caso("Fallo al reingresar stock revierte cantidad y todos los registros", () -> {
            Turno t=abrir(); Venta v=vender(t,1,"30","0"); long d=detalle(t,v);
            ejecutarSql("UPDATE producto SET stock=99999999 WHERE id=?",datos.datos.arroz);
            rechaza(ReglaNegocioException.class,()->revertir(t,d,1)); sinReversion(d);
            verificar(reversiones.buscar(t.id(),v.id(),ana()).estado().equals("COMPLETADA"));
            igual(turnos.resumen(t.id(),ana()).esperado(),"1030");
            igual(numero("SELECT COUNT(*) FROM movimiento_caja WHERE turno_id=? AND tipo='REVERSION_EFECTIVO'",t.id()),"0");
        });
        caso("Motivo vacio, nulo o demasiado largo rechazado", () -> {
            Turno t=abrir(); Venta v=vender(t,1,"30","0"); long d=detalle(t,v);
            for(String motivo:new String[]{null,"  ","x".repeat(201)}) {
                rechaza(ReglaNegocioException.class,()->reversiones.solicitar(t.id(),d,1,motivo,ana()));
            }
        });
        caso("Cantidad cero, negativa o fuera de rango rechazada", () -> {
            Turno t=abrir(); Venta v=vender(t,1,"30","0"); long d=detalle(t,v);
            for(int cantidad:new int[]{0,-1,1000000}) { rechaza(ReglaNegocioException.class,()->reversiones.solicitar(t.id(),d,cantidad,"Prueba",ana())); }
        });
        caso("Otro turno no puede consultar ni devolver la venta", () -> {
            Turno t=abrir(); Venta v=vender(t,1,"30","0"); long d=detalle(t,v);
            Turno otro=turnos.abrir(luis(),datos.datos.cajas.get(1),q("800"));
            rechaza(ReglaNegocioException.class,()->reversiones.buscar(otro.id(),v.id(),luis()));
            rechaza(ReglaNegocioException.class,()->confirmar(reversiones.solicitar(otro.id(),d,1,"Prueba",luis()),datos.supervisora)); sinReversion(d);
        });
        caso("Autorizacion de retiro no se reutiliza", () -> {
            Turno t=abrir(); var s=retiros.solicitar(t.id(),q("100"),ana()); confirmar(s,datos.supervisora);
            rechaza(AutorizacionException.class,()->confirmar(s,datos.supervisora)); igual(turnos.resumen(t.id(),ana()).esperado(),"900");
        });
        caso("Tres fallos bloquean la solicitud de reversion y se auditan", () -> {
            Turno t=abrir(); Venta v=vender(t,1,"30","0"); long d=detalle(t,v); var s=reversiones.solicitar(t.id(),d,1,"Prueba",ana());
            for(int i=0;i<3;i++) { rechaza(AutorizacionException.class,()->s.confirmar(datos.supervisora.getUsername(),"Incorrecta2026".toCharArray())); }
            rechaza(AutorizacionException.class,()->confirmar(s,datos.supervisora)); verificar(s.terminada()); sinReversion(d);
            igual(numero("SELECT COUNT(*) FROM auditoria_autorizacion WHERE solicitado_por=? AND exitoso=0",ana().getId()),"3");
        });
        caso("Dos retiros simultaneos no pueden bajar del minimo", () -> {
            Turno t=abrir(); var a=retiros.solicitar(t.id(),q("400"),ana()); var b=retiros.solicitar(t.id(),q("400"),ana());
            var r=simultaneo(()->intento(()->confirmar(a,datos.supervisora)),()->intento(()->confirmar(b,datos.gerente)));
            verificar(r.stream().filter(Boolean::booleanValue).count()==1); igual(turnos.resumen(t.id(),ana()).esperado(),"600");
        });
        caso("Dos reversiones simultaneas no devuelven dos veces la ultima unidad", () -> {
            Turno t=abrir(); Venta v=vender(t,1,"30","0"); long d=detalle(t,v);
            var a=reversiones.solicitar(t.id(),d,1,"Prueba A",ana()); var b=reversiones.solicitar(t.id(),d,1,"Prueba B",ana());
            var r=simultaneo(()->intento(()->confirmar(a,datos.supervisora)),()->intento(()->confirmar(b,datos.gerente)));
            verificar(r.stream().filter(Boolean::booleanValue).count()==1);
            igual(numero("SELECT SUM(cantidad) FROM reversion WHERE detalle_id=?",d),"1"); igual(turnos.resumen(t.id(),ana()).esperado(),"1000");
        });
        caso("Reversion y cierre simultaneos mantienen esperado consistente", () -> {
            Turno t=abrir(); Venta v=vender(t,1,"30","0"); long d=detalle(t,v);
            simultaneo(()->intento(()->revertir(t,d,1)),()->{turnos.cerrar(t.id(),q("1000"),ana()); return true;});
            BigDecimal veces=numero("SELECT COUNT(*) FROM reversion WHERE detalle_id=?",d);
            BigDecimal esperado=q("1030").subtract(q("30").multiply(veces));
            verificar(numero("SELECT efectivo_esperado FROM turno WHERE id=?",t.id()).compareTo(esperado)==0);
        });
        caso("El servicio comprueba permisos de solicitante sin depender del boton", () -> {
            Turno t=abrir(); Usuario actor=new Usuario(ana().getId(),ana().getUsername(),"Sin permisos",ana().getPasswordHash());
            rechaza(AutorizacionException.class,()->retiros.solicitar(t.id(),q("10"),actor));
            rechaza(AutorizacionException.class,()->reversiones.solicitar(t.id(),-1,1,"Prueba",actor));
        });
        caso("Minimo configurable de Q750 se aplica en la transaccion", () -> {
            Turno t=abrir(); String anterior=System.getProperty("pos.config"); Path dir=Files.createTempDirectory("pos-minimo-"); Path archivo=dir.resolve("config.properties");
            Files.writeString(archivo,Files.readString(Path.of(anterior==null?"config.properties":anterior),StandardCharsets.UTF_8)+"\nefectivo.minimo=750.00\n",StandardCharsets.UTF_8);
            try {
                System.setProperty("pos.config",archivo.toString());
                rechaza(ReglaNegocioException.class,()->confirmar(retiros.solicitar(t.id(),q("251"),ana()),datos.supervisora));
                igual(confirmar(retiros.solicitar(t.id(),q("250"),ana()),datos.supervisora).posterior(),"750");
            } finally {
                if(anterior==null){System.clearProperty("pos.config");}else{System.setProperty("pos.config",anterior);}
                Files.deleteIfExists(archivo); Files.deleteIfExists(dir);
            }
        });
        caso("ESCENARIO COMPLETO: 690 esperado, 680 contado, diferencia -10", () -> {
            Turno t=abrir(); Venta primera=vender(t,5,"200","0");
            ventas.cobrar(t.id(),carrito(datos.datos.leche,4,ana()),q("0"),q("80"),ana()); vender(t,4,"70","50");
            rechaza(ReglaNegocioException.class,()->turnos.abrir(luis(),datos.datos.cajas.get(0),q("800")));
            Turno tl=turnos.abrir(luis(),datos.datos.cajas.get(1),q("800"));
            Retiro retiro=confirmar(retiros.solicitar(t.id(),q("500"),ana()),datos.supervisora); igual(retiro.posterior(),"720");
            long d=detalle(t,primera); var s=reversiones.solicitar(t.id(),d,1,"Devolucion del escenario",ana());
            rechaza(AutorizacionException.class,()->confirmar(s,luis())); Reversion reversion=confirmar(s,datos.supervisora);
            igual(reversion.monto(),"30"); igual(reversion.efectivoPosterior(),"690");
            var cierre=turnos.cerrar(t.id(),q("680"),ana()); igual(cierre.resumen().esperado(),"690"); igual(cierre.diferencia(),"-10");
            igual(turnos.cerrar(tl.id(),q("800"),luis()).diferencia(),"0");
            igual(numero("SELECT stock FROM producto WHERE id=?",datos.datos.arroz),"22");
            igual(numero("SELECT stock FROM producto WHERE id=?",datos.datos.leche),"26");
            verificar(Sesion.getUsuario()==ana());
            Path salida=Path.of("out/escenario-completo.txt"); Files.createDirectories(salida.getParent());
            Files.writeString(salida,"ESCENARIO FASE 11 - EJECUCION AUTOMATIZADA POS_TEST\nDatos propios temporales; no es la base de Windows.\n"
                    +"Fondo: 1000.00\nEfectivo aplicado: 220.00\nTarjeta: 130.00\nRetiro: 500.00\nReembolso: 30.00\n"
                    +"Esperado: "+cierre.resumen().esperado()+"\nContado: "+cierre.contado()+"\nDiferencia: "+cierre.diferencia()+"\nEstado: CERRADO\n",StandardCharsets.UTF_8);
        });
    }
    static void iniciarConexion() throws Exception {
        if (!"pos_test".equalsIgnoreCase(Configuracion.cargar().usuario())) { throw new IllegalStateException("Solo POS_TEST"); }
        if (System.getenv("POS_DB_PASSWORD")==null || System.getenv("POS_DB_PASSWORD").isEmpty()) {
            Console consola=System.console(); if(consola==null){throw new IllegalStateException("Ejecuta desde consola para ingresar clave POS_TEST");}
            char[] clave=consola.readPassword("Clave de Oracle para POS_TEST: ");
            try{Conexion.configurarClaveTemporal(clave);}finally{if(clave!=null){Arrays.fill(clave,'\0');}}
        }
        try(Connection con=Conexion.obtener(); PreparedStatement ps=con.prepareStatement("SELECT SYS_CONTEXT('USERENV','SESSION_USER'),SYS_CONTEXT('USERENV','CON_NAME') FROM dual"); ResultSet rs=ps.executeQuery()){
            rs.next(); if(!"POS_TEST".equals(rs.getString(1))||!"XEPDB1".equals(rs.getString(2))){throw new IllegalStateException("Solo POS_TEST en XEPDB1");}
        }
    }
    public static void main(String[] args) throws Exception {
        iniciarConexion(); PruebasNegocio suite=new PruebasNegocio();
        try{suite.datos.preparar(); suite.ejecutar();}
        finally{try{suite.datos.datos.limpiar();}finally{Sesion.cerrar(); Conexion.limpiarClaveTemporal();}}
        System.out.printf("%nPruebas negocio: %d; correctas: %d; fallos: %d%n",suite.ejecutadas,suite.ejecutadas-suite.fallos,suite.fallos);
        if(suite.fallos>0){throw new AssertionError("Fallaron pruebas de negocio");}
    }
}
