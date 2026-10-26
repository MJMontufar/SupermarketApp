# Validación integral de caja: fases 6, 7, 10 y 11

Ya están disponibles retiro a bóveda, reversión autorizada, reingreso de stock,
comprobantes y cierre. Estas operaciones usan las tablas existentes. Si ya
aplicaste la actualización 04 para apertura cero, esta entrega no pide otro SQL.
No vuelvas a crear las tablas ni los usuarios de Oracle.

## Actualizar y arrancar en Windows

Cierra la aplicación Java. Incorpora el pull request de esta entrega a main y,
en PowerShell, ejecuta desde tu clon:

```powershell
Set-Location "$env:USERPROFILE\Proyectos\SupermarketApp-Git"
git status
```

Si muestra `working tree clean`, actualiza:

```powershell
git pull --ff-only origin main
```

Si aún no aplicaste la actualización de apertura cero, ejecuta una vez:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\actualizar-bd.ps1
```

Arranca:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\iniciar.ps1
```

La contraseña de la consola es la de Oracle **pos**. Las contraseñas de Ana,
Luis y Marta son las que tú definiste en la aplicación, distintas de la de Oracle.

## Tareas del escenario completo

Comienza con Ana y Luis sin turnos anteriores abiertos. Si están abiertos,
termina esas prácticas con su conteo correspondiente antes de empezar otro.
Conserva los precios iniciales: Arroz Q30 y Leche Q20. El stock debe alcanzar
para nueve Arroces y cuatro Leches.

1. Con Ana, abre Caja 1 con fondo **1000.00**.
2. Agrega **5 Arroces**, total Q150. Cobra en EFECTIVO, recibido **200.00**,
   cambio Q50. **Anota el folio de esta venta** y cierra el comprobante.
3. Agrega **4 Leches**, total Q80. Cobra con TARJETA: Q80 y cambio Q0.
4. Agrega **4 Arroces**, total Q120. Cobra MIXTO: tarjeta **50.00** y efectivo
   recibido **70.00**. El cambio es Q0. Cierra el comprobante.
5. Cierra la ventana del punto de venta con X, regresa al menú y cierra sesión.
   Eso conserva abierto el turno de Ana. Ingresa como Luis: Caja 1 debe estar
   ocupada y no aparecer disponible. Abre Caja 2 con fondo **800.00**.
6. Regresa al menú y cambia de sesión a Ana. **Abrir / continuar turno** debe
   recuperar su Caja 1. Pulsa **Corte intermedio**: debe mostrar Q1220 esperados.
7. Escribe retiro **500.00**, solicita autorización y usa Marta con su contraseña
   de la aplicación. El recibo debe identificar a Marta y mostrar **Q720 restantes**.
8. Vuelve al punto de venta y pulsa **Reversión**. Busca el folio de la primera
   venta, selecciona Arroz, cantidad **1** y escribe un motivo, por ejemplo
   `Cliente devuelve un producto`.
9. Solicita autorización. Prueba primero con **Luis**: debe rechazarse aunque
   la contraseña sea correcta. En el mismo diálogo, prueba con **Marta**: debe
   aceptar, conservar la sesión de Ana y mostrar reembolso efectivo **Q30**.
10. El comprobante debe indicar **Q690 restantes**, y la venta debe quedar
    `PARCIALMENTE_REVERSADA`. Se repone una unidad al inventario de Arroz.
11. Vuelve al punto de venta, pulsa **Cerrar turno** y comprueba el resumen:

    | Concepto | Monto |
    |---|---:|
    | Fondo inicial | Q1000.00 |
    | Cobros efectivos aplicados | Q220.00 |
    | Cobros con tarjeta | Q130.00 |
    | Retiros a bóveda | Q500.00 |
    | Reembolsos en efectivo | Q30.00 |
    | Efectivo esperado | **Q690.00** |

12. Declara contado **680.00** y confirma. El reporte debe mostrar diferencia
    **−Q10.00** y estado **CERRADO**. Guarda una captura para la entrega.
13. Si Luis no hizo ventas, entra en su turno y ciérralo declarando **800.00**.

Cada venta confirmada cambia las existencias. No repitas ventas dentro del mismo
turno esperando el mismo resultado. Esta práctica completa parte de un turno
nuevo y los tres cobros indicados. No restaures el stock ejecutando otra vez el
script de datos: los datos iniciales no reinician inventarios ni contraseñas.

## Otras comprobaciones que puedes hacer

En un turno nuevo de Ana, con fondo Q1000:

- Retiro **501.00**: después de autorizar, debe rechazarlo porque dejaría Q499.
- Retiro **500.00**: permitido, deja exactamente Q500.
- Motivo de reversión vacío: rechazado antes de pedir credenciales.
- Intentar devolver más unidades que las restantes: rechazado; no se duplica stock ni reembolso.
- Tres credenciales incorrectas en la misma solicitud: bloquea confirmar y permite cancelar.
- Autoautorizarse con Ana: rechazado.

El mínimo operativo se lee de `efectivo.minimo` en `config.properties` (Q500 por
defecto). Se aplica al retirar a bóveda. Una reversión puede dejar menos que ese
mínimo, pero no devuelve efectivo que no existe en caja.

Según el ejemplo de la fase 7 del PDF, **las reversiones se reembolsan en efectivo**,
incluso si la venta original fue con tarjeta o mixta. No hay conexión con bancos.
El diálogo muestra ese dato antes de autorizar. El precio devuelto es el histórico
registrado en la venta, aunque después cambie el catálogo. Los productos inactivos
pueden recibir su stock de vuelta sin reactivarse.

## Pruebas automáticas

En POS_TEST con las tablas y datos iniciales ya preparados y actualización 04
aplicada (puedes ejecutar `actualizar-bd.ps1 -Pruebas`), desde la raíz:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\pruebas-negocio.ps1
```

Usa la contraseña técnica de Oracle de **pos_test**. Debe terminar con:

```text
Pruebas negocio: 35; correctas: 35; fallos: 0
```

Los primeros nueve casos corresponden uno a uno a la tabla obligatoria de la
fase 10. El último reproduce la fase 11 completa y escribe un reporte en
`out/escenario-completo.txt`. Usa cajeros, cajas y productos temporales propios;
no borra ni reinicia datos de la práctica manual. La limpieza elimina únicamente
sus filas y las claves de identidad pueden avanzar.

Además se prueban permiso explícito del supervisor, precio histórico, devolución
total, rollback después de modificar cantidades, falta de efectivo, mínimo
configurable, dos retiros simultáneos, dos reversiones sobre la misma unidad y
una reversión simultánea con cierre. Los servicios serializan sobre el turno y
ponen todos los cambios de negocio en la misma transacción.

Cuatro pruebas gráficas (`PruebasInterfazCortes`) recorren retiro, rechazo de
Luis, aprobación de Marta y cierre Q690/Q680/−Q10. Se ejecutaron con ventanas
Swing reales en Linux/Xvfb y POS_TEST. Las [capturas](evidencias/ciclo-completo/README.md)
son evidencia automatizada; debes repetir la práctica en tu Windows.

La auditoría registra cada intento de credenciales y conserva su resultado
incluso si después una regla de caja rechaza la operación. Las credenciales
aceptadas no significan que el retiro/reversión se haya guardado; los cambios
de caja solo existen después del commit. La autorización se liga a turno,
importe o detalle/cantidad/motivo y tiene un único uso exitoso.
