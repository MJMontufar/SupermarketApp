param([string]$Destino)

$ErrorActionPreference = 'Stop'
$raiz = [System.IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
if ([string]::IsNullOrWhiteSpace($Destino)) {
    $Destino = Join-Path ([Environment]::GetFolderPath('UserProfile')) 'RespaldosPOS'
}
$destinoCompleto = [System.IO.Path]::GetFullPath($Destino)
$separador = [System.IO.Path]::DirectorySeparatorChar
if ($destinoCompleto.Equals($raiz, [StringComparison]::OrdinalIgnoreCase) -or
    $destinoCompleto.StartsWith($raiz + $separador, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Elige un destino fuera del proyecto para no copiar el respaldo dentro de si mismo.'
}

$marca = Get-Date -Format 'yyyyMMdd-HHmmss-ffffff'
$respaldo = Join-Path $destinoCompleto "SupermarketApp-$marca"
$copia = Join-Path $respaldo 'proyecto'
New-Item -ItemType Directory -Path $copia | Out-Null
New-Item -ItemType Directory -Path (Join-Path $respaldo 'base-datos') | Out-Null

# Copia los archivos actuales, incluidos cambios locales y fotos ignoradas por Git.
$elementos = @('src', 'test', 'sql', 'scripts', 'docs', 'lib', '.vscode',
    'README.md', '.gitignore', 'config.properties', 'config.local.properties')
foreach ($elemento in $elementos) {
    $origen = Join-Path $raiz $elemento
    if (Test-Path -LiteralPath $origen) {
        Copy-Item -LiteralPath $origen -Destination $copia -Recurse -Force
    }
}
$fotos = Join-Path $raiz 'datos/fotos'
if (Test-Path -LiteralPath $fotos) {
    $datos = Join-Path $copia 'datos'
    New-Item -ItemType Directory -Path $datos | Out-Null
    Copy-Item -LiteralPath $fotos -Destination $datos -Recurse -Force
}

if ((Test-Path -LiteralPath (Join-Path $raiz '.git')) -and
    (Get-Command git -ErrorAction SilentlyContinue)) {
    & git -C $raiz bundle create (Join-Path $copia 'historial.bundle') --all
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo guardar el historial Git. Revisa la carpeta parcial antes de continuar.' }
}

@'
RESPALDO LOCAL DE SUPERMARKETAPP

proyecto/: copia de codigo, SQL, scripts, documentacion, driver, configuracion
local y fotografias que existan en este equipo. No incluye Java ni Oracle.
historial.bundle, si existe: historial Git; los cambios sin commit estan en
la copia de archivos, aunque no formen parte de ese historial.

proyecto.zip: la misma copia comprimida. SHA256-proyecto.txt: su checksum.

BASE ORACLE PENDIENTE: este script NO exporta Oracle. Copia el archivo .dmp
y el log de una exportacion correcta de POS en base-datos/. Los scripts SQL
permiten una instalacion nueva, pero no recuperan ventas, turnos, usuarios
ni stock de la practica. Sigue docs/capturas-y-respaldo.md.

Conserva esta carpeta de forma privada: incluye configuracion local y el
respaldo de Oracle puede incluir datos de usuarios. Copia la carpeta completa
a una segunda ubicacion. No elimines la instalacion que ya funciona.
'@ | Set-Content -LiteralPath (Join-Path $respaldo 'LEEME-RESPALDO.txt') -Encoding UTF8

# ZipFile conserva tambien .vscode y .gitignore; Compress-Archive puede omitir ocultos.
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = Join-Path $respaldo 'proyecto.zip'
[System.IO.Compression.ZipFile]::CreateFromDirectory($copia, $zip)
$archivoZip = [System.IO.Compression.ZipFile]::OpenRead($zip)
try {
    if (-not ($archivoZip.Entries | Where-Object { $_.FullName.Replace('\', '/') -eq 'src/com/supermercado/pos/Main.java' })) {
        throw 'El ZIP no contiene Main.java; conserva la carpeta parcial para revision.'
    }
} finally { $archivoZip.Dispose() }
$hash = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash
"$hash  proyecto.zip" | Set-Content -LiteralPath (Join-Path $respaldo 'SHA256-proyecto.txt') -Encoding ASCII

Write-Host "Copia del proyecto y ZIP verificados: $respaldo"
Write-Host 'Base Oracle pendiente: exporta POS y copia el .dmp y su log a base-datos.'
Write-Host 'Conserva la carpeta completa tambien en otra ubicacion.'
