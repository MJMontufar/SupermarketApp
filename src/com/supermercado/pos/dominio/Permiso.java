package com.supermercado.pos.dominio;

/** La identidad del permiso es su codigo, que no cambia dentro de un Set. */
public final class Permiso {
    private final Long id;
    private final String codigo;
    private final String descripcion;

    public Permiso(Long id, String codigo, String descripcion) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El codigo del permiso es obligatorio");
        }
        this.id = id;
        this.codigo = codigo.trim();
        this.descripcion = descripcion;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Permiso permiso && codigo.equals(permiso.codigo);
    }

    @Override
    public int hashCode() { return codigo.hashCode(); }

    @Override
    public String toString() { return codigo; }
}
