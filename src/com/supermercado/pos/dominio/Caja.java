package com.supermercado.pos.dominio;

public record Caja(long id, int numero, String nombre) {
    @Override public String toString() { return numero + " - " + nombre; }
}
