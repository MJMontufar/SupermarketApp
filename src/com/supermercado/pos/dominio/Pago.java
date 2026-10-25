package com.supermercado.pos.dominio;

import java.math.BigDecimal;

/** Monto es el importe aplicado a la venta; recibido incluye el cambio. */
public record Pago(Metodo metodo, BigDecimal monto, BigDecimal recibido, BigDecimal cambio) {
    public enum Metodo { EFECTIVO, TARJETA }
}
