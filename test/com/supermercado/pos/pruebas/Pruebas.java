package com.supermercado.pos.pruebas;

import com.supermercado.pos.dominio.Carrito;
import com.supermercado.pos.dominio.Permiso;
import com.supermercado.pos.dominio.Producto;
import com.supermercado.pos.dominio.Rol;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Dinero;
import com.supermercado.pos.util.Seguridad;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Primera etapa: pruebas propias sin Oracle ni JUnit. */
public final class Pruebas {
    private static int ejecutadas;
    private static int fallos;

    private Pruebas() { }

    private static void verificar(boolean condicion) {
        if (!condicion) {
            throw new AssertionError("No se cumplio el resultado esperado");
        }
    }

    private static void caso(String nombre, Runnable prueba) {
        ejecutadas++;
        try {
            prueba.run();
            System.out.println("OK    " + nombre);
        } catch (RuntimeException | AssertionError e) {
            fallos++;
            System.out.println("FALLO " + nombre);
            e.printStackTrace(System.out);
        }
    }

    private static void esperaExcepcion(Class<? extends RuntimeException> tipo, Runnable accion) {
        try {
            accion.run();
        } catch (RuntimeException e) {
            if (tipo.isInstance(e)) {
                return;
            }
            throw e;
        }
        throw new AssertionError("No se lanzo " + tipo.getSimpleName());
    }

    private static Producto arroz(int stock) {
        return new Producto(1L, "001", "Arroz", new BigDecimal("12.50"), stock);
    }

    private static String vectorPbkdf2() {
        // Resultado comprobado con hashlib.pbkdf2_hmac de Python.
        // Clave de prueba: password; sal ASCII: 1234567890abcdef; 120000 iteraciones.
        return "120000:MTIzNDU2Nzg5MGFiY2RlZg==:mbRfSuiHsZ4fs79gM0ir80cqqg+Euuud5cqFVy6cWE8=";
    }

    private static Usuario usuario() {
        // Solo datos de prueba, sin cuenta real asociada.
        return new Usuario(1L, "ana", "Ana", vectorPbkdf2());
    }

    private static void probarDinero() {
        caso("Montos con dos decimales", () ->
                verificar(Dinero.desdeTexto("20").equals(new BigDecimal("20.00"))));
        caso("HALF_UP: 1.005 se redondea a 1.01", () ->
                verificar(Dinero.desdeTexto("1.005").equals(new BigDecimal("1.01"))));
        caso("HALF_UP con diferencias negativas", () ->
                verificar(Dinero.desdeTexto("-1.005").equals(new BigDecimal("-1.01"))));
        caso("0.10 mas 0.20 es exactamente 0.30", () ->
                verificar(Dinero.desdeTexto("0.10").add(Dinero.desdeTexto("0.20"))
                        .equals(new BigDecimal("0.30"))));
        caso("Monto vacio rechazado", () ->
                esperaExcepcion(IllegalArgumentException.class, () -> Dinero.desdeTexto(" ")));
        caso("Texto no numerico rechazado", () ->
                esperaExcepcion(NumberFormatException.class, () -> Dinero.desdeTexto("Q20")));
    }

