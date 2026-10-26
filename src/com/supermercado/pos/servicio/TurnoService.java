package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.CajaDAO;
import com.supermercado.pos.dao.TurnoDAO;
import com.supermercado.pos.dominio.Caja;
import com.supermercado.pos.dominio.CierreTurno;
import com.supermercado.pos.dominio.ResumenTurno;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public final class TurnoService {
    private final TurnoDAO turnos = new TurnoDAO();
    private final CajaDAO cajas = new CajaDAO();
    public List<Caja> cajasLibres(Usuario actor) {
        Reglas.requerir(actor, "TURNO_ABRIR");
        try (Connection con = Conexion.obtener()) { return cajas.libres(con); }
        catch (SQLException e) { throw new IllegalStateException("No se pudieron consultar las cajas", e); }
    }
    public Optional<Turno> actual(Usuario actor) {
        Reglas.requerir(actor, "TURNO_ABRIR");
        try (Connection con = Conexion.obtener()) { return turnos.abiertoPorCajero(con, actor.getId()); }
        catch (SQLException e) { throw new IllegalStateException("No se pudo consultar el turno", e); }
    }
    public Turno abrir(Usuario actor, long cajaId, BigDecimal fondo) {
        Reglas.requerir(actor, "TURNO_ABRIR");
        if (fondo != null && fondo.signum() == 0) {
            throw new ReglaNegocioException("El fondo cero requiere autorizacion de un supervisor");
        }
        BigDecimal inicial = Reglas.monto(fondo, "Fondo inicial", true);
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                Turno turno = guardarApertura(con, actor, cajaId, inicial, null);
                con.commit(); return turno;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                if (e instanceof SQLException sql && sql.getErrorCode() == 1) {
                    throw new ReglaNegocioException("La caja o el cajero ya tienen un turno abierto");
                }
                throw e;
            }
        } catch (SQLException e) { throw new IllegalStateException("No se pudo abrir el turno", e); }
    }
    private Turno guardarApertura(Connection con, Usuario actor, long cajaId, BigDecimal fondo, Long autor) throws SQLException {
        Reglas.requerir(actor, "TURNO_ABRIR");
        if (!cajas.bloquearActiva(con, cajaId)) { throw new ReglaNegocioException("La caja no existe o esta inactiva"); }
        if (turnos.abiertoPorCajero(con, actor.getId()).isPresent()) {
            throw new ReglaNegocioException("Ya tienes un turno abierto; utiliza Continuar turno");
        }
        try {
            long id = turnos.insertar(con, cajaId, actor.getId(), fondo, autor);
            turnos.fondoInicial(con, id, fondo, autor);
            return turnos.buscar(con, id).orElseThrow();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1) { throw new ReglaNegocioException("La caja o el cajero ya tienen un turno abierto"); }
            throw e;
        }
    }
    public AperturaCero solicitarAperturaCero(Usuario actor, long cajaId) {
        Reglas.requerir(actor, "TURNO_ABRIR");
        return new AperturaCero(actor, cajaId);
    }
    /** No acepta un Usuario supuesto supervisor: siempre solicita sus credenciales. */
    public final class AperturaCero implements SolicitudAutorizada<Turno> {
        private final Usuario actor;
        private final long caja;
        private final AutorizacionService.Solicitud solicitud;
        private AperturaCero(Usuario actor, long caja) {
            this.actor = actor; this.caja = caja;
            solicitud = new AutorizacionService().solicitar(actor, "APERTURA_CERO caja=" + caja);
        }
        public int intentosRestantes() { return solicitud.restantes(); }
        public boolean terminada() { return solicitud.terminada(); }
        public Turno confirmar(String username, char[] clave) {
            return solicitud.ejecutar(username, clave,
                    (con, supervisor) -> guardarApertura(con, actor, caja, Dinero.CERO, supervisor.getId()));
        }
    }
    public ResumenTurno resumen(long id, Usuario actor) {
        Reglas.requerir(actor, "TURNO_CERRAR");
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                bloquearPropio(con, id, actor);
                ResumenTurno resumen = turnos.resumen(con, turnos.buscar(con, id).orElseThrow());
                con.commit(); return resumen;
            } catch (SQLException | RuntimeException e) { con.rollback(); throw e; }
        } catch (SQLException e) { throw new IllegalStateException("No se pudo consultar el resumen", e); }
    }
    public CierreTurno cerrar(long id, BigDecimal contado, Usuario actor) {
        Reglas.requerir(actor, "TURNO_CERRAR");
        BigDecimal declarado = Reglas.monto(contado, "Efectivo contado", false);
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                bloquearPropio(con, id, actor);
                ResumenTurno resumen = turnos.resumen(con, turnos.buscar(con, id).orElseThrow());
                LocalDateTime fecha = LocalDateTime.now();
                turnos.cerrar(con, id, declarado, resumen.esperado(), fecha);
                con.commit();
                return new CierreTurno(resumen, declarado, declarado.subtract(resumen.esperado()), fecha);
            } catch (SQLException | RuntimeException e) { con.rollback(); throw e; }
        } catch (SQLException e) { throw new IllegalStateException("No se pudo cerrar el turno", e); }
    }
    void bloquearPropio(Connection con, long id, Usuario actor) throws SQLException {
        if (!turnos.bloquearAbiertoPropio(con, id, actor.getId())) {
            throw new ReglaNegocioException("El turno no existe, esta cerrado o pertenece a otro cajero");
        }
    }
}
