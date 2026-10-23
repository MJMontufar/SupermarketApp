package com.supermercado.pos.dominio;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class Rol {
    private final Long id;
    private final String nombre;
    private final int nivel;
    private final Set<Permiso> permisos = new LinkedHashSet<>();

    public Rol(Long id, String nombre, int nivel) {
        if (nombre == null || nombre.isBlank() || nivel < 1) {
            throw new IllegalArgumentException("El rol requiere nombre y nivel positivo");
        }
        this.id = id;
        this.nombre = nombre.trim();
        this.nivel = nivel;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public int getNivel() { return nivel; }
    public Set<Permiso> getPermisos() { return Collections.unmodifiableSet(permisos); }

    public void agregarPermiso(Permiso permiso) {
        permisos.add(Objects.requireNonNull(permiso));
    }

    public void quitarPermiso(Permiso permiso) { permisos.remove(permiso); }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Rol rol && nombre.equals(rol.nombre);
    }

    @Override
    public int hashCode() { return nombre.hashCode(); }

    @Override
    public String toString() { return nombre; }
}
