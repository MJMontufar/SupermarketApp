param([switch]$Pruebas)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$driver = Join-Path $raiz 'lib\ojdbc11.jar'
if (-not (Test-Path -LiteralPath $driver)) {
    throw 'Falta lib/ojdbc11.jar. Ejecuta primero scripts/descargar-driver.ps1.'
}
& (Join-Path $PSScriptRoot 'compilar.ps1')
$ejecutor = 'java'
if ($env:JAVA_HOME) { $ejecutor = Join-Path $env:JAVA_HOME 'bin\java.exe' }
$configuracion = Join-Path $raiz 'config.properties'
$clases = Join-Path $raiz 'out'
$opciones = @()
if ($Pruebas) { $opciones += '-Dpos.db.usuario=pos_test' }
& $ejecutor "-Dpos.config=$configuracion" @opciones -cp "$clases;$driver" com.supermercado.pos.util.ActualizarEsquema
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
