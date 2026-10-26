package com.supermercado.pos.dominio;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class Usuario {
    private final Long id;
    private final String username;
    private final String nombreCompleto;
    private final String passwordHash;
    private boolean activo = true;
    private final Set<Rol> roles = new LinkedHashSet<>();
    private final Set<Permiso> permisosExtra = new LinkedHashSet<>();

    public Usuario(Long id, String username, String nombreCompleto, String passwordHash) {
        if (username == null || username.isBlank()
                || username.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("El usuario es obligatorio y no admite espacios");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("El hash es obligatorio");
        }
        this.id = id;
        this.username = username;
        this.nombreCompleto = nombreCompleto;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Set<Rol> getRoles() { return Collections.unmodifiableSet(roles); }
    public Set<Permiso> getPermisosExtra() { return Collections.unmodifiableSet(permisosExtra); }

    // Estos metodos modelan datos. RolService verifica la autorizacion,
    // jerarquia y permisos antes de guardar asignaciones en Oracle.
    public void agregarRol(Rol rol) { roles.add(Objects.requireNonNull(rol)); }
    public void quitarRol(Rol rol) { roles.remove(rol); }
    public void agregarPermisoExtra(Permiso permiso) {
        permisosExtra.add(Objects.requireNonNull(permiso));
    }

    public int nivelMaximo() {
        return roles.stream().mapToInt(Rol::getNivel).max().orElse(0);
    }

    public boolean tienePermiso(String codigo) {
        return roles.stream().flatMap(rol -> rol.getPermisos().stream())
                .anyMatch(permiso -> permiso.getCodigo().equals(codigo))
                || permisosExtra.stream().anyMatch(permiso -> permiso.getCodigo().equals(codigo));
    }

    @Override
    public String toString() { return username; }
}
