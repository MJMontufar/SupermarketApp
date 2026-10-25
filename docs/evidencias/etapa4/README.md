# Evidencias de turnos, ventas y cierre

Capturas reales de `PruebasInterfazCaja` con Java 21, Oracle XE 21.3 y Swing
en Linux/Xvfb. La suite crea usuarios, cajas y productos propios en POS_TEST;
los nombres de prueba y folios visibles no pertenecen a la base de Windows.
Después limpia únicamente sus filas temporales.

| Captura | Comportamiento |
|---|---|
| [Apertura](apertura.png) | Selección de caja libre y fondo inicial Q1,000 |
| [Carrito](carrito.png) | Cinco productos de Q30, total Q150, impuesto incluido |
| [Pago efectivo](pago-efectivo.png) | Recibido Q200 y cambio Q50 |
| [Comprobante](ticket.png) | Venta guardada, detalle, base, impuesto y pagos |
| [Pago mixto](pago-mixto.png) | Total Q120, tarjeta Q50, recibido Q100, cambio Q30 |
| [Cierre](cierre.png) | Tres ventas, esperado Q1,220 y contado Q1,200 |
| [Reporte](reporte-cierre.png) | Turno cerrado y faltante Q20 |

Pasaron los siete casos gráficos: apertura, carrito editable, efectivo con
rechazo previo de pago insuficiente, mixto, tarjeta, reanudación y cierre.
Los cobros suman Q350, efectivo aplicado Q220 y tarjeta Q130.

Estas capturas no sustituyen las 12 pantallas completas exigidas por el PDF ni
la práctica final con retiro a bóveda y reversión autorizada. Sigue pendiente
la comprobación de esta interfaz nueva en Windows y la impresión física.
