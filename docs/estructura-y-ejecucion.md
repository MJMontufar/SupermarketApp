# Estructura del proyecto y ejecución sin PS1

El Paso 4 del material (página 4) presenta esta estructura. La aplicación
conserva las mismas rutas y capas:

```text
pos/
├── lib/ojdbc11.jar
├── config.properties
├── sql/
│   ├── 01_esquema.sql
│   └── 02_datos_iniciales.sql
└── src/com/supermercado/pos/
    ├── Main.java
    ├── dominio/
    ├── dao/
    ├── servicio/
    ├── ui/
    ├── util/
    └── excepcion/
```

`pos/` representa la carpeta raíz; no forma parte del paquete Java. La raíz
puede llamarse `SupermarketApp-Git` sin cambiar `com.supermercado.pos` ni los
imports. No es necesario crear otra carpeta `pos` dentro del clon ni mover
las clases. Si se entrega una copia con nombre `pos`, su contenido mantiene
estas rutas relativas. El driver se descarga con el script indicado en README;
está excluido de Git y no viene en un clon nuevo.

## Carpetas adicionales

| Ruta | Función |
|---|---|
| `test/com/supermercado/pos/pruebas/` | Ejecutores Java con `main` y comprobaciones propias. |
| `scripts/` | Atajos de PowerShell para compilar, descargar el driver, arrancar y probar. |
| `docs/` | Modelo ER, guías y evidencias de las pantallas y el escenario. |
| `.vscode/` | Configuración del editor para reconocer `src`, `test` y el driver. |
| `out/`, `bin-vscode/` | Clases compiladas locales; excluidas de Git. |
| `datos/fotos/` | Fotografías locales opcionales; excluidas de Git y necesarias en su respaldo. |

La separación permite compilar solo `src` para arrancar la aplicación, y añadir
`test` cuando se quieran ejecutar pruebas. Las clases de la aplicación no
dependen de las clases de pruebas. Los scripts no añaden bibliotecas ni cambian
la organización exigida por el documento.

## Qué pruebas exige el documento

La Fase 10 (páginas 22 y 23) exige un ejecutor propio en Java, sin JUnit ni
Mockito, y al menos nueve comprobaciones: caja ocupada, fondo cero sin
autorización, pago insuficiente, cambio en pago mixto, retiro bajo mínimo,
autorización de cajero, autoautorización, devolución excesiva y cierre con
faltante. Exige además un esquema separado para las pruebas que escriben en
la base. La rúbrica de la página 24 asigna un **15 %** a las pruebas.

En este proyecto `Pruebas.java` reúne las comprobaciones de dominio sin Oracle;
los nueve casos obligatorios son los primeros nueve de `PruebasNegocio.java`.
Esta última también reproduce el escenario de la Fase 11. Las demás suites
amplían la cobertura de usuarios, permisos, transacciones, ventanas y fotos.
Todas usan ejecutores propios y las escrituras se restringen a POS_TEST/XEPDB1.

**El documento exige las pruebas Java, pero no exige archivos `.ps1`.**
Los `.ps1` de pruebas pueden omitirse en una copia de ejecución si se conserva
otra forma de lanzar los ejecutores. Eliminar solo esos atajos no elimina las
pruebas Java; eliminar `test/` sí retira su código. Conviene conservar ambos
en el repositorio académico para que el docente pueda reproducir los casos.
Las instrucciones actuales de README usan esos atajos.

## Compilar y arrancar en Windows sin PS1

Requisitos: Java y javac 21 en PATH, Oracle preparado, driver en `lib/ojdbc11.jar`
y configuración válida. Si ya utilizas la aplicación, no repitas la creación
de usuarios ni tablas. En una terminal **PowerShell**, situada en la raíz:

```powershell
Set-Location "$env:USERPROFILE\Proyectos\SupermarketApp-Git"
java -version
javac -version
New-Item -ItemType Directory -Path .\out -Force | Out-Null
$fuentesPos = @(Get-ChildItem -LiteralPath .\src -Recurse -Filter '*.java' |
    ForEach-Object { $_.FullName })
javac -encoding UTF-8 --release 21 -Xlint:all -d .\out @fuentesPos
if ($LASTEXITCODE -eq 0) {
    java -cp ".\out;.\lib\ojdbc11.jar" com.supermercado.pos.Main
}
```

La contraseña técnica de Oracle se solicita sin mostrarla. Después se abre
el login de la aplicación. No se escribe la contraseña en el comando ni en
el repositorio. No se debe ejecutar Java si falló la compilación.
La compilación usa las interfaces JDBC del JDK (`java.sql`); el driver se
incluye en el classpath al ejecutar.

## Ejecutar las pruebas sin PS1

Desde la misma raíz, compila `src` y `test` juntos:

```powershell
$fuentesPruebasPos = @(Get-ChildItem -LiteralPath .\src,.\test -Recurse -Filter '*.java' |
    ForEach-Object { $_.FullName })
New-Item -ItemType Directory -Path .\out -Force | Out-Null
javac -encoding UTF-8 --release 21 -Xlint:all -d .\out @fuentesPruebasPos
if ($LASTEXITCODE -eq 0) {
    java -cp .\out com.supermercado.pos.pruebas.Pruebas
}
```

El resultado esperado es **37 correctas, 0 fallos**, sin conexión a Oracle.
Solo después de compilar correctamente y con POS_TEST preparado, ejecuta
la suite de las reglas obligatorias y el escenario:

```powershell
java '-Dpos.db.usuario=pos_test' -cp ".\out;.\lib\ojdbc11.jar" com.supermercado.pos.pruebas.PruebasNegocio
```

Solicita la clave técnica de **POS_TEST**. Debe terminar con **35 correctas,
0 fallos** y guardar `out/escenario-completo.txt`. Para las 28 comprobaciones
de administración, ejecuta:

```powershell
java '-Dpos.db.usuario=pos_test' -cp ".\out;.\lib\ojdbc11.jar" com.supermercado.pos.pruebas.PruebasAdministracion
```

## Mensajes SQL y contraseñas

`PROMPT` es una instrucción de SQL*Plus para mostrar texto; no crea tablas ni
guarda datos. Los mensajes informativos `PROMPT` se han retirado de los scripts.
Se conservan `PASSWORD pos` y `PASSWORD pos_test` en la preparación inicial:
establecen las claves sin escribirlas en los archivos. También se conservan
las validaciones y errores que impiden ejecutar SQL sobre el esquema equivocado.

Los scripts `00_crear_usuarios.sql`, `03_verificar.sql` y la migración `04`
complementan los dos SQL del esquema mostrado en el documento. No requieren
cambiar las carpetas ni volver a crear la base existente.
