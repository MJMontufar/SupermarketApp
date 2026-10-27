# SupermarketApp

Proyecto de Programación II: POS de supermercado con **Java 21, Swing, JDBC puro
y Oracle Database XE 21c**. Sin Maven, Gradle, frameworks, JUnit ni Mockito.
La única biblioteca externa de la aplicación es `lib/ojdbc11.jar`.

## Qué está implementado

- Login PBKDF2-HMAC-SHA256 con sal aleatoria y menú según permisos.
- Apertura y recuperación de turno, caja/cajero únicos y fondo positivo.
  Q0 solo se permite con autorización de otro usuario de mayor nivel.
- Carrito editable, lector de código de barras como teclado, cobro F12 en
  efectivo/tarjeta/mixto y desglose del impuesto incluido sin sumarlo otra vez.
- Venta, detalles, pagos y descuento de stock en una transacción. El cambio
  sale solo del efectivo; un fallo revierte la operación completa.
- Corte intermedio con mínimo configurable y confirmación de supervisor.
- Reversión parcial/total con precio histórico, motivo, autorización de mayor
  nivel con REVERSION_AUTORIZAR, reposición de stock, estado y reembolso efectivo.
- Tres fallos por solicitud de autorización, un solo uso, auditoría y sesión
  del cajero conservada. Las claves de supervisor se limpian tras cada intento.
- Cierre con resumen, contado, esperado y diferencia; el turno cerrado bloquea
  nuevas ventas, retiros y reversiones.
- Usuarios, altas/edición/desactivación, roles con matriz por módulo, varios
  roles y extras por usuario, permisos heredados en gris y aplicación al siguiente login.
- Catálogos de productos y cajas, búsqueda, edición y desactivación conservando
  historial. Se evita sobrescribir stock cambiado por una venta y desactivar
  o renumerar una caja con turno abierto.
- Comprobantes y reportes de práctica con vista previa e impresión del JDK.
- Tema Swing con grises, acentos esmeralda/violeta y menú por módulos; fotos
  opcionales de productos desde JPG/PNG, con vista previa en el catálogo.

Todas las operaciones con dinero usan `BigDecimal`, escala 2 y `HALF_UP`.
La autorización se comprueba en servicios además de la interfaz. No se permiten
roles propios, niveles iguales/superiores ni conceder permisos que el actor no
posee. Los permisos extra no elevan el nivel jerárquico.

## Empezar en Windows

Si empiezas desde cero o compartirás el proyecto, consulta la
[guía completa en PDF](docs/Guia-SupermarketApp-Windows.pdf) o su
[versión de texto](docs/guia-instalacion-windows.md). Incluye Git, acceso a GitHub,
instalación, Oracle, compilación, presentación, actualizaciones y respaldos.

1. Instala JDK 21, Oracle XE 21c y VS Code con **Extension Pack for Java**
   (Microsoft / Java de Red Hat). Abre la raíz del proyecto, no solo `src`.
2. Sigue [la preparación de Oracle](docs/base-de-datos.md) para crear POS y
   POS_TEST, sus tablas y datos iniciales. No ejecutes el esquema sobre una
   cuenta que ya tiene tablas.
3. Desde la raíz, descarga una vez el driver oficial con checksum:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\descargar-driver.ps1
   ```

4. Si vienes de una versión anterior a la apertura cero autorizada, aplica una
   vez [la actualización de base](docs/apertura-cero.md):

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\actualizar-bd.ps1
   ```

   Conserva usuarios, turnos, ventas y stock. No repitas SQL 00/01 para actualizar.
   Una instalación nueva con el esquema actual ya incluye esas restricciones.
5. Arranca:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\iniciar.ps1
   ```

   La consola pide la contraseña técnica de Oracle de **pos**. El login Swing
   pide una cuenta de la aplicación: son credenciales distintas. `JAVA_HOME`,
   si existe, debe apuntar a un JDK 21. No se usan rutas fijas de tu computadora.

## Usuarios de práctica

| Usuario | Rol | Contraseña |
|---|---|---|
| admin | ADMIN, nivel 4 | `Admin123!`, cuenta pública de entrenamiento del PDF |
| ana | CAJERO, nivel 1 | La que definiste al crearla desde admin |
| luis | CAJERO, nivel 1 | La que definiste al crearla desde admin |
| marta | SUPERVISOR, nivel 2 | La que definiste al crearla desde admin |

Los datos iniciales crean admin. [La guía de usuarios](docs/login-y-usuarios.md)
explica cómo crear las otras tres cuentas. Admin puede restablecer sus contraseñas
sin borrar usuarios ni historial. No guardes contraseñas técnicas de Oracle en Git.

## Actualizar y preparar la entrega

La [lista de trabajo de entrega](docs/entrega.md) reúne los pasos de Git,
práctica completa de caja, administración, pruebas y capturas. Las guías detalladas:

- [Escenario integral, retiro, reversión y cierre](docs/ciclo-completo.md).
- [Roles, permisos y catálogos](docs/administracion.md).
- [Apertura Q0 autorizada](docs/apertura-cero.md).
- [Modelo ER final](docs/modelo-er.md).
- [Mapa de las 12 pantallas y evidencias](docs/evidencias/README.md).
- [Nuevo diseño, fotografías y respaldo de imágenes](docs/diseno-y-fotos.md).
- [Capturas en Windows y respaldo del proyecto y Oracle](docs/capturas-y-respaldo.md).

El escenario completo fue comprobado: fondo Q1000 + efectivo neto Q220 − retiro
Q500 − reembolso Q30 = **Q690 esperados**. Con Q680 contados, diferencia **−Q10**.

## Pruebas en Windows

Con POS_TEST preparado, su actualización 04 aplicada y driver disponible:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verificar-entrega.ps1
```

