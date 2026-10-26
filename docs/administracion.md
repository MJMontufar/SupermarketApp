# Administración: roles, permisos y catálogos

Esta entrega completa la fase 9 y las pantallas 10, 11 y 12 del PDF. No modifica
el esquema ni reinicia usuarios, stock o contraseñas. Los servicios comprueban
permisos y jerarquía además de los botones de Swing.

## Práctica en Windows

Haz esta práctica con una cuenta separada para conservar a Ana, Luis y Marta
con sus roles originales para el escenario de caja.

1. Ingresa como **admin / Admin123!**. En **Administrar usuarios**, crea
   `prueba_caja`, con rol CAJERO y una contraseña de al menos ocho caracteres.
2. Vuelve al menú y abre **Roles y permisos**. Pulsa **Nuevo rol**, escribe
   nombre `APOYO_CAJA`, nivel **1** y marca `VENTA_COBRAR`. Guarda. Selecciona el
   rol en la lista para comprobar su matriz. Los permisos aparecen por módulo.
3. Abre **Asignar roles y permisos a usuarios** y selecciona `prueba_caja`.
   Conserva CAJERO y marca también APOYO_CAJA. En la tabla inferior comprueba
   que `VENTA_COBRAR` aparece heredado y en gris: no puede quitarse desde extras.
   Marca **Extra** para `CATALOGO_ADMIN` y guarda.
4. Cierra sesión y entra como `prueba_caja`. Ahora debe poder abrir productos
   y cajas, pero no usuarios, roles ni asignaciones. Su nivel sigue siendo 1:
   el permiso extra no lo convierte en supervisor ni administrador.
5. Abre **Catálogo de productos → Nuevo producto**. Crea código `DEMO001`,
   nombre `Producto de practica`, precio con impuesto **7.50**, impuesto **12.00**,
   existencias **5** y estado Activo. Si ese código ya existe, elige otro.
   Búscalo en la tabla, edítalo y prueba desactivarlo: permanece en el catálogo,
   pero no aparece disponible para ventas. No se borra su historial.
   Opcionalmente, selecciona la fila del producto y pulsa **Elegir fotografía...**
   para importar un JPG o PNG. Consulta [fotos y respaldo](diseno-y-fotos.md).
6. Abre **Catálogo de cajas → Nueva caja**. Usa un número libre, por ejemplo
   **4**, nombre `Caja de practica` y estado Activa.
7. Para comprobar la protección de caja abierta, abre un turno con esa caja y
   fondo **100.00**. Vuelve al menú con X sin cerrar el turno, abre el catálogo
   e intenta desactivar esa caja: debe rechazarlo. Regresa al turno, ciérralo
   declarando **100.00** si no hiciste ventas y entonces desactiva la caja.
8. Vuelve a admin y deja a `prueba_caja` solo con CAJERO, sin permisos extra.
   Al siguiente login de esa cuenta, productos y cajas vuelven a deshabilitarse.
   Si ya no la usarás, puedes desactivarla desde Administrar usuarios.

Guarda capturas de la matriz, la asignación, productos y cajas. Las contraseñas
de la práctica son tuyas; no necesitas escribirlas en el chat.

## Reglas aplicadas

- Solo `ROL_ADMIN` abre administración de roles y asignaciones. Solo
  `CATALOGO_ADMIN` modifica productos o cajas. Son permisos independientes.
- Se permiten uno o varios roles y permisos extra puntuales. Los efectivos
  son la unión de permisos de los roles y extras. El nivel es el mayor rol,
  no la cantidad de permisos.
- No puedes modificar tus propios roles ni extras, editar un rol que posees,
  ni asignar o editar niveles iguales o superiores al tuyo. Tampoco puedes
  cambiar la asignación de un usuario de nivel igual o superior.
- No puedes conceder permisos que no tienes, ni hacerlo indirectamente
  asignando un rol que los contenga. El alta inicial de usuarios usa la misma
  protección y bloquea el rol para validar su nivel vigente.
- Toda la asignación de roles y extras se guarda en una sola transacción.
  Identificadores inexistentes o inválidos conservan la asignación anterior.
- Una cuenta debe conservar al menos un rol. Los cambios de roles, matrices
  y extras aplican al **siguiente inicio de sesión**; no modifican objetos de
  una sesión ya iniciada.
- Se crean y editan roles; no se borran roles ni permisos históricos desde la
  interfaz. Los nueve permisos iniciales son el catálogo de capacidades.
- Código y número de caja son únicos. Se validan campos, rangos, importes
  no negativos y existencias. Los precios ya incluyen impuesto; se usa
  `BigDecimal`, dos decimales y `HALF_UP`. Un artículo de precio cero puede
  venderse sin crear un pago ficticio y devolverse sin cambiar el efectivo.
- El catálogo conserva productos inactivos. Desactivar no borra registros.
  Una caja con turno abierto no puede desactivarse ni renumerarse.
- Si un producto cambia mientras editas —por ejemplo, otra caja vende una
  unidad—, guardar el formulario anterior se rechaza. Actualiza la lista y
  vuelve a editar. Así no se restaura por accidente el stock vendido.

## Pruebas

En POS_TEST preparado, desde la raíz del proyecto:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\pruebas-administracion.ps1
```

Ingresa la contraseña técnica de Oracle para **pos_test**. Debe terminar con
`Pruebas administracion: 28; correctas: 28; fallos: 0`.

La suite usa sus propios usuarios, roles, productos y cajas y limpia únicamente
sus filas. No modifica las matrices de los roles iniciales. Comprueba jerarquía,
varios roles, extras, siguiente login, permisos ajenos, transacciones, validaciones,
cambios concurrentes de stock y protección de caja abierta.

Cinco pruebas gráficas adicionales crean un rol, asignan dos roles y un extra,
crean producto y caja y verifican el menú al siguiente login, con Oracle real y
Swing en Linux/Xvfb. Consulta las [evidencias](evidencias/administracion/README.md).
La interfaz nueva necesita repetirse en Windows antes de entregar.
