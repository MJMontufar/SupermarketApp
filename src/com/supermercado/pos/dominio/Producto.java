package com.supermercado.pos.dominio;

import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;

public final class Producto {
    private final Long id;
    private final String codigoBarras;
    private final String nombre;
    private BigDecimal precio;
    private BigDecimal porcentajeImpuesto = new BigDecimal("12.00");
    private int stock;
    private boolean activo = true;

    public Producto(Long id, String codigoBarras, String nombre, BigDecimal precio, int stock) {
        if (codigoBarras == null || codigoBarras.isBlank() || nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El producto requiere codigo y nombre");
        }
        this.id = id;
        this.codigoBarras = codigoBarras.trim();
        this.nombre = nombre.trim();
        setPrecio(precio);
        setStock(stock);
    }

    public Long getId() { return id; }
    public String getCodigoBarras() { return codigoBarras; }
    public String getNombre() { return nombre; }
    public BigDecimal getPrecio() { return precio; }
    public BigDecimal getPorcentajeImpuesto() { return porcentajeImpuesto; }
    public int getStock() { return stock; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public void setPrecio(BigDecimal precio) {
        if (precio == null || precio.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        this.precio = Dinero.normalizar(precio);
    }

    public void setPorcentajeImpuesto(BigDecimal porcentajeImpuesto) {
        if (porcentajeImpuesto == null || porcentajeImpuesto.signum() < 0) {
            throw new IllegalArgumentException("El impuesto no puede ser negativo");
        }
        this.porcentajeImpuesto = Dinero.normalizar(porcentajeImpuesto);
    }

    public void setStock(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        this.stock = stock;
    }

    @Override
    public String toString() { return nombre; }
}
