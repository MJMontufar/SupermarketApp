package com.supermercado.pos.util;

import com.supermercado.pos.dominio.Usuario;

/** Estado del usuario autenticado; nunca guarda su contrasena original. */
public final class Sesion {
    private static volatile Usuario usuario;
    private Sesion() { }
    public static Usuario getUsuario() { return usuario; }
    public static void setUsuario(Usuario nuevo) { usuario = nuevo; }
    public static void cerrar() { usuario = null; }
}
