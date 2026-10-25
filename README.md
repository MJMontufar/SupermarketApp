# SupermarketApp

Proyecto de Programación II: sistema POS de supermercado con Java 21, Swing,
JDBC puro y Oracle Database XE 21c. Sin Maven, Gradle ni frameworks.

## Estado del proyecto

Implementado y comprobable sin Oracle:

- Productos y carrito en memoria: cantidades, existencias y total.
- Dinero con `BigDecimal`, escala 2 y `RoundingMode.HALF_UP`.
- Usuarios, roles, permisos heredados y permisos extra.
- Hash PBKDF2-HMAC-SHA256 con sal aleatoria y verificación de contraseñas.
- Ejecutor de pruebas propio, sin JUnit.

La segunda etapa agrega el esquema Oracle de 15 tablas, los índices de turno
único por caja y cajero, datos iniciales, conexión JDBC y pruebas de integridad.
La cuenta de entrenamiento `admin / Admin123!` se crea al cargar los datos,
con contraseña PBKDF2.

La tercera etapa agrega login Swing, menú según permisos, DAO de usuarios y
roles, y administración básica: alta con rol inicial, búsqueda, edición de
nombre/contraseña y desactivación. El servicio aplica permisos y jerarquía,
además de los controles de la interfaz.

Siguen pendientes el ciclo de caja (turnos, ventas, retiros, reversiones y cierre),
los catálogos editables, la matriz de roles/permisos y la asignación múltiple de
roles y permisos extra. Las pruebas sin Oracle funcionan por separado.

Para preparar la base, sigue [la guía de Oracle y JDBC](docs/base-de-datos.md).
El [diagrama entidad-relación](docs/modelo-er.md) corresponde al esquema actual.

## Abrir la aplicación de escritorio

Con POS ya preparado y `lib/ojdbc11.jar` presente, abre una terminal integrada
desde la carpeta `scripts` y ejecuta:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\iniciar.ps1
```

Primero ingresa la clave de Oracle de **pos** en la consola. Después se abre la
ventana de login: usa **admin / Admin123!** para la cuenta de entrenamiento.
Son credenciales distintas. No se necesitan cambios SQL para esta tercera etapa.

Consulta [la guía de login y usuarios](docs/login-y-usuarios.md) para actualizar
tu copia y crear a Ana, Luis y Marta. Conserva tu driver local al actualizar.

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
  dao/         UsuarioDAO, RolDAO
  servicio/    AuthService, UsuarioService
  ui/          Login, menú, lista y formulario de usuarios
  excepcion/   ReglaNegocioException, AutorizacionException
  util/        Dinero, Seguridad, Conexion, Configuracion, Sesion
test/com/supermercado/pos/pruebas/Pruebas.java
scripts/       Compilación y pruebas para Windows
docs/          Explicación de la primera etapa
sql/           Creación de cuentas, esquema, datos y verificación
lib/           Driver JDBC descargado localmente, excluido de Git
config.properties  URL de Oracle, usuario POS y mínimo operativo; sin claves
```

## Validación de esta etapa

Las 37 pruebas sin base de datos pasaron con JDK 21 en Linux, sin advertencias del
compilador. Los scripts se ejercitaron con PowerShell 7 en Linux, incluyendo
rutas con espacios y un fallo de compilación simulado. La ejecución en Windows
de la primera etapa también fue confirmada durante el desarrollo.

La segunda etapa tiene una suite separada para Oracle XE 21c: revisa la guía de
base de datos para ejecutarla exclusivamente en `POS_TEST`. La suite conserva
las tablas y revierte sus registros de prueba con `rollback`. Las secuencias de
identidad pueden avanzar incluso al deshacer una transacción.

Las 9 pruebas Oracle pasaron con Oracle XE 21.3, Java 21 y `ojdbc11` 21.11.
Se ejecutaron los scripts SQL en POS y POS_TEST, la conexión JDBC y la repetición
de datos iniciales conservando precio y stock existentes. El descargador se
probó con PowerShell 7 en Linux, incluyendo descarga, checksum, reutilización
y conservación de un driver distinto. La conexión JDBC y lectura de cinco
productos también se confirmaron en Windows durante el desarrollo.

La tercera etapa pasó 19 pruebas funcionales de autenticación y administración
en POS_TEST. Se comprobó el ingreso oculto de la clave técnica por consola,
sin variable de entorno. Cuatro pruebas adicionales abrieron ventanas Swing
reales en Linux con Xvfb, autenticaron, crearon un cajero desde el formulario y
verificaron los permisos del menú. Hay capturas en
[evidencias de interfaz](docs/evidencias/etapa3/README.md).

## Límites de estas pruebas

Se validan cálculos, cambios del carrito, permisos en memoria y criptografía.
Estas pruebas no sustituyen los nueve casos obligatorios de la fase 10 del PDF:
los servicios de turnos, pagos, retiros, reversiones y cierre siguen pendientes.
Las pruebas Oracle actuales verifican restricciones y rollback JDBC directamente;
no acreditan el comportamiento de servicios que todavía no están implementados.

El carrito comprueba el stock que tiene en memoria, pero no lo descuenta. Al
cobrar, el servicio deberá comprobarlo de nuevo en Oracle y descontarlo dentro
de la misma transacción que guarda la venta y sus pagos.

Las entidades representan datos; `AuthService` autentica y `UsuarioService`
verifica usuario activo, permisos y jerarquía antes de administrar usuarios.
La autorización puntual de reversiones y su auditoría siguen pendientes.

Los precios del carrito ya incluyen impuesto, según el PDF. El desglose del
impuesto se añadirá con la pantalla de cobro; no se suma impuesto al total actual.

Nunca guardes contraseñas reales en el código o en Git. Los hashes de `test`
son únicamente datos de pruebas, sin cuentas asociadas.

Consulta [la explicación inicial](docs/primera-etapa.md) antes de continuar.
