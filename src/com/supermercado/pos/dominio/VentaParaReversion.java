package com.supermercado.pos.dominio;

import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaParaReversion(long id, long turnoId, LocalDateTime fecha, String estado,
        BigDecimal total, List<Detalle> detalles) {
    public VentaParaReversion { total = Dinero.normalizar(total); detalles = List.copyOf(detalles); }
    public record Detalle(long id, long productoId, String producto, int cantidad, int reversada, BigDecimal precio) {
        public Detalle { precio = Dinero.normalizar(precio); }
        public int disponible() { return cantidad - reversada; }
    }
}
