# Mapa de evidencias de las 12 pantallas

Estas son capturas de **ventanas Swing reales** con Java 21, Oracle XE 21.3,
JDBC y Linux/Xvfb. Las suites crean y eliminan sus propias filas en POS_TEST;
los usuarios con prefijo de prueba y folios no son las cuentas del estudiante
ni su instalación Windows. No se generaron maquetas ni se simularon respuestas SQL.

| Apartado del PDF | Ventana | Captura |
|---|---|---|
| 1 | LoginFrame | [Login](final/login.png) |
| 2 | AperturaTurnoFrame | [Apertura](final/apertura.png) |
| 3 | PosFrame | [Carrito](final/carrito.png) |
| 4 | PagoDialog | [Pago mixto](final/pago-mixto.png) |
| 5 | CorteIntermedioDialog | [Corte intermedio](final/corte-intermedio.png) |
| 6 | ReversionFrame | [Solicitud](final/reversion-solicitud.png) |
| 7 | AutorizacionDialog | [Autorización de reversión](final/reversion-autorizacion.png) |
| 8 | CierreTurnoFrame | [Cierre](final/cierre-escenario.png) |
| 9 | UsuariosFrame y alta | [Usuarios](final/usuarios.png), [alta](final/alta-usuario.png) |
| 10 | RolesFrame | [Matriz por módulo](final/roles-matriz.png) |
| 11 | AsignacionPermisosDialog | [Roles, heredados y extras](final/asignacion-permisos.png) |
| 12 | ProductosFrame y CajasFrame | [Productos](final/productos.png), [cajas](final/cajas.png) |

Resultados adicionales:

- [Reporte del escenario completo](final/reporte-escenario.png): Q690 esperados,
  Q680 contados, diferencia −Q10 y turno cerrado.
- [Resultado automatizado de la fase 11](final/resultado-automatico.txt).
- [Luis rechazado al autorizar](final/reversion-cajero-rechazado.png).
- [Recibo de retiro](final/recibo-retiro.png) y [reversión](final/comprobante-reversion.png).
- [Fondo cero autorizado](apertura-cero/README.md).

Las suites gráficas suman 26 casos: usuarios 4, caja 7, apertura cero 3, cortes 4,
administración 5 y fotos 3. La regresión completa suma 219 casos, incluidos los nueve
obligatorios de fase 10 y el escenario de fase 11. La impresión física no se
probó; se verificaron los comprobantes en pantalla.

Para la entrega, sigue [la lista de trabajo](../entrega.md) y conserva también
las capturas de tu práctica en Windows con Ana, Luis y Marta. Las carpetas por
etapa conservan evidencias históricas; `final` reúne las de la versión actual.

El [diseño y las fotos opcionales](../diseno-y-fotos.md) tienen capturas adicionales
en `diseno`. El archivo de color es una imagen sintética de prueba.
