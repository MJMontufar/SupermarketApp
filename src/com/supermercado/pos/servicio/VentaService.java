package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.ProductoDAO;
import com.supermercado.pos.dao.VentaDAO;
import com.supermercado.pos.dominio.Carrito;
import com.supermercado.pos.dominio.LineaCarrito;
import com.supermercado.pos.dominio.Pago;
import com.supermercado.pos.dominio.Producto;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.dominio.Venta;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class VentaService {
    private final ProductoDAO productos = new ProductoDAO();
    private final VentaDAO ventas = new VentaDAO();
    private final TurnoService turnos = new TurnoService();
    public List<Producto> productos(Usuario actor) {
        Reglas.requerir(actor, "VENTA_COBRAR");
        try (Connection con = Conexion.obtener()) { return productos.activos(con); }
        catch (SQLException e) { throw new IllegalStateException("No se pudo consultar el catalogo", e); }
    }
    public Venta cobrar(long turno, Carrito carrito, BigDecimal recibido, BigDecimal tarjeta, Usuario actor) {
        Reglas.requerir(actor, "VENTA_COBRAR");
        if (carrito == null || carrito.estaVacio()) { throw new ReglaNegocioException("El carrito esta vacio"); }
        // Instantanea inmutable: los eventos de la ventana no cambian una venta en proceso.
        List<LineaCarrito> lineas = carrito.lineas();
        BigDecimal total = Dinero.normalizar(lineas.stream().map(LineaCarrito::subtotal).reduce(Dinero.CERO, BigDecimal::add));
        List<Pago> pagos = PlanPago.calcular(total, recibido, tarjeta);
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                turnos.bloquearPropio(con, turno, actor);
                long id = ventas.insertar(con, turno, total);
                BigDecimal base = Dinero.CERO;
                List<Venta.Detalle> detalles = new ArrayList<>();
                // Todas las cajas bloquean los productos en el mismo orden para evitar interbloqueos.
                for (LineaCarrito linea : lineas.stream().sorted(Comparator.comparing(l -> l.producto().getId())).toList()) {
                    if (linea.cantidad() > 999999) { throw new ReglaNegocioException("La cantidad maxima por linea es 999999"); }
                    Producto actual = productos.bloquear(con, linea.producto().getId())
                            .orElseThrow(() -> new ReglaNegocioException("El producto ya no existe"));
                    if (actual.getPrecio().compareTo(linea.precioUnitario()) != 0) {
                        throw new ReglaNegocioException("Cambio el precio de " + actual.getNombre() + "; cancela la venta y actualiza el catalogo");
                    }
                    if (!productos.descontar(con, actual.getId(), linea.cantidad())) {
                        throw new ReglaNegocioException("Sin stock disponible: " + actual.getNombre());
                    }
                    ventas.detalle(con, id, linea);
                    BigDecimal factor = BigDecimal.ONE.add(actual.getPorcentajeImpuesto().divide(new BigDecimal("100")));
                    base = base.add(linea.subtotal().divide(factor, 2, RoundingMode.HALF_UP));
                    detalles.add(new Venta.Detalle(actual.getCodigoBarras(), actual.getNombre(), linea.cantidad(),
                            linea.precioUnitario(), linea.subtotal()));
                }
                for (Pago pago : pagos) { ventas.pago(con, id, pago); }
                LocalDateTime fecha = LocalDateTime.now();
                con.commit();
                return new Venta(id, turno, fecha, detalles, base, total.subtract(base), total, pagos);
            } catch (SQLException | RuntimeException e) { con.rollback(); throw e; }
        } catch (SQLException e) { throw new IllegalStateException("No se pudo guardar la venta", e); }
    }
}
