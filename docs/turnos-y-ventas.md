# Turnos, ventas y cierre

Esta entrega implementa las fases 4 y 5 del PDF y el cierre básico de la fase 8.
Reutiliza las 15 tablas existentes: **no ejecutes otra vez los SQL 00 y 01**.
Conserva a Ana, Luis, Marta y cualquier usuario creado en tu Oracle local.
Los retiros a bóveda y las reversiones autorizadas corresponden a la siguiente
entrega; todavía no tienen servicio ni pantalla.

## Actualizar con Git en Windows

Cierra la aplicación Java antes de actualizar para que no siga ejecutando clases
anteriores. No es necesario detener Oracle. En la página del repositorio,
incorpora primero la rama de esta entrega a `main` mediante su pull request.

En la terminal PowerShell de VS Code:

```powershell
Set-Location "$env:USERPROFILE\Proyectos\SupermarketApp-Git"
git status
```

Si indica `working tree clean`, continúa:

```powershell
git pull --ff-only origin main
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\iniciar.ps1
```

Si Git muestra archivos modificados, consérvalos y revisa antes de actualizar.
No uses `reset --hard`, `clean` ni fuerces el pull. El driver `lib/ojdbc11.jar`
y tu configuración local no están versionados y se conservan en esta carpeta.
Si esta es una clonación nueva y falta el driver, ejecuta primero:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\descargar-driver.ps1
```

Ingresa en la consola la clave técnica de Oracle de **pos**. En la ventana de
login ingresa como **ana**, con la contraseña que tú elegiste para esa cuenta.

## Práctica completa de esta entrega

Estas cantidades suponen los precios iniciales: Arroz Q30, Leche Q20,
Frijol Q15, Azúcar Q10 y Pan Q5, con impuesto incluido. Los cobros son registros
locales de práctica: no hay conexión con un banco ni procesamiento real de tarjeta.

1. En el menú de Ana pulsa **Abrir / continuar turno de caja**. Selecciona
   **Caja 1** y escribe fondo inicial `1000.00`. Pulsa **Abrir turno**.
   Si Ana ya tiene turno abierto, el menú lo recupera directamente. No vuelvas
   a abrir otro para la misma práctica.
2. Para la venta de Q150, selecciona **Arroz** en el catálogo, escribe cantidad
   **5** y pulsa **Agregar seleccionado**. También puedes escribir el código
   `750100000001` en **Código + Enter** y pulsar Enter con esa cantidad.
3. Comprueba el total Q150. Puedes editar la columna **Cantidad**, quitar una
   línea o cancelar el carrito antes de cobrar. El impuesto solo se desglosa;
   no se añade nuevamente al total.
4. Pulsa **Cobrar (F12)**, deja **EFECTIVO** y escribe recibido `200.00`.
   Debe mostrar cambio **Q50.00**. Si escribes `149.00`, no debe permitir
   confirmar. Vuelve a `200.00`, confirma y revisa el comprobante.
5. Cierra el comprobante. En la segunda venta agrega **4 Leches** (código
   `750100000002`), total Q80. Al cobrar elige **TARJETA**: el importe se
   completa automáticamente y el cambio es cero. Confirma y cierra el ticket.
6. En la tercera venta agrega **4 Arroces**, total Q120. Elige **MIXTO**,
   tarjeta `50.00` y efectivo recibido `100.00`. La venta aplica Q70 de
   efectivo y devuelve **Q30 de cambio**. Confirma y cierra el comprobante.
7. Puedes cerrar la ventana del punto de venta con la X para regresar al menú
   y cerrar sesión. **Esto no cierra el turno.** Al volver como Ana, el botón
   del menú debe recuperar la misma caja, el mismo folio de turno y su fondo.
8. Con el carrito vacío, pulsa **Cerrar turno**. El resumen debe mostrar:

   | Concepto | Resultado |
   |---|---:|
   | Ventas | 3 |
   | Total vendido | Q350.00 |
   | Efectivo neto cobrado | Q220.00 |
   | Tarjeta cobrada | Q130.00 |
   | Fondo inicial | Q1,000.00 |
   | Efectivo esperado | Q1,220.00 |

9. Para practicar un faltante, escribe contado `1200.00` y confirma el cierre.
   El reporte debe indicar **diferencia −Q20.00** y estado **CERRADO**.
   Después puede abrirse otro turno; el cerrado ya no acepta cobros.

Estos resultados requieren comenzar un turno nuevo y hacer exactamente esas
ventas. Ventas de otras prácticas dentro del mismo turno cambian el resultado.
Las ventas confirmadas descuentan existencias; cancelar un carrito no las
restaura. La reposición por reversión se implementará en la siguiente entrega.
El caso completo del PDF añade retiro de Q500 y reversión de Q30 para llegar a
Q690 esperados. **Esta entrega no acredita todavía ese escenario completo.**

## Comprobar caja ocupada

Mientras Ana mantiene abierto su turno en Caja 1, regresa al menú y cierra
sesión. Ingresa como Luis y pulsa **Abrir / continuar turno de caja**.
Caja 1 no debe aparecer disponible. Luis puede usar Caja 2 con fondo `800.00`.
La base también rechaza una apertura simultánea sobre la misma caja, aunque
otro proceso hubiera mostrado una lista anterior.

Para terminar esa prueba, cierra el turno de Luis declarando `800.00` si no
registró ventas. Regresa como Ana para continuar o cerrar su turno.

## Qué ocurre en el código

- `AperturaTurnoFrame` recoge caja y fondo; `TurnoService.abrir` comprueba el
  permiso, toma un bloqueo sobre la caja y guarda turno y movimiento inicial
  dentro de una transacción. Los índices únicos de Oracle protegen caja y cajero.
- `PosFrame` mantiene un `Carrito` en memoria. Editar o cancelar no escribe en
  Oracle. El código de barras funciona como entrada de teclado terminada en Enter.
- `PagoDialog` usa `PlanPago` para mostrar el cambio; `VentaService` vuelve a
  calcularlo al cobrar. La tarjeta nunca supera el total; el efectivo aplicado
  es total menos tarjeta y el cambio es recibido menos efectivo aplicado.
- El servicio bloquea el turno y los productos en orden por ID. Guarda venta,
  detalles, pagos y descuento condicional de stock en una sola transacción.
  Cualquier fallo revierte todo. Una segunda caja no puede vender la última
  unidad que ya confirmó otra caja.
- Si el precio cambia antes del cobro o se desactiva el producto, se rechaza
  la operación. Cancela el carrito, actualiza el catálogo y agrega nuevamente
  las líneas para usar los datos vigentes. Los precios de ventas ya guardadas
  permanecen en `venta_detalle`.
- `TurnoService.cerrar` usa el mismo bloqueo que cobrar, calcula el efectivo
  y guarda contado, esperado, diferencia y cierre atómicamente. La diferencia
  es contado menos esperado: negativa indica faltante.
- El cálculo toma **fondo + pagos efectivos aplicados − retiros − reembolsos
  efectivos**. El movimiento `FONDO_INICIAL` es evidencia y no suma otro fondo.
  El resumen ya consulta movimientos de retiros/reembolsos, pero sus servicios
  y autorización aún están pendientes.
- Los importes usan `BigDecimal` con dos decimales y `HALF_UP`. El impuesto
  incluido se calcula por línea y el ticket lo muestra junto con su base.
- JDBC corre en `SwingWorker`; los botones se bloquean mientras se guarda y
  no se puede cerrar un formulario que está confirmando una transacción.
- `TicketDialog` muestra un comprobante y ofrece **Imprimir** mediante el JDK.
  La vista previa fue comprobada; una impresora física depende de Windows y
  no se probó en el entorno cloud. Un fallo de impresión no deshace una venta.

## Pruebas del servicio

Son independientes de tu práctica y se ejecutan **solo como POS_TEST en XEPDB1**.
Si ese esquema ya tiene las tablas y datos iniciales, ejecuta desde la raíz:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\pruebas-caja.ps1
```

