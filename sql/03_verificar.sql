WHENEVER OSERROR EXIT FAILURE
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
SELECT SYS_CONTEXT('USERENV', 'SESSION_USER') usuario,
    SYS_CONTEXT('USERENV', 'CON_NAME') base_actual FROM dual;
SELECT COUNT(*) tablas FROM user_tables;
SELECT codigo FROM permiso ORDER BY codigo;
SELECT nombre, nivel FROM rol ORDER BY nivel;
SELECT numero, nombre, activa FROM caja ORDER BY numero;
SELECT codigo_barras, nombre, precio, stock FROM producto ORDER BY codigo_barras;
SELECT username, activo FROM usuario ORDER BY username;
SELECT index_name FROM user_indexes WHERE index_name IN (
    'UX_TURNO_CAJA_ABIERTO', 'UX_TURNO_CAJERO_ABIERTO') ORDER BY index_name;
