# SupermarketApp

Proyecto de Programación II: sistema POS de supermercado con Java 21, Swing,
JDBC puro y Oracle Database XE 21c. Sin Maven, Gradle ni frameworks.

## Estado de la primera etapa

Implementado y comprobable sin Oracle:

- Productos y carrito en memoria: cantidades, existencias y total.
- Dinero con `BigDecimal`, escala 2 y `RoundingMode.HALF_UP`.
- Usuarios, roles, permisos heredados y permisos extra.
- Hash PBKDF2-HMAC-SHA256 con sal aleatoria y verificación de contraseñas.
- Ejecutor de pruebas propio, sin JUnit.

Todavía faltan las ventanas Swing, las entidades de turno/venta y sus servicios,
la conexión JDBC, los scripts SQL y la administración persistida. No hay login
ni usuarios de aplicación creados todavía. Oracle no es necesario para estas
primeras pruebas; sí será obligatorio para completar el proyecto.

## Abrir en VS Code

1. Instala JDK 21 y la extensión **Extension Pack for Java**, de Microsoft.
2. Abre la carpeta completa `SupermarketApp` con **Archivo → Abrir carpeta**.
3. Usa **Terminal → Nueva terminal**, con PowerShell.
4. Comprueba `java -version` y `javac -version`: ambos deben indicar 21.

La carpeta `.vscode` configura un proyecto Java sin herramientas de construcción.
No se incluyen rutas específicas de ninguna computadora.

## Compilar y ejecutar las pruebas en Windows

Desde la raíz del proyecto:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\pruebas.ps1
```

`ExecutionPolicy Bypass` se aplica únicamente a ese proceso. No modifica la
política global de Windows. El script compila `src` y `test`, ejecuta las pruebas
y devuelve un código distinto de cero si una prueba falla.

Si `JAVA_HOME` está definido, los scripts usan su JDK; debe apuntar a Java 21.
La carpeta `out` contiene clases generadas y está excluida de Git.

Para compilar solamente el código de la aplicación:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\compilar.ps1
```

Alternativa sin ejecutar scripts PowerShell:

```powershell
$fuentes = @(Get-ChildItem src,test -Recurse -Filter *.java | ForEach-Object { $_.FullName })
javac -encoding UTF-8 --release 21 -Xlint:all -d out @fuentes
if ($LASTEXITCODE -eq 0) { java -cp out com.supermercado.pos.pruebas.Pruebas }
```

## Compilar y probar en Linux

Con JDK 21 activo, desde la raíz del proyecto:

```sh
mkdir -p out
find src test -name '*.java' > out/fuentes.txt
javac -encoding UTF-8 --release 21 -Xlint:all -d out @out/fuentes.txt
java -cp out com.supermercado.pos.pruebas.Pruebas
```

Ejecuta el último comando solo si la compilación termina correctamente.

## Estructura

```text
src/com/supermercado/pos/
  dominio/     Producto, Carrito, LineaCarrito, Usuario, Rol, Permiso
  excepcion/   ReglaNegocioException
  util/        Dinero, Seguridad
test/com/supermercado/pos/pruebas/Pruebas.java
scripts/       Compilación y pruebas para Windows
docs/          Explicación de la primera etapa
```

## Validación de esta etapa

Las 37 pruebas de esta etapa pasaron con JDK 21 en Linux, sin advertencias del
compilador. Los scripts se ejercitaron con PowerShell 7 en Linux, incluyendo
rutas con espacios y un fallo de compilación simulado. La ejecución en Windows
PowerShell de tu computadora queda pendiente de comprobar.

## Límites de estas pruebas

Se validan cálculos, cambios del carrito, permisos en memoria y criptografía.
Estas pruebas no sustituyen los nueve casos obligatorios de la fase 10 del PDF:
los turnos, pagos, retiros, reversiones, cierre y rollback se probarán al conectar
los servicios con Oracle, en un esquema de pruebas separado.

El carrito comprueba el stock que tiene en memoria, pero no lo descuenta. Al
cobrar, el servicio deberá comprobarlo de nuevo en Oracle y descontarlo dentro
de la misma transacción que guarda la venta y sus pagos.

Las entidades de seguridad representan roles y permisos; no autentican usuarios
ni autorizan por sí solas operaciones de administración. Los futuros servicios
deberán verificar usuario activo, permisos y jerarquía del actor.

Los precios del carrito ya incluyen impuesto, según el PDF. El desglose del
impuesto se añadirá con la pantalla de cobro; no se suma impuesto al total actual.

Nunca guardes contraseñas reales en el código o en Git. Los hashes de `test`
son únicamente datos de pruebas, sin cuentas asociadas.

Consulta [la explicación inicial](docs/primera-etapa.md) antes de continuar.
