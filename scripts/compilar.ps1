param([switch]$IncluirPruebas)

$ErrorActionPreference = 'Stop'
$raizProyecto = Split-Path -Parent $PSScriptRoot
$compilador = 'javac'
if ($env:JAVA_HOME) {
    $compilador = Join-Path $env:JAVA_HOME 'bin\javac.exe'
}

$version = & $compilador -version 2>&1
if ($LASTEXITCODE -ne 0 -or "$version" -notmatch '^javac 21[\s\.]') {
    throw 'Se necesita JDK 21. Comprueba javac -version y JAVA_HOME.'
}

$fuentes = @(Get-ChildItem -LiteralPath (Join-Path $raizProyecto 'src') -Recurse -Filter '*.java' |
    ForEach-Object { $_.FullName })
if ($IncluirPruebas) {
    $fuentes += @(Get-ChildItem -LiteralPath (Join-Path $raizProyecto 'test') -Recurse -Filter '*.java' |
        ForEach-Object { $_.FullName })
}
$salida = Join-Path $raizProyecto 'out'
New-Item -ItemType Directory -Path $salida -Force | Out-Null
& $compilador -encoding UTF-8 --release 21 -Xlint:all -d $salida @fuentes
if ($LASTEXITCODE -ne 0) {
    throw 'La compilacion fallo. Revisa los errores anteriores.'
}
Write-Host 'Compilacion correcta.'
