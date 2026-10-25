package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.Caja;
import com.supermercado.pos.dominio.ResumenTurno;
import com.supermercado.pos.dominio.Turno;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

public final class TurnoDAO {
    private static final String CONSULTA = """
            SELECT t.id, t.cajero_id, t.apertura, t.fondo_inicial, t.estado, t.apertura_autorizada_por,
                   c.id caja_id, c.numero, c.nombre FROM turno t JOIN caja c ON c.id = t.caja_id
            """;
    public Optional<Turno> abiertoPorCajero(Connection con, long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(CONSULTA + " WHERE t.cajero_id = ? AND t.estado = 'ABIERTO'")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(mapear(rs)) : Optional.empty(); }
        }
    }
    public Optional<Turno> buscar(Connection con, long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(CONSULTA + " WHERE t.id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(mapear(rs)) : Optional.empty(); }
        }
    }
    /** Cobros y cierre toman este mismo bloqueo; ninguno puede cobrar despues del cierre. */
    public boolean bloquearAbiertoPropio(Connection con, long id, long cajero) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT cajero_id, estado FROM turno WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) == cajero && "ABIERTO".equals(rs.getString(2));
            }
        }
    }
    public long insertar(Connection con, long caja, long cajero, BigDecimal fondo, Long autorizadoPor) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO turno (caja_id, cajero_id, fondo_inicial, apertura_autorizada_por) VALUES (?, ?, ?, ?)", new String[] {"ID"})) {
            ps.setLong(1, caja); ps.setLong(2, cajero); ps.setBigDecimal(3, fondo);
            if (autorizadoPor == null) { ps.setNull(4, java.sql.Types.NUMERIC); } else { ps.setLong(4, autorizadoPor); }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getLong(1); }
        }
    }
    public void fondoInicial(Connection con, long turno, BigDecimal fondo, Long autorizadoPor) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO movimiento_caja (turno_id, tipo, monto, autorizado_por) VALUES (?, 'FONDO_INICIAL', ?, ?)")) {
            ps.setLong(1, turno); ps.setBigDecimal(2, fondo);
            if (autorizadoPor == null) { ps.setNull(3, java.sql.Types.NUMERIC); } else { ps.setLong(3, autorizadoPor); }
            ps.executeUpdate();
        }
    }
    public void cerrar(Connection con, long turno, BigDecimal contado, BigDecimal esperado, LocalDateTime fecha) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                UPDATE turno SET estado = 'CERRADO', cierre = ?, efectivo_declarado = ?,
                efectivo_esperado = ?, diferencia = ? WHERE id = ? AND estado = 'ABIERTO'
                """)) {
            ps.setTimestamp(1, Timestamp.valueOf(fecha)); ps.setBigDecimal(2, contado);
            ps.setBigDecimal(3, esperado); ps.setBigDecimal(4, contado.subtract(esperado)); ps.setLong(5, turno);
            if (ps.executeUpdate() != 1) { throw new SQLException("El turno dejo de estar abierto"); }
        }
    }
    public ResumenTurno resumen(Connection con, Turno turno) throws SQLException {
        long ventas;
        BigDecimal vendido;
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*), NVL(SUM(total),0) FROM venta WHERE turno_id = ?")) {
            ps.setLong(1, turno.id());
            try (ResultSet rs = ps.executeQuery()) { rs.next(); ventas = rs.getLong(1); vendido = rs.getBigDecimal(2); }
        }
        BigDecimal efectivo = sumar(con, """
                SELECT NVL(SUM(p.monto),0) FROM pago p JOIN venta v ON v.id = p.venta_id
                WHERE v.turno_id = ? AND p.metodo = 'EFECTIVO'
                """, turno.id());
        BigDecimal tarjeta = sumar(con, """
                SELECT NVL(SUM(p.monto),0) FROM pago p JOIN venta v ON v.id = p.venta_id
                WHERE v.turno_id = ? AND p.metodo = 'TARJETA'
                """, turno.id());
        BigDecimal reversiones = sumar(con, """
                SELECT NVL(SUM(r.monto),0) FROM reversion r JOIN venta_detalle d ON d.id = r.detalle_id
                JOIN venta v ON v.id = d.venta_id WHERE v.turno_id = ?
                """, turno.id());
        BigDecimal retiros = sumar(con,
                "SELECT NVL(SUM(monto),0) FROM movimiento_caja WHERE turno_id = ? AND tipo = 'RETIRO_BOVEDA'", turno.id());
        BigDecimal reembolsos = sumar(con,
                "SELECT NVL(SUM(monto),0) FROM movimiento_caja WHERE turno_id = ? AND tipo = 'REVERSION_EFECTIVO'", turno.id());
        // El movimiento FONDO_INICIAL es evidencia: no se suma por segunda vez.
        BigDecimal esperado = turno.fondoInicial().add(efectivo).subtract(retiros).subtract(reembolsos);
        return new ResumenTurno(turno.id(), ventas, turno.fondoInicial(), vendido, efectivo, tarjeta,
                reversiones, retiros, reembolsos, esperado);
    }
    private BigDecimal sumar(Connection con, String sql, long turno) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, turno);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        }
    }
    private Turno mapear(ResultSet rs) throws SQLException {
        return new Turno(rs.getLong("id"), new Caja(rs.getLong("caja_id"), rs.getInt("numero"), rs.getString("nombre")),
                rs.getLong("cajero_id"), rs.getTimestamp("apertura").toLocalDateTime(),
                rs.getBigDecimal("fondo_inicial"), rs.getString("estado"),
                rs.getObject("apertura_autorizada_por") == null ? null : rs.getLong("apertura_autorizada_por"));
    }
}
