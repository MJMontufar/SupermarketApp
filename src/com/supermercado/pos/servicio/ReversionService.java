package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.MovimientoCajaDAO;
import com.supermercado.pos.dao.ProductoDAO;
import com.supermercado.pos.dao.ReversionDAO;
import com.supermercado.pos.dao.TurnoDAO;
import com.supermercado.pos.dominio.Reversion;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.dominio.VentaParaReversion;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public final class ReversionService {
    private final TurnoService turnos = new TurnoService();
    private final TurnoDAO turnoDao = new TurnoDAO();
    private final ReversionDAO reversiones = new ReversionDAO();
    private final ProductoDAO productos = new ProductoDAO();
    private final MovimientoCajaDAO movimientos = new MovimientoCajaDAO();
    public VentaParaReversion buscar(long turno, long venta, Usuario actor) {
        Reglas.requerir(actor, "REVERSION_SOLICITAR");
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                turnos.bloquearPropio(con, turno, actor);
                VentaParaReversion r = reversiones.buscarVenta(con, turno, venta)
                        .orElseThrow(() -> new ReglaNegocioException("No se encontro esa venta dentro de tu turno actual"));
                con.commit(); return r;
            } catch (SQLException | RuntimeException e) { con.rollback(); throw e; }
        } catch (SQLException e) { throw new IllegalStateException("No se pudo consultar la venta", e); }
    }
    public SolicitudAutorizada<Reversion> solicitar(long turno, long detalle, int cantidad, String motivo, Usuario actor) {
        Reglas.requerir(actor, "REVERSION_SOLICITAR");
        if (cantidad <= 0 || cantidad > 999999) { throw new ReglaNegocioException("La cantidad debe ser un entero positivo de hasta 999999"); }
        if (motivo == null || motivo.isBlank()) { throw new ReglaNegocioException("El motivo es obligatorio"); }
        String razon = motivo.strip();
        if (razon.getBytes(StandardCharsets.UTF_8).length > 200) { throw new ReglaNegocioException("El motivo es demasiado largo; escribe una explicacion breve"); }
        var autorizacion = new AutorizacionService().solicitar(actor,
                "REVERSION turno=" + turno + " detalle=" + detalle + " cantidad=" + cantidad, "REVERSION_AUTORIZAR");
        return new SolicitudAutorizada<>() {
            @Override public int intentosRestantes() { return autorizacion.restantes(); }
            @Override public boolean terminada() { return autorizacion.terminada(); }
            @Override public Reversion confirmar(String username, char[] clave) {
                return autorizacion.ejecutar(username, clave, (con, supervisor) -> {
                    Reglas.requerir(actor, "REVERSION_SOLICITAR"); turnos.bloquearPropio(con, turno, actor);
                    var bloqueado = reversiones.bloquearDetalle(con, turno, detalle)
                            .orElseThrow(() -> new ReglaNegocioException("El detalle no pertenece a una venta de tu turno actual"));
                    var d = bloqueado.detalle();
                    if (cantidad > d.disponible()) { throw new ReglaNegocioException("Cantidad no reversable; quedan " + d.disponible()); }
                    BigDecimal monto = Dinero.normalizar(d.precio().multiply(BigDecimal.valueOf(cantidad)));
                    BigDecimal esperado = turnoDao.resumen(con, turnoDao.buscar(con, turno).orElseThrow()).esperado();
                    if (esperado.compareTo(monto) < 0) { throw new ReglaNegocioException("No hay efectivo suficiente en caja para el reembolso"); }
                    if (!reversiones.sumarReversada(con, detalle, cantidad)) { throw new ReglaNegocioException("La cantidad disponible cambio; consulta otra vez la venta"); }
                    if (!productos.reingresar(con, d.productoId(), cantidad)) { throw new ReglaNegocioException("No se pudo reingresar el stock; revisa el limite de existencias"); }
                    String estado = reversiones.actualizarEstado(con, bloqueado.ventaId());
                    long id = reversiones.insertar(con, detalle, cantidad, monto, razon, actor.getId(), supervisor.getId());
                    movimientos.insertar(con, turno, "REVERSION_EFECTIVO", monto, supervisor.getId(), "REV-" + id);
                    return new Reversion(id, bloqueado.ventaId(), turno, d.producto(), cantidad, monto, razon,
                            actor.getUsername(), supervisor.getUsername(), esperado.subtract(monto), estado, LocalDateTime.now());
                });
            }
        };
    }
}
