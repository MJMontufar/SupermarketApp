package com.supermercado.pos.dominio;

import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Retiro(long id, long turnoId, String referencia, BigDecimal monto,
        BigDecimal anterior, BigDecimal posterior, String cajero, String receptor, LocalDateTime fecha) {
    public Retiro {
        monto = Dinero.normalizar(monto); anterior = Dinero.normalizar(anterior); posterior = Dinero.normalizar(posterior);
    }
}
