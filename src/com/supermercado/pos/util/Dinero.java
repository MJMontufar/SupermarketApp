package com.supermercado.pos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Un solo criterio para representar y redondear los montos del sistema. */
public final class Dinero {
    public static final BigDecimal CERO = new BigDecimal("0.00");

    private Dinero() {
    }

    public static BigDecimal normalizar(BigDecimal monto) {
        return Objects.requireNonNull(monto, "El monto es obligatorio")
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Usar punto decimal y sin separadores de miles, por ejemplo: 1250.50. */
    public static BigDecimal desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }
        return normalizar(new BigDecimal(texto.trim()));
    }
}
