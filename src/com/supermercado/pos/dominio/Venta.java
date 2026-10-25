package com.supermercado.pos.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Venta(long id, long turnoId, LocalDateTime fecha, List<Detalle> detalles,
        BigDecimal subtotal, BigDecimal impuesto, BigDecimal total, List<Pago> pagos) {
    public Venta { detalles = List.copyOf(detalles); pagos = List.copyOf(pagos); }
    public record Detalle(String codigo, String nombre, int cantidad,
            BigDecimal precioUnitario, BigDecimal subtotal) { }
}
