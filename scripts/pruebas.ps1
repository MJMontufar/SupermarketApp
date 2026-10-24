$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'compilar.ps1') -IncluirPruebas

$ejecutor = 'java'
if ($env:JAVA_HOME) {
    $ejecutor = Join-Path $env:JAVA_HOME 'bin\java.exe'
}
$salida = Join-Path (Split-Path -Parent $PSScriptRoot) 'out'
& $ejecutor -cp $salida com.supermercado.pos.pruebas.Pruebas
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
