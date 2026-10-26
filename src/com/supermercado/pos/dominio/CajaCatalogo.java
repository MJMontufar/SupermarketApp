package com.supermercado.pos.dominio;

public record CajaCatalogo(long id,int numero,String nombre,boolean activa) {
    @Override public String toString(){return numero+" - "+nombre;}
}
