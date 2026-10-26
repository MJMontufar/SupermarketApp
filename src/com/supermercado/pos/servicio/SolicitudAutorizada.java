package com.supermercado.pos.servicio;

/** Contrato comun para una operacion puntual de un solo uso, sin cambiar la sesion. */
public interface SolicitudAutorizada<T> {
    T confirmar(String username, char[] clave);
    int intentosRestantes();
    boolean terminada();
}
