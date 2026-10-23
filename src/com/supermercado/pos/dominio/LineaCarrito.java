package com.supermercado.pos.dominio;

import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;
import java.util.Objects;

/** El precio se copia al agregar el producto; no cambia con el catalogo. */
public record LineaCarrito(Producto producto, int cantidad, BigDecimal precioUnitario) {
    public LineaCarrito {
        Objects.requireNonNull(producto, "El producto es obligatorio");
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad debe ser positiva");
        }
        if (precioUnitario == null || precioUnitario.signum() < 0) {
            throw new ReglaNegocioException("El precio no puede ser negativo");
        }
        precioUnitario = Dinero.normalizar(precioUnitario);
    }

    public LineaCarrito(Producto producto, int cantidad) {
        this(producto, cantidad, Objects.requireNonNull(producto).getPrecio());
    }

    public BigDecimal subtotal() {
        return Dinero.normalizar(precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
    }
}
