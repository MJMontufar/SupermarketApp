package com.supermercado.pos.servicio;

import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.AutorizacionException;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;

final class Reglas {
    private Reglas() { }
    static void requerir(Usuario actor, String permiso) {
        if (actor == null || actor.getId() == null || !actor.isActivo() || !actor.tienePermiso(permiso)) {
            throw new AutorizacionException("No tienes permiso para esta operacion");
        }
    }
    static BigDecimal monto(BigDecimal valor, String campo, boolean positivo) {
        if (valor == null || valor.signum() < 0) {
            throw new ReglaNegocioException(campo + ": ingresa un monto " + (positivo ? "positivo" : "no negativo"));
        }
        BigDecimal normal = Dinero.normalizar(valor);
        if ((positivo && normal.signum() == 0) || normal.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new ReglaNegocioException(campo + ": monto fuera del rango permitido");
        }
        return normal;
    }
}
