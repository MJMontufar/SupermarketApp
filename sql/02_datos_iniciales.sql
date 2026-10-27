-- Ejecutar como POS o POS_TEST en XEPDB1, despues de 01_esquema.sql.
-- Solo agrega filas faltantes: no reinicia stock, precios ni contrasenas.
SET ECHO OFF
SET VERIFY OFF
WHENEVER OSERROR EXIT FAILURE
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

BEGIN
    IF SYS_CONTEXT('USERENV', 'CON_NAME') <> 'XEPDB1'
       OR SYS_CONTEXT('USERENV', 'SESSION_USER') NOT IN ('POS', 'POS_TEST')
       OR SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') <> SYS_CONTEXT('USERENV', 'SESSION_USER') THEN
        RAISE_APPLICATION_ERROR(-20001, 'Usa tu propio esquema POS o POS_TEST en XEPDB1.');
    END IF;
END;
/

MERGE INTO permiso d USING (
    SELECT 'TURNO_ABRIR' codigo, 'Abrir turno' descripcion FROM dual UNION ALL
    SELECT 'VENTA_COBRAR', 'Cobrar ventas' FROM dual UNION ALL
    SELECT 'CORTE_INTERMEDIO', 'Solicitar retiro a boveda' FROM dual UNION ALL
    SELECT 'REVERSION_SOLICITAR', 'Solicitar reversion' FROM dual UNION ALL
    SELECT 'REVERSION_AUTORIZAR', 'Autorizar reversion' FROM dual UNION ALL
    SELECT 'TURNO_CERRAR', 'Cerrar turno' FROM dual UNION ALL
    SELECT 'USUARIO_ADMIN', 'Administrar usuarios' FROM dual UNION ALL
    SELECT 'ROL_ADMIN', 'Administrar roles y permisos' FROM dual UNION ALL
    SELECT 'CATALOGO_ADMIN', 'Administrar productos y cajas' FROM dual
) s ON (d.codigo = s.codigo)
WHEN NOT MATCHED THEN INSERT (codigo, descripcion) VALUES (s.codigo, s.descripcion);

MERGE INTO rol d USING (
    SELECT 'CAJERO' nombre, 1 nivel FROM dual UNION ALL
    SELECT 'SUPERVISOR', 2 FROM dual UNION ALL
    SELECT 'GERENTE', 3 FROM dual UNION ALL
    SELECT 'ADMIN', 4 FROM dual
) s ON (d.nombre = s.nombre)
WHEN NOT MATCHED THEN INSERT (nombre, nivel) VALUES (s.nombre, s.nivel);

MERGE INTO rol_permiso d USING (
    SELECT r.id rol_id, p.id permiso_id FROM rol r CROSS JOIN permiso p
    WHERE r.nombre = 'ADMIN'
       OR (r.nombre IN ('CAJERO', 'SUPERVISOR', 'GERENTE') AND p.codigo IN (
           'TURNO_ABRIR', 'VENTA_COBRAR', 'CORTE_INTERMEDIO',
           'REVERSION_SOLICITAR', 'TURNO_CERRAR'))
       OR (r.nombre IN ('SUPERVISOR', 'GERENTE') AND p.codigo = 'REVERSION_AUTORIZAR')
       OR (r.nombre = 'GERENTE' AND p.codigo = 'CATALOGO_ADMIN')
) s ON (d.rol_id = s.rol_id AND d.permiso_id = s.permiso_id)
WHEN NOT MATCHED THEN INSERT (rol_id, permiso_id) VALUES (s.rol_id, s.permiso_id);

MERGE INTO caja d USING (
    SELECT 1 numero, 'Caja 1' nombre FROM dual UNION ALL
    SELECT 2, 'Caja 2' FROM dual UNION ALL
    SELECT 3, 'Caja 3' FROM dual
) s ON (d.numero = s.numero)
WHEN NOT MATCHED THEN INSERT (numero, nombre) VALUES (s.numero, s.nombre);

-- Precios con impuesto incluido, como indica el enunciado.
MERGE INTO producto d USING (
    SELECT '750100000001' codigo, 'Arroz' nombre, 30 precio, 50 stock FROM dual UNION ALL
    SELECT '750100000002', 'Leche', 20, 50 FROM dual UNION ALL
    SELECT '750100000003', 'Frijol', 15, 50 FROM dual UNION ALL
    SELECT '750100000004', 'Azucar', 10, 50 FROM dual UNION ALL
    SELECT '750100000005', 'Pan', 5, 50 FROM dual
) s ON (d.codigo_barras = s.codigo)
WHEN NOT MATCHED THEN INSERT (codigo_barras, nombre, precio, stock)
    VALUES (s.codigo, s.nombre, s.precio, s.stock);

-- Usuario publico de entrenamiento del PDF: admin / Admin123!
-- Hash generado en Java con GenerarHash --demo. No es una clave de Oracle.
MERGE INTO usuario d USING (
    SELECT 'admin' username,
        '120000:64W8pvSt+AMzjwopZ1YG8g==:7gTQHKQvNBsq7U2fS2Oc+/DhrECu9U2qs0fjF93OiCU=' hash_demo
        FROM dual
) s ON (d.username = s.username)
WHEN NOT MATCHED THEN INSERT (username, password_hash, nombre_completo)
    VALUES (s.username, s.hash_demo, 'Administrador de entrenamiento');

MERGE INTO usuario_rol d USING (
    SELECT u.id usuario_id, r.id rol_id FROM usuario u CROSS JOIN rol r
        WHERE u.username = 'admin' AND r.nombre = 'ADMIN'
) s ON (d.usuario_id = s.usuario_id AND d.rol_id = s.rol_id)
WHEN NOT MATCHED THEN INSERT (usuario_id, rol_id) VALUES (s.usuario_id, s.rol_id);

COMMIT;
