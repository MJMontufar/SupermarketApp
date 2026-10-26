package com.supermercado.pos.dominio;

import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Reversion(long id, long ventaId, long turnoId, String producto, int cantidad,
        BigDecimal monto, String motivo, String cajero, String supervisor,
        BigDecimal efectivoPosterior, String estadoVenta, LocalDateTime fecha) {
    public Reversion { monto = Dinero.normalizar(monto); efectivoPosterior = Dinero.normalizar(efectivoPosterior); }
}
