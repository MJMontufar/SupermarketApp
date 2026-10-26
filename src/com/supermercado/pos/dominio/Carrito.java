package com.supermercado.pos.dominio;

import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Carrito en memoria. El stock definitivo se valida en la transaccion JDBC. */
public final class Carrito {
    private final Map<Long, LineaCarrito> lineas = new LinkedHashMap<>();

    public void agregar(Producto producto, int cantidad) {
        if (producto == null || producto.getId() == null || !producto.isActivo()) {
            throw new ReglaNegocioException("El producto no esta disponible");
        }
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad debe ser positiva");
        }
        LineaCarrito anterior = lineas.get(producto.getId());
        int acumulada;
        try {
            acumulada = Math.addExact(cantidad, anterior == null ? 0 : anterior.cantidad());
        } catch (ArithmeticException e) {
            throw new ReglaNegocioException("La cantidad es demasiado grande");
        }
        validarStock(producto, acumulada);
        BigDecimal precio = anterior == null ? producto.getPrecio() : anterior.precioUnitario();
        lineas.put(producto.getId(), new LineaCarrito(producto, acumulada, precio));
    }

    public void cambiarCantidad(long productoId, int cantidad) {
        LineaCarrito linea = lineas.get(productoId);
        if (linea == null) {
            throw new ReglaNegocioException("El producto no esta en el carrito");
        }
        if (!linea.producto().isActivo()) {
            throw new ReglaNegocioException("El producto no esta disponible");
        }
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad debe ser positiva");
        }
        validarStock(linea.producto(), cantidad);
        lineas.put(productoId, new LineaCarrito(linea.producto(), cantidad, linea.precioUnitario()));
    }

    private void validarStock(Producto producto, int cantidad) {
        if (cantidad > producto.getStock()) {
            throw new ReglaNegocioException("Sin stock suficiente: " + producto.getNombre());
        }
    }

    public void quitar(long productoId) { lineas.remove(productoId); }
    public void vaciar() { lineas.clear(); }
    public boolean estaVacio() { return lineas.isEmpty(); }
    public List<LineaCarrito> lineas() { return List.copyOf(lineas.values()); }

    public BigDecimal total() {
        return Dinero.normalizar(lineas.values().stream().map(LineaCarrito::subtotal)
                .reduce(Dinero.CERO, BigDecimal::add));
    }
}