Ejecuta 37 pruebas de dominio, 35 de negocio y 28 de administración: 100 casos.
Las dos últimas suites piden la clave técnica de **pos_test**. Todas crean y
limpian solo sus propias filas temporales; no reinician datos de entrenamiento.
Las secuencias de identidad pueden avanzar aun después de rollback.

También existen scripts separados: `pruebas.ps1`, `pruebas-oracle.ps1`,
`pruebas-login.ps1`, `pruebas-caja.ps1`, `pruebas-apertura-cero.ps1`,
`pruebas-negocio.ps1`, `pruebas-administracion.ps1` y `pruebas-fotos.ps1`.
Este último añade 12 casos de la función opcional, separados de las 100 pruebas rápidas.

La regresión completa pasó **219 casos** con Java 21, Oracle XE 21.3 y ojdbc11
21.11: 37 dominio, 9 integridad Oracle, 19 login/usuarios, 29 apertura/cobro/cierre,
24 apertura cero, 35 negocio integral, 28 administración, 12 fotografías y 26 gráficos con Swing
real en Linux/Xvfb. Incluye los nueve casos obligatorios de la fase 10, concurrencia
con conexiones reales y la fase 11. Compilación sin advertencias con `-Xlint:all -Werror`.

El estudiante confirmó en Windows las 100 pruebas rápidas de dominio, negocio
y administración. El nuevo diseño y las fotografías deben comprobarse allí con
la guía y capturas de su práctica.
Las evidencias del repositorio son de pruebas automatizadas en POS_TEST/Linux;
la impresión física no fue comprobada.

## Compilar y probar sin Oracle

Windows:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\pruebas.ps1
```

Linux con JDK 21 activo:

```sh
mkdir -p out
find src test -name '*.java' > out/fuentes.txt
javac -encoding UTF-8 --release 21 -Xlint:all -Werror -d out @out/fuentes.txt
java -cp out com.supermercado.pos.pruebas.Pruebas
```

Ejecuta Java solo si compila correctamente. Para pruebas JDBC agrega
`lib/ojdbc11.jar` al classpath, separado con `;` en Windows o `:` en Linux.
Las suites de escrituras requieren configurar POS_TEST y comprueban la cuenta
y contenedor reales antes de tocar filas. No utilizan un framework de pruebas.

## Estructura

La estructura principal conserva el Paso 4 del documento (página 4). `pos/`
representa la raíz del proyecto: en tu equipo es `SupermarketApp-Git`.
Las carpetas adicionales separan documentación, pruebas y herramientas de apoyo.
Consulta [qué exige el documento y cómo ejecutar sin archivos PS1](docs/estructura-y-ejecucion.md).

```text
src/com/supermercado/pos/
  Main.java    Punto de entrada de la aplicación
  dominio/     Usuarios, roles, carrito, caja/turno, ventas, pagos, retiros y reversiones
  dao/         PreparedStatement, consultas y escrituras JDBC
  servicio/    Reglas, jerarquía y transacciones
  ui/          Las ventanas Swing y trabajo de BD fuera del hilo de eventos
  excepcion/   ReglaNegocioException y AutorizacionException
  util/        Dinero, PBKDF2, conexión, configuración y sesión
sql/           Cuentas, 15 tablas, semillas, verificación y migración 04
test/          Ejecutores propios de pruebas y pruebas de ventanas reales
scripts/       Compilación, driver, ejecución, migración y verificación para Windows
docs/          Guías, ER, lista de entrega y evidencias
lib/           Driver descargado localmente, excluido de Git
config.properties  URL, cuenta técnica y mínimo operativo; sin contraseña
datos/fotos/   Copias locales de fotos, excluidas de Git; incluir en respaldos
```

El conteo por denominaciones y otros retos opcionales no se implementaron.
La excepción opcional de apertura cero sí está disponible. Los comprobantes son
para la práctica del curso: no procesan tarjetas bancarias ni facturación fiscal.
