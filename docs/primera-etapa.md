# Primera etapa: qué hace cada clase

## El producto y el carrito

`Producto` representa un artículo del catálogo: código de barras, nombre,
precio, porcentaje de impuesto, stock y estado activo. Los atributos privados
se consultan o modifican por sus métodos: eso es encapsulamiento.

`LineaCarrito` representa un producto con cantidad y precio capturado. Es un
`record`: Java genera sus accesores. Usamos `linea.cantidad()` en vez de
`linea.getCantidad()`. Copiamos el precio para que cambiar el catálogo no
cambie las líneas ya agregadas. También se conservará el precio en las ventas.

`Carrito` agrupa líneas por ID de producto. Un `LinkedHashMap` permite buscar
por ese ID y conserva el orden de inserción. Agregar dos veces el mismo producto
acumula su cantidad. El carrito no guarda nada en Oracle.

Ejemplo para leer, sin necesidad de crear otro archivo:

```java
Producto arroz = new Producto(1L, "001", "Arroz", new BigDecimal("12.50"), 10);
Carrito carrito = new Carrito();
carrito.agregar(arroz, 2);
System.out.println(carrito.total()); // 25.00
```

Cancelar un carrito no cambia existencias. Solo un cobro confirmado las
descontará mediante JDBC, dentro de una transacción.

## Por qué BigDecimal

Usamos `new BigDecimal("12.50")`, con texto, para representar el decimal de forma
exacta. Evitamos `new BigDecimal(12.50)`, que primero recibe un `double`.
`Dinero` centraliza el redondeo a dos decimales con `HALF_UP`.
Para comparar montos en las reglas usaremos `compareTo`: `equals` también
compara la escala.

## Usuarios, roles y permisos

Un `Permiso` identifica una acción como `VENTA_COBRAR`. Un `Rol` agrupa permisos
y tiene nivel jerárquico. Un `Usuario` tiene roles y permisos extra.

`tienePermiso` revisa todos los roles y permisos extra. `nivelMaximo` toma el
nivel más alto de sus roles; un permiso extra no aumenta el nivel.

Los permisos se comparan por código y los roles por nombre. Esos campos son
inmutables para mantener consistente su uso dentro de un `Set`.

El alta con un rol inicial ya se valida en `UsuarioService`. La autorización de
reversiones y la asignación posterior de múltiples roles siguen pendientes.

## Contraseñas

`Seguridad.hash` calcula PBKDF2 y devuelve `iteraciones:sal:hash`. Cada sal es
aleatoria. `Seguridad.verificar` compara la clave con el registro almacenado.

El login usa `JPasswordField.getPassword()` y limpia el `char[]` con
`Arrays.fill` en un bloque `finally`. La contraseña original no se guarda.
La regla de mínimo ocho caracteres se valida en `UsuarioService`.

## Qué observar en las pruebas

Cada `OK` es un caso ejecutado. `FALLO` indica un resultado inesperado.
Al final aparecen el total y los fallos. PBKDF2 se verifica también contra un
resultado calculado con otra implementación.

Para entender esta etapa, intenta explicar:

1. ¿Por qué agregar productos al carrito no descuenta stock?
2. ¿Por qué cada línea conserva una copia del precio?
3. ¿Qué diferencia hay entre un permiso y el nivel de un rol?

## Siguiente etapa

Los scripts SQL y la conexión ya se incorporaron en la segunda etapa. Continúa
con [login y usuarios](login-y-usuarios.md); después avanzaremos a los turnos.