Cuando lo solicite, introduce la clave de Oracle de **pos_test**, no la de pos
ni las de Ana o admin. Debe terminar con:

```text
Pruebas caja: 29; correctas: 29; fallos: 0
```

La suite crea sus propios dos cajeros, dos cajas y dos productos, y limpia
solamente sus registros. No reinicia existencias del catálogo de entrenamiento.
Se comprueban apertura, ocupación, fondo cero, permisos, turno ajeno, los tres
pagos, precio histórico, producto inactivo, rollback en la segunda línea,
reanudación, cierre, faltante y concurrencia real de dos conexiones.

Para Linux, con el JDK, las clases compiladas y una clave de desarrollo local ya
configurada en `POS_DB_PASSWORD`:

```sh
java -Dpos.db.usuario=pos_test -cp out:lib/ojdbc11.jar com.supermercado.pos.pruebas.PruebasCaja
```

`PruebasInterfazCaja` recorre siete casos con ventanas y eventos reales. Necesita
un escritorio o Xvfb, el mismo esquema aislado y la clave local ya configurada:

```sh
java -Dpos.db.usuario=pos_test -Dpos.capturas=out/capturas-etapa4 -cp out:lib/ojdbc11.jar com.supermercado.pos.pruebas.PruebasInterfazCaja
```

Las evidencias de esa ejecución están en [capturas de esta etapa](evidencias/etapa4/README.md).
Las suites anteriores de 37 pruebas de dominio, 9 de integridad Oracle, 19 de
login/usuarios y 4 gráficas de usuarios siguen separadas. Los 29 casos nuevos
cubren cinco de los nueve comportamientos obligatorios de la fase 10; faltan
los relacionados con retiro mínimo, autorización y exceso de reversión.
