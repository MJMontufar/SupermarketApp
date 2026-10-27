# Capturas de Windows y respaldo para la presentacion

Las capturas en `docs/evidencias/final` son referencias de pruebas en Linux.
Guarda las capturas de tu equipo en `docs/evidencias/windows`: muestran tu
practica real. Si ya guardaste alguna, reutilizala cuando corresponda a la
version y al escenario que presentaras. No hace falta repetirla por su nombre.

## Preparar la carpeta de capturas

En PowerShell, desde el clon:

```powershell
Set-Location "$env:USERPROFILE\Proyectos\SupermarketApp-Git"
New-Item -ItemType Directory -Path .\docs\evidencias\windows -Force | Out-Null
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\iniciar.ps1
```

Usa **Windows + Mayus + S**, selecciona la ventana y abre la notificacion de
Recortes para guardar como PNG. Si tu version copia solamente al portapapeles,
pega en Paint y guarda con Ctrl+S. La captura debe mostrar la ventana completa,
incluidos titulo, datos y botones. Las claves deben permanecer ocultas.

| Archivo sugerido | Momento de la captura |
|---|---|
| `01-login.png` | Ventana inicial, sin mostrar claves. |
| `02-apertura.png` | Cajero, caja y fondo antes de confirmar. |
| `03-carrito.png` | Productos, cantidades y total antes de cobrar. |
| `04-pago.png` | Pago mixto con sus importes y cambio. |
| `05-corte-intermedio.png` | Resumen y retiro solicitado. |
| `06-reversion.png` | Venta encontrada, producto, cantidad y motivo. |
| `07-autorizacion.png` | Autorizacion del supervisor, clave oculta. |
| `08-cierre.png` | Esperado Q690 y contado Q680, antes de confirmar. |
| `09-usuarios.png` | Lista de usuarios con sus estados. |
| `09b-alta-usuario.png` | Formulario de alta, clave oculta. |
| `10-roles.png` | Rol seleccionado y matriz de permisos. |
| `11-asignacion.png` | Roles, permisos heredados y extras. |
| `12-productos.png` | Catalogo; puede incluir una foto seleccionada. |
| `12b-cajas.png` | Catalogo de cajas. |
| `13-resultado-escenario.png` | Reporte final: esperado Q690, contado Q680, diferencia -Q10 y CERRADO. |
| `14-autorizacion-rechazada.png` | Rechazo de un cajero como autorizador. |

Los dos catalogos pertenecen al apartado 12 del PDF. El alta de usuario y los
dos resultados finales complementan las doce pantallas del mapa.

Para capturar apertura, cobros, retiro, autorizacion y cierre en una misma
practica, sigue [el guion completo](ciclo-completo.md). Anota el folio de la
primera venta y captura las ventanas antes de confirmar o cerrar. Si repites
el escenario, empieza con turnos nuevos y verifica precios y stock; no agregues
ventas de prueba a ese turno ni vuelvas a ejecutar el esquema SQL.

## Respaldar los archivos del proyecto

Termina de guardar las capturas y cierra la aplicacion. Ejecuta:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\respaldar.ps1
```

Se crea una carpeta unica dentro de **`C:\Users\javie\RespaldosPOS`** (o en
`RespaldosPOS` del usuario de Windows que lo ejecute). Incluye una copia de
los archivos actuales y `proyecto.zip`, con codigo, pruebas, scripts, SQL,
documentacion, capturas guardadas en docs, driver y fotos locales existentes.
Tambien incluye configuracion local e historial Git cuando estan disponibles.
Los cambios aun sin commit se conservan en la copia de archivos.

El ZIP se abre para comprobar que contiene `Main.java`; su SHA-256 se guarda
en `SHA256-proyecto.txt`. Se conserva tambien la copia sin comprimir. No se
copia `out`, `bin-vscode`, la instalacion de Java ni la instalacion de Oracle.
La aplicacion no se modifica y el script no ejecuta operaciones en la base.

Para elegir otro destino externo al proyecto:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\respaldar.ps1 -Destino "G:\RespaldosPOS"
```

