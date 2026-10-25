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
        BigDecimal inicial = Reglas.monto(fondo, "Fondo inicial", true);
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                if (!cajas.bloquearActiva(con, cajaId)) { throw new ReglaNegocioException("La caja no existe o esta inactiva"); }
                if (turnos.abiertoPorCajero(con, actor.getId()).isPresent()) {
                    throw new ReglaNegocioException("Ya tienes un turno abierto; utiliza Continuar turno");
                }
                long id = turnos.insertar(con, cajaId, actor.getId(), inicial);
                turnos.fondoInicial(con, id, inicial);
                Turno turno = turnos.buscar(con, id).orElseThrow();
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
