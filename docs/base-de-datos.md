# Segunda etapa: Oracle XE 21c y JDBC

Usa Java 21, Oracle XE 21c y el proyecto completo abierto en VS Code. Conserva
habilitada la extensión de Java de Red Hat. Los comandos PowerShell siguientes
se ejecutan desde la carpeta que contiene `src`, `scripts`, `sql` y `README.md`.

## Cuentas y contraseñas distintas

| Cuenta | Uso | Contraseña |
|---|---|---|
| `SYSTEM` | Administrar Oracle y crear los esquemas | La elegida al instalar Oracle |
| `pos` | Conectar la aplicación a Oracle | La definirás al ejecutar el script 00 |
| `pos_test` | Pruebas sobre tablas separadas | La definirás al ejecutar el script 00 |
| `admin` | Futuro login del supermercado | `Admin123!`, dato público de entrenamiento del PDF |

`admin` es una fila de nuestra tabla `usuario`; no sirve para conectarse con
SQL*Plus. Tampoco se usa la contraseña de `SYSTEM` para entrar como `pos`.
No escribas contraseñas reales en archivos, comandos, capturas ni en GitHub.

## 1. Crear las cuentas de Oracle una sola vez

Abre una terminal en la raíz del proyecto y ejecuta:

```powershell
sqlplus -L system@//localhost:1521/XEPDB1
```

Introduce la contraseña de SYSTEM cuando la solicite. Dentro de `SQL>` ejecuta:

```sql
@"sql/00_crear_usuarios.sql"
```

El script crea las cuentas `pos` y `pos_test` y pide definir sus contraseñas,
dos veces cada una, sin mostrarlas. Guarda ambas de forma privada. Para evitar
el problema de caracteres del instalador, puedes usar claves distintas de
12 a 20 caracteres con mayúsculas, minúsculas y números.

El script rechaza ejecutarse en otro contenedor y conserva cuentas que ya
existan. No vuelvas a crear las cuentas ni borres usuarios para repetir pasos.
Si se interrumpió justo al definir claves, conectado como SYSTEM puedes usar
`PASSWORD pos` y `PASSWORD pos_test` para definirlas interactivamente.

Sal de SQL*Plus con:

```sql
EXIT;
```

## 2. Crear las tablas y cargar datos en POS

En PowerShell:

```powershell
sqlplus -L pos@//localhost:1521/XEPDB1
```

Introduce la clave de **pos**, no la de SYSTEM. Dentro de `SQL>`:

```sql
@"sql/01_esquema.sql"
@"sql/02_datos_iniciales.sql"
@"sql/03_verificar.sql"
```

La verificación debe mostrar usuario `POS`, base `XEPDB1`, 15 tablas, 9 permisos,
4 roles, 3 cajas, 5 productos, `admin` activo y los dos índices de turno abierto.

El esquema 01 se ejecuta una sola vez sobre una cuenta vacía. Si hay un error,
detente y conserva el mensaje: Oracle confirma DDL automáticamente; repetirlo
no deshace una creación parcial. Los scripts no contienen instrucciones DROP.

Los datos 02 pueden repetirse: agregan filas faltantes sin reiniciar precios,
stock ni contraseñas. También agregan las asociaciones iniciales de roles y
permisos. Es un script de preparación; no se ejecuta al abrir la aplicación.

Sal con `EXIT;`.

## 3. Preparar el esquema separado de pruebas

En PowerShell:

```powershell
sqlplus -L pos_test@//localhost:1521/XEPDB1
```

Introduce la clave de **pos_test** y ejecuta dentro de `SQL>`:

```sql
@"sql/01_esquema.sql"
@"sql/02_datos_iniciales.sql"
@"sql/03_verificar.sql"
EXIT;
```

Ahora POS y POS_TEST tienen tablas independientes dentro de la misma XEPDB1.

## 4. Descargar el driver de Oracle

En PowerShell, desde la raíz:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\descargar-driver.ps1
```

Se descarga `ojdbc11.jar` desde la publicación de Oracle en Maven Central y se
verifica su SHA256. No se instala ni utiliza Maven. El binario queda en `lib`
y no se guarda en Git. Un archivo existente con otro checksum se conserva.

## 5. Probar Java contra Oracle

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\conexion.ps1
```

Escribe la clave de **pos** cuando la solicite. Debe mostrar:

```text
Usuario: POS; base: XEPDB1
Productos activos: 5
Conexion JDBC y consulta de productos: OK
```

La clave se ingresa oculta y no se guarda. Para automatizaciones locales ya
configuradas se admite `POS_DB_PASSWORD`; no es necesario definirla para estos
pasos interactivos. Nunca pegues una clave en la línea de comandos.

`config.properties` contiene la URL JDBC, el usuario y el mínimo operativo.
Si necesitas otro puerto o usuario, usa `config.local.properties`, ignorado por
Git, con las claves no secretas correspondientes. La contraseña no se lee de
esos archivos. Los scripts encuentran la configuración aunque la ruta tenga
espacios.

## 6. Pruebas de la base

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\pruebas-oracle.ps1
```

Introduce la clave de **pos_test**. La suite rechaza conectarse con otra cuenta
antes de modificar datos. Esperado:

```text
Pruebas Oracle: 9; correctas: 9; fallos: 0
```

Comprueba datos y hash de admin, exclusividad de turno por caja y cajero,
reapertura después del cierre, fondo positivo, stock atómico, rollback de venta,
detalle y pago, cambio solo en efectivo y límites de cantidad reversada.
Las filas temporales se revierten en cada caso; los IDs de identidad pueden
avanzar, lo cual es normal. Ejecutar esta suite no sustituye las pruebas futuras
de servicios ni el escenario completo de la fase 11.

La suite espera el conjunto inicial de POS_TEST; reserva ese esquema para
pruebas y usa POS para desarrollar y demostrar la aplicación.

## Entender el flujo

`Configuracion` lee valores no secretos. `Conexion` usa `DriverManager` para
abrir una conexión. `PruebaConexion` ejecuta una consulta parametrizada con
`PreparedStatement` y recorre el resultado con `ResultSet`. `try-with-resources`
cierra conexión, sentencia y resultado al terminar.

Las siguientes clases DAO usarán este mismo patrón y recibirán una `Connection`.
El servicio controlará `commit` y `rollback`, para que toda una operación de
negocio se confirme o se deshaga junta.
