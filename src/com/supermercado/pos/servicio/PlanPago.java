package com.supermercado.pos.servicio;

import com.supermercado.pos.dominio.Pago;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Reutilizado por el dialogo y el servicio; el servicio siempre lo recalcula. */
public final class PlanPago {
    private PlanPago() { }
    public static List<Pago> calcular(BigDecimal total, BigDecimal recibido, BigDecimal tarjeta) {
        total = Reglas.monto(total, "Total", true);
        recibido = Reglas.monto(recibido, "Efectivo recibido", false);
        tarjeta = Reglas.monto(tarjeta, "Tarjeta", false);
        if (tarjeta.compareTo(total) > 0) {
            throw new ReglaNegocioException("La tarjeta no puede superar el total; el cambio solo sale del efectivo");
        }
        BigDecimal aplicado = total.subtract(tarjeta);
        if (recibido.compareTo(aplicado) < 0) { throw new ReglaNegocioException("Pago insuficiente"); }
        if (aplicado.signum() == 0 && recibido.signum() > 0) {
            throw new ReglaNegocioException("El pago ya esta cubierto con tarjeta; retira el efectivo ingresado");
        }
        List<Pago> pagos = new ArrayList<>();
        if (aplicado.signum() > 0) {
            pagos.add(new Pago(Pago.Metodo.EFECTIVO, aplicado, recibido, recibido.subtract(aplicado)));
        }
        if (tarjeta.signum() > 0) { pagos.add(new Pago(Pago.Metodo.TARJETA, tarjeta, tarjeta, Dinero.CERO)); }
        return List.copyOf(pagos);
    }
}
