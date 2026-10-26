# Apertura con fondo cero autorizada

Implementa el reto opcional de la RN-03 del PDF, incorporado al alcance del
proyecto. El fondo positivo sigue abriendo normalmente; Q0 requiere una
excepción autorizada. Un monto negativo nunca está permitido.

## Actualizar en Windows

1. Cierra la aplicación Java e incorpora el pull request de esta entrega en
   GitHub. No necesitas detener Oracle.
2. En PowerShell, desde la raíz de tu clon:

   ```powershell
   Set-Location "$env:USERPROFILE\Proyectos\SupermarketApp-Git"
   git status
   ```

   Si muestra `working tree clean`, continúa:

   ```powershell
   git pull --ff-only origin main
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\actualizar-bd.ps1
   ```

3. Cuando solicite la contraseña de Oracle para **pos**, escribe la que ya
   configuraste. El resultado debe indicar **Actualizacion lista para pos**.
   El script usa JDBC y calcula las rutas desde su propia carpeta: no es
   necesario pegar rutas largas en SQL*Plus. Conserva usuarios, ventas,
   existencias y turnos. Se puede repetir sin reiniciar datos.
4. Arranca la aplicación:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\iniciar.ps1
   ```

**No repitas los SQL 00 y 01.** Para una instalación nueva, el esquema 01 ya
incluye las nuevas restricciones. La migración 04 actualiza instalaciones
anteriores sin crear otras tablas ni borrar registros.

La actualización agrega `turno.apertura_autorizada_por` y su referencia a
`usuario`. Añade las nuevas restricciones antes de retirar únicamente el
CHECK anterior exacto `fondo_inicial > 0`. Verifica cuenta y contenedor y se
niega a modificar restricciones inesperadas. Como cualquier DDL de Oracle,
confirma automáticamente: ejecútala sin una transacción de trabajo pendiente.
Si falla, conserva el mensaje y revisa antes de continuar; no borres tablas.

## Probar con Ana y Marta

Usa una caja libre y un cajero sin turno abierto. Si Ana aún tiene un turno de
la práctica anterior, ciérralo con su conteo correspondiente antes de probar.

1. Ingresa como **ana** con su contraseña de la aplicación.
2. Pulsa **Abrir / continuar turno de caja**, selecciona una caja libre y
   escribe fondo inicial **`0.00`**.
3. Al pulsar **Abrir turno**, se muestra el diálogo de autorización con la
   caja, el fondo Q0 y el cajero solicitante.
4. Para autorizar, usa **marta** y la contraseña que elegiste para su cuenta
   SUPERVISOR. No uses la contraseña técnica de Oracle.
5. Si la autorización es correcta, abre el punto de venta con fondo **Q0.00**
   y **Ana sigue siendo la cajera**. Marta queda identificada como autorizadora
   en el turno y en su movimiento inicial.
6. Para terminar una prueba sin ventas, pulsa **Cerrar turno** y declara
   **`0.00`**. El esperado y la diferencia deben ser Q0.00.

También puedes comprobar:

| Intento | Resultado |
|---|---|
| Cancelar la autorización | No abre turno; vuelve a la apertura |
| Contraseña incorrecta | Rechazo y un intento menos |
| Usar las credenciales de Luis, CAJERO | Rechazo aunque la clave sea correcta |
| Ana intenta autorizar su propia apertura | Rechazo |
| Usuario supervisor inactivo | Rechazo |
| Tres fallos en el mismo diálogo | Deshabilita confirmar; permite cancelar |
| Fondo `-1.00` | Rechazo, sin excepción posible |
| Fondo positivo | Apertura normal, sin pedir supervisor |

La política exige otro usuario activo de nivel supervisor o superior, con
`TURNO_ABRIR` y nivel estrictamente mayor al del solicitante. Un permiso extra
no eleva el nivel del rol. Por tanto, un SUPERVISOR que abre su propia caja
necesita un GERENTE o ADMIN; no puede autorizarse. Ana, con nivel CAJERO,
puede ser autorizada por Marta, SUPERVISOR.

El límite es de **tres fallos por solicitud**, no un bloqueo permanente de la
cuenta del supervisor. Al terminar la solicitud, cancela para volver. Una
solicitud nueva vuelve a pedir credenciales; una autorización exitosa no puede
reutilizarse ni aplicarse a otra caja.

## Cómo se implementa

- `TurnoService.abrir` sigue rechazando cero sin autorización. La excepción
  tiene una entrada separada: `solicitarAperturaCero(...).confirmar(...)`.
- `AutorizacionService` consulta y bloquea el usuario autorizador, valida su
  hash PBKDF2, actividad, permiso y jerarquía, y ejecuta la apertura con la
  misma conexión y un único commit para turno y movimiento.
- No se acepta simplemente un objeto `Usuario` que diga ser supervisor: se
  requieren las credenciales en cada solicitud. No se modifica `Sesion`.
- Cada intento de credenciales se registra en `auditoria_autorizacion` con
  solicitante, username ingresado, resultado, contexto y fecha. El resultado
  exitoso significa **credenciales aceptadas**: si después la caja está
  ocupada, la apertura se revierte, pero ese intento queda auditado.
- La auditoría usa una conexión independiente para conservar los intentos
  aunque la operación de caja haga rollback. Si no puede auditarse, no abre.
- La contraseña se recoge en `JPasswordField`, se borra del campo al enviar
  y se limpia del `char[]` al terminar. No se guarda en sesión ni en auditoría.
- Oracle impide fondos negativos, Q0 sin autorizador, autorizador inexistente
  y autoautorización. La comprobación de credenciales y roles está en el servicio.

La autorización puntual también se utilizará al desarrollar retiros y
reversiones. Esas operaciones siguen pendientes; esta entrega no las habilita.

## Pruebas en POS_TEST

Solo si tu esquema POS_TEST ya tiene las tablas y datos iniciales:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\actualizar-bd.ps1 -Pruebas
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\pruebas-apertura-cero.ps1
```

En ambos casos ingresa la clave técnica de **pos_test**. El resultado esperado
es `Pruebas apertura cero: 24; correctas: 24; fallos: 0`. La suite comprueba
credenciales, jerarquía, límites, auditoría, rollback y restricciones reales;
crea y limpia únicamente sus usuarios, cajas, productos y movimientos temporales.

Otras tres pruebas de ventanas (`PruebasInterfazAperturaCero`) verifican
cancelación, tres rechazos y apertura válida manteniendo la sesión del cajero.
Se ejecutan con escritorio o Xvfb, POS_TEST y una clave de desarrollo ya
configurada en el entorno del proceso. Hay [capturas reales](evidencias/apertura-cero/README.md).
La migración fue probada tanto con JDBC como con SQL*Plus, repetida en POS y
POS_TEST, y rechazada correctamente en SYS/CDB$ROOT. Falta repetir esta nueva
interacción en el Windows del estudiante.
