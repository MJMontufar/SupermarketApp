# Driver JDBC de Oracle

Aquí se coloca `ojdbc11.jar`. Es la única biblioteca externa de la aplicación.
No se versiona el binario. No necesitas instalar Maven.

La publicación del fabricante Oracle en Maven Central permite descargar el
archivo directamente:

https://repo.maven.apache.org/maven2/com/oracle/database/jdbc/ojdbc11/21.11.0.0/ojdbc11-21.11.0.0.jar

Guárdalo como `lib/ojdbc11.jar`. El script `scripts/descargar-driver.ps1` descarga
esta misma versión y comprueba su SHA256 antes de instalarla.
