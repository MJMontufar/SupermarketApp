package com.supermercado.pos.ui;

import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;
import javax.swing.JTextField;

final class Campos {
    private Campos() { }
    static BigDecimal monto(JTextField campo) {
        try { return Dinero.desdeTexto(campo.getText()); }
        catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("Ingresa un monto usando punto decimal, por ejemplo 1000.00");
        }
    }
}
