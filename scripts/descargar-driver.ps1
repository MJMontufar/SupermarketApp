$ErrorActionPreference = 'Stop'
$carpeta = Join-Path (Split-Path -Parent $PSScriptRoot) 'lib'
$destino = Join-Path $carpeta 'ojdbc11.jar'
$esperado = '1a9055b71d60388e66e66fd761291143146d0b5b63337b63c8b853d6031bad9c'
New-Item -ItemType Directory -Path $carpeta -Force | Out-Null
if (Test-Path -LiteralPath $destino) {
    if ((Get-FileHash -LiteralPath $destino -Algorithm SHA256).Hash.ToLowerInvariant() -eq $esperado) {
        Write-Host 'El driver JDBC ya esta descargado y verificado.'
        return
    }
    throw 'Ya existe otro ojdbc11.jar. Conserva ese archivo y revisa su version antes de reemplazarlo.'
}
$temporal = Join-Path $carpeta ('ojdbc11-' + [guid]::NewGuid().ToString('N') + '.tmp')
try {
    Invoke-WebRequest -UseBasicParsing -Uri 'https://repo.maven.apache.org/maven2/com/oracle/database/jdbc/ojdbc11/21.11.0.0/ojdbc11-21.11.0.0.jar' -OutFile $temporal
    $real = (Get-FileHash -LiteralPath $temporal -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($real -ne $esperado) {
        throw 'El SHA256 del driver no coincide. No se instalara el archivo.'
    }
    Move-Item -LiteralPath $temporal -Destination $destino
    Write-Host 'Driver JDBC descargado y verificado.'
} finally {
    if (Test-Path -LiteralPath $temporal) {
        Remove-Item -LiteralPath $temporal
    }
}