Elige una ruta corta con espacio disponible. Guarda una segunda copia completa
en otra unidad o almacenamiento: GitHub conserva el codigo subido, pero no
las fotografias ignoradas ni los datos de Oracle.

## Respaldar la base Oracle POS

**El ZIP del proyecto no contiene la base.** Los SQL crean tablas y datos
iniciales, pero no guardan tus usuarios, ventas, turnos ni existencias actuales.
Para conservarlos, usa la herramienta Data Pump incluida con Oracle XE.
Mantén la aplicacion cerrada durante la exportacion.

En PowerShell comprueba que existe la herramienta:

```powershell
Get-Command expdp
```

Si no aparece, busca `expdp.exe` en la carpeta `bin` de tu instalacion Oracle,
junto a `sqlplus.exe`. No cambies de version ni reinstales Oracle para esto.

Ejecuta la exportacion, con nombre nuevo en cada intento:

```powershell
$marcaPos = Get-Date -Format 'yyyyMMdd_HHmmss'
expdp system@localhost:1521/XEPDB1 schemas=POS directory=DATA_PUMP_DIR "dumpfile=POS_$marcaPos.dmp" "logfile=POS_$marcaPos.log"
```

La clave que solicita es la de **SYSTEM de Oracle**, no la de POS ni la del
admin de la aplicacion. No la escribas dentro del comando. El esquema exportado
es POS; POS_TEST solo contiene pruebas y no es necesario para conservar la practica.

Espera el mensaje de trabajo terminado correctamente y revisa el log. Un
archivo creado por un trabajo que termino con errores no confirma un respaldo
completo. Si aparece un error, conserva el mensaje y el log para revisarlos;
no borres ni recrees el esquema para intentar corregirlo.

Los archivos se escriben en el directorio de Oracle, no en el proyecto. Para
ver su ubicacion, abre otra sesion desde PowerShell:

```powershell
sqlplus -L system@localhost:1521/XEPDB1
```

Dentro de SQL*Plus (`SQL>`), ejecuta:

```sql
SET LINESIZE 250
COLUMN directory_path FORMAT A180
SELECT directory_path FROM dba_directories
WHERE directory_name = 'DATA_PUMP_DIR';
EXIT
```

Abre esa ruta en el Explorador y **copia** el `.dmp` y el `.log` que tienen
la misma marca de tiempo hacia `base-datos` de la carpeta de respaldo creada
antes. Conserva los originales. Si Windows solicita permisos de administrador
para acceder al directorio de Oracle, usa tu cuenta administradora.

Al terminar tendras:

```text
RespaldosPOS/SupermarketApp-fecha-hora/
  proyecto/                  copia de archivos, fotos y capturas
  proyecto.zip               la misma copia comprimida
  SHA256-proyecto.txt
  LEEME-RESPALDO.txt
  base-datos/
    POS_fecha_hora.dmp        exportacion real de POS
    POS_fecha_hora.log        registro de exportacion
```

La exportacion y las fotos corresponden a la misma base y sus IDs de productos.
Conserva juntos el dump y `datos/fotos`; una base nueva con otros IDs no garantiza
que las fotos se asocien a los mismos productos. La restauracion con `impdp`
se debe preparar en otro esquema o equipo; no la ejecutes sobre tu instalacion
funcional como ensayo antes de presentar. Este procedimiento verifica la copia
de archivos; el respaldo de la base queda confirmado tras exportar correctamente
y una restauracion solo quedara validada cuando se pruebe por separado.

## Lo que presentaras al catedratico

Conserva el enlace del repositorio en main, README, modelo ER, SQL, tus capturas
y el reporte del escenario. El respaldo privado puede incluir configuracion
local y datos de usuarios; no lo subas completo a GitHub ni compartas claves
tecnicas. Las fotografias son opcionales y deben copiarse aparte si entregas
solamente un clon o ZIP de GitHub.
