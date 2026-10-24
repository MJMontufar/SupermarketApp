# Modelo entidad-relación del esquema actual

```mermaid
erDiagram
    USUARIO ||--o{ USUARIO_ROL : tiene
    ROL ||--o{ USUARIO_ROL : asignado
    ROL ||--o{ ROL_PERMISO : agrupa
    PERMISO ||--o{ ROL_PERMISO : incluido
    USUARIO ||--o{ USUARIO_PERMISO : recibe_extra
    PERMISO ||--o{ USUARIO_PERMISO : asignado
    CAJA ||--o{ TURNO : registra
    USUARIO ||--o{ TURNO : opera
    TURNO ||--o{ MOVIMIENTO_CAJA : registra
    USUARIO o|--o{ MOVIMIENTO_CAJA : autoriza
    TURNO ||--o{ VENTA : contiene
    VENTA ||--o{ VENTA_DETALLE : contiene
    PRODUCTO ||--o{ VENTA_DETALLE : vendido
    VENTA ||--o{ PAGO : recibe
    VENTA_DETALLE ||--o{ REVERSION : admite
    USUARIO ||--o{ REVERSION : solicita
    USUARIO ||--o{ REVERSION : autoriza
    USUARIO ||--o{ AUDITORIA_AUTORIZACION : solicita
    USUARIO o|--o{ AUDITORIA_AUTORIZACION : validado
```

Los scripts SQL son la definición detallada de columnas, claves y restricciones.
Los índices únicos de `turno` permiten solo un turno abierto por caja y por
cajero. Los turnos cerrados conservan el historial sin ocupar esos índices.

Los movimientos de fondo inicial se registrarán para auditoría, pero la fórmula
de efectivo tomará el fondo del turno una sola vez. Los cobros sumarán el monto
aplicado en efectivo, no lo recibido antes de entregar cambio.

La tabla `auditoria_autorizacion` almacenará intentos exitosos y fallidos de
autorización. Su escritura y el límite de intentos se implementarán con el
servicio de autorización; crear la tabla no implementa esas reglas.
