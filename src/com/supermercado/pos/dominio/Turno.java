package com.supermercado.pos.dominio;

import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Turno(long id, Caja caja, long cajeroId, LocalDateTime apertura,
        BigDecimal fondoInicial, String estado, Long aperturaAutorizadaPor) {
    public Turno { fondoInicial = Dinero.normalizar(fondoInicial); }
    public boolean abierto() { return "ABIERTO".equals(estado); }
}