    private static void probarCarrito() {
        caso("Carrito vacio totaliza 0.00", () -> {
            Carrito carrito = new Carrito();
            verificar(carrito.estaVacio() && carrito.total().equals(Dinero.CERO));
        });
        caso("Repetir producto acumula cantidades en una fila", () -> {
            Carrito carrito = new Carrito();
            Producto producto = arroz(10);
            carrito.agregar(producto, 2);
            carrito.agregar(producto, 1);
            verificar(carrito.lineas().size() == 1 && carrito.lineas().getFirst().cantidad() == 3);
            verificar(carrito.total().equals(new BigDecimal("37.50")));
        });
        caso("Varios productos suman Q54.00", () -> {
            Carrito carrito = new Carrito();
            carrito.agregar(arroz(10), 3);
            carrito.agregar(new Producto(2L, "002", "Leche", new BigDecimal("8.25"), 10), 2);
            verificar(carrito.total().equals(new BigDecimal("54.00")));
        });
        caso("Stock insuficiente no modifica carrito ni existencias", () -> {
            Producto producto = arroz(2);
            Carrito carrito = new Carrito();
            carrito.agregar(producto, 1);
            esperaExcepcion(ReglaNegocioException.class, () -> carrito.agregar(producto, 2));
            verificar(carrito.lineas().getFirst().cantidad() == 1 && producto.getStock() == 2);
        });
        caso("Cantidad cero rechazada", () ->
                esperaExcepcion(ReglaNegocioException.class, () -> new Carrito().agregar(arroz(5), 0)));
        caso("Cantidad negativa rechazada", () ->
                esperaExcepcion(ReglaNegocioException.class, () -> new Carrito().agregar(arroz(5), -1)));
        caso("Producto inactivo rechazado", () -> {
            Producto producto = arroz(5);
            producto.setActivo(false);
            esperaExcepcion(ReglaNegocioException.class, () -> new Carrito().agregar(producto, 1));
        });
        caso("Producto sin identificador rechazado", () -> {
            Producto producto = new Producto(null, "003", "Sin guardar", BigDecimal.ONE, 5);
            esperaExcepcion(ReglaNegocioException.class, () -> new Carrito().agregar(producto, 1));
        });
        caso("Desbordamiento de cantidad rechazado sin alterar la fila", () -> {
            Producto producto = arroz(Integer.MAX_VALUE);
            Carrito carrito = new Carrito();
            carrito.agregar(producto, 1);
            esperaExcepcion(ReglaNegocioException.class,
                    () -> carrito.agregar(producto, Integer.MAX_VALUE));
            verificar(carrito.lineas().getFirst().cantidad() == 1);
        });
        caso("Editar cantidad actualiza total sin descontar stock", () -> {
            Producto producto = arroz(10);
            Carrito carrito = new Carrito();
            carrito.agregar(producto, 1);
            carrito.cambiarCantidad(1L, 4);
            verificar(carrito.total().equals(new BigDecimal("50.00")) && producto.getStock() == 10);
        });
        caso("Editar por encima del stock conserva la cantidad anterior", () -> {
            Carrito carrito = new Carrito();
            carrito.agregar(arroz(2), 1);
            esperaExcepcion(ReglaNegocioException.class, () -> carrito.cambiarCantidad(1L, 3));
            verificar(carrito.lineas().getFirst().cantidad() == 1);
        });
        caso("Editar a cero se rechaza", () -> {
            Carrito carrito = new Carrito();
            carrito.agregar(arroz(2), 1);
            esperaExcepcion(ReglaNegocioException.class, () -> carrito.cambiarCantidad(1L, 0));
        });
        caso("Editar una fila inexistente se rechaza", () ->
                esperaExcepcion(ReglaNegocioException.class, () -> new Carrito().cambiarCantidad(9L, 1)));
        caso("Cambiar el catalogo conserva el precio capturado", () -> {
            Producto producto = arroz(10);
            Carrito carrito = new Carrito();
            carrito.agregar(producto, 1);
            producto.setPrecio(new BigDecimal("99.00"));
            carrito.agregar(producto, 1);
            carrito.cambiarCantidad(1L, 3);
            verificar(carrito.total().equals(new BigDecimal("37.50")));
        });
        caso("Quitar y vaciar dejan el carrito vacio", () -> {
            Carrito carrito = new Carrito();
            carrito.agregar(arroz(5), 2);
            carrito.quitar(1L);
            verificar(carrito.estaVacio());
            carrito.agregar(arroz(5), 1);
            carrito.vaciar();
            verificar(carrito.estaVacio() && carrito.total().equals(Dinero.CERO));
        });
        caso("Las lineas no se pueden modificar desde fuera", () -> {
            Carrito carrito = new Carrito();
            carrito.agregar(arroz(5), 1);
            esperaExcepcion(UnsupportedOperationException.class, () -> carrito.lineas().clear());
            verificar(carrito.lineas().size() == 1);
        });
        caso("Catalogo rechaza precio y stock negativos", () -> {
            esperaExcepcion(IllegalArgumentException.class, () -> arroz(-1));
            esperaExcepcion(IllegalArgumentException.class,
                    () -> arroz(1).setPrecio(new BigDecimal("-0.01")));
        });
    }

