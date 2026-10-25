-- Una vez en POS y una vez en POS_TEST. Se puede repetir sin borrar datos.
-- DDL confirma automaticamente: ejecutar sin transacciones de trabajo pendientes.
SET ECHO OFF
SET VERIFY OFF
WHENEVER OSERROR EXIT FAILURE
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

-- INICIO MIGRACION
DECLARE
    cantidad NUMBER;
    tipo VARCHAR2(128);
    PROCEDURE agregar_check(nombre VARCHAR2, condicion VARCHAR2) IS
        existentes NUMBER;
        valida NUMBER;
    BEGIN
        SELECT COUNT(*) INTO existentes FROM user_constraints WHERE constraint_name = nombre;
        IF existentes = 0 THEN
            EXECUTE IMMEDIATE 'ALTER TABLE turno ADD CONSTRAINT ' || nombre || ' CHECK (' || condicion || ') ENABLE VALIDATE';
        ELSE
            SELECT COUNT(*) INTO valida FROM user_constraints
            WHERE constraint_name = nombre AND table_name = 'TURNO' AND constraint_type = 'C'
              AND status = 'ENABLED' AND validated = 'VALIDATED'
              AND REGEXP_REPLACE(REPLACE(UPPER(search_condition_vc), '"', ''), '[[:space:]()]', '')
                = REGEXP_REPLACE(UPPER(condicion), '[[:space:]()]', '');
            IF valida <> 1 THEN
                RAISE_APPLICATION_ERROR(-20004, 'Hay una restriccion distinta con nombre ' || nombre || '; se conserva para revision.');
            END IF;
        END IF;
    END;
BEGIN
    IF SYS_CONTEXT('USERENV', 'CON_NAME') <> 'XEPDB1'
       OR SYS_CONTEXT('USERENV', 'SESSION_USER') NOT IN ('POS', 'POS_TEST')
       OR SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') <> SYS_CONTEXT('USERENV', 'SESSION_USER') THEN
        RAISE_APPLICATION_ERROR(-20001, 'Usa tu propio esquema POS o POS_TEST en XEPDB1.');
    END IF;
    SELECT COUNT(*) INTO cantidad FROM user_tables WHERE table_name IN ('TURNO','USUARIO','AUDITORIA_AUTORIZACION');
    IF cantidad <> 3 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Faltan tablas iniciales; primero prepara el esquema del proyecto.');
    END IF;
    SELECT COUNT(*) INTO cantidad FROM user_tab_columns
        WHERE table_name = 'TURNO' AND column_name = 'APERTURA_AUTORIZADA_POR';
    IF cantidad = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE turno ADD (apertura_autorizada_por NUMBER CONSTRAINT fk_turno_apertura_autor REFERENCES usuario(id))';
    ELSE
        SELECT data_type INTO tipo FROM user_tab_columns
            WHERE table_name = 'TURNO' AND column_name = 'APERTURA_AUTORIZADA_POR';
        IF tipo <> 'NUMBER' THEN RAISE_APPLICATION_ERROR(-20003, 'La columna de autorizacion tiene un tipo distinto; no se modifica.'); END IF;
    END IF;
    SELECT COUNT(*) INTO cantidad FROM user_constraints fk
        JOIN user_cons_columns fc ON fc.constraint_name = fk.constraint_name AND fc.table_name = fk.table_name
        JOIN user_constraints pk ON pk.constraint_name = fk.r_constraint_name
        JOIN user_cons_columns pc ON pc.constraint_name = pk.constraint_name AND pc.position = fc.position
        WHERE fk.table_name = 'TURNO' AND fc.column_name = 'APERTURA_AUTORIZADA_POR'
          AND fk.constraint_type = 'R' AND fk.status = 'ENABLED' AND fk.validated = 'VALIDATED'
          AND pk.table_name = 'USUARIO' AND pc.column_name = 'ID';
    IF cantidad <> 1 THEN RAISE_APPLICATION_ERROR(-20005, 'Falta la referencia de autorizacion a usuario; no se debilitan restricciones.'); END IF;

    -- Primero las nuevas protecciones; despues se retira solo el CHECK anterior exacto.
    agregar_check('CK_TURNO_FONDO_AUTORIZADO',
        'fondo_inicial >= 0 AND (fondo_inicial > 0 OR apertura_autorizada_por IS NOT NULL)');
    agregar_check('CK_TURNO_AUTORIZACION_AJENA',
        'apertura_autorizada_por IS NULL OR apertura_autorizada_por <> cajero_id');
    FOR restriccion IN (
        SELECT constraint_name FROM user_constraints
        WHERE table_name = 'TURNO' AND constraint_type = 'C'
          AND REGEXP_REPLACE(REPLACE(UPPER(search_condition_vc), '"', ''), '[[:space:]()]', '') = 'FONDO_INICIAL>0'
    ) LOOP
        EXECUTE IMMEDIATE 'ALTER TABLE turno DROP CONSTRAINT ' || DBMS_ASSERT.ENQUOTE_NAME(restriccion.constraint_name, FALSE);
    END LOOP;
END;
-- FIN MIGRACION
/
PROMPT Actualizacion lista: fondo cero requiere autorizador distinto. Datos conservados.
