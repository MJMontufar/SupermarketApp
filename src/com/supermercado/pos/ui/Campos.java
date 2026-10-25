package com.supermercado.pos.ui;

import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;
import javax.swing.JTextField;

final class Campos {
    private Campos() { }
    static BigDecimal monto(JTextField campo) {
        BigDecimal exacto = montoExacto(campo);
        if (exacto.signum() < 0) { throw new ReglaNegocioException("El monto no puede ser negativo"); }
        return Dinero.normalizar(exacto);
    }
    static BigDecimal montoExacto(JTextField campo) {
        try { return new BigDecimal(campo.getText().trim()); }
        catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("Ingresa un monto usando punto decimal, por ejemplo 1000.00");
        }
    }
}
