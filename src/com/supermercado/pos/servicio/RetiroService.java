package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.MovimientoCajaDAO;
import com.supermercado.pos.dao.TurnoDAO;
import com.supermercado.pos.dominio.ResumenTurno;
import com.supermercado.pos.dominio.Retiro;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Configuracion;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public final class RetiroService {
    private final TurnoService turnos = new TurnoService();
    private final TurnoDAO dao = new TurnoDAO();
    private final MovimientoCajaDAO movimientos = new MovimientoCajaDAO();
    public ResumenTurno resumen(long turno, Usuario actor) {
        Reglas.requerir(actor, "CORTE_INTERMEDIO");
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                turnos.bloquearPropio(con, turno, actor);
                ResumenTurno r = dao.resumen(con, dao.buscar(con, turno).orElseThrow());
                con.commit(); return r;
            } catch (SQLException | RuntimeException e) { con.rollback(); throw e; }
        } catch (SQLException e) { throw new IllegalStateException("No se pudo consultar la caja", e); }
    }
    public SolicitudAutorizada<Retiro> solicitar(long turno, BigDecimal monto, Usuario actor) {
        Reglas.requerir(actor, "CORTE_INTERMEDIO");
        BigDecimal importe = Reglas.monto(monto, "Retiro", true);
        var autorizacion = new AutorizacionService().solicitar(actor,
                "RETIRO_BOVEDA turno=" + turno + " monto=" + importe, "CORTE_INTERMEDIO");
        return new SolicitudAutorizada<>() {
            @Override public int intentosRestantes() { return autorizacion.restantes(); }
            @Override public boolean terminada() { return autorizacion.terminada(); }
            @Override public Retiro confirmar(String username, char[] clave) {
                return autorizacion.ejecutar(username, clave, (con, supervisor) -> {
                    Reglas.requerir(actor, "CORTE_INTERMEDIO"); turnos.bloquearPropio(con, turno, actor);
                    ResumenTurno r = dao.resumen(con, dao.buscar(con, turno).orElseThrow());
                    BigDecimal posterior = r.esperado().subtract(importe);
                    BigDecimal minimo = Configuracion.cargar().efectivoMinimo();
                    if (posterior.compareTo(minimo) < 0) {
                        throw new ReglaNegocioException("La caja debe conservar al menos Q " + minimo);
                    }
                    long id = movimientos.insertar(con, turno, "RETIRO_BOVEDA", importe, supervisor.getId(), null);
                    String referencia = "RET-" + id; movimientos.referencia(con, id, referencia);
                    return new Retiro(id, turno, referencia, importe, r.esperado(), posterior,
                            actor.getUsername(), supervisor.getUsername(), LocalDateTime.now());
                });
            }
        };
    }
}
