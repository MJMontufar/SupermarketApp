-- Ejecutar conectado como POS o POS_TEST, dentro de XEPDB1.
-- Oracle confirma cada DDL automaticamente. Este archivo no borra objetos.
SET ECHO OFF
SET VERIFY OFF
WHENEVER OSERROR EXIT FAILURE
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
    existentes NUMBER;
BEGIN
    IF SYS_CONTEXT('USERENV', 'CON_NAME') <> 'XEPDB1'
       OR SYS_CONTEXT('USERENV', 'SESSION_USER') NOT IN ('POS', 'POS_TEST')
       OR SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') <> SYS_CONTEXT('USERENV', 'SESSION_USER') THEN
        RAISE_APPLICATION_ERROR(-20001, 'Usa tu propio esquema POS o POS_TEST en XEPDB1.');
    END IF;
    SELECT COUNT(*) INTO existentes FROM user_tables;
    IF existentes > 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'El esquema contiene tablas. No se modifica.');
    END IF;
END;
/

CREATE TABLE permiso (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo VARCHAR2(50) NOT NULL UNIQUE,
    descripcion VARCHAR2(200)
);

CREATE TABLE rol (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR2(50) NOT NULL UNIQUE,
    nivel NUMBER(2) NOT NULL CHECK (nivel > 0)
);

CREATE TABLE rol_permiso (
    rol_id NUMBER NOT NULL REFERENCES rol(id),
    permiso_id NUMBER NOT NULL REFERENCES permiso(id),
    PRIMARY KEY (rol_id, permiso_id)
);

CREATE TABLE usuario (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR2(50) NOT NULL UNIQUE,
    password_hash VARCHAR2(200) NOT NULL,
    nombre_completo VARCHAR2(120),
    activo NUMBER(1) DEFAULT 1 NOT NULL CHECK (activo IN (0, 1))
);

CREATE TABLE usuario_rol (
    usuario_id NUMBER NOT NULL REFERENCES usuario(id),
    rol_id NUMBER NOT NULL REFERENCES rol(id),
    PRIMARY KEY (usuario_id, rol_id)
);

CREATE TABLE usuario_permiso (
    usuario_id NUMBER NOT NULL REFERENCES usuario(id),
    permiso_id NUMBER NOT NULL REFERENCES permiso(id),
    PRIMARY KEY (usuario_id, permiso_id)
);

CREATE TABLE caja (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero NUMBER(4) NOT NULL UNIQUE,
    nombre VARCHAR2(60),
    activa NUMBER(1) DEFAULT 1 NOT NULL CHECK (activa IN (0, 1))
);

CREATE TABLE turno (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    caja_id NUMBER NOT NULL REFERENCES caja(id),
    cajero_id NUMBER NOT NULL REFERENCES usuario(id),
    apertura TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    cierre TIMESTAMP,
    fondo_inicial NUMBER(12,2) NOT NULL,
    apertura_autorizada_por NUMBER CONSTRAINT fk_turno_apertura_autor REFERENCES usuario(id),
    efectivo_declarado NUMBER(12,2) CHECK (efectivo_declarado >= 0),
    efectivo_esperado NUMBER(12,2),
    diferencia NUMBER(12,2),
    estado VARCHAR2(10) DEFAULT 'ABIERTO' NOT NULL
        CHECK (estado IN ('ABIERTO', 'CERRADO')),
    CONSTRAINT ck_turno_fondo_autorizado CHECK (
        fondo_inicial >= 0 AND (fondo_inicial > 0 OR apertura_autorizada_por IS NOT NULL)),
    CONSTRAINT ck_turno_autorizacion_ajena CHECK (
        apertura_autorizada_por IS NULL OR apertura_autorizada_por <> cajero_id),
    CONSTRAINT ck_turno_cierre CHECK (
        (estado = 'ABIERTO' AND cierre IS NULL)
        OR (estado = 'CERRADO' AND cierre IS NOT NULL))
);

-- RN-01: los turnos cerrados producen NULL y no compiten por el indice.
CREATE UNIQUE INDEX ux_turno_caja_abierto
    ON turno (CASE WHEN estado = 'ABIERTO' THEN caja_id END);