    private static void probarPermisos() {
        caso("Permisos iguales por codigo en un Set", () -> {
            Set<Permiso> permisos = new HashSet<>();
            permisos.add(new Permiso(1L, "VENTA_COBRAR", "Cobrar"));
            permisos.add(new Permiso(2L, "VENTA_COBRAR", "Mismo codigo"));
            verificar(permisos.size() == 1);
        });
        caso("Roles iguales por nombre en un Set", () -> {
            Set<Rol> roles = new HashSet<>();
            roles.add(new Rol(1L, "CAJERO", 1));
            roles.add(new Rol(2L, "CAJERO", 1));
            verificar(roles.size() == 1);
        });
        caso("Sin roles no hay nivel ni permisos", () -> {
            Usuario usuario = usuario();
            verificar(usuario.nivelMaximo() == 0 && !usuario.tienePermiso("VENTA_COBRAR"));
        });
        caso("Los permisos se heredan de los roles", () -> {
            Rol rol = new Rol(1L, "CAJERO", 1);
            rol.agregarPermiso(new Permiso(1L, "VENTA_COBRAR", "Cobrar"));
            Usuario usuario = usuario();
            usuario.agregarRol(rol);
            verificar(usuario.tienePermiso("VENTA_COBRAR"));
            verificar(!usuario.tienePermiso("REVERSION_AUTORIZAR"));
        });
        caso("Permisos extra no elevan el nivel del rol", () -> {
            Usuario usuario = usuario();
            usuario.agregarPermisoExtra(new Permiso(1L, "CORTE_INTERMEDIO", "Retirar"));
            verificar(usuario.tienePermiso("CORTE_INTERMEDIO") && usuario.nivelMaximo() == 0);
        });
        caso("Nivel efectivo es el mayor de los roles", () -> {
            Usuario usuario = usuario();
            usuario.agregarRol(new Rol(1L, "CAJERO", 1));
            usuario.agregarRol(new Rol(2L, "SUPERVISOR", 2));
            verificar(usuario.nivelMaximo() == 2);
        });
        caso("Quitar un rol elimina sus permisos", () -> {
            Rol rol = new Rol(1L, "CAJERO", 1);
            rol.agregarPermiso(new Permiso(1L, "VENTA_COBRAR", "Cobrar"));
            Usuario usuario = usuario();
            usuario.agregarRol(rol);
            usuario.quitarRol(rol);
            verificar(!usuario.tienePermiso("VENTA_COBRAR") && usuario.nivelMaximo() == 0);
        });
        caso("Las colecciones de roles y permisos son protegidas", () -> {
            Usuario usuario = usuario();
            Rol rol = new Rol(1L, "CAJERO", 1);
            esperaExcepcion(UnsupportedOperationException.class, () -> usuario.getRoles().add(rol));
            esperaExcepcion(UnsupportedOperationException.class,
                    () -> rol.getPermisos().add(new Permiso(1L, "VENTA_COBRAR", "Cobrar")));
        });
        caso("Username con espacios rechazado", () ->
                esperaExcepcion(IllegalArgumentException.class,
                        () -> new Usuario(1L, "ana lopez", "Ana", vectorPbkdf2())));
        caso("Usuario se desactiva sin borrar su identidad", () -> {
            Usuario usuario = usuario();
            usuario.setActivo(false);
            verificar(!usuario.isActivo() && usuario.getId().equals(1L));
        });
    }

    private static void probarSeguridad() {
        caso("PBKDF2 coincide con un vector calculado independientemente", () -> {
            char[] clave = "password".toCharArray();
            try {
                verificar(Seguridad.verificar(clave, vectorPbkdf2()));
            } finally {
                Arrays.fill(clave, '\0');
            }
        });
        caso("Misma clave produce sales distintas y ambos hashes verifican", () -> {
            char[] clave = "ClaveSoloParaPruebas2026".toCharArray();
            try {
                String primero = Seguridad.hash(clave);
                String segundo = Seguridad.hash(clave);
                verificar(!primero.split(":")[1].equals(segundo.split(":")[1]));
                verificar(Seguridad.verificar(clave, primero) && Seguridad.verificar(clave, segundo));
            } finally {
                Arrays.fill(clave, '\0');
            }
        });
        caso("Clave incorrecta rechazada", () -> {
            char[] clave = "incorrecta".toCharArray();
            try {
                verificar(!Seguridad.verificar(clave, vectorPbkdf2()));
            } finally {
                Arrays.fill(clave, '\0');
            }
        });
        caso("Hashes corruptos y trabajo excesivo se rechazan", () -> {
            char[] clave = "password".toCharArray();
            try {
                verificar(!Seguridad.verificar(clave, "sin-formato"));
                verificar(!Seguridad.verificar(clave, "120000:!:!"));
                verificar(!Seguridad.verificar(clave, vectorPbkdf2().replace("120000:", "0:")));
                verificar(!Seguridad.verificar(clave, vectorPbkdf2().replace("120000:", "2000000000:")));
                verificar(!Seguridad.verificar(clave, null));
            } finally {
                Arrays.fill(clave, '\0');
            }
        });
    }

    public static void main(String[] args) {
        probarDinero();
        probarCarrito();
        probarPermisos();
        probarSeguridad();
        System.out.printf("%nPruebas: %d; correctas: %d; fallos: %d%n",
                ejecutadas, ejecutadas - fallos, fallos);
        if (fallos != 0) {
            System.exit(1);
        }
    }
}
