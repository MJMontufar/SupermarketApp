package com.supermercado.pos.dominio;

import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CierreTurno(ResumenTurno resumen, BigDecimal contado,
        BigDecimal diferencia, LocalDateTime fecha) {
    public CierreTurno { contado = Dinero.normalizar(contado); diferencia = Dinero.normalizar(diferencia); }
}
