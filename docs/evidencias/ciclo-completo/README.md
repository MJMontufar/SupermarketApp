# Evidencias del ciclo completo

Ventanas reales de Java 21/Swing sobre Oracle XE 21.3 en POS_TEST con Linux/Xvfb.
Los nombres, productos y folios son de filas temporales propias de la suite;
no pertenecen a la práctica del estudiante en Windows.

- [Corte intermedio](corte-intermedio.png): Q1220 esperados y retiro de Q500.
- [Recibo de retiro](recibo-retiro.png): receptor identificado y Q720 restantes.
- [Solicitud de reversión](reversion-solicitud.png): una unidad histórica de Q30 y motivo.
- [Cajero rechazado](reversion-cajero-rechazado.png): Luis no autoriza y queda otro intento.
- [Autorización](reversion-autorizacion.png): credenciales ocultas de supervisor, operación concreta.
- [Comprobante de reversión](comprobante-reversion.png): Q30 devueltos, venta parcial, Q690 restantes.
- [Conteo final](cierre-escenario.png): resumen y Q680 contados.
- [Reporte final](reporte-escenario.png): Q690 esperados, diferencia −Q10 y turno cerrado.

Pasaron las cuatro pruebas gráficas de esta suite. Los 35 casos de servicios
incluyen las nueve pruebas obligatorias y la fase 11 completa. La impresión
física no se probó: los comprobantes usan la vista previa de JTextArea.
