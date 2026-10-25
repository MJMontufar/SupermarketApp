package com.supermercado.pos.dominio;

import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;

/** Las ventas son importes originales; los reembolsos se muestran por separado. */
public record ResumenTurno(long turnoId, long ventas, BigDecimal fondo, BigDecimal vendido,
        BigDecimal efectivo, BigDecimal tarjeta, BigDecimal reversiones,
        BigDecimal retiros, BigDecimal reembolsosEfectivo, BigDecimal esperado) {
    public ResumenTurno {
        fondo = Dinero.normalizar(fondo); vendido = Dinero.normalizar(vendido);
        efectivo = Dinero.normalizar(efectivo); tarjeta = Dinero.normalizar(tarjeta);
        reversiones = Dinero.normalizar(reversiones); retiros = Dinero.normalizar(retiros);
        reembolsosEfectivo = Dinero.normalizar(reembolsosEfectivo); esperado = Dinero.normalizar(esperado);
    }
}
