package com.supermercado.pos.excepcion;

public final class AutorizacionException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public AutorizacionException(String mensaje) { super(mensaje); }
}
