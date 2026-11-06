# Script de compilación de JFLAP Studio (.jar + .exe nativo de Windows)
$ErrorActionPreference = "Stop"

$jdkBin = "C:\Program Files\Java\jdk-22\bin"
if (-not (Test-Path $jdkBin)) {
    $jdkDir = Get-ChildItem "C:\Program Files\Java" -Directory -Filter "jdk*" | Sort-Object Name -Descending | Select-Object -First 1
    if ($jdkDir) { $jdkBin = Join-Path $jdkDir.FullName "bin" }
}

$workClasses = Join-Path $PSScriptRoot "classes"
New-Item -ItemType Directory -Force -Path $workClasses | Out-Null

if (Test-Path (Join-Path $PSScriptRoot "JFLAP.jar")) {
    Push-Location $workClasses
    & "$jdkBin\jar.exe" xf (Join-Path $PSScriptRoot "JFLAP.jar")
    Pop-Location
}

$javaFiles = Get-ChildItem -Path (Join-Path $PSScriptRoot "src") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
& "$jdkBin\javac.exe" -encoding UTF-8 --release 8 -cp $workClasses -d $workClasses $javaFiles

& "$jdkBin\jar.exe" cfe (Join-Path $PSScriptRoot "JFLAP.jar") JFLAP -C $workClasses .

$csc = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
if (Test-Path $csc) {
    & $csc /nologo /target:winexe `
        /out:(Join-Path $PSScriptRoot "JFLAP Studio.exe") `
        /win32icon:(Join-Path $PSScriptRoot "assets\JFLAP_Studio.ico") `
        /resource:"$(Join-Path $PSScriptRoot 'JFLAP.jar'),JFLAP.jar" `
        (Join-Path $PSScriptRoot "launcher\JFLAPStudioLauncher.cs")
}

Remove-Item -Recurse -Force $workClasses
Write-Host "Compilación completada: JFLAP.jar y JFLAP Studio.exe actualizados."
