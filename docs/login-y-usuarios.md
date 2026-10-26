# Tercera etapa: login y usuarios

Requisitos: POS preparado en XEPDB1, JDK 21, driver JDBC descargado y la prueba
`conexion.ps1` terminada correctamente. Esta etapa modifica Java; reutiliza las
tablas, contraseñas de Oracle y datos existentes.

## Actualizar una copia descargada como ZIP

1. Conserva una copia de tu carpeta actual y de cualquier cambio propio.
2. Descarga el ZIP de la rama actualizada en GitHub.
3. Abre la carpeta que viene dentro del ZIP: debe contener `src`, `scripts`,
   `test`, `sql`, `docs` y `README.md`.
4. Copia su contenido dentro de la raíz de tu proyecto actual. Actualiza los
   archivos del proyecto, conservando `lib/ojdbc11.jar` y cualquier configuración
   local. No coloques otra carpeta de proyecto dentro de la existente.
5. Abre esa raíz en VS Code. Sigue usando la extensión Java de Red Hat.

Si usas Git, puedes actualizar tu checkout con la rama correspondiente,
conservando primero tus cambios locales.

## Arrancar en Windows

En el explorador de VS Code, clic derecho sobre `scripts` y selecciona
**Open in Integrated Terminal / Abrir en terminal integrada**. Ejecuta:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\iniciar.ps1
```

Cuando pregunte `Clave de Oracle para pos`, introduce la contraseña de **pos**.
Es la cuenta técnica para abrir conexiones JDBC. Se conserva únicamente en
memoria durante este proceso y se limpia al salir; no se escribe en archivos.

Después se abre una ventana de Swing. Ingresa:

```text
Usuario: admin
Contraseña: Admin123!
```

Es la cuenta de entrenamiento del PDF, almacenada como hash PBKDF2 en la tabla
`usuario`. No es una cuenta de Oracle. El botón Enter también permite ingresar.
Una clave incorrecta o una cuenta inactiva muestran el mismo rechazo genérico.

## Crear los usuarios del escenario

Con admin, pulsa **Administrar usuarios → Nuevo usuario**. Crea uno por uno:

| Usuario | Nombre sugerido | Rol inicial |
|---|---|---|
| `ana` | Ana | CAJERO |
| `luis` | Luis | CAJERO |
| `marta` | Marta | SUPERVISOR |

Elige contraseñas de al menos ocho caracteres y consérvalas de forma privada.
El formulario muestra solo roles de nivel inferior al actor: admin tiene nivel
4 y puede asignar CAJERO (1), SUPERVISOR (2) y GERENTE (3).

El nombre de usuario es único y no admite espacios. Un usuario con historial
se conserva: para impedir nuevos ingresos, edítalo y desmarca **Usuario activo**.
La tabla permite buscar por usuario o nombre y ordenar pulsando las columnas.

Al editar, una contraseña vacía conserva la existente. Los roles se muestran
sin modificarse en este formulario. La asignación múltiple de roles y permisos
extra se añadirá en la etapa de roles/permisos.

**Cerrar sesión** cierra las ventanas de administración y permite otro login.
Un cajero puede ingresar, pero el botón de administración queda deshabilitado.
Los turnos y el punto de venta se incorporarán en la siguiente etapa.

## Qué hace cada capa

```text
LoginFrame → AuthService → UsuarioDAO → Oracle
UsuariosFrame / UsuarioDialog → UsuarioService → UsuarioDAO / RolDAO → Oracle
```

- La ventana recoge datos y muestra resultados; no contiene SQL.
- `AuthService` comprueba cuenta activa y contraseña con `Seguridad.verificar`.
- Los DAO reciben una `Connection` y usan `PreparedStatement` y `ResultSet`.
- `UsuarioService` controla las transacciones y aplica permisos y jerarquía.
- `Sesion` conserva el usuario autenticado, sin su contraseña original.

Las consultas y PBKDF2 se ejecutan con `SwingWorker` para que la ventana siga
respondiendo. Su resultado se presenta en el hilo de eventos de Swing.
Los `char[]` de las contraseñas de usuarios se limpian al terminar.

Crear un usuario y su rol inicial es una sola transacción. La edición de perfil
conserva sus roles. El servicio impide desactivar la propia cuenta, asignar un
rol igual o superior y editar otros usuarios de nivel igual o superior.

Los permisos se cargan en el login. Un cambio posterior aplica al próximo
ingreso, como permite el enunciado. La autorización de un supervisor para una
reversión será otra operación y no cambiará la sesión del cajero.

## Pruebas de servicios

En POS_TEST deben estar cargados el esquema y los datos de la guía de Oracle.
Desde la carpeta `scripts`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\pruebas-login.ps1
```

Solicita la clave técnica de **pos_test**. Esperado:

```text
Pruebas login: 19; correctas: 19; fallos: 0
```

La suite crea usuarios temporales mediante los servicios reales y elimina solo
sus propias filas al terminar. No borra usuarios del desarrollo ni reinicia el
esquema. Rechaza ejecutarse fuera de POS_TEST/XEPDB1. Verifica, entre otros,
credenciales, cuenta inactiva, consultas parametrizadas, contraseñas mínimas,
duplicados, roles inferiores, conservación de roles y límites de administración.

Las cuatro pruebas de `PruebasInterfaz` se ejecutaron con una pantalla Xvfb de
Linux y Oracle real, usando solo APIs estándar de Swing/AWT. Esa automatización
requiere una clave técnica configurada en el entorno del proceso y POS_TEST.
No es necesaria para abrir manualmente la aplicación en Windows.

Estas pruebas complementan los casos de [negocio integral](ciclo-completo.md) y
[administración](administracion.md), disponibles en las entregas posteriores.