CREATE UNIQUE INDEX ux_turno_cajero_abierto
    ON turno (CASE WHEN estado = 'ABIERTO' THEN cajero_id END);

CREATE TABLE movimiento_caja (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    turno_id NUMBER NOT NULL REFERENCES turno(id),
    tipo VARCHAR2(20) NOT NULL CHECK (
        tipo IN ('FONDO_INICIAL', 'RETIRO_BOVEDA', 'REVERSION_EFECTIVO')),
    monto NUMBER(12,2) NOT NULL CHECK (monto >= 0),
    autorizado_por NUMBER REFERENCES usuario(id),
    referencia VARCHAR2(40),
    fecha TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE TABLE producto (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_barras VARCHAR2(30) NOT NULL UNIQUE,
    nombre VARCHAR2(120) NOT NULL,
    precio NUMBER(12,2) NOT NULL CHECK (precio >= 0),
    porcentaje_impuesto NUMBER(5,2) DEFAULT 12 NOT NULL CHECK (porcentaje_impuesto >= 0),
    stock NUMBER(8) DEFAULT 0 NOT NULL CHECK (stock >= 0),
    activo NUMBER(1) DEFAULT 1 NOT NULL CHECK (activo IN (0, 1))
);

CREATE TABLE venta (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    turno_id NUMBER NOT NULL REFERENCES turno(id),
    fecha TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    total NUMBER(12,2) NOT NULL CHECK (total >= 0),
    estado VARCHAR2(25) DEFAULT 'COMPLETADA' NOT NULL CHECK (
        estado IN ('COMPLETADA', 'PARCIALMENTE_REVERSADA', 'ANULADA'))
);

CREATE TABLE venta_detalle (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venta_id NUMBER NOT NULL REFERENCES venta(id),
    producto_id NUMBER NOT NULL REFERENCES producto(id),
    cantidad NUMBER(6) NOT NULL CHECK (cantidad > 0),
    cantidad_reversada NUMBER(6) DEFAULT 0 NOT NULL,
    precio_unitario NUMBER(12,2) NOT NULL CHECK (precio_unitario >= 0),
    subtotal NUMBER(12,2) NOT NULL CHECK (subtotal >= 0),
    CONSTRAINT ck_detalle_reversado CHECK (
        cantidad_reversada BETWEEN 0 AND cantidad)
);

CREATE TABLE pago (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venta_id NUMBER NOT NULL REFERENCES venta(id),
    metodo VARCHAR2(10) NOT NULL CHECK (metodo IN ('EFECTIVO', 'TARJETA')),
    monto NUMBER(12,2) NOT NULL CHECK (monto >= 0),
    recibido NUMBER(12,2) NOT NULL CHECK (recibido >= 0),
    cambio NUMBER(12,2) DEFAULT 0 NOT NULL CHECK (cambio >= 0),
    CONSTRAINT ck_pago_cambio CHECK (
        (metodo = 'EFECTIVO' AND recibido = monto + cambio)
        OR (metodo = 'TARJETA' AND recibido = monto AND cambio = 0))
);

CREATE TABLE reversion (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    detalle_id NUMBER NOT NULL REFERENCES venta_detalle(id),
    cantidad NUMBER(6) NOT NULL CHECK (cantidad > 0),
    monto NUMBER(12,2) NOT NULL CHECK (monto >= 0),
    motivo VARCHAR2(200) NOT NULL,
    solicitado_por NUMBER NOT NULL REFERENCES usuario(id),
    autorizado_por NUMBER NOT NULL REFERENCES usuario(id),
    fecha TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT ck_reversion_actor CHECK (solicitado_por <> autorizado_por)
);

-- Fase 7.5: registrar intentos de autorizacion, sin almacenar contrasenas.
CREATE TABLE auditoria_autorizacion (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    solicitado_por NUMBER NOT NULL REFERENCES usuario(id),
    username_ingresado VARCHAR2(50),
    autorizado_por NUMBER REFERENCES usuario(id),
    exitoso NUMBER(1) NOT NULL CHECK (exitoso IN (0, 1)),
    motivo VARCHAR2(200) NOT NULL,
    fecha TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);
