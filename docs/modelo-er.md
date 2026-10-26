# Modelo entidad-relación del esquema actual

```mermaid
erDiagram
    USUARIO {
        bigint id PK
        string username UK
        string password_hash
        string nombre_completo
        boolean activo
    }
    ROL {
        bigint id PK
        string nombre UK
        int nivel
    }
    PERMISO {
        bigint id PK
        string codigo UK
        string descripcion
    }
    USUARIO_ROL {
        bigint usuario_id PK,FK
        bigint rol_id PK,FK
    }
    USUARIO_PERMISO {
        bigint usuario_id PK,FK
        bigint permiso_id PK,FK
    }
    ROL_PERMISO {
        bigint rol_id PK,FK
        bigint permiso_id PK,FK
    }
    CAJA {
        bigint id PK
        int numero UK
        string nombre
        boolean activa
    }
    TURNO {
        bigint id PK
        bigint caja_id FK
        bigint cajero_id FK
        bigint apertura_autorizada_por FK
        datetime apertura
        datetime cierre
        decimal fondo_inicial
        decimal efectivo_declarado
        decimal efectivo_esperado
        decimal diferencia
        string estado
    }
    PRODUCTO {
        bigint id PK
        string codigo_barras UK
        string nombre
        decimal precio
        decimal porcentaje_impuesto
        int stock
        boolean activo
    }
    VENTA {
        bigint id PK
        bigint turno_id FK
        datetime fecha
        decimal total
        string estado
    }
    VENTA_DETALLE {
        bigint id PK
        bigint venta_id FK
        bigint producto_id FK
        int cantidad
        int cantidad_reversada
        decimal precio_unitario
        decimal subtotal
    }
    PAGO {
        bigint id PK
        bigint venta_id FK
        string metodo
        decimal monto
        decimal recibido
        decimal cambio
    }
    MOVIMIENTO_CAJA {
        bigint id PK
        bigint turno_id FK
        string tipo
        decimal monto
        bigint autorizado_por FK
        string referencia
        datetime fecha
    }
    REVERSION {
        bigint id PK
        bigint detalle_id FK
        int cantidad
        decimal monto
        string motivo
        bigint solicitado_por FK
        bigint autorizado_por FK
        datetime fecha
    }
    AUDITORIA_AUTORIZACION {
        bigint id PK
        bigint solicitado_por FK
        string username_ingresado
        bigint autorizado_por FK
        boolean exitoso
        string motivo
        datetime fecha
    }
    USUARIO ||--o{ USUARIO_ROL : tiene
    ROL ||--o{ USUARIO_ROL : asignado
    ROL ||--o{ ROL_PERMISO : agrupa
    PERMISO ||--o{ ROL_PERMISO : incluido
    USUARIO ||--o{ USUARIO_PERMISO : recibe_extra
    PERMISO ||--o{ USUARIO_PERMISO : asignado
    CAJA ||--o{ TURNO : registra
    USUARIO ||--o{ TURNO : opera
    USUARIO o|--o{ TURNO : autoriza_apertura_cero
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

El diagrama muestra columnas y relaciones; `PK` identifica claves primarias,
`FK` referencias y `UK` valores únicos. Los tipos son lógicos; los scripts SQL
son la definición exacta de tipos Oracle, tamaños, nulabilidad y restricciones.
Los índices únicos de `turno` permiten solo un turno abierto por caja y por
cajero. Los turnos cerrados conservan el historial sin ocupar esos índices.

Los movimientos de fondo inicial se registran para auditoría, pero la fórmula
de efectivo toma el fondo del turno una sola vez. Los cobros suman el monto
aplicado en efectivo, no lo recibido antes de entregar cambio.

La tabla `auditoria_autorizacion` registra intentos exitosos y fallidos de
autorización de apertura con Q0, retiro a bóveda y reversión. La solicitud admite tres fallos y una sola
autorización exitosa. El turno guarda `apertura_autorizada_por`, y el movimiento
inicial registra el mismo autorizador. Una apertura positiva no lo necesita.

Las restricciones prohíben fondo negativo, fondo cero sin identificador de
autorizador y autoautorización. El servicio valida además las credenciales,
actividad, nivel y permiso del supervisor; la sesión del cajero no se sustituye.
Los retiros guardan receptor y referencia en movimiento_caja. Las reversiones
guardan solicitante, autorizador, cantidad, monto y motivo; actualizan detalle,
stock, estado y movimiento efectivo en una misma transacción. El cierre usa
los mismos importes y el mismo bloqueo de turno que las demás operaciones.
