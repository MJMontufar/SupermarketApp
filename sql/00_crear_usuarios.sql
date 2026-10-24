-- Ejecutar como SYSTEM conectado a XEPDB1, una sola vez.
-- PASSWORD solicita las claves sin guardarlas en este archivo.
SET ECHO OFF
SET VERIFY OFF
WHENEVER OSERROR EXIT FAILURE
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
    existentes NUMBER;
BEGIN
    IF SYS_CONTEXT('USERENV', 'CON_NAME') <> 'XEPDB1'
       OR SYS_CONTEXT('USERENV', 'SESSION_USER') NOT IN ('SYSTEM', 'SYS') THEN
        RAISE_APPLICATION_ERROR(-20001, 'Conectate como SYSTEM a XEPDB1.');
    END IF;
    SELECT COUNT(*) INTO existentes FROM dba_users
        WHERE username IN ('POS', 'POS_TEST');
    IF existentes > 0 THEN
        RAISE_APPLICATION_ERROR(-20002,
            'POS o POS_TEST ya existe. Se conserva: no vuelvas a crear los usuarios.');
    END IF;
END;
/

CREATE USER pos NO AUTHENTICATION
    DEFAULT TABLESPACE users QUOTA UNLIMITED ON users;
CREATE USER pos_test NO AUTHENTICATION
    DEFAULT TABLESPACE users QUOTA UNLIMITED ON users;

PROMPT Define la clave de la cuenta de base de datos POS.
PASSWORD pos
PROMPT Define otra clave para la cuenta de pruebas POS_TEST.
PASSWORD pos_test

GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO pos;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO pos_test;
PROMPT Usuarios preparados. Conectate ahora como POS a XEPDB1.
