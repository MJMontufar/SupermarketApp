$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$driver = Join-Path $raiz 'lib\ojdbc11.jar'
if (-not (Test-Path -LiteralPath $driver)) { throw 'Falta lib/ojdbc11.jar.' }
& (Join-Path $PSScriptRoot 'compilar.ps1') -IncluirPruebas
$ejecutor = 'java'
if ($env:JAVA_HOME) { $ejecutor = Join-Path $env:JAVA_HOME 'bin\java.exe' }
$configuracion = Join-Path $raiz 'config.properties'
$clases = Join-Path $raiz 'out'
& $ejecutor "-Dpos.config=$configuracion" '-Dpos.db.usuario=pos_test' -cp "$clases;$driver" com.supermercado.pos.pruebas.PruebasCaja
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
