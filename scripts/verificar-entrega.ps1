$ErrorActionPreference = 'Stop'
Write-Host 'Verificacion local: dominio, nueve reglas obligatorias, escenario completo y administracion.'
Write-Host 'Las dos suites de base de datos requieren POS_TEST preparado y piden su clave tecnica.'
foreach ($archivo in @('pruebas.ps1', 'pruebas-negocio.ps1', 'pruebas-administracion.ps1')) {
    & (Join-Path $PSScriptRoot $archivo)
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
Write-Host 'Verificacion local terminada correctamente. Continua con la practica de ventanas en docs/entrega.md.'
