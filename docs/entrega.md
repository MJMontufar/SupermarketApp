# Lista de trabajo para completar la entrega

La aplicación incluye el ciclo de caja y las pantallas del mapa del PDF. Falta
que ejecutes esta lista en tu Windows, guardes tus capturas y revises la entrega
con las instrucciones concretas de tu docente. Las evidencias automáticas del
repositorio son de POS_TEST en Linux; no sustituyen tu comprobación local.

## 1. Actualizar una sola vez

Cierra Java, incorpora el pull request a `main` en GitHub y en PowerShell:

```powershell
Set-Location "$env:USERPROFILE\Proyectos\SupermarketApp-Git"
git status
```

Si muestra `working tree clean`:

```powershell
git pull --ff-only origin main
```

Conserva cualquier modificación propia antes de actualizar. No uses reset ni
clean para eliminarla. El driver local y `config.local.properties` no se versionan.

Si ya aplicaste la actualización 04 de apertura cero, **no necesitas ejecutar
más SQL para esta entrega**. Si no la aplicaste:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\actualizar-bd.ps1
```

Usa la clave técnica de Oracle para **pos**. No repitas 00/01 sobre tablas existentes.

## 2. Arrancar y revisar los nuevos botones

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\iniciar.ps1
```

La consola pide la clave de Oracle **pos**. Después, el login pide un usuario de
la aplicación. Con admin aparecen usuarios, roles, asignaciones y catálogos.
Con Ana, entra a su turno y comprueba los botones **Corte intermedio** y
**Reversión**. No registres ventas al azar en el turno que usarás para el guion.

## 3. Ejecutar el escenario de caja

Sigue [el guion completo de 13 pasos](ciclo-completo.md):

- Ana abre Caja 1 con Q1000.
- Cobra Q150 efectivo (recibe Q200), Q80 tarjeta y Q120 mixto (Q70 efectivo/Q50 tarjeta).
- Luis comprueba Caja 1 ocupada y abre Caja 2 con Q800.
- Ana entrega Q500 a Marta mediante corte intermedio.
- Devuelve una unidad de Q30 de la primera venta: primero intenta autorizar
  con Luis (rechazo), después con Marta (aceptación).
- Cierra Ana con Q680 contados: **Q690 esperados, diferencia −Q10**.
- Cierra Luis con Q800 si no hizo ventas.

Anota el folio de la primera venta. Guarda la captura del reporte final y del
rechazo de Luis. Si hiciste otras ventas en ese turno, los resultados cambian:
comienza otro turno para repetir exactamente el guion.

## 4. Probar administración

Sigue [la práctica de administración](administracion.md) con `prueba_caja`:
crea un rol de nivel 1, asígnalo junto con CAJERO, agrega CATALOGO_ADMIN como
extra y vuelve a iniciar sesión. Prueba productos y cajas, luego retira ese extra
para comprobar que se aplica al siguiente login. Conserva Ana, Luis y Marta con
sus roles originales para la demostración del curso.

## 5. Ejecutar comprobación automática local

Solo si **POS_TEST ya está preparado** con tablas y datos iniciales. Aplica una
vez su actualización si aún no lo hiciste:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\actualizar-bd.ps1 -Pruebas
```

Después puedes ejecutar tres suites en una sola llamada:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verificar-entrega.ps1
```

La primera no usa Oracle; las siguientes dos piden la clave de **pos_test**.
Se esperan **37 de dominio, 35 de negocio y 28 de administración: 100 correctas**.
El script se detiene si alguna falla. Las nueve pruebas obligatorias de la fase
10 son los primeros nueve casos de la suite de negocio. El resultado automatizado
de la fase 11 queda en `out/escenario-completo.txt`.

En cloud se ejecutó además la regresión completa: **204 pruebas**, incluidas
las suites previas y 23 pruebas de ventanas con Oracle real. Ninguna suite de
escrituras usa POS; todas comprueban POS_TEST/XEPDB1 antes de preparar datos.

## 6. Guardar las capturas requeridas

Crea una carpeta de evidencias de tu práctica y captura:

1. Login.
2. Apertura de turno.
3. Punto de venta con carrito.
4. Pago.
5. Corte intermedio.
6. Solicitud de reversión.
7. Autorización de supervisor.
8. Cierre de turno.
9. Lista y alta de usuarios.
10. Roles y matriz de permisos.
11. Asignación de roles/permisos.
12. Catálogo de productos **y** cajas (dos ventanas del mismo apartado).

Agrega el reporte del escenario Q690/Q680/−Q10. Puedes consultar la
[referencia de evidencias automáticas](evidencias/README.md), que identifica
cada pantalla y su resultado.

## 7. Revisar el material para entregar

- Repositorio con los commits por fases incorporados a main.
- README con preparación, ejecución y cuenta de entrenamiento admin/Admin123!.
- Usuarios Ana, Luis y Marta creados con sus roles y contraseñas de práctica.
  No subas la contraseña técnica de Oracle a Git.
- [Modelo entidad-relación](modelo-er.md), scripts SQL y driver descargable
  mediante el script con checksum.
- Tus capturas y el resultado del escenario integral.
- Capacidad de explicar el recorrido: ventana captura → servicio valida y
  coordina transacción → DAO ejecuta JDBC → commit o rollback.

No se requiere publicar una web: es una aplicación de escritorio Swing.
La impresión física depende de una impresora configurada; se verificaron las
vistas previas, no una impresora real. El conteo por denominaciones y otros
retos opcionales del PDF quedan fuera; sí está implementada la excepción
opcional de apertura cero autorizada.
