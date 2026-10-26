# Evidencias de apertura cero autorizada

Capturas reales de Java 21, Swing y Oracle XE 21.3 en Linux/Xvfb, ejecutadas
con datos temporales propios de POS_TEST. Los nombres de usuario son de prueba;
no son las cuentas de Windows ni contraseñas de Oracle.

- [Solicitud de autorización](autorizacion-apertura.png): caja, Q0, cajero y credenciales del supervisor en campo oculto.
- [Tres intentos fallidos](autorizacion-bloqueada.png): contraseña vacía y confirmación deshabilitada.
- [Turno abierto sin fondo](turno-sin-fondo.png): mantiene al cajero y muestra Q0.00.

Pasaron los tres casos gráficos: cancelar sin abrir, rechazos con auditoría y
apertura correcta conservando la sesión. La prueba limpia solo sus propias
filas al terminar. La interfaz necesita todavía su comprobación local en Windows.
